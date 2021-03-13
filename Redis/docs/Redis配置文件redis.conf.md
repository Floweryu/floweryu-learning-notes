```
###网络相关###
# bind 127.0.0.1 # 绑定监听的网卡IP，注释掉或配置成0.0.0.0可使任意IP均可访问
protected-mode no # 关闭保护模式，使用密码访问
port 6379  # 设置监听端口，建议生产环境均使用自定义端口
timeout 30 # 客户端连接空闲多久后断开连接，单位秒，0表示禁用

###通用配置###
daemonize yes # 在后台运行
pidfile /var/run/redis_6379.pid  # pid进程文件名
logfile /usr/local/redis/logs/redis.log # 日志文件的位置

###RDB持久化配置###
save 900 1 # 900s内至少一次写操作则执行bgsave进行RDB持久化
save 300 10
save 60 10000 
# 如果禁用RDB持久化，可在这里添加 save ""
rdbcompression yes #是否对RDB文件进行压缩，建议设置为no，以（磁盘）空间换（CPU）时间
dbfilename dump.rdb # RDB文件名称
dir /usr/local/redis/datas # RDB文件保存路径，AOF文件也保存在这里

###AOF配置###
appendonly yes # 默认值是no，表示不使用AOF增量持久化的方式，使用RDB全量持久化的方式
appendfsync everysec # 可选值 always， everysec，no，建议设置为everysec

###设置密码###
requirepass 123456 # 设置复杂一点的密码
```

