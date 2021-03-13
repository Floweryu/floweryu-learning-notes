# 1. 先了解JVM内存分布：

- **程序计数器**是jvm执行程序的流水线，存放一些跳转指令。
- **本地方法栈**是jvm调用操作系统方法所使用的栈。
-  **虚拟机栈**是jvm执行java代码所使用的栈。
- **方法区**存放了一些常量、静态变量、类信息等，可以理解成class文件在内存中的存放位置。
- **虚拟机堆**是jvm执行java代码所使用的堆。
- 字符串常量池：
	1. 直接使用双引号声明出来的 String 对象会直接存储在常量池中。
	2. 如果不是用双引号声明的 String 对象，可以使用 String 提供的 intern 方`String.intern()。`它的作用是：如果运行时常量池中已经包含一个等于此 String 对象内容的字符串，则返回常量池中该字符串的引用；如果没有，则在常量池中创建与此 String 内容相同的字符串，并返回常量池中创建的字符串的引用。

# 2. Java中的常量池

实际上分为两种形态：**静态常量池**和**运行时常量池**。

## 2.1 静态常量池

即*.class文件中的常量池，class文件中的常量池不仅仅包含字符串(数字)字面量，还包含类、方法的信息，占用class文件绝大部分空间。

这种常量池主要用于存放两大类常量：**字面量**(Literal)和**符号引用量**(Symbolic References).

- 字面量相当于Java语言层面常量的概念，如文本字符串，声明为final的常量值等。
- 符号引用则属于编译原理方面的概念，包括了如下三种类型的常量：
  - 类和接口的全限定名
  - 字段名称和描述符
  - 方法名称和描述符

## 2.2 运行时常量池

jvm虚拟机在完成类装载操作后，将class文件中的常量池载入到内存中，并保存在**方法区**中，我们常说的常量池，就是指方法区中的运行时常量池。

运行时常量池相对于class文件常量池的另外一个重要特征是**具备动态性**，Java语言并不要求常量一定只有编译期才能产生，也就是并非预置入CLass文件中常量池的内容才能进入方法区运行时常量池，运行期间也可能将新的常量放入池中，这种特性被开发人员利用比较多的就是**String类的intern()**方法。

> String的intern()方法会查找在常量池中是否存在一份equal相等的字符串,如果有则返回该字符串的引用,如果没有则添加自己的字符串进入常量池。

## 2.3  常量池的好处

常量池是为了避免频繁的创建和销毁对象而影响系统性能，其实现了对象的共享.

例如字符串常量池，在编译阶段就把所有的字符串文字放到一个常量池中。
（1）**节省内存空间**：常量池中所有相同的字符串常量被合并，只占用一个空间。
（2）**节省运行时间**：比较字符串时，比equals()快。对于两个引用变量，只用判断引用是否相等，也就可以判断实际值是否相等。

# 3. 字符串常量池

## 3.1 `String str = "helloworld"`创建过程

首先，会在栈区创建`str`引用，然后在**字符串常量池**中寻找是否存在指向其内容为`helloworld`的对象。如果有，则`str`的引用直接指向它；如果没有，则创建一个新的，然后将`str`的引用指向字符串常量池中的对象。

- 如果后来又用`String`定义了一个`str1`的字符串常量`String str1 = "helloworld"`，则直接将`str1`指向字符串常量池中已经存在的对象`helloworld`，不会再去创建新的对象。
- 当对`str = "hello"`进行新的赋值，则`str`会指向字符常量池的`hello`。
- 这时如果定义`String str3 = "hello"`则`str == str3`返回`true`。因为它们地址一样，值也一样。
- 进行字符串连接操作：令`str = str + "world"`，此时`str`指向的是在堆中新建的内容`"helloworld"`对象，所以`str == str1`返回的是`false`，因为它们地址不一样。

根据下图，尽量避免多个字符串拼接，因为这样会重新创建对象。如果需要改变字符串的花，可以使用 `StringBuilder `或者 `StringBuffer`。

![在这里插入图片描述](https://i.loli.net/2021/03/13/tGJTK7XEjQWkSCI.png)

### 3.1.1 `String str = new String("helloworld")`

直接在堆中创建对象

- 如果有`String str1 = new String("helloworld")`，则`str1`不会指向之前创建的对象，而是重新创建一个对象并指向它。所以两个对象的地址也就不一样
- 而`equals()`在String中被重写，比较的是内容

## 3.2 字符串拼接典例

```java
String s1 = "Hello";
String s2 = "Hello";
String s3 = "Hel" + "lo";
String s4 = "Hel" + new String("lo");
String s5 = new String("Hello");
String s6 = s5.intern();
String s7 = "H";
String s8 = "ello";
String s9 = s7 + s8;

System.out.println(s1 == s2);  // true
System.out.println(s1 == s3);  // true
System.out.println(s1 == s4);  // false
System.out.println(s1 == s9);  // false
System.out.println(s4 == s5);  // false
System.out.println(s1 == s6);  // true
```
- `s1 == s2`：这个非常好理解，s1、s2在赋值时，均使用的字符串字面量。说白话点，就是直接把字符串写死。在编译期间，这种字面量会直接放入class文件的常量池中，从而实现复用，载入运行时常量池后，s1、s2指向的是同一个内存地址，所以相等。

 - `s1 == s3`：这个地方有个坑，s3虽然是动态拼接出来的字符串，但是所有参与拼接的部分都是已知的字面量，在编译期间，这种拼接会被优化，编译器直接帮你拼好，因此`String s3 = "Hel" + "lo"`在class文件中被优化成`String s3 = "Hello"`，所以`s1 == s3`成立。**只有使用引号包含文本的方式创建的String对象之间使用“+”连接产生的新对象才会被加入字符串池中**。

 - `s1 == s4`：当然不相等，s4虽然也是拼接出来的，但`new String("lo")`这部分不是已知字面量，是一个不可预料的部分，编译器不会优化，必须等到运行时才可以确定结果，结合字符串不变定理，鬼知道s4被分配到哪去了，所以地址肯定不同。**对于所有包含new方式新建对象（包括null）的“+”连接表达式，它所产生的新对象都不会被加入字符串池中**。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210313173410866.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MzIwNzAyNQ==,size_16,color_FFFFFF,t_70)

- `s1 == s9`：也不相等，道理差不多，虽然s7、s8在赋值的时候使用的字符串字面量，但是拼接成s9的时候，s7、s8作为两个变量，都是不可预料的，编译器毕竟是编译器，不可能当解释器用，不能在编译期被确定，所以不做优化，只能等到运行时，**在堆中创建s7、s8拼接成的新字符串，在堆中地址不确定，不可能与方法区常量池中的s1地址相同。**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210313173433452.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MzIwNzAyNQ==,size_16,color_FFFFFF,t_70)
- ` s4 == s5`: 已经不用解释了，绝对不相等，二者都在堆中，但地址不同。

-  `s1 == s6`：这两个相等完全归功于intern方法，s5在堆中，内容为Hello ，intern方法会尝试将Hello字符串添加到常量池中，并返回其在常量池中的地址，因为常量池中已经有了Hello字符串，所以intern方法直接返回地址；而s1在编译期就已经指向常量池了，因此s1和s6指向同一地址，相等。

## 3.3 特殊情况典例：

### 【示例】1：

```java
public static final String A = "ab"; // 常量A
public static final String B = "cd"; // 常量B
public static void main(String[] args) {
     String s = A + B;  // 将两个常量用+连接对s进行初始化 
     String t = "abcd";   
    if (s == t) {   
         System.out.println("s等于t，它们是同一个对象");   
     } else {   
         System.out.println("s不等于t，它们不是同一个对象");   
     }   
 } 
s等于t，它们是同一个对象
```
A和B都是常量，值是固定的，因此s的值也是固定的，它在类被编译时就已经确定了。也就是说：`String s=A+B; `等同于：`String s="ab"+"cd"`;

### 【示例】2：

```java
public static final String A; // 常量A
public static final String B;    // 常量B
static {   
     A = "ab";   
     B = "cd";   
 }   
 public static void main(String[] args) {   
    // 将两个常量用+连接对s进行初始化   
     String s = A + B;   
     String t = "abcd";   
    if (s == t) {   
         System.out.println("s等于t，它们是同一个对象");   
     } else {   
         System.out.println("s不等于t，它们不是同一个对象");   
     }   
 } 
s不等于t，它们不是同一个对象
```
A和B虽然被定义为常量，但是它们都没有马上被赋值。在运算出s的值之前，他们何时被赋值，以及被赋予什么样的值，都是个变数。因此A和B在被赋值之前，性质类似于一个变量。那么s就不能在编译期被确定，而只能在运行时被创建了。

# 4. 参考文章：

- [https://juejin.im/post/6844903663496871943](https://juejin.im/post/6844903663496871943)
- [https://blog.csdn.net/weixin_44259720/article/details/88237822](https://blog.csdn.net/weixin_44259720/article/details/88237822)
- https://www.cnblogs.com/syp172654682/p/8082625.html
