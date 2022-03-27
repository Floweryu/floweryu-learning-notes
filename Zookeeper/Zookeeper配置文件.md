```bash
# The number of milliseconds of each tick
# zookeeper时间配置的基本单位(毫秒)
tickTime=2000
# The number of ticks that the initial
# synchronization phase can take
# 允许follower初始化连接到leader最大时长，它表示tickTime的倍数, 即：initLimit * tickTime
initLimit=10
# The number of ticks that can pass between
# sending a request and getting an acknowledgement
# 允许follower与leader数据同步的最大时长，它表示tickTime的倍数
syncLimit=5
# the directory where the snapshot is stored.
# do not use /tmp for storage, /tmp here is just
# example sakes.
# zookeeper 数据存储目录及日志保存目录(如果没有指明dataLogDir, 则日志也保存在这个文件中)
dataDir=/home/zookeeper/zkData
# the port at which the clients will connect
# 对客户端提供的端口号
clientPort=2181
# the maximum number of client connections.
# increase this if you need to handle more clients
# 单个客户端与zookeeper最大并发连接数
#maxClientCnxns=60
#
# Be sure to read the maintenance section of the
# administrator guide before turning on autopurge.
#
# http://zookeeper.apache.org/doc/current/zookeeperAdmin.html#sc_maintenance
#
# The number of snapshots to retain in dataDir
# 保存的数据快照数量, 之外的将会被清除
#autopurge.snapRetainCount=3
# Purge task interval in hours
# Set to "0" to disable auto purge feature
# 自动触发清除任务时间间隔，小时为单位, 默认为0, b
#autopurge.purgeInterval=1

## Metrics Providers
#
# https://prometheus.io Metrics Exporter
#metricsProvider.className=org.apache.zookeeper.metrics.prometheus.PrometheusMetricsProvider
#metricsProvider.httpPort=7000
#metricsProvider.exportJvmInfo=true
```

