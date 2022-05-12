## 1. ByteBuf优点

与Java NIO的Byte Buffer相比，ByteBuf的优势如下：

- Pooling池化，减少了内存复制和GC，提升了效率
- 复合缓冲区类型，支持零复制
- 不需要调用flip()方法去切换读/写模式
- 扩展性良好
- 可以自定义缓冲区类型
- 读取和写入索引分开
- 方法的链式调用
- 可以进行引用计数，方便重复使用

## 2. ByteBuf的逻辑部分

ByteBuf是一个字节容器，内部是一个字节数组。从逻辑上来分，字节容器内部可以分为四个部分。

![image-20220502162739088](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202205021627245.png)

- 第一个部分是已用字节，表示已经使用完的废弃的无效字节。
- 第二部分是可读字节，这部分数据是ByteBuf保存的有效数据，从ByteBuf中读取的数据来自这一部分。
- 第三部分是可写字节，写入到ByteBuf的数据会写道这一部分中。
- 第四部分是可扩容字节，表示该ByteBuf最多还能扩容的大小。

## 3. ByteBuf的重要属性

ByteBuf通过三个整型的属性有效地区分可读数据和可写数据，使得读写之间没有冲突。这三个属性定义在**AbstructByteBuf**抽象类中。

- readerIndex：读指针。表示读取的起始位置，每读取一个字节，rederIndex自动增加1。一旦readerIndex与writerIndex相等，则表示ByteBuf不可读了。
- writerIndex：写指针。指示写入的起始位置，每写入一个字节，writerIndex自动增加1。一旦增加到writerIndex与**capacity()**容量相等，则表示ByteBuf已经不可写了。**capacity()**是一个成员方法，不是一个成员属性，**表示ByteBuf中可以写入的容量**。但不是最大容量**maxCapacity**。
- maxCapacity：最大容量。表示ByteBuf可以扩容的最大容量。当向ByteBuf中写数据的时候，如果容量不足，可以进行扩容。扩容的最大限度由**maxCapacity**的值来决定，超过maxCapacity值就会报错。

ByteBuf的这三个重要属性，如下图所示：

![image-20220503133022480](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202205031330922.png)

## 4. ByteBuf的三组方法

#### 4.1 容量系列

- `capacity()`：表示ByteBuf的容量，它的值是一下三部分之和：废弃的字节数、可读字节数、可写字节数。
- `maxCapacity()`：表示ByteBuf最大能够容纳的最大字节数，当向ByteBuf中写数据的时候，如果发现容量不足，则进行扩容，直到扩容到maxCapacity设定的上限。

#### 4.2 写入系列

- `isWritable()`：表示ByteBuf是否可写。如果`capacity()`容量大于writerIndex指针的位置，则表示可写，否则不可写。注意：如果isWritable()返回false，并不代表不能再往ByteBuf中写数据了，如果Netty发现往ByteBuf中写数据写不进去的话，会自动扩容ByteBuf.

```java
@Override
public boolean isWritable() {
    return capacity() > writerIndex;
}

@Override
public boolean isWritable(int numBytes) {
    return capacity() - writerIndex >= numBytes;
}
```

- `writableBytes()`：取得可写入的字节数，它的值等于容量capacity()减去writerIndex

```java

@Override
public int writableBytes() {
    return capacity() - writerIndex;
}
```

- `maxWritableBytes()`：取得最大的可写字节数，它的值等于最大容量maxCapacity()减去writerIndex

```java
@Override
public int maxWritableBytes() {
    return maxCapacity() - writerIndex;
}
```

- `writeBytes(byte[] src)`：把src字节数组中的数据全部写到ByteBuf。
- `writeTYPE(TYPE value)`：写入基础数据类型的数据。TYPE表示基础数据类型，包含8大数据类型：`writeByte()、writeBoolean()、writeChar()、writeShort()、writeInt()、writeLong()、writeFloat()、writeDouble()`。
- `setTYPE(TYPE value)`：基础数据类型的设置，不改变writerIndex指针值，同上8大数据类型。与`writeTYPE`不同的是，**setTYPE系列不改变写指针writerIndex的值，writeTYPE系列会改变写指针writerIndex的值。**
- `markWriterIndex()`：表示把当前写指针`writerIndex`属性的值保存在`markedWriterIndex`中。
- `resetWriterIndex()`：把之前保存的`markedWriterIndex`值恢复到写指针`writerIndex`属性中。

#### 4.3 读取系列

- `isReadable()`：返回ByteBuf是否可读。如果**writerIndex**指针的值大于**readerIndex**指针的值，则表示可读，否则为不可读。

```java
public boolean isReadable() {
    return writerIndex > readerIndex;
}

public boolean isReadable(int numBytes) {
    return writerIndex - readerIndex >= numBytes;
}
```

- `readableBytes()`：返回表示ByteBuf当前可读的字节数，它的值等于writerIndex减去readerIndex。

```java
@Override
public int readableBytes() {
    return writerIndex - readerIndex;
}
```

- `readBytes(byte[] dst)`：读取ByteBuf中的数据，将数据从ByteBuf中读取到dst数组中，这里dst数组的大小，通常等于`readableBytes()`
- `readType()`：读取基础数据类型，可以读取8大基础数据类型。会改变都指针readerIndex的值。
- `getTYPE(TYPE value)`：读取基础数据类型，不改变读指针readerIndex的值。
- `marReaderIndex()`：表示把当前写指针`readerIndex`属性的值保存在`markedReaderIndex`中。
- `resetReaderIndex()`：把之前保存的`markedReaderIndex`值恢复到写指针`readerIndex`属性中。

## 5. 基本使用案例

```java
@Test
public void writeReadTest() {
    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(9, 100);
    System.out.println("分配ByteBuf: " + buffer);
    buffer.writeBytes(new byte[]{1, 2, 3, 4});
    System.out.println("写入字符: " + buffer);

    // 读取字符，不改变指针
    for (int i = 0; i < buffer.readableBytes(); i++) {
        System.out.println("读取到的字符: " + buffer.getByte(i));
    }
    System.out.println("读完字符， buffer: " + buffer);

    // 读取字符，改变读指针
    while (buffer.isReadable()) {
        System.out.println("取一个字符： " + buffer.readByte());
    }
    System.out.println("取完字符， buffer: " + buffer);
}
```

![image-20220504152656346](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202205041527409.png)



## 6. ByteBuf的引用计数

Netty的ByteBuf的内存回收是通过引用计数方式管理。采用计数器追踪ByteBuf的生命周期，一是对Pooled ByteBuf的支持；二是能够尽快发现可以回收的ByteBuf。

**什么是池化ByteBuf缓冲区呢？**

在通信过程中，Buffer缓冲区实例会被频繁创建、使用、释放。频繁创建对象、内存分配、释放内存，系统的开销大、性能低。为了解决这一问题，Netty创建了一个Buffer对象池，将没有被引用的Buffer对象，放入对象缓存池中；当需要时，则重新从对象缓存池中取出，而不需要重新创建。

引用计数的大致规则如下：

在默认情况下，当创建完一个ByteBuf后，它的引用为1；每次调用`retain()`方法，它的引用就加1；每次调用`release()`方法，就将引用计数减1；如果引用为0，再次访问这个ByteBuf对象，将会抛出异常，表示这个ByteBuf没有进程引用它，所占用的内存需要回收。

```java
public void refTest() {
    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
    System.out.println("创建后: " + buffer.refCnt());

    buffer.retain();
    System.out.println("retain后： " + buffer.refCnt());

    buffer.release();
    System.out.println("release后： " + buffer.refCnt());

    buffer.release();
    System.out.println("release后： " + buffer.refCnt());

    buffer.retain();
    System.out.println("retain后： " + buffer.refCnt());
}
```

![image-20220504171917241](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202205041719689.png)

**所以，Netty中，引用计数器为0的缓冲区不能再使用**。因此，retain和release方法应该结对使用。

如果retain和release这两个方法，一次都不调用呢？则在缓冲区使用完成后，调用一次release，即释放一次。

当引用计数为0，Netty会对ByteBuf进行回收。	

1. Pooled池化的内存，放入可以重新分配的ByteBuf池子，等待下一次分配。
2. 未池化的内存：如果是堆结构缓存，会被JVM的垃圾回收机制回收；如果是Direct类型，调用本地方法释放外部内存

## 7. ByteBuf的Allocator分配器

Netty通过ByteBufAllocator分配器来创建缓冲区和分配内存空间。Netty提供了两种实现：PoolByteBufAllocator和UnpooledByteBufAllocator。

PoolByteBufAllocator将ByteBuf实例放入池中，提高了性能，将内存碎片减少到最小，采用了jemalloc高效内存分配的策略。

UnpooledByteBufAllocator是普通的未池化ByteBuf分配器，没有将ByteBuf放入池中，每次被调用时，返回一个新的ByteBuf实例。通过java的垃圾回收机制进行回收。

默认的分配器为ByteBufAllocator.DEFAULT（池化），可以通过java系统参数配置选项**io.netty.allocator.type**进行配置，配置时使用字符串值：unpooled，pooled

```java
static {
    String allocType = SystemPropertyUtil.get(
        "io.netty.allocator.type", PlatformDependent.isAndroid() ? "unpooled" : "pooled");
    allocType = allocType.toLowerCase(Locale.US).trim();

    ByteBufAllocator alloc;
    if ("unpooled".equals(allocType)) {
        alloc = UnpooledByteBufAllocator.DEFAULT;
        logger.debug("-Dio.netty.allocator.type: {}", allocType);
    } else if ("pooled".equals(allocType)) {
        alloc = PooledByteBufAllocator.DEFAULT;
        logger.debug("-Dio.netty.allocator.type: {}", allocType);
    } else {
        alloc = PooledByteBufAllocator.DEFAULT;
        logger.debug("-Dio.netty.allocator.type: pooled (unknown: {})", allocType);
    }

    DEFAULT_ALLOCATOR = alloc;

    THREAD_LOCAL_BUFFER_SIZE = SystemPropertyUtil.getInt("io.netty.threadLocalDirectBufferSize", 0);
    logger.debug("-Dio.netty.threadLocalDirectBufferSize: {}", THREAD_LOCAL_BUFFER_SIZE);

    MAX_CHAR_BUFFER_SIZE = SystemPropertyUtil.getInt("io.netty.maxThreadLocalCharBufferSize", 16 * 1024);
    logger.debug("-Dio.netty.maxThreadLocalCharBufferSize: {}", MAX_CHAR_BUFFER_SIZE);
}
```

## 8. ByteBuf的缓冲区类型

![image-20220505205645069](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202205052056322.png)

- Direct Memory不属于Java堆内存，所分配的内存起其实是调用操作系统malloc()函数获取的。由Netty本地内存堆Native堆进行管理。
- Direct Memory容量可以通过`-XX:MaxDirectMemorySize`来指定，如果不指定，则默认与Java堆的最大值（-Xmx指定）一样。这个不是绝对的，有的JVM默认与-Xmx无直接关系。
- Direct Memory使用避免了Java堆和Native堆之间来回复制数据
- 需要频繁创建缓冲区的场合，由于创建和销毁Direct Buffer的代价比较高，所以不宜使用Direct Buffer。
- Direct Buffer读写比Heap Buffer快，但是他的创建和销毁较慢
- 在Java垃圾回收机制回收时，Netty框架也会释放不再使用的Direct Buffer缓冲区，因为它的内存为堆外内存，所以清理工作不会为虚拟机带来压力。垃圾回收的场景：1. 仅在Java堆被填满，以至于无法为新的堆分配请求提供服务时发生。2. 在Java应用程序中调用System.gc()函数来释放内存。

## 9. 三类ByteBuf使用案例

先说Direct ByteBuf：

- 通过调用分配器`directBuffer()`创建
- 不能读取内部数组
- 读取缓冲数据进行业务处理比较麻烦，需要通过getBytes/readBytes等方法先将数据复制到Java的堆内存

再说`heap buffer`：

- 通过调用`buffer`创建
- 可以调用`hasArray()`判断是否为Heap堆缓冲，但返回false不一定是heap buffer，有可能是其他缓冲区
- 

```java
//堆缓冲区
@Test
public  void testHeapBuffer() {
    //取得堆内存
    //取得堆内存--netty4默认直接buffer，而非堆buffer
    //ByteBuf heapBuf = ByteBufAllocator.DEFAULT.buffer();
    ByteBuf heapBuf = ByteBufAllocator.DEFAULT.heapBuffer();
    heapBuf.writeBytes("hello world".getBytes(UTF_8));
    if (heapBuf.hasArray()) {
        //取得内部数组
        byte[] array = heapBuf.array();
        int offset = heapBuf.arrayOffset() + heapBuf.readerIndex();
        int length = heapBuf.readableBytes();
        System.out.println(new String(array, offset, length, UTF_8));
    }
    heapBuf.release();

}

//直接缓冲区
@Test
public  void testDirectBuffer() {
    ByteBuf directBuf =  ByteBufAllocator.DEFAULT.directBuffer();
    directBuf.writeBytes("hello world".getBytes(UTF_8));
    if (!directBuf.hasArray()) {
        int length = directBuf.readableBytes();
        byte[] array = new byte[length];
        //读取数据到堆内存
        directBuf.getBytes(directBuf.readerIndex(), array);
        System.out.println(new String(array, UTF_8));
    }
    directBuf.release();
}
```

## 10. ByteBuf自动释放

#### 10.1 在入站处理时，如何创建ByteBuf

Netty的Reactor反应器会在底层的Java NIO通道读取数据，即`AbstractNioByteChannel.NioByteUnsafe.read()`处，调用`ByteBufAllocator`方法创建ByteBuf实例。从操作系统缓冲区把数据读取到ByteBuf实例中，然后调用`pipeline.fireChannelRead(byteBuf)`方法将读取到的数据包送到入站处理流水线中。

#### 10.2 入站处理时，ByteBuf如何释放

