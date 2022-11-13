## 一、前言

存储方式和效率：文件系统 > KV存储 > 关系型数据库。直接操作文件系统肯定是最快的，但是可靠性低，这一点上关系型数据库与文件系统刚好相反。

## 二、消息存储整体架构

消息存储架构图中主要有下面三个跟消息存储相关的文件构成：CommitLog、ConsumeQueue、IndexFile。

- CommitLog：**消息存储文件**，所有的主题消息都存储在CommitLog中。消息内容不定长，单个文件大小默认1G，文件名长度为20位，左边补零，剩余为起始偏移量，比如：00000000000000000000代表了第一个文件，起始偏移量为0，文件大小为1G=1073741824；当第一个文件写满了，第二个文件为00000000001073741824，起始偏移量为1073741824，以此类推。消息主要是顺序写入日志文件，当文件满了，写入下一个文件。
- ConsumeQueue：**消息消费索引**，引入的目的主要是提高消息消费的性能。由于RocketMQ是基于主题topic的订阅模式，消息消费是针对主题进行的，如果要遍历commitlog文件，根据topic检索消息是非常低效的。Consumer可根据ConsumeQueue来查找待消费的消息。其中，ConsumeQueue作为消费消息的索引，**保存了指定Topic下的队列消息在CommitLog中的起始物理偏移量offset，消息大小size和消息Tag的HashCode值**。consumequeue文件可以看成是基于topic的commitlog索引文件，故consumequeue文件夹的组织方式如下：topic/queue/file三层组织结构，具体存储路径为：$HOME/store/consumequeue/{topic}/{queueId}/{fileName}。同样consumequeue文件采取定长设计，每一个条目共20个字节，分别为8字节的commitlog物理偏移量、4字节的消息长度、8字节tag hashcode，单个文件由30W个条目组成，可以像数组一样随机访问每一个条目，每个ConsumeQueue文件大小约5.72M；
-  IndexFile：**提供了一种可以通过key或时间区间来查询消息的方法**。Index文件的存储位置是：$HOME/store/index/{fileName}，文件名fileName是以创建时的时间戳命名的，固定的单个IndexFile文件大小约为400M，一个IndexFile可以保存 2000W个索引，IndexFile的底层存储设计为在文件系统中实现HashMap结构，故RocketMQ的索引文件其底层实现为hash索引。

RocketMQ采用的是混合型的存储结构，即为Broker单个实例下所有的队列共用一个日志数据文件（即为CommitLog）来存储。RocketMQ的混合型存储结构(多个Topic的消息实体内容都存储于一个CommitLog中)针对Producer和Consumer分别采用了数据和索引部分相分离的存储结构，Producer发送消息至Broker端，然后Broker端使用同步或者异步的方式对消息刷盘持久化，保存至CommitLog中。只要消息被刷盘持久化至磁盘文件CommitLog中，那么Producer发送的消息就不会丢失。正因为如此，Consumer也就肯定有机会去消费这条消息。当无法拉取到消息后，可以等下一次消息拉取，同时服务端也支持长轮询模式，如果一个消息拉取请求未拉取到消息，Broker允许等待30s的时间，只要这段时间内有新消息到达，将直接返回给消费端。这里，RocketMQ的具体做法是，使用Broker端的后台服务线程—ReputMessageService不停地分发请求并异步构建ConsumeQueue（逻辑消费队列）和IndexFile（索引文件）数据。

## 三、消息存储实现类

消息存储实现类：org.apache.rocketmq.store.DefaultMessageStore，操作存储文件的API核心类，介绍一下里面的属性：

| 字段                                                         | 含义                                                         |
| ------------------------------------------------------------ | :----------------------------------------------------------- |
| MessageStoreConfig                                           | 消息存储配置属性                                             |
| CommitLog                                                    | commitLog文件存储实现类                                      |
| ConcurrentMap<String/* topic */, ConcurrentMap<Integer/* queueId */, ConsumeQueue>> consumeQueueTable | 消息队列存储缓存表，按消息主题分组                           |
| FlushConsumeQueueService                                     | 消息队列文件ConsumeQueue刷盘线程                             |
| CleanCommitLogService                                        | 清楚CommitLog文件服务                                        |
| CleanConsumeQueueService                                     | 清楚ConsumeQueue文件服务                                     |
| IndexService                                                 | 索引文件实现类                                               |
| AllocateMappedFileService                                    | MapedFile分配服务                                            |
| ReputMessageService                                          | CommitLog消息分发，根据CommitLog文件构建ConsumeQueue、IndexFile文件 |
| HAService                                                    | 存储HA机制                                                   |
| TransientStorePool                                           | 消息堆内存缓存                                               |
| MessageArrivingListener                                      | 消息拉取长轮询模式消息达到监听器                             |
| BrokerConfig                                                 | Broker配置属性                                               |
| StoreCheckpoint                                              | 文件刷盘检测点                                               |
| LinkedList< CommitLogDispatcher >                            | CommitLog文件转发请求                                        |

## 四、消息发送存储流程

消息存储源码入口：**org.apache.rocketmq.store.DefaultMessageStore#putMessage**

```java
public PutMessageResult putMessage(MessageExtBrokerInner msg) {
    return waitForPutResult(asyncPutMessage(msg));
}
```

#### 1. 检查存储状态

**org.apache.rocketmq.store.DefaultMessageStore#checkStoreStatus**方法，下面几种拒绝消息写入：

- 当前Broker停止工作
- 当前Broker为SLAVE角色，不能写入
- 当前Rocket不支持写入：可能因为broker的磁盘已满、写入逻辑队列错误、写入索引文件错误等原因。
- 操作系统页缓存繁忙：broker持有锁的时间超过**osPageCacheBusyTimeOutMills**，则算作操作系统页缓存繁忙。

```java
private PutMessageStatus checkStoreStatus() {
    // 当前Broker停止工作
    if (this.shutdown) {
        log.warn("message store has shutdown, so putMessage is forbidden");
        return PutMessageStatus.SERVICE_NOT_AVAILABLE;
    }

    // 当前Broker为SLAVE角色，不能写入
    if (BrokerRole.SLAVE == this.messageStoreConfig.getBrokerRole()) {
        long value = this.printTimes.getAndIncrement();
        if ((value % 50000) == 0) {
            log.warn("broke role is slave, so putMessage is forbidden");
        }
        return PutMessageStatus.SERVICE_NOT_AVAILABLE;
    }

    // 当前Rocket不支持写入：可能因为broker的磁盘已满、写入逻辑队列错误、写入索引文件错误等原因。
    if (!this.runningFlags.isWriteable()) {
        long value = this.printTimes.getAndIncrement();
        if ((value % 50000) == 0) {
            log.warn("the message store is not writable. It may be caused by one of the following reasons: " +
                     "the broker's disk is full, write to logic queue error, write to index file error, etc");
        }
        return PutMessageStatus.SERVICE_NOT_AVAILABLE;
    } else {
        this.printTimes.set(0);
    }

    // 操作系统页缓存繁忙：broker持有锁的时间超过**osPageCacheBusyTimeOutMills**，则算作操作系统页缓存繁忙。
    if (this.isOSPageCacheBusy()) {
        return PutMessageStatus.OS_PAGECACHE_BUSY;
    }
    return PutMessageStatus.PUT_OK;
}

@Override
public boolean isOSPageCacheBusy() {
    long begin = this.getCommitLog().getBeginTimeInLock();
    long diff = this.systemClock.now() - begin;

    return diff < 10000000
        && diff > this.messageStoreConfig.getOsPageCacheBusyTimeOutMills();
}
```

#### 2. 检查消息是否合法

**org.apache.rocketmq.store.DefaultMessageStore#checkMessage**方法，判断条件：

- topic的字符串长度不能大于127
- 消息字符串长度不能大于32767

```java
private PutMessageStatus checkMessage(MessageExtBrokerInner msg) {
    // topic的字符串长度不能大于127
    if (msg.getTopic().length() > Byte.MAX_VALUE) {
        log.warn("putMessage message topic length too long " + msg.getTopic().length());
        return PutMessageStatus.MESSAGE_ILLEGAL;
    }

    // 消息字符串长度不能大于32767
    if (msg.getPropertiesString() != null && msg.getPropertiesString().length() > Short.MAX_VALUE) {
        log.warn("putMessage message properties length too long " + msg.getPropertiesString().length());
        return PutMessageStatus.MESSAGE_ILLEGAL;
    }
    return PutMessageStatus.PUT_OK;
}
```

#### 3. 检查 light message queue(LMQ)，即微消息队列

**org.apache.rocketmq.store.DefaultMessageStore#checkLmqMessage**方法：

```java
private PutMessageStatus checkLmqMessage(MessageExtBrokerInner msg) {
    if (msg.getProperties() != null
        && StringUtils.isNotBlank(msg.getProperty(MessageConst.PROPERTY_INNER_MULTI_DISPATCH))
        && this.isLmqConsumeQueueNumExceeded()) {
        return PutMessageStatus.LMQ_CONSUME_QUEUE_NUM_EXCEEDED;
    }
    return PutMessageStatus.PUT_OK;
}

private boolean isLmqConsumeQueueNumExceeded() {
    if (this.getMessageStoreConfig().isEnableLmq() && this.getMessageStoreConfig().isEnableMultiDispatch()
        && this.lmqConsumeQueueNum.get() > this.messageStoreConfig.getMaxLmqConsumeQueueNum()) {
        return true;
    }
    return false;
}
```

#### 4. 开始存储消息到commitLog

**org.apache.rocketmq.store.CommitLog#asyncPutMessage**方法：

**1. 设置消息存储时间和CRC(速度快)**

```java
msg.setStoreTimestamp(System.currentTimeMillis());
// Set the message body BODY CRC (consider the most appropriate setting
// on the client)
msg.setBodyCRC(UtilAll.crc32(msg.getBody()));
```

**2. 如果消息有延迟级别并且不是事务消息**

```java
final int tranType = MessageSysFlag.getTransactionValue(msg.getSysFlag());
if (tranType == MessageSysFlag.TRANSACTION_NOT_TYPE
    || tranType == MessageSysFlag.TRANSACTION_COMMIT_TYPE) {
    // Delay Delivery
    if (msg.getDelayTimeLevel() > 0) {
        // 提供以下延迟级别： 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h 不得大于最大值
        if (msg.getDelayTimeLevel() > this.defaultMessageStore.getScheduleMessageService().getMaxDelayLevel()) {
            msg.setDelayTimeLevel(this.defaultMessageStore.getScheduleMessageService().getMaxDelayLevel());
        }

        // 将topic换成RMQ_SYS_SCHEDULE_TOPIC延迟消息
        topic = TopicValidator.RMQ_SYS_SCHEDULE_TOPIC;
        // 消息队列id换成延迟消息队列id
        int queueId = ScheduleMessageService.delayLevel2QueueId(msg.getDelayTimeLevel());

        // Backup real topic, queueId
        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_REAL_TOPIC, msg.getTopic());
        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_REAL_QUEUE_ID, String.valueOf(msg.getQueueId()));
        msg.setPropertiesString(MessageDecoder.messageProperties2String(msg.getProperties()));

        msg.setTopic(topic);
        msg.setQueueId(queueId);
    }
}
```

