## 一. 并发场景下的List

## 1. CopyOnWriteArrayList

`CopyOnWriteArrayList` 是线程安全的 `ArrayList`。`CopyOnWrite` 意思为**写的时候会将共享变量新复制一份**出来。复制的好处在于**读操作是无锁的**（也就是无阻塞）。

CopyOnWriteArrayList **仅适用于写操作非常少的场景**，而且能够容忍读写的短暂不一致。如果读写比例均衡或者有大量写操作的话，使用 CopyOnWriteArrayList 的性能会非常糟糕。

### CopyOnWriteArrayList 原理

CopyOnWriteArrayList 内部维护了一个数组，成员变量 array 就指向这个内部数组，所有的读操作都是基于 array 进行的。

> 无论是读操作还是添加、删除操作，都是使用拷贝数组这一原理实现的。

- lock：执行写时的复制操作，需要使用可重入锁加锁
- array：对象数组，用于存放元素

```java
/** The lock protecting all mutators */
final transient ReentrantLock lock = new ReentrantLock();

/** The array, accessed only via getArray/setArray. */
private transient volatile Object[] array;
```

#### 读操作

读操作不用加锁，所以性能很高。因为读操作是在内部数组的快照上工作的。

```java
@SuppressWarnings("unchecked")
private E get(Object[] a, int index) {
    return (E) a[index];
}

/**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
public E get(int index) {
    return get(getArray(), index);
}

final Object[] getArray() {
    return array;
}
```

#### 添加操作

先将原容器copy一份，然后在新的副本上执行添加操作，最后再将array的引用指向新的容器，此过程加锁。

```java
public boolean add(E e) {
    // 加锁，保证线程安全
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        // 拷贝原容器，长度为原来容器长度+1
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        // 在新的副本上执行添加操作
        newElements[len] = e;
        // 将原来容器的引用指向新的副本
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}

final void setArray(Object[] a) {
    array = a;
}

public void add(int index, E element) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        if (index > len || index < 0)
            throw new IndexOutOfBoundsException("Index: "+index+
                                                ", Size: "+len);
        Object[] newElements;
        int numMoved = len - index;
        // 如果添加位置在最后
        if (numMoved == 0)
            newElements = Arrays.copyOf(elements, len + 1);
        else {
             // 否则：先将添加位置之外的其它元素拷贝到新副本中，并切换引用
            newElements = new Object[len + 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index, newElements, index + 1,
                             numMoved);
        }
        newElements[index] = element;
        setArray(newElements);
    } finally {
        lock.unlock();
    }
}
```

#### 删除操作

先将删除元素之外的其它元素拷贝到一个新的容器中，然后再切换引用，将原容器的引用指向新副本。需要加锁。

```java
public E remove(int index) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        E oldValue = get(elements, index);
        // 计算删除该位置元素需要移动数组元素的个数
        int numMoved = len - index - 1;
        // 如果删除的元素位于数组最后一个
        if (numMoved == 0)
            // 直接拷贝前len-1个数据到新副本上，再切换引用
            setArray(Arrays.copyOf(elements, len - 1));
        else {
            // 否则：先将删除元素之外的其它元素拷贝到新副本中，并切换引用
            Object[] newElements = new Object[len - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index,
                             numMoved);
            setArray(newElements);
        }
        return oldValue;
    } finally {
        lock.unlock();
    }
}
```

## 2. ConcurrentLinkedQueue

`ConcurrentLinkedQueue`是`Queue`的一个安全的实现，以非阻塞方式采用CAS操作，来保证元素的一致性。

`ConcurrentLinkedQueue`是一个基于节点的无界线程安全队列，它采用先进先出的规则对节点进行排序，当添加一个元素的时候，它会添加到队列的尾部，当获取一个元素时，它会返回队列头部的元素。

**Node节点类型**：

```java
private static class Node<E> {
        volatile E item;
        volatile Node<E> next;

        /**
         * Constructs a new node.  Uses relaxed write because item can
         * only be seen after publication via casNext.
         */
        Node(E item) {
            UNSAFE.putObject(this, itemOffset, item);
        }

        boolean casItem(E cmp, E val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        void lazySetNext(Node<E> val) {
            UNSAFE.putOrderedObject(this, nextOffset, val);
        }

        boolean casNext(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        // Unsafe mechanics

        private static final sun.misc.Unsafe UNSAFE;
        private static final long itemOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                itemOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }
```

`ConcurrentLinkedQueue`的构造方法：

```java
// 默认的构造方法: head节点存储值为空  tail节点等于head节点
public ConcurrentLinkedQueue() {
    head = tail = new Node<E>(null);
}

// 根据其它集合来创建队列
public ConcurrentLinkedQueue(Collection<? extends E> c) {
    Node<E> h = null, t = null;
    // 遍历节点
    for (E e : c) {
        checkNotNull(e);
        Node<E> newNode = new Node<E>(e);
        if (h == null)
            h = t = newNode;
        else {
            t.lazySetNext(newNode);
            t = newNode;
        }
    }
    if (h == null)
        h = t = new Node<E>(null);
    head = h;
    tail = t;
}
```

#### 入队操作

```java
public boolean add(E e) {
    return offer(e);
}
public boolean offer(E e) {
    // e是null则直接抛异常
    checkNotNull(e);
    // 创建入队节点
    final Node<E> newNode = new Node<E>(e);

    // 1. 根据tail找到尾节点 2. 将新节点置为尾节点的下一个节点 3. casTail更新尾节点
    for (Node<E> t = tail, p = t;;) {
        // p 用来表示队列的尾节点  初始值为tail节点
        // q 用来表示p的next节点
        Node<E> q = p.next;
        // 判断p是不是尾节点 tail不一定是尾节点 判断依据：改节点的next是不是null
        // 如果p是尾节点
        if (q == null) {
            // 设置p的下一个节点为新节点 设置成功返回true 设置失败返回false 失败则说明有其它线程更新尾节点
            if (p.casNext(null, newNode)) {
                // 如果 p!=t 则将入队节点设置为尾节点  更新失败没关系  因为失败了表示其它线程更新了tail节点
                if (p != t) 
                    casTail(t, newNode);
                return true;
            }
        }
        // 多线程操作时 由于poll会把旧的head变为自引用 然后将head的next设置为新的head
        // 所以这里需要重新寻找新的head
        else if (p == q)
            p = (t != (t = tail)) ? t : head;
        // 寻找尾节点
        else
            p = (p != t && t != (t = tail)) ? t : q;
    }
}
```

入队过程主要做了两件事：

- 第一是定位尾节点
- 第二是使用CAS算法能将入队节点设置为尾节点的next节点，如果不成功则重试

**为什么不把tail节点设置为尾节点**

对于先进先出的队列入队所要做的事情就是将入队节点设置成尾节点，为什么不写成下面来实现？

```java
public boolean offer(E e) {
    checkNotNull(e);
    final Node<E> newNode = new Node<E>(e);
    
    for (;;) {
        Node<E> t = tail;
        
        if (t.casNext(null ,newNode) && casTail(t, newNode)) {
            return true;
        }
    }
}
```

让tail节点永远作为队列的尾节点，这样实现代码量非常少，而且逻辑非常清楚和易懂。但是这么做有个缺点就是每次都需要使用循环CAS更新tail节点。如果能减少CAS更新tail节点的次数，就能提高入队的效率。

#### 出队操作

```java
public E poll() {
    restartFromHead:
    for (;;) {
        // p 节点表示首节点  即要出队的节点
        for (Node<E> h = head, p = h, q;;) {
            E item = p.item;
            // 如果p的节点元素不为null  则通过CAS设置p节点引用的元素为null  成功则返回p节点的元素
            if (item != null && p.casItem(item, null)) {
                // Successful CAS is the linearization point
                // for item to be removed from this queue.
                // 更新head
                if (p != h) // hop two nodes at a time
                    updateHead(h, ((q = p.next) != null) ? q : p);
                return item;
            }
            // 如果头节点的元素为空或头节点发生了变化，这说明头节点已经被另外一个线程修改了。
            // 那么获取p节点的下一个节点，如果p节点的下一节点为null，则表明队列已经空了
            else if ((q = p.next) == null) {
                updateHead(h, p);
                return null;
            }
            // p == q，则使用新的head重新开始
            else if (p == q)
                continue restartFromHead;
            // 如果下一个元素不为空，则将头节点的下一个节点设置成头节点
            else
                p = q;
        }
    }
}
```

主要逻辑就是首先获取头节点的元素，然后判断头节点元素是否为空，如果为空，表示另外一个线程已经进行了一次出队操作将该节点的元素取走，如果不为空，则使用CAS的方式将头节点的引用设置成null，如果CAS成功，则直接返回头节点的元素，如果不成功，表示另外一个线程已经进行了一次出队操作更新了head节点，导致元素发生了变化，需要重新获取头节点。

#### `size()`方法

```java
public int size() {
    int count = 0;
    for (Node<E> p = first(); p != null; p = succ(p))
        if (p.item != null)
            // Collection.size() spec says to max out
            if (++count == Integer.MAX_VALUE)
                break;
    return count;
}
```

size()方法用来获取当前队列的元素个数，但在并发环境中，其结果可能不精确，因为整个过程都没有加锁，所以从调用size方法到返回结果期间有可能增删元素，导致统计的元素个数不精确。

#### `contains(Object o)`方法

```java
public boolean contains(Object o) {
    if (o == null) return false;
    for (Node<E> p = first(); p != null; p = succ(p)) {
        E item = p.item;
        if (item != null && o.equals(item))
            return true;
    }
    return false;
}
```

该方法和size方法类似，有可能返回错误结果，比如调用该方法时，元素还在队列里面，但是遍历过程中，该元素被删除了，那么就会返回false。

## 3.Collections.synchronizedList

使用方法：

```java
List list = Collections.synchronizedList(new ArrayList());
```

看看构造方法：

```java
public static <T> List<T> synchronizedList(List<T> list) {
    return (list instanceof RandomAccess ?
            new SynchronizedRandomAccessList<>(list) :
            new SynchronizedList<>(list));
}

static <T> List<T> synchronizedList(List<T> list, Object mutex) {
    return (list instanceof RandomAccess ?
            new SynchronizedRandomAccessList<>(list, mutex) :
            new SynchronizedList<>(list, mutex));
}
```

Collections.synchronizedList主要通过`synchronized`来保证线程安全

```java
    static class SynchronizedList<E>
        extends SynchronizedCollection<E>
        implements List<E> {
        private static final long serialVersionUID = -7754090372962971524L;

        final List<E> list;

        SynchronizedList(List<E> list) {
            super(list);
            this.list = list;
        }
        SynchronizedList(List<E> list, Object mutex) {
            super(list, mutex);
            this.list = list;
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            synchronized (mutex) {return list.equals(o);}
        }
        public int hashCode() {
            synchronized (mutex) {return list.hashCode();}
        }

        public E get(int index) {
            synchronized (mutex) {return list.get(index);}
        }
        public E set(int index, E element) {
            synchronized (mutex) {return list.set(index, element);}
        }
        public void add(int index, E element) {
            synchronized (mutex) {list.add(index, element);}
        }
        public E remove(int index) {
            synchronized (mutex) {return list.remove(index);}
        }

        public int indexOf(Object o) {
            synchronized (mutex) {return list.indexOf(o);}
        }
        public int lastIndexOf(Object o) {
            synchronized (mutex) {return list.lastIndexOf(o);}
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            synchronized (mutex) {return list.addAll(index, c);}
        }

        public ListIterator<E> listIterator() {
            return list.listIterator(); // Must be manually synched by user
        }

        public ListIterator<E> listIterator(int index) {
            return list.listIterator(index); // Must be manually synched by user
        }

        public List<E> subList(int fromIndex, int toIndex) {
            synchronized (mutex) {
                return new SynchronizedList<>(list.subList(fromIndex, toIndex),
                                            mutex);
            }
        }

        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            synchronized (mutex) {list.replaceAll(operator);}
        }
        @Override
        public void sort(Comparator<? super E> c) {
            synchronized (mutex) {list.sort(c);}
        }

        private Object readResolve() {
            return (list instanceof RandomAccess
                    ? new SynchronizedRandomAccessList<>(list)
                    : this);
        }
    }
```



## 参考文献

- https://blog.csdn.net/qq_38293564/article/details/80798310

