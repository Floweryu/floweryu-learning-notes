参考来自：[https://www.bilibili.com/video/BV1c4411d7jb?p=60](https://www.bilibili.com/video/BV1c4411d7jb?p=60)
## 一、流量控制
所谓**流量控制**：让发送方发送速率不要太快，让接收方来得及接收。

利用**滑动窗口机制**可以实现流量控制。

在通信过程中，接收方根据自己**接收缓存的大小**，**动态的调整发送方发送窗口的大小**。接收方设置确认报文段的**窗口字段**调整的窗口大小通知给发送方。**发送方的发送窗口取决于接收方的窗口大小和拥塞窗口大小的最小值**。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201127142436463.png#pic_center)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201127142702606.png#pic_center)
TCP会为每一个连接设置一个持续计时器，只要TCP连接的一方收到对方零窗口的通知，就启动持续计时器。

若持续计时器设置的时间到期，就发送一个零窗口探测报文段，接收方接收到探测报文段时，给出现在的窗口值。

若窗口值仍然为0，那么发送方就重新设置持续计时器。

如果零窗口探测报文段丢失，则有重传计时器后，再次发送零窗口探测报文段。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201127143311612.png#pic_center)
## 二、拥塞控制
在某一段时间，若**对网络中某一资源的需求超过了该资源所能提供的可用部分**（`对资源需求的总和 > 可用资源`），网络性能就要变坏，这种情况叫做**拥塞**。

网络中许多资源同时呈现供应不足，就会出现网络性能变坏，网络吞吐量将随输入负荷的增大而下降。

**接收窗口**：接收方根据接收缓存设置的值，并告知给发送方，反映接收方容量。
**拥塞窗口**：发送方根据自己估算的网络拥塞程度而设置的窗口值，反映网络当前流量。

### `慢开始`和`拥塞避免`
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201127155945500.png#pic_center)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201129112629677.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MzIwNzAyNQ==,size_16,color_FFFFFF,t_70)

### `快重传`和`快恢复`
有时，个别报文段会在网络中丢失，但实际上网络并没有发生阻塞。这将会导致发送方超时重传，并误认为网络发生了拥塞。发送方把拥塞窗口cwnd值又设置为1，并错误的启动了慢开始算法，因而降低了传输效率。

采用**快重传算法**可以让发送方尽早知道发生了个别报文段的丢失。从而尽快进行重传，而不是等超时重传计时器超时再重传。
- 要求接收方不要等待自己发送数据时才捎带确认，而是要**立即发送确认**。
- 即使收到了失序的报文段也要立即发出对已收到的报文段的**重复确认**。
- 发送方一旦收到3个连续的重复确认，就将相应的报文段**立即重传**，而不是等待改报文段超时重传计数器超时再重传。
- 对于个别丢失的报文段，发送方不会出现超时重传，也就不会误认为出现了阻塞。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201129113549645.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MzIwNzAyNQ==,size_16,color_FFFFFF,t_70)
发送方一旦收到3个重复确认，就知道现在只是丢失了个别的报文段。于是不启动慢开始算法，而是执行快恢复算法。
- **发送方将慢开始门限ssthresh值和拥塞窗口cwnd值调整为当前窗口的一半，开始执行拥塞避免算法**。
- 也有的是把快恢复开始的拥塞窗口cwnd值再增大一些，即等于新的`ssthresh + 3`。这是因为丢失的3个报文段相当于是减少了3个报文段，所以可以把拥塞窗口增大些。

![在这里插入图片描述](https://img-blog.csdnimg.cn/2020112911435886.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MzIwNzAyNQ==,size_16,color_FFFFFF,t_70)

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201127160829831.png#pic_center)

