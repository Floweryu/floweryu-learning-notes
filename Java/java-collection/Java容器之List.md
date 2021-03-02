# Java容器之List

## List接口

- 特点：有序，有索引，可重复

- 常用方法：https://docs.oracle.com/javase/8/docs/api/
- `List`是一个接口，继承于`Collection`的接口，代表着有序的队列。

## `ArrayList`和`LinkedList`

作为`List`最常用的实现

 - `ArrayList` 基于动态数组实现，存在容量限制，当元素超过最大容量时，会自动扩容。`LinkedList`基于双向链表实现，不存在容量限制。
 - `ArrayList`随机访问速度较快，随机插入、删除速度较慢。`LinkedList`随机插入、删除速度较快，随机访问速度较慢。
 - 	`ArrayList`和`LinkedList`都不是线程安全的。

## `Vector` 和 `Stack`

`Vector` 和 `Stack` 的设计目标是作为线程安全的 `List` 实现，替代 `ArrayList`。

- `Vector` - `Vector` 和 `ArrayList` 类似，也实现了 `List` 接口。但是， `Vector` 中的主要方法都是 `synchronized` 方法，即通过互斥同步方式保证操作的线程安全。
- `Stack` - `Stack` 也是一个同步容器，它的方法也用 `synchronized` 进行了同步，它实际上是继承于 `Vector` 类。

## 1. `ArrayList`

### 1.1 要点

`ArrayList` 是一个数组队列，相当于动态数组。**`ArrayList` 默认初始容量大小为 `10` ，添加元素时，如果发现容量已满，会自动扩容为原始大小的 `1.5 `倍**。应该尽量在初始化 `ArrayList 时`，为其指定合适的初始化容量大小，减少扩容操作产生的性能开销。

`ArrayList`源码定义：

```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
```
从 ArrayList 的定义，不难看出 `ArrayList` 的一些基本特性：

 - ` ArrayList `实现了 List 接口，并继承了 `AbstractList`，它支持所有 List 的操作。
 - `ArrayList `实现了 `RandomAccess` 接口，**支持随机访问**。`RandomAccess` 是 Java 中用来被 List 实现，为` List `提供快速访问功能的。在 `ArrayList` 中，我们即可以**通过元素的序号快速获取元素对象**；这就是快速随机访问。
 - `ArrayList `实现了 `Cloneable` 接口，**支持深拷贝**。
 - `ArrayList` 实现了 `Serializable` 接口，**支持序列化**，能通过序列化方式传输。
 - `ArrayList `是**非线程安全**的。
### 1.2 原理
#### ArrayList 的数据结构
`ArrayList` 包含了两个重要的元素：`elementData` 和 `size`。

```java
transient Object[] elementData;
private int size;
```

 - `size` - 是**动态数组的实际大小**。**默认初始容量大小为 10** （可以在构造方法中指定初始大小），添加元素时如果发现容量已满，会自动扩容,如果实际大小为偶数就是1.5倍，否则是1.5倍左右！ 奇偶不同，比如 ：10+10/2 = 15, 33+33/2=49。如果是奇数的话会丢掉小数.
 - `elementData` - 是一个` Object `数组，用于保存添加到 `ArrayList` 中的元素。

#### ArrayList 的序列化
`ArrayList` 具有动态扩容特性，因此保存元素的数组不一定都会被使用，那么就没必要全部进行序列化。为此，`ArrayList` 定制了其序列化方式。具体做法是：

 - 存储元素的 `Object` 数组（即 `elementData`）使用 `transient` 修饰，使得它可以被 Java 序列化所忽略。
 - `ArrayLis`t 重写了 `writeObject()` 和 `readObject()` 来控制序列化数组中有元素填充那部分内容。

#### ArrayList 的访问元素

```java
// 获取第 index 个元素
public E get(int index) {
    rangeCheck(index);
    return elementData(index);
}

E elementData(int index) {
    return (E) elementData[index];
}
```
实现非常简单，其实就是通过数组下标访问数组元素，其时间复杂度为 O(1)，所以很快。

#### ArrayList 的添加元素
`ArrayList` 添加元素时，如果发现容量已满，会自动扩容为原始大小的 1.5 倍。
`ArrayList` 添加元素的实现主要基于以下关键性源码：

```java
public boolean add(E e) {
    ensureCapacityInternal(size + 1);  // Increments modCount!!
    elementData[size++] = e;
    return true;
}

private void ensureCapacityInternal(int minCapacity) {
    ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
}

private void ensureExplicitCapacity(int minCapacity) {
    modCount++;

    // overflow-conscious code
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}

private static int calculateCapacity(Object[] elementData, int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        return Math.max(DEFAULT_CAPACITY, minCapacity);
    }
    return minCapacity;
}

private void grow(int minCapacity) {
    // overflow-conscious code
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    // minCapacity is usually close to size, so this is a win:
    elementData = Arrays.copyOf(elementData, newCapacity);
}

private static int hugeCapacity(int minCapacity) {
    if (minCapacity < 0) // overflow
        throw new OutOfMemoryError();
    return (minCapacity > MAX_ARRAY_SIZE) ?
        Integer.MAX_VALUE :
    MAX_ARRAY_SIZE;
}
```
·`ArrayList `执行添加元素动作（add 方法）时，调用 `ensureCapacityInternal()` 方法来保证容量足够。

 - 如果容量足够时，将数据作为数组中 `size+1` 位置上的元素写入，并将` size` 自增 1。
 - 如果容量不够时，需要使用 `grow() `方法进行扩容数组，新容量的大小为 `oldCapacity + (oldCapacity >> 1)`，也就是旧容量的 `1.5` 倍。扩容操作实际上是调用` Arrays.copyOf()` 把原数组拷贝为一个新数组，因此最好在创建 `ArrayList `对象时就指定大概的容量大小，减少扩容操作的次数。

#### ArrayList 的删除元素

```java
public E remove(int index) {
    rangeCheck(index);

    modCount++;
    E oldValue = elementData(index);

    int numMoved = size - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index,
                         numMoved);
    elementData[--size] = null; // clear to let GC do its work

    return oldValue;
}

private void rangeCheck(int index) {
    if (index >= size)
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}
```
`ArrayList `执行删除元素（`remove 方法`）作时，实际上是调用 `System.arraycopy()` 将 `index+1` 后面的元素都复制到` index `位置上，复制的代价很高。

#### ArrayList 的 Fail-Fast

`modCount` 用来记录 `ArrayList `结构发生变化的次数。结构发生变化是指添加或者删除至少一个元素的所有操作，或者是调整内部数组的大小，仅仅只是设置元素的值不算结构发生变化。

在`AbstractList`源码中，有关于其的解释：

```java
/**
* The number of times this list has been <i>structurally modified</i>.
* Structural modifications are those that change the size of the
* list, or otherwise perturb it in such a fashion that iterations in
* progress may yield incorrect results.
* 这个列表在结构上被修改的次数。
* 结构修改是指改变列表的大小，或者以一种可能产生不正确结果的迭代方式扰乱它。
*
* <p>This field is used by the iterator and list iterator implementation
* returned by the {@code iterator} and {@code listIterator} methods.
* If the value of this field changes unexpectedly, the iterator (or list
* iterator) will throw a {@code ConcurrentModificationException} in
* response to the {@code next}, {@code remove}, {@code previous},
* {@code set} or {@code add} operations.  This provides
* <i>fail-fast</i> behavior, rather than non-deterministic behavior in
* the face of concurrent modification during iteration.
* 该字段用于iterator和listIterator方法返回的iterator和list iterator实现数据迭代。
* 如果该字段出现了意外的改变，iterator/list iterator在执行next、remove、previous、
* set、add等操作时将抛出ConcurrentModificationException。 这样就提供了快速失败行为，
* 而不会出现在执行迭代过程中的不确定行为。
*
* <p><b>Use of this field by subclasses is optional.</b> If a subclass
* wishes to provide fail-fast iterators (and list iterators), then it
* merely has to increment this field in its {@code add(int, E)} and
* {@code remove(int)} methods (and any other methods that it overrides
* that result in structural modifications to the list).  A single call to
* {@code add(int, E)} or {@code remove(int)} must add no more than
* one to this field, or the iterators (and list iterators) will throw
* bogus {@code ConcurrentModificationExceptions}.  If an implementation
* does not wish to provide fail-fast iterators, this field may be
* ignored.
* 子类是否使用该字段是可选的。 如果子类想要提供快速失败的iterator/list iterator，
* 只需要在其add、remove或者其他重载的会导致list结构改变的方法中增加该字段的值。
* 单次对add或者remove的调用对该字段值的增加不能超过1，否则iterator/list iterator
* 将抛出虚假的ConcurrentModificationExceptions。 如果实现类不想提供快速失败的迭代器，
* 可以忽略掉该字段。
*/
protected transient int modCount = 0;
```

在进行序列化或者迭代等操作时，需要比较操作前后` modCount `是否改变，如果改变了需要抛出 `ConcurrentModificationException`。

```java
private void writeObject(java.io.ObjectOutputStream s)
    throws java.io.IOException{
    // Write out element count, and any hidden stuff
    int expectedModCount = modCount;
    s.defaultWriteObject();

    // Write out size as capacity for behavioural compatibility with clone()
    s.writeInt(size);

    // Write out all elements in the proper order.
    for (int i=0; i<size; i++) {
        s.writeObject(elementData[i]);
    }

    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }
}
```
## 2. `LinkedList`
### 2.1 要点
`LinkedList` 基于双向链表实现。由于是双向链表，所以**顺序访问会非常高效**，而**随机访问效率比较低**。

```java
public class LinkedList<E>
    extends AbstractSequentialList<E>
    implements List<E>, Deque<E>, Cloneable, java.io.Serializable
```
从` LinkedList `的定义，可以得出` LinkedList` 的一些基本特性：
- `LinkedList` 实现了` List `接口，并继承了` AbstractSequentialList` ，它支持所有 `List `的操作。
-` LinkedList` 实现了 `Deque `接口，也可以被当作**队列**（Queue）或**双端队列**（Deque）进行操作，此外，也可以**用来实现栈**。
- `LinkedList` 实现了 `Cloneable` 接口，**支持深拷贝**。
- `LinkedList` 实现了 `Serializable` 接口，**支持序列化**。
- `LinkedList `是**非线程安全**的。

### 2.2 原理
#### LinkedList 的数据结构
`LinkedList `内部维护了一个双链表。
`LinkedList` 通过` Node `类型的头尾指针（`first` 和` last`）来访问数据。

```java
// 链表长度
transient int size = 0;
// 链表头节点
transient Node<E> first;
// 链表尾节点
transient Node<E> last;
```

 - ` size `- 表示**双链表中节点的个数**，初始为 0。
 - `first` 和 `last` - 分别是**双链表的头节点和尾节点**。

`Node` 是` LinkedList `的内部类，它表示链表中的元素实例。`Node` 中包含三个元素：

 - `prev `是该节点的上一个节点；
- ` next `是该节点的下一个节点；
- `item` 是该节点所包含的值。

```java
private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;
    ...
}
```
#### LinkedList 的序列化
`LinkedList` 与 `ArrayList` 一样也定制了自身的序列化方式。具体做法是：

 - 将 `size` （双链表容量大小）、`first `和`last` （双链表的头尾节点）修饰为 `transient`，使得它们可以被 Java 序列化所忽略。
 - 重写了` writeObject()` 和 `readObject()` 来控制序列化时，只处理双链表中**能被头节点链式引用的节点元素**

#### LinkedList 的访问元素

```java
Node<E> node(int index) {
    // assert isElementIndex(index);

    if (index < (size >> 1)) {
        Node<E> x = first;
        for (int i = 0; i < index; i++)
            x = x.next;
        return x;
    } else {
        Node<E> x = last;
        for (int i = size - 1; i > index; i--)
            x = x.prev;
        return x;
    }
}
```
获取 `LinkedList` 第 `index `个元素的算法是：

 - 判断 `index` 在链表前半部分，还是后半部分。
 - 如果是前半部分，从头节点开始查找；如果是后半部分，从尾结点开始查找。

显然，`LinkedList` 这种顺序访问元素的方式比 `ArrayList `随机访问元素要慢

#### LinkedList 的添加元素

```java
public void add(E e) {
    checkForComodification();
    lastReturned = null;
    if (next == null)
        linkLast(e);
    else
        linkBefore(e, next);
    nextIndex++;
    expectedModCount++;
}

void linkLast(E e) {
    final Node<E> l = last;
    final Node<E> newNode = new Node<>(l, e, null);
    last = newNode;
    if (l == null)
        first = newNode;
    else
        l.next = newNode;
    size++;
    modCount++;
}

private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;

    Node(Node<E> prev, E element, Node<E> next) {
        this.item = element;
        this.next = next;
        this.prev = prev;
    }
}
```
算法如下：

 - 将新添加的数据包装为`Node`；
 - 如果尾指针为` null`，将头指针指向新节点；
 - 如果尾指针不为 `null`，将新节点作为尾指针的后继节点；
 - 将尾指针指向新节点；

#### LinkedList 的删除元素.

```java
public boolean remove(Object o) {
    if (o == null) {
        // 遍历找到要删除的元素节点
        for (Node<E> x = first; x != null; x = x.next) {
            if (x.item == null) {
                unlink(x);
                return true;
            }
        }
    } else {
        // 遍历找到要删除的元素节点
        for (Node<E> x = first; x != null; x = x.next) {
            if (o.equals(x.item)) {
                unlink(x);
                return true;
            }
        }
    }
    return false;
}

E unlink(Node<E> x) {
    // assert x != null;
    final E element = x.item;
    final Node<E> next = x.next;
    final Node<E> prev = x.prev;

    if (prev == null) {
        first = next;
    } else {
        prev.next = next;
        x.prev = null;
    }

    if (next == null) {
        last = prev;
    } else {
        next.prev = prev;
        x.next = null;
    }

    x.item = null;
    size--;
    modCount++;
    return element;
}
```
算法思路如下：

 - 遍历找到要删除的元素节点，然后调用 `unlink` 方法删除节点；
 - `unlink` 删除节点的方法：
 	- 如果当前节点有前驱节点，则**让前驱节点指向当前节点的下一个节点**；否则，让**双链表头指针指向下一个节点**。
 	- 如果当前节点有后继节点，则让**后继节点指向当前节点的前一个节点**；否则，让**双链表尾指针指向上一个节点**

***