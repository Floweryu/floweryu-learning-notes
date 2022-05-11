浅层复制可以有效避免内存复制。ByteBuf的浅层复制有两种：切片浅层复制和整体浅层复制。

## 切片浅层复制 slice

slice方法可以获取到一个ByteBuf的一个切片。一个ByteBuf可以进行多次的切片浅层复制，多次切片后的ByteBuf对象可以共享一个存储区域。

slice方法有两个重载版本：

- `public ByteBuf slice()`：返回的是ByteBuf中可读部分的切片
- `public ByteBuf slice(int index, int length)`：可以灵活设置不同起始位置和长度，获取不同区域的切片

```java
@Override
public ByteBuf slice(int index, int length) {
    ensureAccessible();
    return new UnpooledSlicedByteBuf(this, index, length);
}

@Override
public ByteBuf slice() {
    return slice(readerIndex, readableBytes());
}
```

看下面实例的输入：

```java
@Test
public void sliceTest() {
    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(9, 1000);
    System.out.println("分配ByteBuf: " + buffer);
    buffer.writeBytes(new byte[]{1, 2, 3, 4});
    System.out.println("写入字节: " + buffer);
    ByteBuf slice = buffer.slice();
    System.out.println("切片slice: " + slice);
}
```

![image-20220511200621818](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202205112006914.png)

调用slice()方法后，新生成的ByteBuf对象，有下面几个重要属性值：

- readerIndex（读指针）的值为0
- writerIndex（写指针）的值为源ByteBuf的readableBytes()可读字节数
- maxCapacity（最大容量）的值为源ByteBuf的readableBytes()可读字节数

**切片后的两个特点**：

- 切片不可以写入。通过上面的maxCapacity的值和writerIndex相等已经可以看出。
- 切片和源ByteBuf的可读字节数相同。原因：切片后的`可读字节数=writerIndex - readerIndex`，也就是源ByteBuf的`readableBytes()-0`.

**切片后的新ByteBuf和源ByteBuf的关联**：

- 切片不会复制源ByteBuf的底层数据，底层数组和源ByteBuf的底层数组是同一个（指向同一个引用）。
- 切片不会改变源ByteBuf的引用计数。

## duplicate整体浅层复制：

和slice切片不同，`duplicate()`返回的是源ByteBuf的整个对象的一个浅层复制，包括以下内容：

- duplicate的读写指针、最大容量值，与源ByteBuf的读写指针相同。
- `duplicate()`不会复制源ByteBuf的底层数据。

`duplicate()`和`slice()`都是浅层复制。不同的是，`slice()`方法是切取一段的浅层复制，而`duplicate()`是整体的浅层复制。

```java
@Test
public void duplicateTest() {
    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(9, 1000);
    System.out.println("分配ByteBuf: " + buffer);
    buffer.writeBytes(new byte[]{1, 2, 3, 4});
    System.out.println("写入字节: " + buffer);
    ByteBuf slice = buffer.duplicate();
    System.out.println("切片slice: " + slice);
}
```

![image-20220511202519992](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202205112025510.png)

## 浅层复制的问题

浅层复制不会去实际复制数据，也不会改变ByteBuf的引用计数，这就导致一个问题：在源ByteBuf调用release之后，一旦引用计数为零，就变的不能访问了；如果强行对浅层复制进行读写，则会报错。

所以，在调用浅层复制实例时，可以调用一次`retain()`方法来增加引用，表示对应底层内存多了一次引用，引用计数为2。在浅层复制实例用完后，需要调用两次`release()`方法，将引用计数多减一，这样就不会影响源ByteBuf内存释放。



