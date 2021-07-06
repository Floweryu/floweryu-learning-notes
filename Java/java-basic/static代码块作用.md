关于静态代码块：
- 随着类的加载而执行，只执行一次，并且优于主函数。
- 静态代码块是给类初始化的，构造代码块是给对象初始化的。
- 静态代码块中的变量是局部变量，与普通函数中的局部变量性质没有区别。

在类的加载过程中，`static`修饰的遍历会被初始化为0，然后再进行赋值。

```java
public class Four {
    private static int num = 1;

    static {
        num = 10;
        number = 50;
        System.out.println(num);
        System.out.println(number);         //报错： 非法前向引用，这里只能赋值，不能引用
    }

    private static int number = 5;      // linking之prepare, number = 0  --> inital 1

    public static void main(String[] args) {
        System.out.println(Four.num);
        System.out.println(Four.number);
    }
}

```

