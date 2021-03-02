#### `==`的含义
- 基本数据类型：`byte, short, int, long, float, double, char, boolean`。它们之间的比较用`==`，**比较的是它们的值**。
- 引用数据类型：比较的是它们**在堆内存中的存放地址**。

#### `equals()`含义
它不能用于比较基本数据类型的变量。`equals()`方法存在于`Object()`类中，而`Object()`类是所有类的直接或间接父类。但又两种使用情况：
- 情况1：类没有覆盖`equals()`方法。则通过`equals()`比较该类的两个对象，等价于通过`==`比较这两个对象，**也就是比较两个对象的地址**。
- 情况2：类覆盖了`equals()`方法。使用`equals()`方法来**比较两个对象的内容是否相等**。若内容相等，则返回true.(即认为这两个对象相等）。

#### 示例：

```java
public class App {
    public static void main(String[] args) throws Exception {
        String s1 = "Hello";
        String s2 = "Hello";
        System.out.println(s1 == s2);   // true
    }
}
```
上述代码输出为`true`。是不是很奇怪？这就涉及到Java的内存。`s1`实际上创建了一个字符串常量，存放在**字符串常量池**中。`s2`再创建时，会去寻找字符串常量池中有没有该内容的字符串。有的话则**直接将s2的引用指向字符串常量池中的字符串**。所以这里`s1和s2指向的是同一个地址`，因此打印`true`。
[详细可参考这篇文章](https://blog.csdn.net/weixin_43207025/article/details/109576226).
```java
package equals.src;

public class App {
    public static void main(String[] args) throws Exception {
        String str1 = "hello";
        String str2 = new String("hello");
        String str3 = str2;
        System.out.println(str1 == str2);   // false
        System.out.println(str2 == str3);   // true
        System.out.println(str1 == str3);   // false
        System.out.println(str1.equals(str2));  // true
        System.out.println(str2.equals(str3));  // true
        System.out.println(str1.equals(str3));  // true
    }
}

```
这与上述不同的地方在于，`str2`使用`new`创建了一个字符串。这时，会申请一个新的内存地址。而不是去指向字符串常量池中。所以`str1和str2指向的地址就不同`。推理，`str1和str3也不同`。而后面的`equals`使用的是`String`类覆盖的的`equals`方法，只要内容一致就返回`true`。

