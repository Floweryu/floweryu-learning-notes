# 为什么需要线程同步？

java允许多线程并发控制，当多个线程同时操作一个可共享的资源变量时将会导致数据不准确，相互之间产生冲突，因此加入同步锁以避免在该线程没有完成操作之前，被其他线程的调用，从而保证了该变量的唯一性和准确性。

# 1. 同步方法

即有`synchronized`关键字修饰的方法。

由于java的每个对象都有一个内置锁，当用此关键字修饰方法时，内置锁会保护整个方法。在调用该方法前，需要获得内置锁，否则就处于阻塞状态。

```java
 public synchronized void save(){}
```

`synchronized`关键字也可以修饰静态方法，此时如果调用该静态方法，将会锁住整个类。

# 2. 同步代码块

即有`synchronized`关键字修饰的语句块。被该关键字修饰的语句块会自动被加上内置锁，从而实现同步。

```java
synchronized(object) {}
```

同步是一种高开销的操作，因此应该尽量减少同步的内容。通常没有必要同步整个方法，使用`synchronized`代码块同步关键代码即可。

# 3. 使用特殊域变量(volatile)实现线程同步

1. `volatile`关键字为域变量的访问提供了一种免锁机制
2. 使用`volatile`修饰域相当于告诉虚拟机该域可能会被其他线程更新
3. 因此每次使用该域就要重新计算，而不是使用寄存器中的值
4. `volatile`不会提供任何原子操作，它也不能用来修饰`final`类型的变量

多线程中的非同步问题主要出现在对域的读写上，如果让域自身避免这个问题，则就不需要修改操作该域的方法。用`final`域，有锁保护的域和`volatile`域可以避免非同步的问题。

# 4. 使用重入锁实现线程同步

`ReentrantLock`类是可重入、互斥、实现了Lock接口的锁，它与使用`synchronized`方法和快具有相同的基本行为和语义，并且扩展了其能力。

ReenreantLock类的常用方法有：

- `ReentrantLock()`: 创建一个`ReentrantLock`实例
- `lock()` : 获得锁
- `unlock()` : 释放锁

`ReentrantLock()`还有一个可以创建公平锁的构造方法，但由于能大幅度降低程序运行效率，不推荐使用

# 5. 使用局部变量实现线程同步

如果使用`ThreadLocal`管理变量，则每一个使用该变量的线程都获得该变量的副本，副本之间相互独立，这样每一个线程都可以随意修改自己的变量副本，而不会对其他线程产生影响。

`ThreadLocal `类的常用方法:

- `ThreadLocal()` : 创建一个线程本地变量
- `get() `: 返回此线程局部变量的当前线程副本中的值
- `initialValue() `: 返回此线程局部变量的当前线程的"初始值"

- `set(T value) `: 将此线程局部变量的当前线程副本中的值设置为`value`

`ThreadLocal`与`同步机制`:

1. `ThreadLocal`与同步机制都是为了解决多线程中相同变量的访问冲突问题。
2. 前者采用以"空间换时间"的方法，后者采用以"时间换空间"的方式

# 6. 使用阻塞队列实现线程同步

主要是使用**LinkedBlockingQueue**来实现线程的同步，`LinkedBlockingQueue<E>`是一个基于已连接节点的，范围任意的`blocking queue`，队列是先进先出的顺序（FIFO）。

**LinkedBlockingQueue 类常用方法** ：

- `LinkedBlockingQueue()` : 创建一个容量为`Integer.MAX_VALUE`的`LinkedBlockingQueue `
- `put(E e) `: 在队尾添加一个元素，如果队列满则阻塞
- `size()` : 返回队列中的元素个数
- `take()` : 移除并返回队头元素，如果队列空则阻塞 

# 7. 使用原子变量实现线程同步

需要使用线程同步的根本原因在于对普通变量的操作不是原子的.

原子操作就是指将读取变量值、修改变量值、保存变量值看成一个整体来操作。即这几种行为要么同时完成，要么都不完成。

在`java`的**util.concurrent.atomic包中提供了创建了原子类型变量的工具类**，使用该类可以简化线程同步。

其中**AtomicInteger** 表可以用原子方式更新`int`的值，可用在应用程序中(如以原子方式增加的计数器)，
但不能用于替换`Integer`；可扩展`Number`，允许那些处理机遇数字类的工具和实用工具进行统一访问。

**AtomicInteger类常用方法：**

- `AtomicInteger(int initialValue)` : 创建具有给定初始值的新的`AtomicInteger`
- `addAddGet(int dalta)` : 以原子方式将给定值与当前值相加
- `get()` : 获取当前值



# 参考资料

- https://www.cnblogs.com/xhjt/p/3897440.html