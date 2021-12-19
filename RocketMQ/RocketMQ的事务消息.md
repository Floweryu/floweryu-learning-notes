## 1. 使用场景

在淘宝购物车下单时，涉及到购物车系统和交易系统，这两个系统之间的数据最终一致性可以通过分布式事务消息的异步处理实现。在这种场景下，交易系统是最为核心的系统，需要最大限度地保证下单成功。而购物车系统只需要订阅消息队列RocketMQ版的交易订单消息，做相应的业务处理，即可保证最终的数据一致性。

## 2. 执行流程

![img](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202112192043820.png)

#### 事务消息发送步骤如下：

1. 生产者将半事务消息发送至消息队列RocketMQ服务端。
2. 服务端将消息持久化成功之后，向生产者返回Ack确认消息已经发送成功，此时消息为半事务消息。
3. 生产者开始执行本地事务逻辑。
4. 生产者根据本地事务执行结果向服务端提交二次确认结果（Commit或是Rollback），服务端收到确认结果后处理逻辑如下：
   - 二次确认结果为Commit：服务端将半事务消息标记为可投递，并投递给消费者。
   - 二次确认结果为Rollback：服务端不会将该消息投递给消费者，并按照如下逻辑进行回查处理。

#### 事务消息回查步骤如下：

1. 在断网或者是生产者应用重启的特殊情况下，上述步骤4提交的二次确认最终未到达服务端，经过固定时间后，服务端将对消息生产者即生产者集群中任一生产者实例发起消息回查。
2. 生产者收到消息回查后，需要检查对应消息的本地事务执行的最终结果。
3. 生产者根据检查得到的本地事务的最终状态再次提交二次确认，服务端仍按照步骤4对半事务消息进行处理。



#### 事务消息存储的`TOPIC`：

1. 生产者发送消息到BROKER,该消息是半事务消息，且事务消息的发送是同步发送的方式。
2. BROKER接收到消息后，会将消息进行切换，所有事务消息统一会写入**RMQ_SYS_TRANS_HALF_TOPIC**中，写入成功后会给生产者返回成功状态。
3. 本地生产获取到该消息的事务Id，进行本地事务处理。
4. 本地事务执行成功提交COMMIT，失败则提交ROLLBACK，超时提交或提交UNKNOW状态则会触发BROKER的事务回查。
5. 若提交了COMMIT或ROLLBACK状态，BROKER会将该消息写入到**RMQ_SYS_TRANS_OP_HALF_TOPIC**中，该TOPIC主要记录已经COMMIT或ROLLBACK的半事务消息，BROKER利用**RMQ_SYS_TRANS_HALF_TOPIC**和**RMQ_SYS_TRANS_OP_HALF_TOPIC**计算出需要回查的事务消息。如果是COMMIT消息，BROKER会将消息从**RMQ_SYS_TRANS_HALF_TOPIC**取出来存储到真正的TOPIC中，从而消费者可以正常进行消费，如果是ROOLBACK则不进行操作.
6. 如果本地事务执行超时或者返回了UNKNOW状态，则BROKER会进行事务回查。BROKER每次回查时会将消息在**RMQ_SYS_TRANS_HALF_TOPIC**中写一次。

所以，经历的三个TOPIC的功能如下：

- **RMQ_SYS_TRANS_HALF_TOPIC**：记录所有的半事务消息
- **RMQ_SYS_TRANS_OP_HALF_TOPIC**：记录已经提交了状态的半事务消息
- **REAL_TOPIC**：事务消息真正的TOPIC，在事务COMMIT后才会将消息写入该TOPIC，从而消费者才能消费



#### BROKER处理半事务消息

从RocketMQ的源码按照一下步骤点下去，既可以看到此处存储到TOPIC的逻辑：

```java
/**
 * 源码查看路径 broker包的processor目录下
 * SendMessageProcessor.java-->processRequest->asyncProcessRequest
 */
// 下面这部分代码判断是否为事务消息，因为事务消息是单条的
if (requestHeader.isBatch()) {
    return this.asyncSendBatchMessage(ctx, request, mqtraceContext, requestHeader);
} else {
    return this.asyncSendMessage(ctx, request, mqtraceContext, requestHeader);
}

/**
 * 点击asyncSendMessage，后面有下面代码
 */
String transFlag = origProps.get(MessageConst.PROPERTY_TRANSACTION_PREPARED);	// PROPERTY_TRANSACTION_PREPARED = "TRAN_MSG"
if (transFlag != null && Boolean.parseBoolean(transFlag)) {
    if (this.brokerController.getBrokerConfig().isRejectTransactionMessage()) {
        response.setCode(ResponseCode.NO_PERMISSION);
        response.setRemark(
            "the broker[" + this.brokerController.getBrokerConfig().getBrokerIP1()
            + "] sending transaction message is forbidden");
        return CompletableFuture.completedFuture(response);
    }
    putMessageResult = this.brokerController.getTransactionalMessageService().asyncPrepareMessage(msgInner);
} else {
    putMessageResult = this.brokerController.getMessageStore().asyncPutMessage(msgInner);
}

/**
 * 点击asyncPrepareMessage一直下去，会进入TransactionlMessageBridge.java类中的下面方法
 */
public CompletableFuture<PutMessageResult> asyncPutHalfMessage(MessageExtBrokerInner messageInner) {
    return store.asyncPutMessage(parseHalfMessageInner(messageInner));
}

/**
 * 将消息进行转换，最终将消息存储到统一处理事务的Topic中：RMQ_SYS_TRANS_HALF_TOPIC
 * @return 转换后的消息
 */
private MessageExtBrokerInner parseHalfMessageInner(MessageExtBrokerInner msgInner) {
    // 将消息所属真正Topic存储到消息的properties中
    MessageAccessor.putProperty(msgInner, MessageConst.PROPERTY_REAL_TOPIC, msgInner.getTopic());

    //将消息应该写的queue存储到消息的properties中
    MessageAccessor.putProperty(msgInner, MessageConst.PROPERTY_REAL_QUEUE_ID,
                                String.valueOf(msgInner.getQueueId()));

    //设置事务消息标志：Unknow，因为现在还没有接收到该事务消息的状态
    msgInner.setSysFlag(
        MessageSysFlag.resetTransactionValue(msgInner.getSysFlag(), MessageSysFlag.TRANSACTION_NOT_TYPE));

    //设置消息存储到的Topic: 统一事务消息Topic: RMQ_SYS_TRANS_HALF_TOPIC
    msgInner.setTopic(TransactionalMessageUtil.buildHalfTopic());

    //所有事务消息存放在该Topic的第一个队列里
    msgInner.setQueueId(0);

    //将其余该消息的属性统一存放进来
    msgInner.setPropertiesString(MessageDecoder.messageProperties2String(msgInner.getProperties()));
    return msgInner;
}
```

可以看到所有的prepare消息都是存储在一个Topic中的一个队列里，该Topic就是上面的**Half Topic**，最后会对消息进行存储逻辑的操作，并调用`handlePutMessageResult`构造返回结果返回给生产者

#### BROKER结束事务消息

生产者在发送prepare消息后—>执行本地事务逻辑—>broker接收请求结束本次事务状态：Broker在接收请求后根据命令会执行`EndTransactionProcessor`的`processRequest`方法，该方法中下面的逻辑是真正处理事务消息状态的：

```java
OperationResult result = new OperationResult();
if (MessageSysFlag.TRANSACTION_COMMIT_TYPE == requestHeader.getCommitOrRollback()) {
    // 获取RMQ_SYS_TRANS_HALF_TOPIC中的prepare消息
    result = this.brokerController.getTransactionalMessageService().commitMessage(requestHeader);
    if (result.getResponseCode() == ResponseCode.SUCCESS) {

        // 校验消息是否正确：Half中的该消息是不是真正的本次请求处理的消息
        RemotingCommand res = checkPrepareMessage(result.getPrepareMessage(), requestHeader);
        if (res.getCode() == ResponseCode.SUCCESS) {

            // 将prepare消息转换为原消息，该消息的Topic就是真正消息的Topic
            MessageExtBrokerInner msgInner = endMessageTransaction(result.getPrepareMessage());
            msgInner.setSysFlag(MessageSysFlag.resetTransactionValue(msgInner.getSysFlag(), requestHeader.getCommitOrRollback()));
            msgInner.setQueueOffset(requestHeader.getTranStateTableOffset());
            msgInner.setPreparedTransactionOffset(requestHeader.getCommitLogOffset());
            msgInner.setStoreTimestamp(result.getPrepareMessage().getStoreTimestamp());
            MessageAccessor.clearProperty(msgInner, MessageConst.PROPERTY_TRANSACTION_PREPARED);

            //将消息发送到真正的Topic里，该消息可以开始下发给消费者
            RemotingCommand sendResult = sendFinalMessage(msgInner);
            if (sendResult.getCode() == ResponseCode.SUCCESS) {
                //将消息放入RMQ_SYS_TRANS_OP_HALF_TOPIC
                this.brokerController.getTransactionalMessageService().deletePrepareMessage(result.getPrepareMessage());
            }
            return sendResult;
        }
        return res;
    }
} else if (MessageSysFlag.TRANSACTION_ROLLBACK_TYPE == requestHeader.getCommitOrRollback()) {
    // 同commitMessage方法一样，返回真正的操作的消息：将RMQ_SYS_TRANS_HALF_TOPIC中的该消息还原为原消息
    result = this.brokerController.getTransactionalMessageService().rollbackMessage(requestHeader);
    if (result.getResponseCode() == ResponseCode.SUCCESS) {
        RemotingCommand res = checkPrepareMessage(result.getPrepareMessage(), requestHeader);
        if (res.getCode() == ResponseCode.SUCCESS) {
            // 将消息放入RMQ_SYS_TRANS_OP_HALF_TOPIC
            this.brokerController.getTransactionalMessageService().deletePrepareMessage(result.getPrepareMessage());
        }
        return res;
    }
}
```

该方法会判断本次事务的最终状态，如果是COMMIT：

1. 获取**RMQ_SYS_TRANS_HALF_TOPIC**中的消息
2. 将该消息转换为原消息
3. 将消息写入到真正的TOPIC里，这里是事务消息的真正落盘，从而消息可以被消费者消费到
4. 如果落盘成功，则删除prepare消息，其实是将消息写入到**RMQ_SYS_TRANS_OP_HALF_TOPIC**里，该消息的内容就是这条消息在**RMQ_SYS_TRANS_HALF_TOPIC**队列里的offset，原因见后面的分析

如果是ROOLBACK，则直接将消息转换为原消息，所以在真正的TOPIC看不到ROOLBACK的消息，并写入到**RMQ_SYS_TRANS_OP_HALF_TOPIC**里

#### 事务消息回查

在RocketMQ中，消息都是顺序写随机读的，以offset来记录消息的存储位置与消费位置，所以对于事务消息的半事务消息来说，不可能做到物理删除，broker启动时每间隔60s会开始检查一下有哪些半事务消息需要回查，从上面的分析我们知道，所有半事务消息都存储在**RMQ_SYS_TRANS_HALF_TOPIC**中，那么如何从该Topic中取出需要回查的消息进行回查呢？这就需要**RMQ_SYS_TRANS_OP_HALF_TOPIC**以及一个内部的消费进度计算出需要回查的半事务消息进行回查：

- **RMQ_SYS_TRANS_HALF_TOPIC**：建一个队列，存储所有的半事务消息
- **RMQ_SYS_TRANS_OP_HALF_TOPIC**：建立的对列数与**RMQ_SYS_TRANS_HALF_TOPIC**相同，存储所有已经确定状态的半事务消息（rollback与commit状态），消息内容是该条消息在**RMQ_SYS_TRANS_HALF_TOPIC**的Offset
- **RMQ_SYS_TRANS_HALF_TOPIC消费进度**：默认消费者是**CID_RMQ_SYS_TRANS**，每次取半事务消息判断回查时，从该消费进度开始依次获取消息。
- **RMQ_SYS_TRANS_OP_HALF_TOPIC消费进度**：默认消费者是**CID_RMQ_SYS_TRANS**，每次获取半事务消息都需要判断是否在RMQ_SYS_TRANS_OP_HALF_TOPIC中已存在该消息了，若存在表示该半事务消息已结束流程，不需要再进行事务回查，每次判断都是从RMQ_SYS_TRANS_OP_HALF_TOPIC中获取一定消息数量出来进行对比的，获取的消息就是从RMQ_SYS_TRANS_OP_HALF_TOPIC中该消费进度开始获取的，最大一次获取32条。

broker在启动时会启动线程回查的服务，在`TransactionMessageCheckService`的`run`方法中，该方法会执行到onWaitEnd方法：

```java
@Override
protected void onWaitEnd() {
    // 获取超时时间 6s
    long timeout = brokerController.getBrokerConfig().getTransactionTimeOut();

    // 获取最大检测次数 15次
    int checkMax = brokerController.getBrokerConfig().getTransactionCheckMax();

    // 获取当前时间
    long begin = System.currentTimeMillis();
    log.info("Begin to check prepare message, begin time:{}", begin);

    //开始检测
    this.brokerController.getTransactionalMessageService().check(timeout, checkMax, this.brokerController.getTransactionalMessageCheckListener());
    log.info("End to check prepare message, consumed time:{}", System.currentTimeMillis() - begin);
}
```

该方法的最后会执行到`TransactionMessageServiceImpl`的`check`方法，该方法就是真正执行事务回查检测的方法，该方法的主要作用就是计算出需要回查的prepare消息进行事务回查，大致逻辑是：

- 获取**RMQ_SYS_TRANS_HALF_TOPIC**的所有队列，循环队列开始检测需要获取的半事务消息，实际上**RMQ_SYS_TRANS_HALF_TOPIC**只有一个队列。
- 获取**RMQ_SYS_TRANS_HALF_TOPIC**与**RMQ_SYS_TRANS_OP_HALF_TOPIC**的消费进度。
- 调用`fillOpRemoveMap`方法，获取**RMQ_SYS_TRANS_OP_HALF_TOPIC**中已完成的半事务消息。
- 从**RMQ_SYS_TRANS_HALF_TOPIC**中当前消费进度依次获取消息，与第3步获取的已结束的半事务消息进行对比，判断是否进行回查：
- 如果**RMQ_SYS_TRANS_OP_HALF_TOPIC**消息中包含该消息，则不进行回查，
- 如果不包含，获取**RMQ_SYS_TRANS_HALF_TOPIC**中的该消息，判断写入时间是否符合回查条件，若是新消息则不处理下次处理，并将消息重新写入**RMQ_SYS_TRANS_HALF_TOPIC**，判断回查次数是否小于15次，写入时间是否小于72h，如果不满足就丢弃消息，若满足则更新回查次数，并将消息重新写入**RMQ_SYS_TRANS_HALF_TOPIC**并进行事务回查，
- 在循环完后重新更新**RMQ_SYS_TRANS_HALF_TOPIC**与**RMQ_SYS_TRANS_OP_HALF_TOPIC**中的消费进度，下次判断回查逻辑时，将从最新的消费进度获取信息

## 3. 使用规则

#### 生产消息规则

- 事务消息发送完成本地事务后，可在`execute`方法中返回以下三种状态：
  - `TransactionStatus.CommitTransaction`：提交事务，允许消费者消费该消息。
  - `TransactionStatus.RollbackTransaction`：回滚事务，消息将被丢弃不允许消费。
  - `TransactionStatus.Unknow`：暂时无法判断状态，等待固定时间以后消息队列服务端根据回查规则向生产者进行消息回查。

- 通过`ONSFactory.createTransactionProducer`创建事务消息的Producer时必须指定`LocalTransactionChecker`的实现类，处理异常情况下事务消息的回查。

- 回查规则：本地事务执行完成后，若消息队列服务端收到的本地事务返回状态为`TransactionStatus.Unknow`，或生产者应用退出导致本地事务未提交任何状态。则消息队列服务端会向消息生产者发起事务回查，第一次回查后仍未获取到事务状态，则之后每隔一段时间会再次回查。

  - 回查间隔时间：系统默认每隔30秒发起一次定时任务，对未提交的半事务消息进行回查，共持续12小时。

  - 第一次消息回查最快时间：该参数支持自定义设置。若指定消息未达到设置的最快回查时间前，系统默认每隔30秒一次的回查任务不会检查该消息。

    以Java为例，以下设置表示：第一次回查的最快时间为60秒。

    ```java
    Message message = new Message();
    message.putUserProperties(PropertyKeyConst.CheckImmunityTimeInSeconds,"60");
    ```

    > 因为系统默认的回查间隔，第一次消息回查的实际时间会向后有0秒~30秒的浮动。
    >
    > 例如：指定消息的第一次消息最快回查时间设置为60秒，系统在第58秒时达到定时的回查时间，但设置的60秒未到，所以该消息不在本次回查范围内。等待间隔30秒后，下一次的系统回查时间在第88秒，该消息才符合条件进行第一次回查，距设置的最快回查时间延后了28秒。

#### 消费消息规则

- 事务消息的Group ID不能与其他类型消息的Group ID共用。与其他类型的消息不同，事务消息有回查机制，回查时消息队列RocketMQ版服务端会根据Group ID去查询生产者客户端。

