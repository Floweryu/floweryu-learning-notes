# Java容器之Queue

## 1. Queue 简介

### 1.1. Queue 接口

```java
public interface Queue<E> extends Collection<E> {}
```

### 1.2. AbstractQueue 抽象类

**`AbstractQueue` 类提供 `Queue` 接口的核心实现**，以最大限度地减少实现 `Queue` 接口所需的工作。

`AbstractQueue` 抽象类定义如下：

```java
public abstract class AbstractQueue<E>
    extends AbstractCollection<E>
    implements Queue<E> {}
```

### 1.3. Deque 接口

```java
public interface Deque<E> extends Queue<E> {}
```

Deque 接口是 double ended queue 的缩写，即**双端队列**。Deque 继承 Queue 接口，并扩展支持**在队列的两端插入和删除元素**。

所以提供了特定的方法，如:

- 尾部插入时需要的`addLast(e), offerLast(e)`。
- 尾部删除所需要的`removeLast(), pollLast()`。

大多数的实现对元素的数量没有限制，但这个接口既支持有容量限制的 deque，也支持没有固定大小限制的。

## 2. ArrayDeque

```java
public class ArrayDeque<E> extends AbstractCollection<E>
                           implements Deque<E>, Cloneable, Serializable {}
```

`ArrayDeque` 是 `Deque` 的顺序表实现。

`ArrayDeque` 用一个动态数组实现了栈和队列所需的所有操作。

## 3. LinkedList

`LinkedList` 是 `Deque` 的链表实现

```java
public class LinkedListQueueDemo {

    public static void main(String[] args) {
        //add()和remove()方法在失败的时候会抛出异常(不推荐)
        Queue<String> queue = new LinkedList<>();

        queue.offer("a"); // 入队
        queue.offer("b"); // 入队
        queue.offer("c"); // 入队
        for (String q : queue) {
            System.out.println(q);
        }
        System.out.println("===");
        System.out.println("poll=" + queue.poll()); // 出队
        for (String q : queue) {
            System.out.println(q);
        }
        System.out.println("===");
        System.out.println("element=" + queue.element()); //返回第一个元素
        for (String q : queue) {
            System.out.println(q);
        }
        System.out.println("===");
        System.out.println("peek=" + queue.peek()); //返回第一个元素
        for (String q : queue) {
            System.out.println(q);
        }
    }

}
```

## 4. PriorityQueue

`PriorityQueue` 类定义如下：

```java
public class PriorityQueue<E> extends AbstractQueue<E>
    implements java.io.Serializable {}
```

`PriorityQueue` 要点：

- `PriorityQueue` 实现了 `Serializable`，支持序列化。
- `PriorityQueue` 类是无界优先级队列。
- `PriorityQueue` 中的元素根据自然顺序或 `Comparator` 提供的顺序排序。
- `PriorityQueue` 不接受 null 值元素。
- `PriorityQueue` 不是线程安全的。