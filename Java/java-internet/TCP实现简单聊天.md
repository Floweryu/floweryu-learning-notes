下面是客户端代码，简单的处理了一下异常：
```java
Socket socket = null;
try {
    // 1. 知道服务器的地址、端口号
    InetAddress serverIp = InetAddress.getByName("127.0.0.1");
    int port = 9999;

    // 2. 创建连接
    socket = new Socket(serverIp, port);

    // 3. 发送消息
    OutputStream os = socket.getOutputStream();
    os.write("hello, world".getBytes());

} catch (Exception e) {
    e.printStackTrace();
} finally {
    try {
        socket.close();
    } catch (Exception e) {
        e.printStackTrace();
    }

}
```

下面是服务端代码，异常也是简单处理的：

```java
ServerSocket serverSocket = null;
try {
    // 1. 服务端监听地址
    serverSocket = new ServerSocket(9999);

    // 2. 等待客户端连接
    Socket socket = serverSocket.accept();

    // 3. 读取客户端消息
    InputStream is = socket.getInputStream();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int len;
    while ((len = is.read(buffer)) != -1) {
        baos.write(buffer, 0, len);
    }
    System.out.println(baos.toString());
} catch (Exception e) {
    e.printStackTrace();
} finally {
    try {
        serverSocket.close();
    } catch (Exception e) {
        e.printStackTrace();
    }

}
```

