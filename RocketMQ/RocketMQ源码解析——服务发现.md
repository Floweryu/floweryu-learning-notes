## 一、服务发现

RocketMQ有下面几个角色

**NameSrv: 注册中心**

**Broker: 消息服务器**

**Producer: 消息生产者**

**Consumer: 消息消费者**

RocketMQ没有使用Zookeeper作为服务的注册中心，而是自研的NameSrv，每个NameSrv都是无关联的节点。

当消息服务器启动后，会将自己的地址信息等，注册到所有的NameSrv。


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3fa252bdbb174ebb9315293442e72f91~tplv-k3u1fbpfcp-watermark.image?)

当Producer和Consumer启动后，会主动连接NameServer，获取可用的Broker列表，并选取Broker进行连接，进行消息发送与拉取。


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ea2b11fe35bd4ac694ac5bd173d6f54f~tplv-k3u1fbpfcp-watermark.image?)
## 二、源码分析

### 2.1 路由注册

在源码的broker包根目录下，有一个`BrokerStartup`启动类。


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/df85901275254794a89c0a3dc22ff503~tplv-k3u1fbpfcp-watermark.image?)

入口代码如下：

```
 public static void main(String[] args) {
     start(createBrokerController(args));
 }
```

主要进行了两件事：

0.  创建BrokerController，用来管理Broker节点
0.  启动BrokerController

第一步：创建BrokerController过程，主要是分析配置信息，比如：NameSrv集群的地址表、Broker信的角色信息(Master/Salve)等，并对其进行初始化。

```
 final BrokerController controller = new BrokerController(
     brokerConfig,
     nettyServerConfig,
     nettyClientConfig,
     messageStoreConfig);
```

```
 Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
     private volatile boolean hasShutdown = false;
     private AtomicInteger shutdownTimes = new AtomicInteger(0);
 
     @Override
     public void run() {
         synchronized (this) {
             log.info("Shutdown hook was invoked, {}", this.shutdownTimes.incrementAndGet());
             if (!this.hasShutdown) {
                 this.hasShutdown = true;
                 long beginTime = System.currentTimeMillis();
                 controller.shutdown();
                 long consumingTimeTotal = System.currentTimeMillis() - beginTime;
                 log.info("Shutdown hook over, consuming total time(ms): {}", consumingTimeTotal);
             }
         }
     }
 }, "ShutdownHook"));
```

如果代码中使用了线程池，一种优雅的停机方式是注册一个JVM钩子函数，在JVM进程关闭之前，先将线程池关闭，及时释放资源。

第二步是主要的，启动各种服务。

```
 public void start() throws Exception {
     if (this.messageStore != null) {
         // 这里的messageStore是在创建controller时初始化的，controller.initialize(); 是DefaultMessageStore类
         // 启动消息存储服务，包括启动Broker的高可用机制；启动以下任务：
         // 1. 启动把内存当中的消息刷到磁盘中的任务
         // 2. 把 commitLog 中的消息分发到 consumerQueue 文件中任务
         // 3. cleanFilesPeriodically(): 清除过期的 commitLog/ consumerQueue 日志文件, 10s
         // 4. checkSelf(): 检查 commitLog/ consumerQueue 的 映射文件，10min
         // 5. 如果 commitLog 锁时间超过了阈值，持久化它的锁信息, 1s
         // 6. isSpaceFull(): 检测磁盘空间是否足够, 10s
         // 需要掌握的java的知识点：scheduleAtFixedRate, RandomAccessFile
         this.messageStore.start();
     }
 
     if (this.remotingServer != null) {
         // 使用Netty暴露Socket服务处理外部请求的调用
         this.remotingServer.start();
     }
 
     if (this.fastRemotingServer != null) {
         // 使用Netty暴露Socket服务处理外部请求的调用
         this.fastRemotingServer.start();
     }
 
     if (this.fileWatchService != null) {
         // 启动文件监听服务
         this.fileWatchService.start();
     }
 
     if (this.brokerOuterAPI != null) {
         // 启动 brokerOuterAPI 也就是 RemotingClient，使得 Broker 可以调用其它方
         this.brokerOuterAPI.start();
     }
 
     if (this.pullRequestHoldService != null) {
         // 启动 pullRequestHoldService 服务用于处理 Consumer 拉取消息
         this.pullRequestHoldService.start();
     }
 
     if (this.clientHousekeepingService != null) {
         // 启动 clientHousekeepingService 服务用于处理 Producer、Consumer、FilterServer 的存活
         this.clientHousekeepingService.start();
     }
 
     if (this.filterServerManager != null) {
         // 启动 filterServerManager 服务用于定时更新 FilterServer
         this.filterServerManager.start();
     }
 
     if (!messageStoreConfig.isEnableDLegerCommitLog()) {
         startProcessorByHa(messageStoreConfig.getBrokerRole());
         handleSlaveSynchronize(messageStoreConfig.getBrokerRole());
         // 注册 Broker 信息到 NameServer
         this.registerBrokerAll(true, false, true);
     }
 
     // 在注册完后，会创建定时任务发送心跳包
     this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
 
         @Override
         public void run() {
             try {
                 // 每10s向NameSrv发送心跳包，NameSrv会定时扫描broker列表，去掉长时间没发送心跳包的broker
                 BrokerController.this.registerBrokerAll(true, false, brokerConfig.isForceRegister());
             } catch (Throwable e) {
                 log.error("registerBrokerAll Exception", e);
             }
         }
     }, 1000 * 10, Math.max(10000, Math.min(brokerConfig.getRegisterNameServerPeriod(), 60000)), TimeUnit.MILLISECONDS);
 
     if (this.brokerStatsManager != null) {
         // 启动 Broker 中的指标统计
         this.brokerStatsManager.start();
     }
 
     if (this.brokerFastFailure != null) {
         // 启动 Broker 请求列表的过期请求清除任务
         this.brokerFastFailure.start();
     }
 }
```

调用**this.registerBrokerAll**方法注册broker到NameSrv上，在其内部调用**doRegisterBrokerAll**方法。**doRegisterBrokerAll**方法内部调用**this.brokerOuterAPI.registerBrokerAll**方法封装请求头，然后遍历NameSrv列表，向每个NameSrv发起注册请求。

> Broker启动时会向集群中所有NameServer发送心跳语句，每隔30s想集群中所有NameServer发送心跳包，NameServer收到心跳包时会更新brokerLiveTable缓存中BrokerLiveInfo的lastUpdateTimeStamp，然后NameServer每隔10s扫描brokerLiveTable，如果连续120s没有收到心跳包，NameServer将移除broker信息同时关闭Socket连接

```
 public synchronized void registerBrokerAll(final boolean checkOrderConfig, boolean oneway, boolean forceRegister) {
     TopicConfigSerializeWrapper topicConfigWrapper = this.getTopicConfigManager().buildTopicConfigSerializeWrapper();
 
     if (!PermName.isWriteable(this.getBrokerConfig().getBrokerPermission())
         || !PermName.isReadable(this.getBrokerConfig().getBrokerPermission())) {
         ConcurrentHashMap<String, TopicConfig> topicConfigTable = new ConcurrentHashMap<>();
         for (TopicConfig topicConfig : topicConfigWrapper.getTopicConfigTable().values()) {
             TopicConfig tmp =
                 new TopicConfig(topicConfig.getTopicName(), topicConfig.getReadQueueNums(), topicConfig.getWriteQueueNums(),
                                 this.brokerConfig.getBrokerPermission());
             topicConfigTable.put(topicConfig.getTopicName(), tmp);
         }
         topicConfigWrapper.setTopicConfigTable(topicConfigTable);
     }
 
     if (forceRegister || needRegister(this.brokerConfig.getBrokerClusterName(),
                                       this.getBrokerAddr(),
                                       this.brokerConfig.getBrokerName(),
                                       this.brokerConfig.getBrokerId(),
                                       this.brokerConfig.getRegisterBrokerTimeoutMills())) {
         doRegisterBrokerAll(checkOrderConfig, oneway, topicConfigWrapper);
     }
 }
 
 private void doRegisterBrokerAll(boolean checkOrderConfig, boolean oneway,
                                  TopicConfigSerializeWrapper topicConfigWrapper) {
     // 注册broker的信息到NameSrv上
     List<RegisterBrokerResult> registerBrokerResultList = this.brokerOuterAPI.registerBrokerAll(
         this.brokerConfig.getBrokerClusterName(),
         this.getBrokerAddr(),
         this.brokerConfig.getBrokerName(),
         this.brokerConfig.getBrokerId(),
         this.getHAServerAddr(),
         topicConfigWrapper,
         this.filterServerManager.buildNewFilterServerList(),
         oneway,
         this.brokerConfig.getRegisterBrokerTimeoutMills(),
         this.brokerConfig.isCompressedRegister());
 
     if (registerBrokerResultList.size() > 0) {
         RegisterBrokerResult registerBrokerResult = registerBrokerResultList.get(0);
         if (registerBrokerResult != null) {
             if (this.updateMasterHAServerAddrPeriodically && registerBrokerResult.getHaServerAddr() != null) {
                 this.messageStore.updateHaMasterAddress(registerBrokerResult.getHaServerAddr());
             }
 
             this.slaveSynchronize.setMasterAddr(registerBrokerResult.getMasterAddr());
 
             if (checkOrderConfig) {
                 this.getTopicConfigManager().updateOrderTopicConfig(registerBrokerResult.getKvTable());
             }
         }
     }
 }
```

进入**this.brokerOuterAPI.registerBrokerAll**方法：

```
 public List<RegisterBrokerResult> registerBrokerAll(
     final String clusterName,
     final String brokerAddr,
     final String brokerName,
     final long brokerId,
     final String haServerAddr,
     final TopicConfigSerializeWrapper topicConfigWrapper,
     final List<String> filterServerList,
     final boolean oneway,
     final int timeoutMills,
     final boolean compressed) {
 
     // 线程安全的List 适用于写操作少的场景，因为每次都要复制副本
     final List<RegisterBrokerResult> registerBrokerResultList = new CopyOnWriteArrayList<>();
     // 获取NameServerAddress列表
     List<String> nameServerAddressList = this.remotingClient.getNameServerAddressList();
     if (nameServerAddressList != null && nameServerAddressList.size() > 0) {
 
         final RegisterBrokerRequestHeader requestHeader = new RegisterBrokerRequestHeader();
         requestHeader.setBrokerAddr(brokerAddr);
         requestHeader.setBrokerId(brokerId);
         requestHeader.setBrokerName(brokerName);
         requestHeader.setClusterName(clusterName);
         requestHeader.setHaServerAddr(haServerAddr);
         requestHeader.setCompressed(compressed);
 
         RegisterBrokerBody requestBody = new RegisterBrokerBody();
         requestBody.setTopicConfigSerializeWrapper(topicConfigWrapper);
         requestBody.setFilterServerList(filterServerList);
         final byte[] body = requestBody.encode(compressed);
         final int bodyCrc32 = UtilAll.crc32(body);
         requestHeader.setBodyCrc32(bodyCrc32);
         // 多线程批量发送请求，使用CountDownLatch同步返回
         final CountDownLatch countDownLatch = new CountDownLatch(nameServerAddressList.size());
         for (final String namesrvAddr : nameServerAddressList) {
             brokerOuterExecutor.execute(() -> {
                 try {
                     RegisterBrokerResult result = registerBroker(namesrvAddr, oneway, timeoutMills, requestHeader, body);
                     if (result != null) {
                         registerBrokerResultList.add(result);
                     }
 
                     log.info("register broker[{}]to name server {} OK", brokerId, namesrvAddr);
                 } catch (Exception e) {
                     log.warn("registerBroker Exception, {}", namesrvAddr, e);
                 } finally {
                     countDownLatch.countDown();
                 }
             });
         }
 
         try {
             // 如果等待一定时间后不再等待，主线程继续执行
             countDownLatch.await(timeoutMills, TimeUnit.MILLISECONDS);
         } catch (InterruptedException e) {
         }
     }
 
     return registerBrokerResultList;
 }
```

接下来就是发送网络请求的**registerBroker**方法，主要用到基于Netty封装的**NettyRemotingClient**，该方法设置请求的Code为**REGISTER_BROKER(103)** 。

然后NameSrv会接收到该注册消息，根据Code是**REGISTER_BROKER(103)** 调用`org.apache.rocketmq.namesrv.routeinfo.RouteInfoManager#registerBroker`方法将Broker信息保存起来，使用了读写锁。

#### NameServer存储信息

先看看NameServer存储了哪些路由信息，在RouteInfoManager类中：

```
 private final static long BROKER_CHANNEL_EXPIRED_TIME = 1000 * 60 * 2;
 private final ReadWriteLock lock = new ReentrantReadWriteLock();
 private final HashMap<String/* topic */, Map<String /* brokerName */ , QueueData>> topicQueueTable;
 private final HashMap<String/* brokerName */, BrokerData> brokerAddrTable;
 private final HashMap<String/* clusterName */, Set<String/* brokerName */>> clusterAddrTable;
 private final HashMap<String/* brokerAddr */, BrokerLiveInfo> brokerLiveTable;
 private final HashMap<String/* brokerAddr */, List<String>/* Filter Server */> filterServerTable;
```

-   topicQueueTable：Topic消息队列路由信息，消息发送时根据路由表进行负载均衡。
-   brokerAddrTable：Broker基础信息，包含brokerName、所属集群名字，主备Broker地址。
-   clusterAddrTable：Broker集群信息，存储集群中所有Broker名称。
-   brokerLiveTable：Broker状态信息。NameServer每次收到心跳包时会替换该信息。
-   filterServerTable：Broker上的FilterServer列表，用于类模式消息过滤。

> RocketMQ一个Topic拥有多个消息队列，一个Broker为每一主题默认创建4个读队列4个写队列。多个Broker组成一个集群，BrokerName由相同的多台Broker组成的Master-Slave架构，brokerId为0代表Master，大于0代表Slave。BrokerLiveInfo中的lastUpdateTimestamp存储上次收到Broker心跳包的时间。

类图如下：


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2e9e8fc6f7184f26a5fe8eeadeef0b7e~tplv-k3u1fbpfcp-watermark.image?)

topicQueueTable、brokerAddrTable运行时结构如下：


![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/42f36eeea66e4dbfa65f0176b7c51150~tplv-k3u1fbpfcp-watermark.image?)

brokerLiveTable、clusterAddrTable运行时结构如下：


![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3bfbc50de4bb4954950467a9658bf9ae~tplv-k3u1fbpfcp-watermark.image?)

#### NameServer处理心跳包

RouteInfoManager#registerBroker方法。分为下面几步执行：

**1. clusterAddrTable表维护：**

```
 // 防止并发修改路由表
 this.lock.writeLock().lockInterruptibly();
 // 该broker集群如果不存在，则创建新的
 Set<String> brokerNames = this.clusterAddrTable.computeIfAbsent(clusterName, k -> new HashSet<>());
 brokerNames.add(brokerName);
```

**2. brokerAddrTable表维护**

首先从bokerAddrTable根据brokerName尝试获取broker信息，如果不存在，则新建BrokerData并放入到bookerAddrTable中，registerFirst设置为true，表示第一次注册，否则直接替换原来的，registerFirst设置为false，表示非第一次注册

```
 BrokerData brokerData = this.brokerAddrTable.get(brokerName);
 if (null == brokerData) {
     // 表示第一次注册
     registerFirst = true;
     brokerData = new BrokerData(clusterName, brokerName, new HashMap<>());
     this.brokerAddrTable.put(brokerName, brokerData);
 }
 Map<Long, String> brokerAddrsMap = brokerData.getBrokerAddrs();
 //Switch slave to master: first remove <1, IP:PORT> in namesrv, then add <0, IP:PORT>
 //The same IP:PORT must only have one record in brokerAddrTable
 // 移除旧的broker映射关系
 Iterator<Entry<Long, String>> it = brokerAddrsMap.entrySet().iterator();
 while (it.hasNext()) {
     Entry<Long, String> item = it.next();
     if (null != brokerAddr && brokerAddr.equals(item.getValue()) && brokerId != item.getKey()) {
         log.debug("remove entry {} from brokerData", item);
         it.remove();
     }
 }
 
 String oldAddr = brokerData.getBrokerAddrs().put(brokerId, brokerAddr);
 if (MixAll.MASTER_ID == brokerId) {
     log.info("cluster [{}] brokerName [{}] master address change from {} to {}",
              brokerData.getCluster(), brokerData.getBrokerName(), oldAddr, brokerAddr);
 }
 // 该broker之前是否注册过
 registerFirst = registerFirst || (null == oldAddr);
```

**3. topicQueueTable维护**

```
 if (null != topicConfigWrapper
     && MixAll.MASTER_ID == brokerId) {
     if (this.isBrokerTopicConfigChanged(brokerAddr, topicConfigWrapper.getDataVersion())
         || registerFirst) {
         ConcurrentMap<String, TopicConfig> tcTable =
             topicConfigWrapper.getTopicConfigTable();
         if (tcTable != null) {
             for (Map.Entry<String, TopicConfig> entry : tcTable.entrySet()) {
                 this.createAndUpdateQueueData(brokerName, entry.getValue());
             }
         }
     }
 }
```

如果broker是Master，并且BrokerTopic的信息发生变化或者是初次注册，则需要创建或更新Topic路由信息，为默认Topic自动注册路由信息。

**4. 更新brokerLiveTable，broker存活信息，是路由删除的重要依据**

```
 BrokerLiveInfo prevBrokerLiveInfo = this.brokerLiveTable.put(brokerAddr,
         new BrokerLiveInfo(
                 System.currentTimeMillis(),
                 topicConfigWrapper.getDataVersion(),
                 channel,
                 haServerAddr));
 if (null == prevBrokerLiveInfo) {
     log.info("new broker registered, {} HAServer: {}", brokerAddr, haServerAddr);
 }
```

**5.注册Broker的过滤器Server地址列表**

```
 if (filterServerList != null) {
     if (filterServerList.isEmpty()) {
         this.filterServerTable.remove(brokerAddr);
     } else {
         this.filterServerTable.put(brokerAddr, filterServerList);
     }
 }
 
 if (MixAll.MASTER_ID != brokerId) {
     String masterAddr = brokerData.getBrokerAddrs().get(MixAll.MASTER_ID);
     if (masterAddr != null) {
         BrokerLiveInfo brokerLiveInfo = this.brokerLiveTable.get(masterAddr);
         if (brokerLiveInfo != null) {
             result.setHaServerAddr(brokerLiveInfo.getHaServerAddr());
             result.setMasterAddr(masterAddr);
         }
     }
 }
```

一个Broker上会关联多个FilterServer消息过滤器。如果此Broker为从节点，则需要查找该Broker的Master节点信息，并更新对应的masterAddr属性。

#### 设计亮点

NameServer每收到一个心跳包，都会更细上述表的信息。上面源码更新各种表信息时，使用了锁粒度较小的读写锁，允许多个消息发送者并发读，保证消息高并发。但同一时刻NameServer只处理一个Broker心跳包，多个心跳包请求穿行执行。这是**读写锁经典使用场景**。

### 2.2 路由删除

NameServer会每隔10s来扫描**brokerLiveTable**状态表，如果BrokerLive的lastUpdateTimestamp的时间戳距当前时间超过120s，则认为Broker失效，移除该Broker，同时更新**topicQueueTable、brokerAddrTable、brokerLiveTable、filterServerTable**

RocketMQ有两个触发点触发路由删除：

1. NameServer定时扫描brokerLiveTable检测上次心跳包与当前系统时间的时间差，如果时间戳大于120s，则需要移除该Broker信息
2. Broker在正常被关闭的情况下，会执行unregisterBroker指令

下面介绍第一种方式：

```java
public int scanNotActiveBroker() {
    int removeCount = 0;
    Iterator<Entry<String, BrokerLiveInfo>> it = this.brokerLiveTable.entrySet().iterator();
    while (it.hasNext()) {
        Entry<String, BrokerLiveInfo> next = it.next();
        long last = next.getValue().getLastUpdateTimestamp();
        // 判断是否
        if ((last + BROKER_CHANNEL_EXPIRED_TIME) < System.currentTimeMillis()) {
            RemotingUtil.closeChannel(next.getValue().getChannel());
            it.remove();
            log.warn("The broker channel expired, {} {}ms", next.getKey(), BROKER_CHANNEL_EXPIRED_TIME);
            this.onChannelDestroy(next.getKey(), next.getValue().getChannel());

            removeCount++;
        }
    }

    return removeCount;
}
```

**RouteInfoManager#onChannelDestroy**方法核心处理：关闭channel，删除与该broker相关的路由信息。

第一步：申请写锁，将brokerAddress从brokerLiveTable表和filterServerTable表中移除

```java
this.lock.writeLock().lockInterruptibly();
this.brokerLiveTable.remove(brokerAddrFound);
this.filterServerTable.remove(brokerAddrFound);
```

第二步：维护brokerAddrTable。从brokerData中的brokerAddr中找到具体的broker，从BrokerData中移除。最后如果移除后BrokerData中不再包含其他Broker，则从brokerAddrTable中移除该brokerName对应条目。

```java
String brokerNameFound = null;
boolean removeBrokerName = false;
Iterator<Entry<String, BrokerData>> itBrokerAddrTable =
    this.brokerAddrTable.entrySet().iterator();
while (itBrokerAddrTable.hasNext() && (null == brokerNameFound)) {
    BrokerData brokerData = itBrokerAddrTable.next().getValue();

    Iterator<Entry<Long, String>> it = brokerData.getBrokerAddrs().entrySet().iterator();
    while (it.hasNext()) {
        Entry<Long, String> entry = it.next();
        Long brokerId = entry.getKey();
        String brokerAddr = entry.getValue();
        if (brokerAddr.equals(brokerAddrFound)) {
            brokerNameFound = brokerData.getBrokerName();
            it.remove();
            log.info("remove brokerAddr[{}, {}] from brokerAddrTable, because channel destroyed",
                     brokerId, brokerAddr);
            break;
        }
    }

    if (brokerData.getBrokerAddrs().isEmpty()) {
        removeBrokerName = true;
        itBrokerAddrTable.remove();
        log.info("remove brokerName[{}] from brokerAddrTable, because channel destroyed",
                 brokerData.getBrokerName());
    }
}
```

第三步：根据brokerName，从clusterAddrTable表中找到Broker并从集群中移除。移除后集群（brokerNames）中不包含任何Broker，则将该集群从clusterAddrTable中移除

```java
if (brokerNameFound != null && removeBrokerName) {
    Iterator<Entry<String, Set<String>>> it = this.clusterAddrTable.entrySet().iterator();
    while (it.hasNext()) {
        Entry<String, Set<String>> entry = it.next();
        String clusterName = entry.getKey();
        Set<String> brokerNames = entry.getValue();
        boolean removed = brokerNames.remove(brokerNameFound);
        if (removed) {
            log.info("remove brokerName[{}], clusterName[{}] from clusterAddrTable, because channel destroyed",
                     brokerNameFound, clusterName);

            if (brokerNames.isEmpty()) {
                log.info("remove the clusterName[{}] from clusterAddrTable, because channel destroyed and no broker in this cluster",
                         clusterName);
                it.remove();
            }

            break;
        }
    }
}
```

第四步：根据brokerName，遍历所有主题队列，如果队列中包含了当前Broker的队列，则移除。如果topic只包含待移除Broker的队列，从路由表中删除该topic.

```java
if (removeBrokerName) {
    String finalBrokerNameFound = brokerNameFound;
    Set<String> needRemoveTopic = new HashSet<>();

    topicQueueTable.forEach((topic, queueDataMap) -> {
        QueueData old = queueDataMap.remove(finalBrokerNameFound);
        log.info("remove topic[{} {}], from topicQueueTable, because channel destroyed",
                 topic, old);

        if (queueDataMap.size() == 0) {
            log.info("remove topic[{}] all queue, from topicQueueTable, because channel destroyed",
                     topic);
            needRemoveTopic.add(topic);
        }
    });

    needRemoveTopic.forEach(topicQueueTable::remove);
}
```

第五步：释放锁

```java
this.lock.writeLock().unlock();
```

### 2.3 路由发现

启动一个生产者很简单，代码如下：

```
 DefaultMQProducer producer = new DefaultMQProducer("Producer");
 producer.setNamesrvAddr("127.0.0.1:9876");
 producer.start();
```

上面先告知Producer NameSrv 的地址，紧接着调用了**start**启动生产者。


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2d9b6c01aa0f40f1ae999213644a4503~tplv-k3u1fbpfcp-watermark.image?)

下面会执行到**org.apache.rocketmq.client.impl.factory.MQClientInstance#startScheduledTask**方法，该方法也启动了一些任务：

```
private void startScheduledTask() {
	......

    // 这里主要看这个方法
    this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

        @Override
        public void run() {
            try {
                // 更新Topic的路由信息
                MQClientInstance.this.updateTopicRouteInfoFromNameServer();
            } catch (Exception e) {
                log.error("ScheduledTask updateTopicRouteInfoFromNameServer exception", e);
            }
        }
    }, 10, this.clientConfig.getPollNameServerInterval(), TimeUnit.MILLISECONDS);

	......
}
```

主要看**updateTopicRouteInfoFromNameServer**这个任务：

```
    public void updateTopicRouteInfoFromNameServer() {
        Set<String> topicList = new HashSet<String>();

        // Consumer
        {
            Iterator<Entry<String, MQConsumerInner>> it = this.consumerTable.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, MQConsumerInner> entry = it.next();
                MQConsumerInner impl = entry.getValue();
                if (impl != null) {
                    Set<SubscriptionData> subList = impl.subscriptions();
                    if (subList != null) {
                        for (SubscriptionData subData : subList) {
                            topicList.add(subData.getTopic());
                        }
                    }
                }
            }
        }

        // Producer
        {
            Iterator<Entry<String, MQProducerInner>> it = this.producerTable.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, MQProducerInner> entry = it.next();
                MQProducerInner impl = entry.getValue();
                if (impl != null) {
                    Set<String> lst = impl.getPublishTopicList();
                    topicList.addAll(lst);
                }
            }
        }

        for (String topic : topicList) {
            this.updateTopicRouteInfoFromNameServer(topic);
        }
    }
```

从生产者和消费者收集Topic信息，然后遍历Topic列表，调用**this.updateTopicRouteInfoFromNameServer(topic)** 方法获取每个Topic的路由信息，保存到**TopicRouteData**中，包含Topic对应的Broker和Queue。然后将Brooker信息保存到**brokerAddrTable**表中。

```
public class TopicRouteData extends RemotingSerializable {
    private String orderTopicConf;
    private List<QueueData> queueDatas;
    private List<BrokerData> brokerDatas;
    private HashMap<String/* brokerAddr */, List<String>/* Filter Server */> filterServerTable;
    ......
}
```

到这里，**生产者就成功从NameSrv获取到了Broker信息**。

## 知识点

-   ReentrantReadWriteLock读写锁使用
-   CopyOnWriteArrayList
-   CountDownLatch
-   HashMap的computeIfAbsent方法
-   scheduleAtFixedRate
-   RandomAccessFile
-   Runtime.getRuntime().addShutdownHook 关闭线程池