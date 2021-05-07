# 1. 可变性

String 类中使⽤ final 关键字修饰字符数组来保存字符串， `private final char value[] `，所以 String 对象是不可变的。

> 在 Java 9 之后，String 类的实现改⽤ byte 数组存储字符串 private final byte[] value

⽽ StringBuilder 与 StringBuffer 都继承⾃ `AbstractStringBuilder `类，在 `AbstractStringBuilder `中 也是使⽤字符数组保存字符串 `char[] value` 但是没有⽤ final 关键字修饰，所以这两种对象都是可变的。

StringBuilder 与 StringBuffer 的构造⽅法都是调⽤⽗类构造⽅法也就是 AbstractStringBuilder 实 现的。

# 2. 线程安全性

String 中的对象是不可变的，也就可以理解为常量，线程安全。AbstractStringBuilder 是 StringBuilder 与 StringBuffer 的公共⽗类，定义了⼀些字符串的基本操作，如 expandCapacity、 append、insert、indexOf 等公共⽅法。

StringBuffer 对⽅法加了同步锁或者对调⽤的⽅法加了同步锁，所以是线程安全的。

StringBuilder 并没有对⽅法进⾏加同步锁，所以是⾮线程安全的。

# 3. 性能

每次对 String 类型进⾏改变的时候，都会⽣成⼀个新的 String 对象，然后将指针指向新的 String 对象。

StringBuffer 每次都会对 StringBuffer对象本身进⾏操作，⽽不是⽣成新的对象并改变对象引⽤。

相同情况下使⽤ StringBuilder 相⽐使⽤ StringBuffer 仅能获得 10%~15% 左右的性能提升，但却要冒多线程不安全的⻛险。 

对于三者使⽤的总结： 

1. 操作少量的数据: 适⽤ `String`
2.  单线程操作字符串缓冲区下操作⼤量数据: 适⽤ `StringBuilder `
3.  多线程操作字符串缓冲区下操作⼤量数据: 适⽤ `StringBuffer`

