答案是不能被继承。下面来看看究竟是为什么？

Java中对`String`类的定义：

```java
public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
    /** The value is used for character storage. */
    private final char value[];

    /** Cache the hash code for the string */
    private int hash; // Default to 0

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -6849794470754667710L;
    
     public String(char value[], int offset, int count) {
        if (offset < 0) {
            throw new StringIndexOutOfBoundsException(offset);
        }
        if (count <= 0) {
            if (count < 0) {
                throw new StringIndexOutOfBoundsException(count);
            }
            if (offset <= value.length) {
                this.value = "".value;
                return;
            }
        }
        // Note: offset or count might be near -1>>>1.
        if (offset > value.length - count) {
            throw new StringIndexOutOfBoundsException(offset + count);
        }
        this.value = Arrays.copyOfRange(value, offset, offset+count);
    }

```
可见，它是被`final`修饰的，是一个被`final`修饰的`char`数组。**而`final`修饰的类是不能被继承的**。

### `final`修饰符的用法
#### 1. 修饰类
当一个类被`final`修饰时，表明这个类不能被继承。被`final`关键字修饰的类中的成员变量可以根据需要设置为`final`，但是要注意`final`类中的所有成员方法都会被隐式的地指定为`final`方法。

#### 2. 修饰方法
当一个方法被`final`修饰时，父类的该方法不能被子类所覆盖。

使用`final`方法的原因有两个：
1. 把方法锁定，以防止任何继承类修改它的含义
2. 效率。在早期的Java实现版本中，会将final方法转为内嵌调用。但是如果方法过于庞大，可能看不到内嵌调用带来的任何性能提升。在最近的Java版本中，不需要使用final方法进行这些优化了。

#### 3. 修饰变量
当`final`关键字修饰变量时，表示该变量是常量，在初始化的时候要赋值，并且只能赋值一次，初始化之后不能被修改。

如果是基本数据类型的变量，数值在初始化后便不能修改。如果是引用类型的变量，在对其初始化后就不能让其指向另一个变量，但是它指向的内容是可变的。

当用final作用于类的成员变量时，成员变量必须在声明时或者构造器中进行初始化赋值，否则会报错，而局部变量只需要在使用之前被初始化赋值即可。

### 	`final`和`static`的区别
`static`作用于成员变量用来表示只保存一份副本，而`final`的作用是用来保证变量不可变。
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020111515285339.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MzIwNzAyNQ==,size_16,color_FFFFFF,t_70#pic_center)
可以看出，用`static`修饰的变量，在任何实例中都是不变的。甚至编译器会提示一个警告，要求使用`Myclass.staticVal`来获取`static`变量修饰的值。

而用`final`修饰的变量，不同的实例不一样，但在每个实例中它们是不变的。
