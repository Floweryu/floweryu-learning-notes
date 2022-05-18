## 1. 固定长度解码器：FixedLengthFrameDecoder

> 适用场景：每个接收到的数据包的长度，都是固定的。

该解码器会将入站的ByteBuf数据包拆分成一个个固定长度的数据包，然后发送给下一个入站处理器。

## 2. 行分割解码器：LineBaseFrameDecoder

> 适用场景：每个ByteBuf数据包，适用换行符作为数据包的边界分隔符

该场景下，会使用换行符把ByteBuf数据包分割成一个个完整的数据包发送到下一个入站处理器。

**工作原理**：遍历ByteBuf数据包中的可读字节，判断在 二进制字节流中，是否存在换行符。如果有，就以此位置为结束位置，把可读索引到结束位置之间的字节作为解码成功后的ByteBuf数据包。

支持配置一个最大长度值，表示一行最大能包含的字节数，如果读取到最大长度的位置后，还未发现换行符，就会抛出异常。

使用时需要在每个数据包后面加上换行符

## 3. 自定义分隔符解码器：DelimiterBasedFrameDecoder

是行分割解码器的通用版本，可以自定义分隔符。

使用方法：

```java
// 将自定义分割符spliter2构造成ByteBuf传入DelimiterBasedFrameDecoder构造器
final ByteBuf delimiter = Unpooled.copiedBuffer(spliter2.getBytes(StandardCharsets.UTF_8));
ChannelInitializer i = new ChannelInitializer<EmbeddedChannel>() {
    protected void initChannel(EmbeddedChannel ch) {
        // true 表示解码时丢弃分隔符
        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, true, delimiter));
        ch.pipeline().addLast(new StringDecoder());
        ch.pipeline().addLast(new StringProcessHandler());
    }
};
// 然后在每个数据包后面添加上分隔符
```



## 4. 自定义长度解码器：LengthFieldBasedFrameDecoder

在ByteBuf数据包中，加了一个长度子段，保存了原始数据包的长度，解码的时候，会按照这个长度进行原始数据包的提取。

```java
    public LengthFieldBasedFrameDecoder(
            int maxFrameLength, // 发送的数据包最大长度
            int lengthFieldOffset,	// 长度字段偏移量
        	int lengthFieldLength,	// 长度子段自己占用的字节数
            int lengthAdjustment, 	// 长度字段的偏移量矫正
        	int initialBytesToStrip	// 丢弃的起始字节数
    ) {
    }
```

- maxFrameLength：发送的数据包最大长度。
- lengthFieldOffset：长度字段偏移量。指长度字段位于整个数据内部的字节数组中的下标值。
- lengthFieldLength：长度字段所占的字节数。如果长度字段是一个int整数，则为4，如果说是short，则为2.
- lengthAdjustment：长度的矫正值。计算公司：内容字段偏移量-长度字段偏移量-长度字段的字节数。
- initialBytesToStrip：丢弃的起始字节数（内容字段偏移量）。在有效数据字段content前面，还有一些其他字段的字节，作为最终的解析结果，可以丢弃。