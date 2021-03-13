# 1. Java基本数据类型

Java基本类型共有八种，其中有4种整形，2种浮点型，1种用于表示Unicode编码的字符单元的字符类型`char`，1种用于表示真值的`boolean`类型。

JAVA中的数值类型（整型和浮点型）不存在无符号的，它们的取值范围是固定的，不会随着机器硬件环境或者操作系统的改变而改变。

整型可分为：`byte、short、int、long`

浮点类型：`float、double`

`char`类型：`char`

`boolean`类型：`boolean`

8种数据类型表示的范围如下：

- `byte`：1字节，8位，最大存储数据量是255，存放的数据范围是**-128~127**之间。
- `short`：2字节，16位，最大数据存储量是65536，数据范围是**-32768~32767**之间。
- `int`：4字节，32位，最大数据存储容量是$2^{32}-1$，数据范围是$-2^{31}\sim2^{31}-1$。
- `long`：8字节，64位，最大数据存储容量是$2^{64}-1$，数据范围为$-2^{63}\sim2^{63}-1$。
- `float`：4字节，32位，数据范围在$\pm 3.40282647E+38F(有效位数6\sim7位)$，直接赋值时**必须在数字后加上f或F，没有后缀的默认位double类型**。
- `double`：8字节，64位，数据范围在$\pm1.797...E+308(有效位数位15位)$，赋值时**可以加d或D也可以不加**。
- `boolean`：只有`true`和`false`两个取值。
- `char`：16位，存储`Unicode`码，用单引号赋值。

# 2. 对应的封装容器类

| 简单类型   | boolean | byte |   char    | short |   Int   | long | float | double | void |
| ---------- | :-----: | :--: | :-------: | :---: | :-----: | :--: | :---: | :----: | :--: |
| 二进制位数 |    1    |  8   |    16     |  16   |   32    |  64  |  32   |   64   |  --  |
| 封装器类   | Boolean | Byte | Character | Short | Integer | Long | Float | Double | Void |

对于数值类型的基本类型的取值范围，我们无需强制去记忆，因为它们的值都已经以常量的形式定义在对应的包装类中了。如：

基本类型byte 二进制位数：Byte.SIZE最小值：Byte.MIN_VALUE最大值：Byte.MAX_VALUE

基本类型short二进制位数：Short.SIZE最小值：Short.MIN_VALUE最大值：Short.MAX_VALUE

基本类型char二进制位数：Character.SIZE最小值：Character.MIN_VALUE最大值：Character.MAX_VALUE

基本类型double 二进制位数：Double.SIZE最小值：Double.MIN_VALUE最大值：Double.MAX_VALUE

# 3. 数据类型之间的转换

## 3.1 自动转换

当一个较"小"数据与一个较"大"的数据一起运算时,系统将自动将"小"数据转换成"大"数据,再进行运算.

下面的语句可以在Java中直接通过：

```java
byte b;int i=b; long l=b; float f=b; double d=b;
```

如果低级类型为char型，向高级类型（整型）转换时，会转换为对应ASCII码值，例如:

```java
char c='c'; int i=c;

System.out.println("output:"+i);  //输出：output:99;
```

对于`byte,short,char`三种类型而言，他们是平级的，因此不能相互自动转换，可以使用下述的强制类型转换:

```java
short i=99 ; char c=(char)i;

System.out.println("output:"+c); //输出：output:c;
```

## 3.2 强制转换

将"大"数据转换为"小"数据时，你可以使用强制类型转换。

即你必须采用下面这种语句格式： `int n=(int)3.14159/2;`可以想象，这种转换肯定可能会导致溢出或精度的下降。

**表达式的数据类型自动提升**, 关于类型的自动提升，注意下面的规则：

1. 所有的`byte,short,char`型的值将被提升为`int`型；
2. 如果有一个操作数是`long`型，计算结果是`long`型；
3. 如果有一个操作数是`float`型，计算结果是`float`型；
4. 如果有一个操作数是`double`型，计算结果是`double`型；

【示例】

```java
byte b; 
b=3; 
b=(byte)(b*3);	//必须声明byte。
```

## 3.3 包装类过渡类型转换

一般情况下，我们首先声明一个变量，然后生成一个对应的包装类，就可以利用包装类的各种方法进行类型转换了。例如：

1. 当希望把`float`型转换为`double`型时：

```java
float f1=100.00f;

Float F1=new Float(f1);

double d1=F1.doubleValue(); //F1.doubleValue()为Float类的返回double值型的方法
```

2. 当希望把`double`型转换为`int`型时：

```java
double d1=100.00;

Double D1=new Double(d1);

int i1=D1.intValue();
```

简单类型的变量转换为相应的包装类，可以利用包装类的构造函数。即：`Boolean(boolean value)、Character(char value)、Integer(int value)、Long(long value)、Float(float value)、Double(double value)`

而在各个包装类中，总有形为`××Value()`的方法，来得到其对应的简单类型数据。利用这种方法，也可以实现不同数值型变量间的转换。

例如，对于一个双精度实型类，`intValue()`可以得到其对应的整型变量，而`doubleValue()`可以得到其对应的双精度实型变量。

## 3.4 字符串与其它类型间的转换

其它类型向字符串的转换：

1. 调用类的串转换方法:`X.toString();`

2. 自动转换`:X+"";`

3. 使用`String`的方法:`String.volueOf(X);`

字符串作为值,向其它类型的转换：

1. 先转换成相应的封装器实例,再调用对应的方法转换成其它类型。

> 例如，字符中"32.1"转换double型的值的格式为:`new Float("32.1").doubleValue()`。也可以用:`Double.valueOf("32.1").doubleValue()`

2. 静态`parseXXX`方法

```java
String s = "1";
byte b = Byte.parseByte( s );
short t = Short.parseShort( s );
int i = Integer.parseInt( s );
long l = Long.parseLong( s );
Float f = Float.parseFloat( s );
Double d = Double.parseDouble( s );
```

3. `Character`的`getNumericValue(char ch)`方法：

## 3.5 Date类与其它数据类型的相互转换

整型和Date类之间并不存在直接的对应关系，只是你可以使用int型为分别表示年、月、日、时、分、秒，这样就在两者之间建立了一个对应关系，在作这种转换时，你可以使用Date类构造函数的三种形式：

1. `Date(int year, int month, int date)`：以int型表示年、月、日

2. `Date(int year, int month, int date, int hrs, int min)`：以int型表示年、月、日、时、分

3. `Date(int year, int month, int date, int hrs, int min, int sec)`：以int型表示年、月、日、时、分、秒

在长整型和Date类之间有一个很有趣的对应关系，就是将一个时间表示为距离格林尼治标准时间1970年1月1日0时0分0秒的毫秒数。对于这种对应关系，Date类也有其相应的构造函数：`Date(long date)`。

获取Date类中的年、月、日、时、分、秒以及星期你可以使用Date类的`getYear()、getMonth()、getDate()、getHours()、getMinutes()、getSeconds()、getDay()`方法，你也可以将其理解为将Date类转换成int。

而Date类的`getTime()`方法可以得到我们前面所说的一个时间对应的长整型数，与包装类一样，Date类也有一个`toString()`方法可以将其转换为`String`类。

# 4. 参考资料

- https://www.cnblogs.com/doit8791/archive/2012/05/25/2517448.html
- 《Java核心技术卷I》