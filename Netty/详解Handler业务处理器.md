整个IO处理操作环节包括：从通道读数据包、数据包解码、业务处理、目标数据解码、把数据包写到通道，然后由通道发送到对端。

![image-20220502135023159](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202205021350547.png)

## 1. ChannelInboundHandler通道入站处理器

- channelRegistered：当通道注册完成后，Netty会调用fireChannelRegistered，触发通道注册事件。通道会启动该入站操作的流水线处理，在通道注册过的入站处理器Handler的channelRegistered方法，会被调用到。
- channelActive：当通道激活完成后，Netty会调用fireChannelActive，触发通道激活事件。通道会启动该入站操作的流水线处理，在通道注册过的入站处理器Handler的channelActive方法，会被调用到。
- channelRead：当通道缓冲区可读，Netty会调用fireChannelRead，触发通道激活事件。通道会启动该入站操作的流水线处理，在通道注册过的入站处理器Handler的channelRead方法，会被调用到。
- channelReadComplete：当通道缓冲区读完，Netty会调用fireChannelReadComplete，触发通道激活事件。通道会启动该入站操作的流水线处理，在通道注册过的入站处理器Handler的channelReadComplete方法，会被调用到。
- channelInactive：当连接被断开或者不可用，Netty会调用fireChannelInactive，触发通道激活事件。通道会启动该入站操作的流水线处理，在通道注册过的入站处理器Handler的channelInactive方法，会被调用到。
- exceptionCaught：当通道处理过程发生异常，Netty会调用fireExceptionCaught，触发异常捕获事件。通道会启动异常捕获的流水线处理，在通道注册过处理器Handler的exceptionCaught方法，会被调用到。该方法在ChannelHandler中定义，入站、出站处理器都用到该方法。

## 2. ChannelOutboundHandler通道出站处理器

- bind：监听地址(IP + 端口号)绑定。完成底层Java IO通道的IP地址绑定。如果使用TCP传输协议，该方法用于服务端。
- connect：连接服务端。完成底层Java IO通道的服务端的连接操作。如果使用TCP传输协议，这个方法用户客户端。
- write：写数据到底层。完成Netty通道向底层Java IO通道的数据写入操作。此方法仅仅触发操作，并不是完成实际的数据写入操作。
- flush：腾空缓冲区中的数据，把这些数据写到对端。
- read：从底层读数据。完成Netty通道从Java IO通道的数据读取。
- disConnect：断开服务器连接。断开底层Java IO通道的服务端连接。如果使用TCP传输协议，此方法主要用于客户端。
- close：主动关闭通道。关闭底层的通道。

