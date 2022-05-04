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

## 3. ChannelHandlerContext上下文 ??

在Handler业务处理器被添加到流水线中时，会创建一个通道处理器上下文ChannelHandlerContext，它代表了ChannelHander通道处理器和ChannelPipeline通道流水线之间的关联。

> 在Channel，ChannelPipeline，ChannelHandlerContext三个类中，会有同样的出站和入站处理方法，同一个操作出现在不同的类中，功能有什么不同呢？
>
> 如果通过Channel或ChannelPipeline的实例来调用这些方法，他们就会在整个流水线中传播。通过ChannelHandlerContext通道处理器会进行上下文调用，就只会从当前节点开始执行Handler业务处理器，并传播到同类型处理器的下一个节点。

Channel，ChannelPipeline和ChannelHandlerContext三者关系：

Channel通道拥有一条ChannelPipeline通道流水线，每一个流水线节点为一个ChannelHandlerContext通道处理上线文对象，每一个上下文中包裹了一个ChannelHandler处理器。在ChannelHandler通道处理器的入站/出站方法中，Netty都会传递一个Context上下文实例作为实际参数。

## 4. 截断流水线处理

入站处理器的截断：

- 不调用`super.channelXXX(ctx, msg);`
- 不调用`ctx.fireChannelXXX(msg);`

出站处理器只要开始执行，就不能被截断。

## 5. Handler业务处理器的热拔插

Netty处理器流水线是一个双向链表，可以动态的进行业务处理的热拔插：动态地增加、删除流水线上的业务处理器Handler。

主要的方法声明在ChannelPipeline接口中，下面是使用示例：

```java
public class InPipeline {
    public static class SimpleInHandlerA extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("入站处理器 A: 被回调 ");
            super.channelRead(ctx, msg);
            // 移除该处理器
            ctx.pipeline().remove(this);
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
```

Netty的通道初始化处理器——ChannelInitializer，在注册回调channelRegistered方法中，就将自己从流水线中删除。因为一条通道只需要做一次初始化的工作。

```java
    private boolean initChannel(ChannelHandlerContext ctx) throws Exception {
        if (initMap.add(ctx)) { // Guard against re-entrance.
            try {
                initChannel((C) ctx.channel());
            } catch (Throwable cause) {
                // Explicitly call exceptionCaught(...) as we removed the handler before calling initChannel(...).
                // We do so to prevent multiple calls to initChannel(...).
                exceptionCaught(ctx, cause);
            } finally {
                if (!ctx.isRemoved()) {
                    ctx.pipeline().remove(this);
                }
            }
            return true;
        }
        return false;
    }
```

