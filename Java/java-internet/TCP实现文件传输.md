客户端代码：

```java
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class FileClient {
    public static void main(String[] args) throws Exception{
        // 1. 创建一个socket连接
        Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), 9000);

        // 2. 创建一个输出流
        OutputStream os = socket.getOutputStream();

        // 3. 读取文件
        FileInputStream fis = new FileInputStream(new File("5ca55d40c4477.jpg"));

        // 4. 写出文件
        byte[] buffer = new byte[1024];
        int len;
        while ((len = fis.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }

        socket.shutdownOutput();    // 传输完毕

        // 接收服务端的消息
        InputStream is = socket.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer2 = new byte[1024];
        int len2;
        while ((len2 = is.read(buffer2)) != -1) {
            baos.write(buffer2, 0, len2);
        }
        System.out.println(baos);

        baos.close();
        is.close();
        fis.close();
        os.close();
        socket.close();

    }
}

```

服务端代码：

```java
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {
    public static void main(String[] args) throws Exception {

        // 1. 监听客户端连接
        ServerSocket serverSocket = new ServerSocket(9000);
        Socket socket = serverSocket.accept();

        // 2. 创建一个输入流
        InputStream is = socket.getInputStream();

        // 3. 写出文件
        FileOutputStream fos = new FileOutputStream(new File("receive.jpg"));

        // 4. 读取文件
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            fos.write(buffer, 0, len);
        }

        OutputStream os = socket.getOutputStream();
        os.write("接收完毕，可以断开连接".getBytes());

        fos.close();
        is.close();
        socket.close();
        serverSocket.close();
    }
}

```

