### 一、`synchronized`关键字的了解

`synchronized`解决的是多个线程之间访问资源的同步性，`synchronized`可以保证被它修饰的方法或代码块在任意时刻只能有一个线程执行。

### 二、`synchronized`关键字主要的使用方式

- **修饰实例方法**：作用于当前对象实例加锁，进入同步代码前要获得当前对象实例的锁
- **修饰静态方法**：给当前类加锁，会作用于类的所有对象实例，因为静态成员不属于任何一个实例对象（`static`表明这是该类的一个静态资源，不管`new`了多少个对象，只有一份）。所以，如果一个线程A调用一个实例对象的非静态方法，而线程B需要调用这个对象所属类的静态`synchronized`方法，是允许的，不会发生互斥现象。**因为访问静态`synchronized`方法占用的锁是当前类的锁，而访问非静态`synchronized`方法占用的是当前实例对象的锁，两者占用的锁不一致。**
- **修饰代码块**：指定加锁对象，对给定对象加锁，进入同步代码库前要获得给定对象的锁。

#### 总结：

`synchronized`关键字加到`static`静态方法和`synchronized(class)`加到代码块上都是给Class类加锁。`synchronized`关键字加到实例方法上是给对象实例加上锁。尽量不要用`synchronized(String a)`，因为，在JVM，字符串常量有缓存功能。
#### 具体使用：

**双重校验锁实现对象单例**
```java
public class App {
    private volatile static App uniqueInstance;

    private App() {

    }

    public synchronized static App getUniqueInstance() {
        // 先判断对象是否被实例过
        if (uniqueInstance == null) {
            // 类对象加锁
            synchronized (App.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new App();
                }
            }
        }
        return uniqueInstance;
    }
}

```
需要注意 `uniqueInstance` 采⽤` volatile` 关键字修饰也是很有必要。

`uniqueInstance = new Singleton();`

这段代码其实是分为三步执⾏：
1. 为 `uniqueInstance` 分配内存空间
2. 初始化 `uniqueInstance`
3. 将 `uniqueInstance` 指向分配的内存地址

但是由于 JVM 具有**指令重排**的特性，执⾏顺序有可能变成 `1>3>2`。指令重排在单线程环境下不会出现问题，但是在多线程环境下会导致⼀个线程获得还没有初始化的实例。例如，线程 T1 执⾏了 1 和 3，此时 T2 调⽤ getUniqueInstance() 后发现 `uniqueInstance` 不为空，因此返回`uniqueInstance`，但此时 `uniqueInstance` 还未被初始化。

**使⽤ `volatile` 可以禁⽌ JVM 的指令重排**，保证在多线程环境下也能正常运⾏。

### 三、`synchronized`关键字的底层原理
#### 1. `synchronized`同步语句块情况
```java
public class App {
    public static void main(String[] args) {
        App app = new App();
        app.method();
    }

    public void method() {
        synchronized (this) {
            System.out.println("synchronized 代码块");
        }
    }
}
```
先执行`javac App.java`命令编译生成字节码`.class`文件，然后执行`javap -c -s -v -l App.class`可以查看具体信息：

> 相关命令可输入 javac -help 和 javap -help 查看

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201114102211473.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MzIwNzAyNQ==,size_16,color_FFFFFF,t_70#pic_center)

从上面可以看出：
`synchronized`同步语句块使用的是`monitorenter`和`monitorexit`指令，其中，**`monitorenter`指令指向同步代码块的开始位置，`monitorexit`指令指明同步代码块的结束位置。**

当执行`monitorenter`指令时，线程试图获取锁`monitor(存在于每个java对象的对象头中，synchronized锁就是通过这种方式获取锁的，也是为什么Java中任意对象可以作为锁的原因)`的所有权。当计数器为0时可以成功获取，获取后将锁计数器设为1也就是加1.在执行`monitorexit`后将锁计数器设为0，表面锁被释放。如果获取对象失败，则当前线程就要阻塞等待，直到锁被另一个线程释放为止。

#### 2. `synchronized`修饰方法情况
```java
public class App {
    public static void main(String[] args) {
        App app = new App();
        app.method_two();
    }

    public synchronized void method_two() {
        System.out.println("synchronized 代码块");
    }
}

```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201114103446964.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MzIwNzAyNQ==,size_16,color_FFFFFF,t_70#pic_center)
修饰方法时，并没有`monitorenter`和`monitorexit`指令，而是`ACC_SYNCHRONIZED`标识，该标识指明该方法是一个同步方法。JVM通过该`ACC_SYNCHRONIZED`访问标志来辨别一个方法是否声明为同步方法，从而执行相应的同步调用。

### 四、`synchronized`和`Lock`的区别
`synchronized`无法通过**破坏不可抢占条件**来避免死锁。原因是`synchronized`申请资源的时候，如果申请不到，线程已经进入阻塞状态，无法做任何事情，释放不了已经占有的资源。

而`Lock`**提供了一组无条件、可轮询、定时的以及可中断的锁操作，所有获取锁、释放锁的操作都是显示操作。**

- **能响应中断**：`synchronized`的问题是，当持有锁A后，如果尝试获取锁B失败，那么线程就会进入阻塞状态，就会发生死锁，而且没有任何机会来唤醒阻塞的线程。但是如果阻塞状态的线程能够响应中断信号，即给阻塞线程发送中断信号，能够唤醒它，那这个阻塞线程的就有机会释放曾经持有的锁A，这样就破坏了不可抢占条件。
- **支持超时**：如果线程在一段时间内没有获得锁，不是进入阻塞状态，而是返回一个错误，那么这个线程也有机会释放曾经获得的锁，这样也可以破坏不可抢占条件。
- **非阻塞的获取锁**：如果尝试获取锁失败，并不进入阻塞状态，而是直接返回，这个线程也有机会释放曾经获取的锁，也可以破坏不可抢占条件。

### 五、`synchronized`和`ReentrantLock`的区别
#### 1. 两者都是可重入锁
**可重入锁**：自己可以再次获取自己的内部锁。比如一个线程获取了某个对象的锁，此时这个对象锁还没有释放，当其再次想要获取这个对象的锁的时候还是可以获取到的。如果不可锁重入的话，就会造成死锁。同一个线程每次获取锁，锁的计数器就会加1，要等到锁的计数器下降为0的时候才能释放锁。

#### 2. `synchronized`依赖于`JVM`而`ReentrantLock`依赖于`API`

`synchronized 是依赖于 JVM 实现的`，并没有直接暴露给我们。

`ReentrantLock` 是 JDK 层⾯实现的（也就是 API 层⾯，需要 `lock() 和 unlock()` ⽅法配合`try/finally` 语句块来完成），所以可以通过查看它的源代码，来看它是如何实现的。

#### 3. `ReentrantLock` ⽐ `synchronized` 增加了⼀些⾼级功能
##### (1). 等待可中断：
`ReentrantLock`提供了⼀种能够中断等待锁的线程的机制，通过`lock.lockInterruptibly()`来实现这个机制。也就是说正在等待的线程可以选择放弃等待，改为处理其他事情

##### (2). 可实现公平锁:
`ReentrantLock`可以**指定是公平锁还是⾮公平锁**。⽽`synchronized`只能是**⾮公平锁**。**所谓的公平锁就是先等待的线程先获得锁**。 `ReentrantLock`默认情况是⾮公平的，可以通过 `ReentrantLock`类的 `ReentrantLock(boolean fair) `构造⽅法来制定是否是公平的。

##### (3). 可实现选择性通知（锁可以绑定多个条件）
`synchronized`关键字与`wait()`和`notify()/notifyAll()`⽅法相结合可以实现等待/通知机制，`ReentrantLock`类当然也可以实现，但是需要借助于`Condition`接⼝与`newCondition() `⽅法。

线程对象可以注册在指定的`Condition`中，从⽽可以**有选择性的进⾏线程通知**，在调度线程上更加灵活。 **在使⽤`notify()/notifyAll()`⽅法进⾏通知时，被通知的线程是由 JVM 选择的，⽤ReentrantLock类结合Condition实例可以实现“选择性通知”**,这个功能是`Condition接⼝`默认提供的.

⽽`synchronized`关键字就相当于整个`Lock`对象中只有⼀个`Condition`实例，所有的线程都注册在它⼀个身上。如果执⾏`notifyAll()`⽅法的话就会通知所有处于等待状态的线程这样会造成很⼤的效率问题，⽽`Condition`实例的`signalAll()`⽅法 只会唤醒注册在该`Condition`实例中的所有等待线程。
