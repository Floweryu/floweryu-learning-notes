## 1. 下载镜像

```bas
docker pull elasticsearch:7.16.1
```

## 2. 创建容器挂载目录并授权，持久化容器数据和配置

```bash
mkdir -p /elasticsearch/data
mkdir -p /elasticsearch/config
mkdir -p /elasticsearch/plugins
```

授予读写权限

```bash
chmod -R 777 /elasticsearch
```

## 3. 运行下面命令启动

```bash
#配置es可以被远程的任何机器访问 --可根据实际业务需求进行设定
echo "http.host: 0.0.0.0">>/elasticsearch/config/elasticsearch.yml

##配置docker中的es，命令依次的含义如下：
#1.--name表示重命名 9200端口是es接收请求暴露的端口 9300是es在分布式集群下节点间通信的端口
#2.指定现在已单节点模式运行
#3.指定es的初始和最大的占用内存 --此处根据业务实际情况设定,此处最大内存设置过小可能导致es启动失败
#4.配置文件挂载  挂载后在docker外面修改相应的文件，与之挂载的docker内部文件会相应修改
#5.数据文件挂载
#6.插件挂载
#7.后台启动
docker run --name elasticsearch -p 9200:9200 -p 9300:9300 \
-e "discovery.type=single-node" \
-e ES_JAVA_OPTS="-Xms64m -Xmx512m" \
-v /elasticsearch/config/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml \
-v /elasticsearch/data:/usr/share/elasticsearch/data \
-v /elasticsearch/plugins:/usr/share/elasticsearch/plugins \
-d elasticsearch:7.16.1
```

### 4. 也可以使用 docker-compose 启动容器

创建 docker-compose.yml 文件:

```bash
version: '2'
services:
  elasticsearch:
    container_name: elasticsearch
    image: elasticsearch:7.16.1
    ports:
      - "9200:9200"
    volumes:
      - /elasticsearch/config/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml
      - /elasticsearch/data:/usr/share/elasticsearch/data
      - /elasticsearch/plugins:/usr/share/elasticsearch/plugins
    environment:
      - "ES_JAVA_OPTS=-Xms64m -Xmx512m"
      - "discovery.type=single-node"
      - "COMPOSE_PROJECT_NAME=elasticsearch-server"
    restart: always
```

##### 使用 docker-compose 在 docker-compose.yml 所在目录启动容器

```bash
docker-compose up -d
```

## 5. 访问 http://loaclhost:9200/

```bash
{
  "name" : "5b3052a25a82",
  "cluster_name" : "elasticsearch",
  "cluster_uuid" : "kSKiwkgtSiaExfpxmIkdaQ",
  "version" : {
    "number" : "7.16.1",
    "build_flavor" : "default",
    "build_type" : "docker",
    "build_hash" : "5b38441b16b1ebb16a27c107a4c3865776e20c53",
    "build_date" : "2021-12-11T00:29:38.865893768Z",
    "build_snapshot" : false,
    "lucene_version" : "8.10.1",
    "minimum_wire_compatibility_version" : "6.8.0",
    "minimum_index_compatibility_version" : "6.0.0-beta1"
  },
  "tagline" : "You Know, for Search"
}
```

## 6. 拉去 kibina 镜像并运行

具体配置参考：https://www.elastic.co/guide/cn/kibana/current/docker.html

```bash
docker search kibana

拉取镜像
docker pull kibana:7.16.1



# 运行启动kibana
docker run --name kibana -e ELASTICSEARCH_URL=http://106.15.42.148:9200/ -p 5601:5601 -d kibana:7.16.1
```
如果没有启动成功，则进入容器内部，修改`kibana.yml`文件

```bash
# 进入kibina容器
docker exec -it 894934073dda /bin/bash

cd config/
vim kibana.yml
```

```yaml

# 修改kibana.yml里面的 elasticsearch.hosts 为自己服务部署的ES地址
# ** THIS IS AN AUTO-GENERATED FILE **
#
# Default Kibana configuration for docker target
server.host: "0.0.0.0"
server.shutdownTimeout: "5s"
elasticsearch.hosts: [ "http://106.15.42.148:9200" ]  # 修改为自己ES服务的地址
monitoring.ui.container.elasticsearch.enabled: true
```

