# 1. 下面程序实现了简单的消息发送和接收

发送端

```java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 * @author Floweryu
 * @date 2021/5/30 16:48
 */
public class UdpSender {
    public static void main(String[] args) throws IOException {

        // 1. 建立连接
        DatagramSocket socket = new DatagramSocket(8888);

        // 2. 读取数据
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            String data = bufferedReader.readLine();
            byte[] bytes = data.getBytes();
			
            // 3. 将数据和网络地址封装成包
            DatagramPacket packet = new DatagramPacket(bytes, 0, bytes.length, new InetSocketAddress("localhost", 6666));

            // 4. 发送
            socket.send(packet);

            if (data.equals("exit")) {
                break;
            }
        }

        socket.close();
    }
}

```

接收端：

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

# 2. 下面使用多线程实现消息的收发

发送方线程：

```java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 * @author Floweryu
 * @date 2021/5/30 17:32
 */
public class TalkSend implements Runnable{
    DatagramSocket socket = null;
    BufferedReader bufferedReader = null;

    private final int fromPort;
    private final String toIp;
    private final int toPort;

    public TalkSend(int fromPort, String toIp, int toPort) {
        this.fromPort = fromPort;
        this.toIp = toIp;
        this.toPort = toPort;

        try{
            // 创建连接
            socket = new DatagramSocket(this.fromPort);
            // 开启输入流
            bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                String data = bufferedReader.readLine();
                byte[] bytes = data.getBytes();

                // 将数据和网络打包
                DatagramPacket packet = new DatagramPacket(bytes, 0, bytes.length, new InetSocketAddress(this.toIp, this.toPort));

                socket.send(packet);

                if (data.equals("exit")) {
                    break;
                }
            }catch (Exception e) {
                e.printStackTrace();
            }

        }

        socket.close();
    }
}

```

接收方线程：

```java
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * @author Floweryu
 * @date 2021/5/30 17:39
 */
public class TalkReceive implements Runnable{
    DatagramSocket datagramSocket = null;
    private final int port;
    private final String msgFrom;

    public TalkReceive(int port, String msgFrom) {
        this.port = port;
        this.msgFrom = msgFrom;

        try {
            datagramSocket = new DatagramSocket(this.port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        while (true) {

            // 接收
            try {
                // 接收数据存储
                byte[] bytes = new byte[1024];
                DatagramPacket datagramPacket = new DatagramPacket(bytes, 0, bytes.length);
                datagramSocket.receive(datagramPacket);
                // 读取数据
                byte[] data = datagramPacket.getData();
                String receiveData = new String(data, 0, data.length);
                System.out.println(this.msgFrom + ":" + receiveData);

                if (receiveData.equals("exit")) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        datagramSocket.close();
    }
}

```

学生：

```java
/**
 * @author Floweryu
 * @date 2021/5/30 22:09
 */
public class TalkStudent {
    public static void main(String[] args) {
        new Thread(new TalkSend(7777, "localhost", 9999)).start();
        new Thread(new TalkReceive(8888, "老师")).start();
    }
}

```

教师：

```java
/**
 * @author Floweryu
 * @date 2021/5/30 22:12
 */
public class TalkTeacher {
    public static void main(String[] args) {
        new Thread(new TalkSend(5555, "localhost", 8888)).start();
        new Thread(new TalkReceive(9999, "学生")).start();
    }
}
```