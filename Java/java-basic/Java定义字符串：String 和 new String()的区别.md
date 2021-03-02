### 先了解三个不同存储区：
- 栈区：存放基本类型和引用类型的引用地址。
- 堆区：存放对象
- 字符串常量池：
	1. 直接使用双引号声明出来的 String 对象会直接存储在常量池中。
	2. 如果不是用双引号声明的 String 对象，可以使用 String 提供的 intern 方`String.intern()。`它的作用是：如果运行时常量池中已经包含一个等于此 String 对象内容的字符串，则返回常量池中该字符串的引用；如果没有，则在常量池中创建与此 String 内容相同的字符串，并返回常量池中创建的字符串的引用。

### 分别区分以下它们：
#### `String str = "helloworld"`
首先，会在栈区创建`str`引用，然后在**字符串常量池**中寻找是否存在指向其内容为`helloworld`的对象。如果有，则`str`的引用直接指向它；如果没有，则创建一个新的，然后将`str`的引用指向字符串常量池中的对象。

- 如果后来又用`String`定义了一个`str1`的字符串常量`String str1 = "helloworld"`，则直接将`str1`指向字符串常量池中已经存在的对象`helloworld`，不会再去创建新的对象。
- 当对`str = "hello"`进行新的赋值，则`str`会指向字符常量池的`hello`。
- 这时如果定义`String str3 = "hello"`则`str == str3`返回`true`。因为它们地址一样，值也一样。
- 进行字符串连接操作：令`str = str + "world"`，此时`str`指向的是在堆中新建的内容`"helloworld"`对象，所以`str == str1`返回的是`false`，因为它们地址不一样。

##### String的字符串拼接
根据下图，尽量避免多个字符串拼接，因为这样会重新创建对象。如果需要改变字符串的花，可以使用 StringBuilder 或者 StringBuffer。
```java
		  String str1 = "str";
		  String str2 = "ing";
		  
		  String str3 = "str" + "ing";//常量池中的对象
		  String str4 = str1 + str2; //在堆上创建的新的对象	  
		  String str5 = "string";//常量池中的对象
		  System.out.println(str3 == str4);//false
		  System.out.println(str3 == str5);//true
		  System.out.println(str4 == str5);//false
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201109141243406.png#pic_center)
#### `String str = new String("helloworld")`
直接在堆中创建对象
- 如果有`String str1 = new String("helloworld")`，则`str1`不会指向之前创建的对象，而是重新创建一个对象并指向它。所以两个对象的地址也就不一样
- 而`equals()`在String中被重写，比较的是内容


### 参考文章：
- [https://juejin.im/post/6844903663496871943](https://juejin.im/post/6844903663496871943)
- [https://blog.csdn.net/weixin_44259720/article/details/88237822](https://blog.csdn.net/weixin_44259720/article/details/88237822)
