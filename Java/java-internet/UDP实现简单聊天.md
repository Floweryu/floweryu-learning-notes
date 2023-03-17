发送端：

```java
1
import java.net.DatagramPacket;
2
import java.net.DatagramSocket;
3
import java.net.InetAddress;
4
​
5
public class UdpClient {
6
    public static void main(String[] args) throws Exception {
7
        // 1. 建立udp连接
8
        DatagramSocket socket = new DatagramSocket();
9
        
10
        // 2. 建立包
11
        String msg = "hello world";
12
        InetAddress localhostAddress = InetAddress.getByName("localhost");
13
        int port = 9090;
14
    
15
        DatagramPacket datagramPacket = new DatagramPacket(msg.getBytes(), 0, msg.getBytes().length, localhostAddress, port);
16
    
17
        // 3. 发送包
18
        socket.send(datagramPacket);
19
        
20
        socket.close();
21
        
22
    }
23
}
24
​
```

接收端：

```java
1
import java.net.DatagramPacket;
2
import java.net.DatagramSocket;
3
​
4
import javax.naming.ldap.SortKey;
5
​
6
public class UdpServer {
7
    public static void main(String[] args) throws Exception {
8
        // 1. 建立连接
9
        DatagramSocket socket = new DatagramSocket(9090);
10
        
11
        // 2. 接收包存储
12
        byte[] buffer = new byte[1024];
13
        DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
14
        
15
        // 3. 接收
16
        socket.receive(packet);
17
        
18
        System.out.println(packet.getAddress().getHostAddress());
19
        System.out.println(new String(packet.getData(), 0, packet.getLength()));
20
        
21
        socket.close();
22
    }
23
}
```

```java
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * @author Floweryu
 * @date 2021/5/30 16:49
 */
public class UdpReceive {
    public static void main(String[] args) throws IOException {

        DatagramSocket datagramSocket = new DatagramSocket(6666);


        while (true) {
            // 接收数据存储
            byte[] bytes = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(bytes, 0, bytes.length);
            // 接收
            datagramSocket.receive(datagramPacket);

            // 读取数据
            byte[] data = datagramPacket.getData();
            String receiveData = new String(data, 0, data.length);
            System.out.println(receiveData);

            if (receiveData.equals("exit")) {
                break;
            }
        }
        datagramSocket.close();
    }
}

```