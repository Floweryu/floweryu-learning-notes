### 1. 打开Windows Terminal的json配置文件

```json
{
    "commandline": "ssh aliyun", # 设置的terminal窗口打开命令
    "guid": "{a5f1d6f0-67db-c8c7-5554-889333cf370b}",  # guidk
    "name": "aliyun",
    "tabTitle": "aliyun-Linux"
}
```

### 2. 打开C:/Users/name/.ssh目录

在下面的`config`文件中添加

```bash
Host          aliyun
HostName      106.15.42.123	# 服务器的公网ip
Port          22
User          root
IdentityFile  C:/Users/name/.ssh/Aliyun.pem # 连接服务器的密钥,这里我是使用密钥登录服务器
```

