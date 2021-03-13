# 1. 抽象类

## 1.1 抽象方法

抽象方法是一种特殊的方法：**它只有声明，而没有具体的实现。**

抽象方法的声明格式为：

```java
abstract void fun();
```

抽象方法必须用abstract关键字进行修饰。

## 1.2 抽象类理解

如果一个类含有抽象方法，则称这个类为抽象类，抽象类必须在类前用`abstract`关键字修饰。

因为抽象类中含有无具体实现的方法，所以不能用抽象类创建对象。

抽象类就是为了继承而存在的。对于一个父类，如果它的某个方法在父类中实现出来没有任何意义，必须根据子类的实际需求来进行不同的实现，那么就可以将这个方法声明为`abstract`方法，此时这个类也就成为abstract类了。

包含抽象方法的类称为抽象类，但**并不意味着抽象类中只能有抽象方法**，它和普通类一样，同样**可以拥有成员变量和普通的成员方法**。

抽象类和普通类的主要有三点区别：

1. 抽象方法必须为`public`或者`protected`（因为如果为`private`，则不能被子类继承，子类便无法实现该方法），缺省情况下默认为`public`。
2. 抽象类不能用来创建对象。
3. 如果一个类继承于一个抽象类，则子类必须实现父类的抽象方法。如果子类没有实现父类的抽象方法，则必须将子类也定义为为`abstract`类。

# 2. 接口

接口中可以含有 变量和方法。

接口中的变量会被隐式地指定为`public static final`变量（并且只能是`public static final`变量，用`private`修饰会报编译错误），

接口中的方法会被隐式地指定为`public abstract`方法且只能是`public abstract`方法（用其他关键字，比如`private、protected、static、 final`等修饰会报编译错误），并且接口中所有的方法不能有具体的实现，也就是说，接口中的方法必须都是抽象方法。

要让一个类遵循某组特地的接口需要使用`implements`关键字，具体格式如下：

```java
class ClassName implements Interface1,Interface2,[....]{
    
}
```

# 3. 抽象类与接口的区别

**语法层面上的区别：**

1. 抽象类可以提供成员方法的实现细节，而接口中只能存在`public abstract `方法；
2. 抽象类中的成员变量可以是各种类型的，而接口中的成员变量只能是`public static final`类型的；
3. 接口中不能含有静态代码块以及静态方法，而抽象类可以有静态代码块和静态方法；
4. 一个类只能继承一个抽象类，而一个类却可以实现多个接口。

**设计层面上的区别：**

1. 抽象类是对一种事物的抽象，即对类抽象，而接口是对行为的抽象。
2. 设计层面不同，抽象类作为很多子类的父类，它是一种模板式设计。而接口是一种行为规范，它是一种辐射式设计。

【看看下面例子】：

门和警报的例子：门都有`open( )`和`close( )`两个动作，此时我们可以定义通过抽象类和接口来定义这个抽象概念：

```java
abstract class Door {
    public abstract void open();
    public abstract void close();
}
```

或者

```java
interface Door {
    public abstract void open();
    public abstract void close();
}
```

现在如果我们需要门具有报警alarm( )的功能，那么该如何实现？下面提供两种思路：

1. 将这三个功能都放在抽象类里面，但是这样一来所有继承于这个抽象类的子类都具备了报警功能，但是有的门并不一定具备报警功能；
2. 将这三个功能都放在接口里面，需要用到报警功能的类就需要实现这个接口中的open( )和close( )，也许这个类根本就不具备open( )和close( )这两个功能，比如火灾报警器。

可以看出， Door的`open()` 、`close()`和`alarm()`根本就属于两个不同范畴内的行为，`open()`和`close()`属于门本身固有的行为特性，而`alarm()`属于延伸的附加行为。因此最好的解决办法是单独将报警设计为一个接口，包含`alarm()`行为。

Door设计为单独的一个抽象类，包含`open`和`close`两种行为。再设计一个报警门继承Door类和实现`Alarm`接口。

```java
interface Alram {
    void alarm();
}
 
abstract class Door {
    void open();
    void close();
}
 
class AlarmDoor extends Door implements Alarm {
    void oepn() {
      //....
    }
    void close() {
      //....
    }
    void alarm() {
      //....
    }
}
```

# 4. 参考资料

- https://www.cnblogs.com/dolphin0520/p/3811437.html