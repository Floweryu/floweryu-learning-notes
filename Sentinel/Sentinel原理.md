从`SphU.entry()`方法向下执行，会进入到下面这个方法中

```java
private Entry entryWithPriority(ResourceWrapper resourceWrapper, int count, boolean prioritized, Object... args)
        throws BlockException {
        Context context = ContextUtil.getContext();
        if (context instanceof NullContext) {
            // The {@link NullContext} indicates that the amount of context has exceeded the threshold,
            // so here init the entry only. No rule checking will be done.
            return new CtEntry(resourceWrapper, null, context);
        }

        if (context == null) {
            // Using default context.
            context = InternalContextUtil.internalEnter(Constants.CONTEXT_DEFAULT_NAME);
        }

        // Global switch is close, no rule checking will do.
    	// 全局开关关闭，说明没有规则检查，直接返回一个CtEntry对象，不再进行后续的限流检测
        if (!Constants.ON) {
            return new CtEntry(resourceWrapper, null, context);
        }

    	// 根据包装过的资源获取对应的SlotChain
        ProcessorSlot<Object> chain = lookProcessChain(resourceWrapper);

        /*
         * Means amount of resources (slot chain) exceeds {@link Constants.MAX_SLOT_CHAIN_SIZE},
         * so no rule checking will be done.
         */
        if (chain == null) {
            return new CtEntry(resourceWrapper, null, context);
        }

        Entry e = new CtEntry(resourceWrapper, chain, context);
        try {
            // 执行下面该方法
            chain.entry(context, resourceWrapper, null, count, prioritized, args);
        } catch (BlockException e1) {
            e.exit(count, args);
            // 如果出现异常继续向上抛出，如果上层捕获到了BlockException，则说明请求被限流了，否则请求正常执行
            throw e1;
        } catch (Throwable e1) {
            // This should not happen, unless there are errors existing in Sentinel internal.
            RecordLog.info("Sentinel unexpected exception", e1);
        }
    	// 正常执行会返回entry对象
        return e;
    }
```

### SlotChain的获取

```java
ProcessorSlot<Object> lookProcessChain(ResourceWrapper resourceWrapper) {
    // 这里使用两次判断为null(双重检查锁或缓存机制)的原因如下：
    // 假设有两个线程A，B同时到达第一个if，都为null，此时A线程进入synchronized
    // 遇到第二个if，当然还是null，A线程就会执行第二个if里面的语句，创建一个chain，然后存到缓存中，退出synchronized
    // B线程进入synchronized，获取chain，但此时缓存命中，就不用再执行第二个if里面的语句了，减少了synchronized锁的时间
    ProcessorSlotChain chain = chainMap.get(resourceWrapper);
    if (chain == null) {
        synchronized (LOCK) {
            chain = chainMap.get(resourceWrapper);
            if (chain == null) {
                // Entry size limit.
                if (chainMap.size() >= Constants.MAX_SLOT_CHAIN_SIZE) {
                    return null;
                }
				// 构造SlotChain
                chain = SlotChainProvider.newSlotChain();
                
               	// map扩容
                Map<ResourceWrapper, ProcessorSlotChain> newMap = new HashMap<ResourceWrapper, ProcessorSlotChain>(
                    chainMap.size() + 1);
                newMap.putAll(chainMap);
                // 将资源和slot-chain映射
                newMap.put(resourceWrapper, chain);
                chainMap = newMap;
            }
        }
    }
    return chain;
}

public static ProcessorSlotChain newSlotChain() {
    if (slotChainBuilder != null) {
        return slotChainBuilder.build();
    }

    // Resolve the slot chain builder SPI.
    slotChainBuilder = SpiLoader.of(SlotChainBuilder.class).loadFirstInstanceOrDefault();

    // 确保slotChainBuilder不为null
    if (slotChainBuilder == null) {
        // Should not go through here.
        RecordLog.warn("[SlotChainProvider] Wrong state when resolving slot chain builder, using default");
        slotChainBuilder = new DefaultSlotChainBuilder();
    } else {
        RecordLog.info("[SlotChainProvider] Global slot chain builder resolved: {}",
                       slotChainBuilder.getClass().getCanonicalName());
    }
    return slotChainBuilder.build();
}


public class DefaultSlotChainBuilder implements SlotChainBuilder {

    @Override
    public ProcessorSlotChain build() {
        ProcessorSlotChain chain = new DefaultProcessorSlotChain();

        List<ProcessorSlot> sortedSlotList = SpiLoader.of(ProcessorSlot.class).loadInstanceListSorted();
        for (ProcessorSlot slot : sortedSlotList) {
            if (!(slot instanceof AbstractLinkedProcessorSlot)) {
                RecordLog.warn("The ProcessorSlot(" + slot.getClass().getCanonicalName() + ") is not an instance of AbstractLinkedProcessorSlot, can't be added into ProcessorSlotChain");
                continue;
            }

            chain.addLast((AbstractLinkedProcessorSlot<?>) slot);
        }

        return chain;
    }
}
```

在 Sentinel 里面，所有的资源都对应一个资源名称（`resourceName`），每次资源调用都会创建一个 `Entry` 对象。Entry 可以通过对主流框架的适配自动创建，也可以通过注解的方式或调用 `SphU` API 显式创建。Entry 创建的时候，同时也会创建一系列功能插槽（slot chain），这些插槽有不同的职责，例如:

- **`NodeSelectorSlot`**：收集资源的路径，并将这些资源的调用路径，以树状结构存储起来，用于根据调用路径来限流降级。
- **`ClusterBuilderSlot`** ：用于存储资源的统计信息以及调用者信息，例如该资源的 RT, QPS, thread count 等等，这些信息将用作为多维度限流，降级的依据；
- **`StatisticSlot`** ：用于记录、统计不同纬度的 runtime 指标监控信息；
- **`SystemSlot`** ：通过系统的状态，来控制总的入口流量；
- **`AuthoritySlot`** ：根据配置的黑白名单和调用来源信息，来做黑白名单控制；
- **`FlowSlot`** ：用于根据预设的限流规则以及前面 slot 统计的状态，来进行流量控制；
- **`DegradeSlot`** ：通过统计信息以及预设的规则，来做熔断降级；

下面借用官网的图片：官方链接如下：https://sentinelguard.io/zh-cn/docs/basic-implementation.html

![image-20220801191458948](https://s2.loli.net/2022/08/01/9ae41dBnACkl8tm.png)

整个调用链中最核心的就是 **`StatisticSlot`(**用于记录、统计不同纬度的 runtime 指标监控信息) 以及**`FlowSlot`(**根据预设的限流规则以及前面 slot 统计的状态，来进行流量控制）

可以查看**`DefaultProcessorSlotChain.java`**类的源码：

```java
public class DefaultProcessorSlotChain extends ProcessorSlotChain {

    // 先创建头结点
    AbstractLinkedProcessorSlot<?> first = new AbstractLinkedProcessorSlot<Object>() {

        @Override
        public void entry(Context context, ResourceWrapper resourceWrapper, Object t, int count, boolean prioritized, Object... args)
            throws Throwable {
            super.fireEntry(context, resourceWrapper, t, count, prioritized, args);
        }

        @Override
        public void exit(Context context, ResourceWrapper resourceWrapper, int count, Object... args) {
            super.fireExit(context, resourceWrapper, count, args);
        }

    };
    // 创建尾结点，直接指向头结点
    AbstractLinkedProcessorSlot<?> end = first;
    ....

}
```

可以看出**ProcessorSlotChain**是一个链表，里面有两个**AbstractLinkedProcessorSlot**类型的链表：first和end，即链表的头结点和尾结点。

![image-20220801191557361](https://s2.loli.net/2022/08/01/VistdyuBMJv8DEb.png)

然后添加通过`addLast`方法添加节点

```java
@Override
public void addLast(AbstractLinkedProcessorSlot<?> protocolProcessor) {
    end.setNext(protocolProcessor);	// 由于end=first，所以是在first.next下面添加节点
    end = protocolProcessor;	// 然后调整end的指针指向新节点
}

// AbstractLinkedProcessorSlot抽象类中的方法
public void setNext(AbstractLinkedProcessorSlot<?> next) {
    this.next = next;
}
```

![image-20220801191607238](https://s2.loli.net/2022/08/01/tyoI2krTDFqZcSu.png)

然后依次类推，可以得到下面的链路SlotChain：

![image-20220801191620840](https://s2.loli.net/2022/08/01/krMqmgzZ6YTNOHS.png)

### SlotChain的链路执行

#### StatisticSlot中的entry逻辑

```java
public void entry(Context context, ResourceWrapper resourceWrapper, DefaultNode node, int count, boolean prioritized, Object... args) throws Throwable {
        Iterator var8;
        ProcessorSlotEntryCallback handler;
        try {
            // 做一些检查 传递到下一个entry，会触发后续的slot的entry方法，如果规则不通过，就会抛出BlockException异常，则会在下一步的node中统计被block的数量。反之，会在node中统计通过的请求数和线程数等信息。
            this.fireEntry(context, resourceWrapper, node, count, prioritized, args);
            // 执行到这里表示通过了检查，没有被限流
            node.increaseThreadNum();
            node.addPassRequest(count);
            if (context.getCurEntry().getOriginNode() != null) {
                context.getCurEntry().getOriginNode().increaseThreadNum();
                context.getCurEntry().getOriginNode().addPassRequest(count);
            }

            if (resourceWrapper.getEntryType() == EntryType.IN) {
                Constants.ENTRY_NODE.increaseThreadNum();
                Constants.ENTRY_NODE.addPassRequest(count);
            }

            Iterator var13 = StatisticSlotCallbackRegistry.getEntryCallbacks().iterator();

            while(var13.hasNext()) {
                ProcessorSlotEntryCallback<DefaultNode> handler = (ProcessorSlotEntryCallback)var13.next();
                handler.onPass(context, resourceWrapper, node, count, args);
            }
        } catch (PriorityWaitException var10) {
            node.increaseThreadNum();
            if (context.getCurEntry().getOriginNode() != null) {
                context.getCurEntry().getOriginNode().increaseThreadNum();
            }

            if (resourceWrapper.getEntryType() == EntryType.IN) {
                Constants.ENTRY_NODE.increaseThreadNum();
            }

            var8 = StatisticSlotCallbackRegistry.getEntryCallbacks().iterator();

            while(var8.hasNext()) {
                handler = (ProcessorSlotEntryCallback)var8.next();
                handler.onPass(context, resourceWrapper, node, count, args);
            }
        } catch (BlockException var11) {
            BlockException e = var11;
            context.getCurEntry().setBlockError(var11);
            node.increaseBlockQps(count);
            if (context.getCurEntry().getOriginNode() != null) {
                context.getCurEntry().getOriginNode().increaseBlockQps(count);
            }

            if (resourceWrapper.getEntryType() == EntryType.IN) {
                Constants.ENTRY_NODE.increaseBlockQps(count);
            }

            var8 = StatisticSlotCallbackRegistry.getEntryCallbacks().iterator();

            while(var8.hasNext()) {
                handler = (ProcessorSlotEntryCallback)var8.next();
                handler.onBlocked(e, context, resourceWrapper, node, count, args);
            }

            throw e;
        } catch (Throwable var12) {
            context.getCurEntry().setError(var12);
            throw var12;
        }

    }
```
`node.addPassRequest`方法是在`fireEntry`执行后才执行的，也就是说，当前请求通过了sentinel的流控规则，此时需要将当次请求记录下来

```java
public void addPassRequest(int count) {
    super.addPassRequest(count);
    this.clusterNode.addPassRequest(count);
}
```

