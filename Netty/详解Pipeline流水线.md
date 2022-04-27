## 1. Pipeline入站处理流程

先看下面程序，建立了三个入站处理器，按照A—>B—>C的顺序添加：

```java
public class InPipeline {
    public static class SimpleInHandlerA extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("入站处理器 A: 被回调 ");
            super.channelRead(ctx, msg);
        }
    }
    public static class SimpleInHandlerB extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("入站处理器 B: 被回调 ");
            super.channelRead(ctx, msg);
        }
    }
    public static class SimpleInHandlerC extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("入站处理器 C: 被回调 ");
            super.channelRead(ctx, msg);
        }
    }
    
}

@Test
public void testPipelineInBound() {
    ChannelInitializer i = new ChannelInitializer<EmbeddedChannel>() {
        protected void initChannel(EmbeddedChannel ch) {
            ch.pipeline().addLast(new InPipeline.SimpleInHandlerA());
            ch.pipeline().addLast(new InPipeline.SimpleInHandlerB());
            ch.pipeline().addLast(new InPipeline.SimpleInHandlerC());

        }
    };
    EmbeddedChannel channel = new EmbeddedChannel(i);
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(1);
    //向通道写一个入站报文
    channel.writeInbound(buf);
    try {
        Thread.sleep(Integer.MAX_VALUE);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}
```

结果如下：

```bash
入站处理器 A: 被回调 
入站处理器 B: 被回调 
入站处理器 C: 被回调 
```

![image-20220427202922780](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202204272029041.png)

## 2. Pipeline出站处理流程

同入站一致，按照ABC的顺序添加出战处理器：

```java
public class OutPipeline {

    public static class SimpleOutHandlerA extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            System.out.println("出站处理器 A: 被回调" );
            super.write(ctx, msg, promise);
        }
    }
    public static class SimpleOutHandlerB extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            System.out.println("出站处理器 B: 被回调" );
            super.write(ctx, msg, promise);
        }
    }
    public static class SimpleOutHandlerC extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            System.out.println("出站处理器 C: 被回调" );
            super.write(ctx, msg, promise);
        }
    }
}

    @Test
    public void testPipelineOutBound() {
        ChannelInitializer i = new ChannelInitializer<EmbeddedChannel>() {
            protected void initChannel(EmbeddedChannel ch) {
                ch.pipeline().addLast(new OutPipeline.SimpleOutHandlerA());
                ch.pipeline().addLast(new OutPipeline.SimpleOutHandlerB());
                ch.pipeline().addLast(new OutPipeline.SimpleOutHandlerC());
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(i);
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1);
        //向通道写一个出站报文
        channel.writeOutbound(buf);
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```

结果如下：

```bash
出站处理器 C: 被回调
出站处理器 B: 被回调
出站处理器 A: 被回调
```

![image-20220427204228917](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202204272042063.png)

## 3. ChannelHandlerContext上下文

在Handler业务处理器被添加到流水线中时，会创建一个通道处理器上下文ChannelHandlerContext，它代表了ChannelHander通道处理器和ChannelPipeline通道流水线之间的关联。

在Channel，ChannelPipeline，ChannelHandlerContext三个类中，会有同样的出站和入站处理方法