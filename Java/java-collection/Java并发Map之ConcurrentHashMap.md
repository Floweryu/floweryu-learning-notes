# 1. `ConcurrentHashMap`特性

`ConcurrentHashMap` 是线程安全的 `HashMap` ，用于替代 `HashTable`

```java
public interface ConcurrentMap<K, V> extends Map<K, V> {}

public class ConcurrentHashMap<K,V> extends AbstractMap<K,V>
    implements ConcurrentMap<K,V>, Serializable {}
```

`ConcurrentHashMap`实现了`ConcurrentMap`接口，而`ConcurrentMap`接口又继承了`Map`接口。

`ConcurrentHashMap` 没有实现对 `Map` 加锁以提供独占访问。因此无法通过在客户端加锁的方式来创建新的原子操作。但是，一些常见的复合操作，如：“若没有则添加”、“若相等则移除”、“若相等则替换”，都已经实现为原子操作，并且是围绕 `ConcurrentMap` 的扩展接口而实现。

```java
public interface ConcurrentMap<K, V> extends Map<K, V> {

    // 仅当 K 没有相应的映射值才插入
    V putIfAbsent(K key, V value);

    // 仅当 K 被映射到 V 时才移除
    boolean remove(Object key, Object value);

    // 仅当 K 被映射到 oldValue 时才替换为 newValue
    boolean replace(K key, V oldValue, V newValue);

    // 仅当 K 被映射到某个值时才替换为 newValue
    V replace(K key, V value);
}
```

# 2. `ConcurrentHashMap`实现原理

## 2.1 Java 1.7的实现

   - 数据结构：**数组＋单链表**
   - 并发机制：采用分段锁机制细化锁粒度，降低阻塞，从而提高并发性。

关于分段锁，`ConcurrentHashMap`有3个参数：

> `ConcurrentHashMap`采用了非常精妙的"分段锁"策略，`ConcurrentHashMap`的主干是个`Segment`数组。
>
> `Segment`继承了`ReentrantLock`，所以它就是一种可重入锁。在`ConcurrentHashMap`中，一个`Segment`就是一个子哈希表，`Segment`里维护了一个`HashEntry`数组，并发环境下，对于不同`Segment`的数据进行操作是不用考虑锁竞争的。
>
> 所以，对于同一个`Segment`的操作才需考虑线程同步，不同的`Segment`则无需考虑。
>
> `Segment`类似于`HashMap`，一个`Segment`维护着一个`HashEntry`数组。
>
> `HashEntry`是目前提到的最小的逻辑处理单元了。一个`ConcurrentHashMap`维护一个`Segment`数组，一个`Segment`维护一个`HashEntry`数组。

## 2.2  Java 1.8的实现

- 数据结构：**数组＋单链表＋红黑树**
- 并发机制：**取消分段锁，之后基于 CAS + synchronized 实现**。


- **数据结构改进**：与 HashMap 一样，将原先 `数组＋单链表` 的数据结构，变更为 `数组＋单链表＋红黑树` 的结构。当出现哈希冲突时，数据会存入数组指定桶的单链表，**当链表长度达到 8，则将其转换为红黑树结构，长度为6时，又会转换为链表**，这样其查询的时间复杂度可以降低到 $O(logN)$，以改进性能（因为链表的查询性能较差，改成红黑树查询效率更高）
- **并发机制改进**：
  - 取消 `segments` 字段，直接采用 `transient volatile HashEntry<K,V>[] table` 保存数据，采用 `table` 数组元素作为锁，从而实现了对每一行数据进行加锁，进一步减少并发冲突的概率。
  - 使用 `CAS + sychronized` 操作，在特定场景进行无锁并发操作。使用 `Unsafe`、`LongAdder` 之类底层手段，进行极端情况的优化。现代 JDK 中，`synchronized` 已经被不断优化，可以不再过分担心性能差异，另外，相比于 `ReentrantLock`，它可以减少内存消耗，这是个非常大的优势。

# 3. 参考资料

- https://cloud.tencent.com/developer/article/1509556
- https://dunwu.github.io/javacore/concurrent