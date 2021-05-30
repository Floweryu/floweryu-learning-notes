发送端：

```java
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpClient {
    public static void main(String[] args) throws Exception {
    	// 1. 建立udp连接
        DatagramSocket socket = new DatagramSocket();
        
        // 2. 建立包
        String msg = "hello world";
        InetAddress localhostAddress = InetAddress.getByName("localhost");
        int port = 9090;
    
        DatagramPacket datagramPacket = new DatagramPacket(msg.getBytes(), 0, msg.getBytes().length, localhostAddress, port);
    
        // 3. 发送包
        socket.send(datagramPacket);
        
        socket.close();
        
    }
}

```

接收端：

```java
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import javax.naming.ldap.SortKey;

public class UdpServer {
	public static void main(String[] args) throws Exception {
		// 1. 建立连接
		DatagramSocket socket = new DatagramSocket(9090);
		
		// 2. 接收包存储
		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
		
		// 3. 接收
		socket.receive(packet);
		
		System.out.println(packet.getAddress().getHostAddress());
		System.out.println(new String(packet.getData(), 0, packet.getLength()));
		
		socket.close();
	}
}
```

