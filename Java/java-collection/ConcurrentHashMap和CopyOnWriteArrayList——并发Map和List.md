# 一、并发容器简介
|并发容器|对应的普通容器 |描述 |
|--|--|--|
| `ConcurrentHashMap` | `HashMap` | Java 1.8 之前采用分段锁机制细化锁粒度，降低阻塞，从而提高并发性；Java 1.8 之后基于 CAS 实现。|
|`ConcurrentSkipListMap` |`SortedMap	` | `基于跳表实现的`|
|`CopyOnWriteArrayList` |`ArrayList` | |
|`CopyOnWriteArraySet` |`Set` |基于 `CopyOnWriteArrayList `实现。|
|`ConcurrentSkipListSet` |`SortedSet` | 基于 `ConcurrentSkipListMap` 实现。|
|`ConcurrentLinkedQueue` |`Queue` | 线程安全的无界队列。底层采用单链表。支持 FIFO。|
|`ConcurrentLinkedDeque` |`Deque` | 线程安全的无界双端队列。底层采用双向链表。支持 FIFO 和 FILO。|
|`ArrayBlockingQueue` |`Queue` | 数组实现的阻塞队列。|
|`LinkedBlockingQueue` |`Queue` | 链表实现的阻塞队列。|
|`LinkedBlockingDeque` |`Deque` |	双向链表实现的双端阻塞队列。|

 - `Concurrent*`
   - 这类型的锁竞争相对于 `CopyOnWrite*` 要高一些，但写操作代价要小一些。
   - 此外，`Concurrent*` 往往提供了较低的遍历一致性，即：当利用迭代器遍历时，如果容器发生修改，迭代器仍然可以继续进行遍历。代价就是，在获取容器大小 `size()` ，容器是否为空等方法，不一定完全精确，但这是为了获取并发吞吐量的设计取舍，可以理解。与之相比，如果是使用同步容器，就会出现` fail-fast `问题，即：检测到容器在遍历过程中发生了修改，则抛出 `ConcurrentModificationException`，不再继续遍历。
 - `CopyOnWrite*`：一个线程写，多个线程读。读操作时不加锁，写操作时通过在副本上加锁保证并发安全，空间开销较大。
 - `Blocking*`：内部实现一般是基于锁，提供阻塞队列的能力。

## 1.1 并发场景下的`Map`
如果对数据有强一致要求，则需使用 `Hashtable`；在大部分场景通常都是弱一致性的情况下，使用 `ConcurrentHashMap` 即可；如果数据量在千万级别，且存在大量增删改操作，则可以考虑使用 `ConcurrentSkipListMap`

## 1.2 并发场景下的 `List`
读多写少用 `CopyOnWriteArrayList`。

写多读少用 `ConcurrentLinkedQueue` ，但由于是无界的，要有容量限制，避免无限膨胀，导致内存溢出。 

# 二、 `Map`
`Map` 接口的两个实现是 `ConcurrentHashMap` 和 `ConcurrentSkipListMap`，它们从应用的角度来看，主要区别在于`ConcurrentHashMap` 的 `key` 是无序的，而 `ConcurrentSkipListMap` 的` key `是有序的。所以如果你需要保证` key `的顺序，就只能使用` ConcurrentSkipListMap`。

使用 `ConcurrentHashMap` 和 `ConcurrentSkipListMap` 需要注意的地方是，它们的` key `和` value` 都不能为空，否则会抛出NullPointerException这个运行时异常.

## 2.1 `ConcurrentHashMap`
`ConcurrentHashMap` 是线程安全的` HashMap` ，用于替代 `Hashtable`。
### ConcurrentHashMap 的原理
#### Java 1.7
   - 数据结构：**数组＋单链表**
   - 并发机制：采用分段锁机制细化锁粒度，降低阻塞，从而提高并发性。

分段锁，是将内部进行分段（Segment），里面是 `HashEntry` 数组，和 `HashMap` 类似，哈希相同的条目也是以链表形式存放。 `HashEntry` 内部使用 `volatile` 的 `value `字段来保证可见性，也利用了不可变对象的机制，以改进利用 `Unsafe` 提供的底层能力，比如 `volatile access`，去直接完成部分操作，以最优化性能，毕竟` Unsafe `中的很多操作都是 JVM intrinsic 优化过的。
####  Java 1.8

- 数据结构：**数组＋单链表＋红黑树**
- 并发机制：取消分段锁，之后基于 CAS + synchronized 实现。


- **数据结构改进**：与 HashMap 一样，将原先 `数组＋单链表` 的数据结构，变更为 `数组＋单链表＋红黑树` 的结构。当出现哈希冲突时，数据会存入数组指定桶的单链表，**当链表长度达到 8，则将其转换为红黑树结构**，这样其查询的时间复杂度可以降低到 $O(logN)$，以改进性能（因为链表的查询性能较差，改成红黑树查询效率更高）
- **并发机制改进**：
	- 取消 `segments` 字段，直接采用 `transient volatile HashEntry<K,V>[] table` 保存数据，采用 `table` 数组元素作为锁，从而实现了对每一行数据进行加锁，进一步减少并发冲突的概率。
	- 使用 `CAS + sychronized` 操作，在特定场景进行无锁并发操作。使用 `Unsafe`、`LongAdder` 之类底层手段，进行极端情况的优化。现代 JDK 中，`synchronized` 已经被不断优化，可以不再过分担心性能差异，另外，相比于 `ReentrantLock`，它可以减少内存消耗，这是个非常大的优势。

# 三、`List`
## 3.1 `CopyOnWriteArrayList`
`CopyOnWriteArrayList` 是线程安全的 `ArrayList`。`CopyOnWrite` 字面意思为**写的时候会将共享变量新复制一份出来**。**复制的好处在于读操作是无锁的**·（也就是无阻塞）。

`CopyOnWriteArrayList` 仅适用于写操作非常少的场景，而且能够容忍读写的短暂不一致。如果读写比例均衡或者有大量写操作的话，使用 `CopyOnWriteArrayList` 的性能会非常糟糕。

### CopyOnWriteArrayList 原理
`CopyOnWriteArrayList` 内部维护了一个数组，成员变量` array `就指向这个内部数组，所有的读操作都是基于 `array` 进行的，如下图所示，迭代器` Iterator` 遍历的就是 `array` 数组。

- `lock` - 执行写时复制操作，需要使用可重入锁加锁
- `array` - 对象数组，用于存放元素

```java
    /** The lock protecting all mutators */
    final transient ReentrantLock lock = new ReentrantLock();

    /** The array, accessed only via getArray/setArray. */
    private transient volatile Object[] array;
```

### 读操作
在 `CopyOnWriteAarrayList `中，读操作不同步，因为它们在内部数组的快照上工作，所以多个迭代器可以同时遍历而不会相互阻塞.

`CopyOnWriteArrayList `的读操作是不用加锁的，性能很高。

```java
public E get(int index) {
    return get(getArray(), index);
}
private E get(Object[] a, int index) {
    return (E) a[index];
}
```
### 写操作
所有的写操作都是同步的。他们**在备份数组的副本上工作**。写操作完成后，后备阵列将被替换为复制的阵列，并释放锁定。支持数组变得易变，所以替换数组的调用是原子.

写操作后创建的迭代器将能够看到修改的结构。

写时复制集合返回的迭代器不会抛出 `ConcurrentModificationException`，因为它们在数组的快照上工作，并且无论后续的修改如何，都会像迭代器创建时那样完全返回元素。

**添加操作** ：添加的逻辑很简单，先将原容器` copy `一份，然后在新副本上执行写操作，之后再切换引用。当然此过程是要加锁的。

```java
public boolean add(E e) {
    //ReentrantLock加锁，保证线程安全
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        //拷贝原容器，长度为原容器长度加一
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        //在新副本上执行添加操作
        newElements[len] = e;
        //将原容器引用指向新副本
        setArray(newElements);
        return true;
    } finally {
        //解锁
        lock.unlock();
    }
}
```
**删除操作** ： 删除操作同理，将除要删除元素之外的其他元素拷贝到新副本中，然后切换引用，将原容器引用指向新副本。同属写操作，需要加锁。

```java
public E remove(int index) {
    //加锁
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        E oldValue = get(elements, index);
        int numMoved = len - index - 1;
        if (numMoved == 0)
            //如果要删除的是列表末端数据，拷贝前len-1个数据到新副本上，再切换引用
            setArray(Arrays.copyOf(elements, len - 1));
        else {
            //否则，将除要删除元素之外的其他元素拷贝到新副本中，并切换引用
            Object[] newElements = new Object[len - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index,
                              numMoved);
            setArray(newElements);
        }
        return oldValue;
    } finally {
        //解锁
        lock.unlock();
    }
}
```

***

#### 转载自：
h[ttps://dunwu.github.io/javacore/concurrent/java-concurrent-container.html#_4-1-copyonwritearraylist](https://dunwu.github.io/javacore/concurrent/java-concurrent-container.html#_4-1-copyonwritearraylist)
