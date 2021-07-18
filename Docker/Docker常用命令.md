# 帮助命令

```shell
docker version	# 显示docker版本信息
docker info		# 查看docker信息
docker 命令 --help	#万能命令
```

# 镜像命令

官方文档：https://docs.docker.com/engine/reference/commandline/docker/

**docker images** 查看本地所有镜像

```shell
[root@iZuf616vx1rni5mn9jvi9oZ rabbitmq]# docker images
REPOSITORY    TAG       IMAGE ID       CREATED        SIZE
hello-world   latest    d1165f221234   4 months ago   13.3kB

# 解释
REPOSITORY	镜像的仓库源
TAG			镜像的标签
IMAGE ID	镜像的ID
CREATED		镜像的创建时间
SIZE		镜像的大小

# 可选项
  -a, --all             Show all images (default hides intermediate images)
      --digests         Show digests
  -f, --filter filter   Filter output based on conditions provided
      --format string   Pretty-print images using a Go template
      --no-trunc        Don't truncate output
  -q, --quiet           Only show image IDs
```

**docker search** 搜索镜像

```shell
[root@iZuf616vx1rni5mn9jvi9oZ rabbitmq]# docker search mysql
NAME                              DESCRIPTION                                     STARS     OFFICIAL   AUTOMATED
mysql                             MySQL is a widely used, open-source relation…   11138     [OK]       
mariadb                           MariaDB Server is a high performing open sou…   4221      [OK]       
mysql/mysql-server                Optimized MySQL Server Docker images. Create…   829                  [OK]

# 可选项
  -f, --filter filter   Filter output based on conditions provided
      --format string   Pretty-print search using a Go template
      --limit int       Max number of search results (default 25)
      --no-trunc        Don't truncate output
# 举例
[root@iZuf616vx1rni5mn9jvi9oZ rabbitmq]# docker search mysql --filter=stars=3000
NAME      DESCRIPTION                                     STARS     OFFICIAL   AUTOMATED
mysql     MySQL is a widely used, open-source relation…   11138     [OK]       
mariadb   MariaDB Server is a high performing open sou…   4221      [OK]
```

**docker pull**	下载镜像

```shell
[root@iZuf616vx1rni5mn9jvi9oZ rabbitmq]# docker pull mysql
Using default tag: latest	# 如果不写 tag，默认是 latest 
latest: Pulling from library/mysql
b4d181a07f80: Pull complete 	# 分层下载，docker image的核心 联合文件系统
a462b60610f5: Pull complete 
578fafb77ab8: Pull complete 
524046006037: Pull complete 
d0cbe54c8855: Pull complete 
aa18e05cc46d: Pull complete 
32ca814c833f: Pull complete 
9ecc8abdb7f5: Pull complete 
ad042b682e0f: Pull complete 
71d327c6bb78: Pull complete 
165d1d10a3fa: Pull complete 
2f40c47d0626: Pull complete 
Digest: sha256:52b8406e4c32b8cf0557f1b74517e14c5393aff5cf0384eff62d9e81f4985d4b	# 签名
Status: Downloaded newer image for mysql:latest
docker.io/library/mysql:latest	# 真实地址

# 等价
docker pull mysql
docker pull docker.io/library/mysql:latest

# 指定版本下载
[root@iZuf616vx1rni5mn9jvi9oZ rabbitmq]# docker pull mysql:5.7
5.7: Pulling from library/mysql
b4d181a07f80: Already exists 	# 这部分和之前下载的可以共用，节省内存
a462b60610f5: Already exists 
578fafb77ab8: Already exists 
524046006037: Already exists 
d0cbe54c8855: Already exists 
aa18e05cc46d: Already exists 
32ca814c833f: Already exists 
52645b4af634: Pull complete 
bca6a5b14385: Pull complete 
309f36297c75: Pull complete 
7d75cacde0f8: Pull complete 
Digest: sha256:1a2f9cd257e75cc80e9118b303d1648366bc2049101449bf2c8d82b022ea86b7
Status: Downloaded newer image for mysql:5.7
docker.io/library/mysql:5.7

# 可选项
 -a, --all-tags                Download all tagged images in the repository
      --disable-content-trust   Skip image verification (default true)
      --platform string         Set platform if server is multi-platform capable
  -q, --quiet                   Suppress verbose output
```

**docker rmi** 删除镜像

```shell
# 通过id删除hello-world镜像
[root@iZuf616vx1rni5mn9jvi9oZ rabbitmq]# docker rmi -f d1165f221234
Untagged: hello-world:latest
Untagged: hello-world@sha256:df5f5184104426b65967e016ff2ac0bfcd44ad7899ca3bbcf8e44e4461491a9e
Deleted: sha256:d1165f2212346b2bab48cb01c1e39ee8ad1be46b87873d9ca7a4e434980a7726
[root@iZuf616vx1rni5mn9jvi9oZ rabbitmq]# docker images
REPOSITORY   TAG       IMAGE ID       CREATED       SIZE
mysql        5.7       09361feeb475   3 weeks ago   447MB
mysql        latest    5c62e459e087   3 weeks ago   556MB

# 删除所有镜像
[root@iZuf616vx1rni5mn9jvi9oZ rabbitmq]# docker rmi -f $(docker images -aq)
Untagged: mysql:5.7
Untagged: mysql@sha256:1a2f9cd257e75cc80e9118b303d1648366bc2049101449bf2c8d82b022ea86b7
Deleted: sha256:09361feeb4753ac9da80ead4d46e2b21247712c13c9ee3f1e5d55630c64c544f
Deleted: sha256:e454d1e47d2f346e0b2365c612cb6f12476ac4a3568ad5f62d96aa15bccf3e19
Deleted: sha256:e0457c6e331916c8ac6838ef4b22a6f62b21698facf4e143aa4b3863f08cf7d2
Deleted: sha256:ed73046ee2cd915c08ed37a545e1b89da70dc9bafeacfbd9fddff8f967373941
Deleted: sha256:419d7a76abf4ca51b81821da16a6c8ca6b59d02a0f95598a2605a1ed77c012eb
Untagged: mysql:latest
Untagged: mysql@sha256:52b8406e4c32b8cf0557f1b74517e14c5393aff5cf0384eff62d9e81f4985d4b
Deleted: sha256:5c62e459e087e3bd3d963092b58e50ae2af881076b43c29e38e2b5db253e0287
Deleted: sha256:b92a81bddd621ceee73e48583ed5c4f0d34392a5c60adf37c0d7acc98177e414
Deleted: sha256:265829a9fa8318ae1224f46ab7bc0a10d12ebb90d5f65d71701567f014685a9e
Deleted: sha256:2b9144b43d615572cb4a8fb486dfad0f78d1748241e49adab91f6072183644e9
Deleted: sha256:944ffc10a452573e587652116c3217cf571a32c45a031b79fed518524c21fd4f
Deleted: sha256:b9108f19e3abf550470778a9d91959ce812731d3268d7224e328b0f7d8a73d26
Deleted: sha256:9aecb80117a5517daf84c1743af298351a08e48fa04b8e99dcb63c817326a748
Deleted: sha256:d8773288899b1230986eba7486009df11d5dd6c628b1d4fd0443e873c6b00f70
Deleted: sha256:45a0a6bb39a4d7b37a6c598ae6af47f8a36ef63eaa9ef92d565137156aa36f54
Deleted: sha256:341f6b75346e72e9fa503aeb5362d1fe4f00449e02d3320e5c68f3052b7c2c13
Deleted: sha256:023f47f19f876ffa0225502a85b30954a44e54dc8223329fec32b336315c75c3
Deleted: sha256:058c443dffe18a5d2aad04cd5451a8540c7272ce9f8515d27e815303b1c25b59
Deleted: sha256:764055ebc9a7a290b64d17cf9ea550f1099c202d83795aa967428ebdf335c9f7
[root@iZuf616vx1rni5mn9jvi9oZ rabbitmq]# docker images
REPOSITORY   TAG       IMAGE ID   CREATED   SIZE

[root@iZuf616vx1rni5mn9jvi9oZ rabbitmq]# docker rmi -f $(docker images -aq)	# 删除全部镜像
[root@iZuf616vx1rni5mn9jvi9oZ rabbitmq]# docker rmi -f 镜像id		# 删除一个镜像
[root@iZuf616vx1rni5mn9jvi9oZ rabbitmq]# docker rmi -f 镜像id 镜像id 镜像id 	#删除多个镜像
```

# 容器命令

**下载一个`centos`镜像**

```shell
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker pull centos
```

**新建容器并启动镜像**

```shell
docker run [可选参数] image

# 参数说明
--name="Name"	# 容器名字，用来区分容器
-d				# 以后台方式运行
-it				# 使用交互方式运行，进入容器查看内容
-p				# 指定容器端口(小p)
	-p 	# ip:主机端口:容器端口	(常用)
	-p  #容器端口
-P				# 随机指定端口(大P)

# 测试
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker images
REPOSITORY   TAG       IMAGE ID       CREATED        SIZE
centos       latest    300e315adb2f   7 months ago   209MB
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker run -it centos /bin/bash
# 查看内部的centos 很多命令不完善
[root@cd195c8870c7 /]# ls
bin  dev  etc  home  lib  lib64  lost+found  media  mnt  opt  proc  root  run  sbin  srv  sys  tmp  usr  var
# 从容器退回主机
[root@cd195c8870c7 /]# exit
exit
```

**列出正在运行的容器**

```shell
# docker ps 命令
		# 列出正在运行的容器
-a		# 列出正在运行的容器 + 历史运行过的容器
-n=?	# 显示最近创建的容器
-q		# 只显示容器的编号

[root@iZuf616vx1rni5mn9jvi9oZ /]# docker ps
CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
[root@iZuf616vx1rni5mn9jvi9oZ /]# docker ps -a
CONTAINER ID   IMAGE          COMMAND       CREATED         STATUS                          PORTS     NAMES
cd195c8870c7   centos         "/bin/bash"   3 minutes ago   Exited (0) About a minute ago             nostalgic_nobel
9e9b427792e2   d1165f221234   "/hello"      3 hours ago     Exited (0) 3 hours ago                    romantic_cray
[root@iZuf616vx1rni5mn9jvi9oZ /]# docker ps -a -n=1
CONTAINER ID   IMAGE     COMMAND       CREATED         STATUS                     PORTS     NAMES
cd195c8870c7   centos    "/bin/bash"   6 minutes ago   Exited (0) 4 minutes ago             nostalgic_nobel
```

**退出容器**

```shell
exit	# 容器停止并退出
Ctrl + P + Q	# 容器不停止但退出
```

**删除容器**

```shell
docker rm 容器id		# 删除指定容器，不能删除正在运行的容器，用 rm -f 可以强制删除
docker rm -f $(docker ps -aq)	# 删除所有容器
docker ps -a -q|xargs docker rm	# 删除所有容器
```

**启动和停止容器操作**

```shell
docker start 容器id		# 启动容器
docker restart 容器id		# 重启容器
docker stop 容器id		# 停止当前正在运行的容器
docker kill 容器id		# 强制停止当前容器
```

# 其他命令

**后台启动容器**

```shell
# 命令：docker run -d 镜像名
[root@iZuf616vx1rni5mn9jvi9oZ /]# docker run -d centos
# 问题：使用docker ps，发现centos停止
# docker使用后台运行，就必须要有一个前台进程，docker 发现没有应用，就会自动停止
```

**查看日志**

```shell
# 命令：docker logs -tf --tail number 容器
# -tf 显示全部
# --tail number 查看日志行数
Options:
      --details        Show extra details provided to logs
  -f, --follow         Follow log output
      --since string   Show logs since timestamp (e.g. 2013-01-02T13:23:37Z) or relative (e.g. 42m for 42 minutes)
  -n, --tail string    Number of lines to show from the end of the logs (default "all")
  -t, --timestamps     Show timestamps
      --until string   Show logs before a timestamp (e.g. 2013-01-02T13:23:37Z) or relative (e.g. 42m for 42 minutes)
```

**查看容器进程信息**

```shell
# 命令：docker top 容器id
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker top 8492e483cfbd
UID    PID      PPID     C   STIME   TTY     TIME       CMD
root   224214   224194   0   22:44   pts/0   00:00:00   /bin/bash
```

**查看容器内部信息**

```shell
# 命令：docker inspect 容器Id
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker inspect 8492e483cfbd
[
    {
        "Id": "8492e483cfbd5494bdce220368c643316b1f6066008dbec0da51597421f7b583",
        "Created": "2021-07-18T14:44:38.186015752Z",
        "Path": "/bin/bash",
        "Args": [],
...
```

**进入当前正在运行的容器**

```shell
# 命令：docker exec -it 容器id bashShell
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker exec -it 8492e483cfbd /bin/bash
[root@8492e483cfbd /]# ls
bin  dev  etc  home  lib  lib64  lost+found  media  mnt  opt  proc  root  run  sbin  srv  sys  tmp  usr  var
[root@8492e483cfbd /]# ps -ef
UID          PID    PPID  C STIME TTY          TIME CMD
root           1       0  0 14:44 pts/0    00:00:00 /bin/bash
root          16       0  0 15:04 pts/1    00:00:00 /bin/bash
root          31      16  0 15:04 pts/1    00:00:00 ps -ef
[root@8492e483cfbd /]# 

# 方式二：docker attach 容器id
# 进入后正在执行
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker attach 8492e483cfbd
[root@8492e483cfbd /]# docker ps

# docker exec 进入容器后开启一个新的终端，可以在里面操作
# docker attach 进入容器正在执行的终端，不会启动新的进程
```

**从容器内拷贝文件到主机上**

```shell
# docker cp 容器id:容器内路径 目的主机路径
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker cp 8492e483cfbd:/home/test.java /home
```

