### DHCP作用

![image-20200608211922363](Untitled.assets/image-20200608211922363.png)



### DHCP工作过程

![image-20200608214102636](Untitled.assets/image-20200608214102636.png)

- DHCP服务器被动打开UDP端口67，等待客户端发来的报文
- DHCP客户从UDP端口68发送DHCP发现报文
- 凡收到DHCP发现报文的DHCP服务器都发出DHCP提供报文，因此DHCP客户可能收到多个DHCP提供报文
- DHCP客户从几个DHCP服务器中选择其中一个，并向所选择的DHCP服务器发送DHCP请求报文
- DHCP发现报文：源ip地址为：0.0.0.0，因为还没有分配到ip地址；目的ip地址为255.255.255.255，进行广播发送，因为不知道有多少个DHCP服务器
- DHCP报文中封装有事务ID和DHCP客户端的MAC地址
- DHCP服务器接收到发现报文后，向主机发送报文，源IP地址为DHCP服务器的IP地址，目的IP仍为广播IP地址，因为主机还没有收到IP地址
- DHCP在挑选IP地址时会使用ARP确保所选IP地址未被网络中其它主机占用。若被占用，会给DHCP服务器发送"DHCP DECLINE"报文撤销IP地址租约，并重新发送"DHCP DISCOVER" 报文.
- 当租用期过了一半时，DHCP客户会向DHCP服务器发送DHCP请求报文，来请求更新租用期。封装该IP数据报的源IP地址为DHCP客户之前租用的IP地址，目的IP地址为DHCP服务器的地址。DHCP若同意，则发回DHCP确认报文，这样DHCP客户就得到了新的租用期。DHCP若不同意，则发出DHCP否认报文，这时DHCP客户必须立即停止使用之前租用的IP地址，并重新发送DHCP发现报文申请IP地址
- 主机根据事务ID来判断报文是不是之前请求的报文。DHCP报文中包含：IP地址，子网掩码，地址租期，默认网关，DNS服务器等

### DHCP中继代理

![image-20200608214246183](Untitled.assets/image-20200608214246183.png)

可以减少网络中DHCP服务器的数量。

