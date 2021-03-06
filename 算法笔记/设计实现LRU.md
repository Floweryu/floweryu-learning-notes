# 1. 单线程版本

使用`LinkedHashMap`数据结构

```java
import java.util.*;

public class Solution {
    /**
     * lru design
     * @param operators int整型二维数组 the ops
     * @param k int整型 the k
     * @return int整型一维数组
     */
    public int[] LRU (int[][] operators, int k) {
        // write code here
        Map<Integer, Integer> map = new LinkedHashMap<>();
        List<Integer> list = new LinkedList<>();
        for (int[] operator : operators) {
            int key = operator[1];
            switch(operator[0]) {
                case 1:
                    int value = operator[2];
                    if (map.size() < k) {
                        map.put(key, value);
                    } else {
                        Iterator it = map.keySet().iterator();
                        map.remove(it.next());
                        map.put(key, value);
                    }
                    break;
                case 2:
                    if (map.containsKey(key)) {
                        int val = map.get(key);
                        list.add(val);
                        map.remove(key);
                        map.put(key, val);
                    } else {
                        list.add(-1);
                    }
                    break;
                default:
            }
        }
        int[] res = new int[list.size()];
        int i = 0;
        for (int val : list) {
            res[i++] = val;
        }
        return res;
    }
}
```

# 2. 多线程版本

## 2.1 实现方法

`ConcurrentHashMap` + `ConcurrentLinkedQueue` +`ReadWriteLock`

### **ConcurrentLinkedQueue简单介绍**

**ConcurrentLinkedQueue是一个基于单向链表的无界无锁线程安全的队列，适合在高并发环境下使用，效率比较高。** 我们在使用的时候，可以就把它理解为我们经常接触的数据结构——队列，不过是增加了多线程下的安全性保证罢了。**和普通队列一样，它也是按照先进先出(FIFO)的规则对接点进行排序。** 另外，队列元素中不可以放置null元素。

`ConcurrentLinkedQueue中`最主要的两个方法是：`offer(value)`和`poll()`，分别实现队列的两个重要的操作：入队和出队(`offer(value)`等价于 `add(value)`)。

我们添加一个元素到队列的时候，它会添加到队列的尾部，当我们获取一个元素时，它会返回队列头部的元素。

利用`ConcurrentLinkedQueue`队列先进先出的特性，每当我们 `put`/`get`(缓存被使用)元素的时候，我们就将这个元素存放在队列尾部，这样就能保证队列头部的元素是最近最少使用的。

### **ReadWriteLock简单介绍**

`ReadWriteLock` 是一个接口，位于`java.util.concurrent.locks`包下，里面只有两个方法分别返回读锁和写锁：

```text
public interface ReadWriteLock {
    /**
     * 返回读锁
     */
    Lock readLock();

    /**
     * 返回写锁
     */
    Lock writeLock();
}
```

`ReentrantReadWriteLock` 是`ReadWriteLock`接口的具体实现类。

**读写锁还是比较适合缓存这种读多写少的场景。读写锁可以保证多个线程和同时读取，但是只有一个线程可以写入。但是，有一个问题是当读锁被线程持有的时候，读锁是无法被其它线程申请的，会处于阻塞状态，直至读锁被释放。**

另外，**同一个线程持有写锁时是可以申请读锁，但是持有读锁的情况下不可以申请写锁。**

### **ScheduledExecutorService 简单介绍**

`ScheduledExecutorService` 是一个接口，`ScheduledThreadPoolExecutor` 是其主要实现类。

**`ScheduledThreadPoolExecutor`** **主要用来在给定的延迟后运行任务，或者定期执行任务。** 这个在实际项目用到的比较少，因为有其他方案选择比如`quartz`。但是，在一些需求比较简单的场景下还是非常有用的！

**`ScheduledThreadPoolExecutor`** **使用的任务队列** **`DelayQueue`** **封装了一个** **`PriorityQueue`，`PriorityQueue`** **会对队列中的任务进行排序，执行所需时间短的放在前面先被执行，如果执行所需时间相同则先提交的任务将被先执行。**

## 2.2 原理

LRU缓存指的是当缓存大小已达到最大分配容量的时候，如果再要去缓存新的对象数据的话，就需要将缓存中最近访问最少的对象删除掉以便给新来的数据腾出空间。

`ConcurrentHashMap` 是线程安全的Map，我们可以利用它缓存 key,value形式的数。`ConcurrentLinkedQueue`是一个线程安全的基于链表的队列（先进先出），我们可以用它来维护 key 。每当我们put/get(缓存被使用)元素的时候，我们就将这个元素对应的 key 存放在队列尾部，这样就能保证队列头部的元素是最近最少使用的。当我们的缓存容量不够的时候，我们直接移除队列头部对应的key以及这个key对应的缓存即可！

另外，我们用到了`ReadWriteLock`(读写锁)来保证线程安全。

## 2.3 代码实现

```java
/**
 * @author shuang.kou
 * <p>
 * 使用 ConcurrentHashMap+ConcurrentLinkedQueue+ReadWriteLock实现线程安全的 LRU 缓存
 * 这里只是为了学习使用，本地缓存推荐使用 Guava 自带的。
 */
public class MyLruCache<K, V> {

    /**
     * 缓存的最大容量
     */
    private final int maxCapacity;

    private ConcurrentHashMap<K, V> cacheMap;
    private ConcurrentLinkedQueue<K> keys;
    /**
     * 读写锁
     */
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock writeLock = readWriteLock.writeLock();
    private Lock readLock = readWriteLock.readLock();

    public MyLruCache(int maxCapacity) {
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("Illegal max capacity: " + maxCapacity);
        }
        this.maxCapacity = maxCapacity;
        cacheMap = new ConcurrentHashMap<>(maxCapacity);
        keys = new ConcurrentLinkedQueue<>();
    }

    public V put(K key, V value) {
        // 加写锁
        writeLock.lock();
        try {
            //1.key是否存在于当前缓存
            if (cacheMap.containsKey(key)) {
                moveToTailOfQueue(key);
                cacheMap.put(key, value);
                return value;
            }
            //2.是否超出缓存容量，超出的话就移除队列头部的元素以及其对应的缓存
            if (cacheMap.size() == maxCapacity) {
                System.out.println("maxCapacity of cache reached");
                removeOldestKey();
            }
            //3.key不存在于当前缓存。将key添加到队列的尾部并且缓存key及其对应的元素
            keys.add(key);
            cacheMap.put(key, value);
            return value;
        } finally {
            writeLock.unlock();
        }
    }

    public V get(K key) {
        //加读锁
        readLock.lock();
        try {
            //key是否存在于当前缓存
            if (cacheMap.containsKey(key)) {
                // 存在的话就将key移动到队列的尾部
                moveToTailOfQueue(key);
                return cacheMap.get(key);
            }
            //不存在于当前缓存中就返回Null
            return null;
        } finally {
            readLock.unlock();
        }
    }

    public V remove(K key) {
        writeLock.lock();
        try {
            //key是否存在于当前缓存
            if (cacheMap.containsKey(key)) {
                // 存在移除队列和Map中对应的Key
                keys.remove(key);
                return cacheMap.remove(key);
            }
            //不存在于当前缓存中就返回Null
            return null;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 将元素添加到队列的尾部(put/get的时候执行)
     */
    private void moveToTailOfQueue(K key) {
        keys.remove(key);
        keys.add(key);
    }

    /**
     * 移除队列头部的元素以及其对应的缓存 (缓存容量已满的时候执行)
     */
    private void removeOldestKey() {
        K oldestKey = keys.poll();
        if (oldestKey != null) {
            cacheMap.remove(oldestKey);
        }
    }

    public int size() {
        return cacheMap.size();
    }

}
```

### 并发测试

```java
int threadNum = 10;
int batchSize = 10;
//init cache
MyLruCache<String, Integer> myLruCache = new MyLruCache<>(batchSize * 10);
//init thread pool with 10 threads
ExecutorService fixedThreadPool = Executors.newFixedThreadPool(threadNum);
//init CountDownLatch with 10 count
CountDownLatch latch = new CountDownLatch(threadNum);
AtomicInteger atomicInteger = new AtomicInteger(0);
long startTime = System.currentTimeMillis();
for (int t = 0; t < threadNum; t++) {
    fixedThreadPool.submit(() -> {
        for (int i = 0; i < batchSize; i++) {
            int value = atomicInteger.incrementAndGet();
            myLruCache.put("id" + value, value);
        }
        latch.countDown();
    });
}
//wait for 10 threads to complete the task
latch.await();
fixedThreadPool.shutdown();
System.out.println("Cache size:" + myLruCache.size());//Cache size:100
long endTime = System.currentTimeMillis();
long duration = endTime - startTime;
System.out.println(String.format("Time cost：%dms", duration));//Time cost：511ms
```

# 3. **实现一个线程安全并且带有过期时间的 LRU 缓存**

实际上就是在我们上面时间的LRU缓存的基础上加上一个定时任务去删除缓存，单纯利用 JDK 提供的类，我们实现定时任务的方式有很多种：

1. `Timer` :不被推荐，多线程会存在问题。
2. `ScheduledExecutorService` ：定时器线程池，可以用来替代 `Timer`
3. `DelayQueue` ：延时队列
4. `quartz` ：一个很火的开源任务调度框架，很多其他框架都是基于 `quartz` 开发的，比如当当网的`elastic-job`就是基于`quartz`二次开发之后的分布式调度解决方案
5. ......

最终我们选择了 `ScheduledExecutorService`，主要原因是它易用（基于`DelayQueue`做了很多封装）并且基本能满足我们的大部分需求。

我们在我们上面实现的线程安全的 LRU 缓存基础上，简单稍作修改即可！我们增加了一个方法：

```java
private void removeAfterExpireTime(K key, long expireTime) {
    scheduledExecutorService.schedule(() -> {
        //过期后清除该键值对
        cacheMap.remove(key);
        keys.remove(key);
    }, expireTime, TimeUnit.MILLISECONDS);
}
```

我们put元素的时候，如果通过这个方法就能直接设置过期时间。

**完整源码如下：**

```java
/**
 * @author shuang.kou
 * <p>
 * 使用 ConcurrentHashMap+ConcurrentLinkedQueue+ReadWriteLock+ScheduledExecutorService实现线程安全的 LRU 缓存
 * 这里只是为了学习使用，本地缓存推荐使用 Guava 自带的，使用 Spring 的话，推荐使用Spring Cache
 */
public class MyLruCacheWithExpireTime<K, V> {

    /**
     * 缓存的最大容量
     */
    private final int maxCapacity;

    private ConcurrentHashMap<K, V> cacheMap;
    private ConcurrentLinkedQueue<K> keys;
    /**
     * 读写锁
     */
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock writeLock = readWriteLock.writeLock();
    private Lock readLock = readWriteLock.readLock();

    private ScheduledExecutorService scheduledExecutorService;

    public MyLruCacheWithExpireTime(int maxCapacity) {
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("Illegal max capacity: " + maxCapacity);
        }
        this.maxCapacity = maxCapacity;
        cacheMap = new ConcurrentHashMap<>(maxCapacity);
        keys = new ConcurrentLinkedQueue<>();
        scheduledExecutorService = Executors.newScheduledThreadPool(3);
    }

    public V put(K key, V value, long expireTime) {
        // 加写锁
        writeLock.lock();
        try {
            //1.key是否存在于当前缓存
            if (cacheMap.containsKey(key)) {
                moveToTailOfQueue(key);
                cacheMap.put(key, value);
                return value;
            }
            //2.是否超出缓存容量，超出的话就移除队列头部的元素以及其对应的缓存
            if (cacheMap.size() == maxCapacity) {
                System.out.println("maxCapacity of cache reached");
                removeOldestKey();
            }
            //3.key不存在于当前缓存。将key添加到队列的尾部并且缓存key及其对应的元素
            keys.add(key);
            cacheMap.put(key, value);
            if (expireTime > 0) {
                removeAfterExpireTime(key, expireTime);
            }
            return value;
        } finally {
            writeLock.unlock();
        }
    }

    public V get(K key) {
        //加读锁
        readLock.lock();
        try {
            //key是否存在于当前缓存
            if (cacheMap.containsKey(key)) {
                // 存在的话就将key移动到队列的尾部
                moveToTailOfQueue(key);
                return cacheMap.get(key);
            }
            //不存在于当前缓存中就返回Null
            return null;
        } finally {
            readLock.unlock();
        }
    }

    public V remove(K key) {
        writeLock.lock();
        try {
            //key是否存在于当前缓存
            if (cacheMap.containsKey(key)) {
                // 存在移除队列和Map中对应的Key
                keys.remove(key);
                return cacheMap.remove(key);
            }
            //不存在于当前缓存中就返回Null
            return null;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 将元素添加到队列的尾部(put/get的时候执行)
     */
    private void moveToTailOfQueue(K key) {
        keys.remove(key);
        keys.add(key);
    }

    /**
     * 移除队列头部的元素以及其对应的缓存 (缓存容量已满的时候执行)
     */
    private void removeOldestKey() {
        K oldestKey = keys.poll();
        if (oldestKey != null) {
            cacheMap.remove(oldestKey);
        }
    }

    private void removeAfterExpireTime(K key, long expireTime) {
        scheduledExecutorService.schedule(() -> {
            //过期后清除该键值对
            cacheMap.remove(key);
            keys.remove(key);
        }, expireTime, TimeUnit.MILLISECONDS);
    }

    public int size() {
        return cacheMap.size();
    }

}
```

**测试效果：**

```text
MyLruCacheWithExpireTime<Integer,String> myLruCache = new MyLruCacheWithExpireTime<>(3);
myLruCache.put(1,"Java",3;
myLruCache.put(2,"C++",3;
myLruCache.put(3,"Python",1500);
System.out.println(myLruCache.size());//3
Thread.sleep(2;
System.out.println(myLruCache.size());//2
```

# 资料来自

- https://zhuanlan.zhihu.com/p/135936339