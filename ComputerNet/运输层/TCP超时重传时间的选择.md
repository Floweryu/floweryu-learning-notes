学习视频：https://www.bilibili.com/video/BV1c4411d7jb?p=62

## $RTT_S$计算方法

- 超时重传时间RTO的值应该设置为**略大于**报文段往返时间RTT的值
- 不能直接使用某次测量得到的RTT样本来计算超时重传时间RTO
- 利用每次测量得到的RTT样本，计算加权平均往返时间RTT（又称为平滑的往返时间）

​					$$RTT_{S1}=RTT_1$$

​					$$新的RTT_S=(1-\alpha)*旧的RTT_s+\alpha*新的RTT样本,   0\le\alpha<1$$

- 若$\alpha$很接近于0，则新样本对RTTs影响不大

- 若$\alpha $很接近于1，则新样本对RTTs影响较大

- 推荐$\alpha $值为 1 / 8，即0.125

超时重传时间RTO的值应略大于加权平均往返时间

## $RTT_D$计算方法

​						$$RTT_{D1} = RTT_1 \div 2$$

​						$$新的RTT_D = (1 - \beta) \times 旧的RTT_D + \beta \times | RTT_S - 新的RTT样本|\\\beta =0.25$$

## $RTO$计算方法

​						$RTO = RTT_S + 4 * RTT_D$

- 针对出现超时重传无法测准往返时间RTT问题，在计算加权平均往返时间RTTs时，只要报文重传，就不采用其往返时间RTT样本
- 改进算法：报文重传一次，就将超时重传时间RTO增大一些：$新RTO = 2倍旧RTO$

![image-20200607212236845](TCP超时重传时间的选择.assets/image-20200607212236845.png)

## 示例

![image-20200607213152744](TCP超时重传时间的选择.assets/image-20200607213152744.png)