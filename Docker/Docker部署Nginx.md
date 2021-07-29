> Docker部署nginx

```shell
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker search nginx		# 搜索nginx, 可以去docker hub上搜索
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker pull nginx			# 下载镜像
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker images				
REPOSITORY   TAG       IMAGE ID       CREATED        SIZE
nginx        latest    08b152afcfae   29 hours ago   133MB
centos       latest    300e315adb2f   7 months ago   209MB

# 运行
# -d 后台运行
# --name 命名
# -p 映射端口  外网的3344访问到容器内部的80
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker run -d --name nginx01 -p 3344:80 nginx
77d296e05ad8ead7395d747592bbe05fafa3cb91a947875b2a4ad8165c6cfa7d
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker ps
CONTAINER ID   IMAGE     COMMAND                  CREATED         STATUS         PORTS                                   NAMES
77d296e05ad8   nginx     "/docker-entrypoint.…"   4 seconds ago   Up 4 seconds   0.0.0.0:3344->80/tcp, :::3344->80/tcp   nginx01
[root@iZuf616vx1rni5mn9jvi9oZ ~]# curl localhost:3344
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
......

# 进入容器内部
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker exec -it nginx01 /bin/bash
root@77d296e05ad8:/# whereis nginx	# 查看nginx配置文件
nginx: /usr/sbin/nginx /usr/lib/nginx /etc/nginx /usr/share/nginx
root@77d296e05ad8:/# cd /etc/nginx
root@77d296e05ad8:/etc/nginx# ls
conf.d	fastcgi_params	mime.types  modules  nginx.conf  scgi_params  uwsgi_params
root@77d296e05ad8:/etc/nginx# 

[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker ps
CONTAINER ID   IMAGE     COMMAND                  CREATED          STATUS          PORTS                                   NAMES
77d296e05ad8   nginx     "/docker-entrypoint.…"   46 minutes ago   Up 46 minutes   0.0.0.0:3344->80/tcp, :::3344->80/tcp   nginx01

# 退出容器
[root@iZuf616vx1rni5mn9jvi9oZ ~]# docker stop 77d296e05ad8
77d296e05ad8

```

