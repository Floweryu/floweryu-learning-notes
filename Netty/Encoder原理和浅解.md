## 1. MessageToByteEncoder编码器

作用：将POJO对象编码成一个ByteBuf数据包。需要实现此抽象类的encode方法。

```java
public class Integer2ByteEncoder extends MessageToByteEncoder<Integer> {
    @Override
    public void encode(ChannelHandlerContext ctx, Integer msg, ByteBuf out)
            throws Exception {
        out.writeInt(msg);
        Logger.info("encoder Integer = " + msg);
    }
}
```



## 2. MessageToMessageEncoder编码器

作用：将POJO对象编码成另一个POJO对象。

```java
public class String2IntegerEncoder extends MessageToMessageEncoder<String> {

    @Override
    protected void encode(ChannelHandlerContext c, String s, List<Object> list) throws Exception {
        char[] array = s.toCharArray();
        for (char a : array) {
            //48 是0的编码，57 是9 的编码
            if (a >= 48 && a <= 57) {
                list.add(new Integer(a));
            }
        }
    }
}
```

