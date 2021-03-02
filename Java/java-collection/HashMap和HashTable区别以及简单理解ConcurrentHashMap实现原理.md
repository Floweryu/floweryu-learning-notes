**HashMap**：HashMap是线程不安全的。在并发环境中，可能会出现环状链表。**原因？？？？**
**HashTable**：HashTable和HashMap实现的原理几乎一样。差别：
1. HashTable不允许key和value为null
2. HashTable是线程安全的。但是其代价很大。get/put相关的操作都是`synchronized`的，多线程访问的时候，只要有一个线程访问或操作该对象，那其他线程只能阻塞，相当于将所有的线程串行化，这会导致性能非常差。

`HashTable`性能差主要是由于所有操作需要竞争同一把锁，而**如果容器中有多把锁，每一把锁锁一段数据，这样在多线程访问时不同段的数据时，就不会存在锁竞争了**，这样便可以有效地提高并发效率。这就是`ConcurrentHashMap`所采用的"**分段锁**"思想。

### `ConcurrentHashMap`
`ConcurrentHashMap`采用了非常精妙的"分段锁"策略，`ConcurrentHashMap`的主干是个`Segment`数组。

`Segment`继承了`ReentrantLock`，所以它就是一种可重入锁。在`ConcurrentHashMap`中，一个`Segment`就是一个子哈希表，`Segment`里维护了一个`HashEntry`数组，并发环境下，对于不同`Segment`的数据进行操作是不用考虑锁竞争的。

所以，对于同一个`Segment`的操作才需考虑线程同步，不同的`Segment`则无需考虑。

`Segment`类似于`HashMap`，一个`Segment`维护着一个`HashEntry`数组。

`HashEntry`是目前提到的最小的逻辑处理单元了。一个`ConcurrentHashMap`维护一个`Segment`数组，一个`Segment`维护一个`HashEntry`数组。
