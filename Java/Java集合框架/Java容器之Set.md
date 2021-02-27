# Java容器之Set

## Set接口

```java
public interface Set<E> extends Collection<E>
```

- 特点：元素无序，不可重复
- 常用方法：https://docs.oracle.com/javase/8/docs/api/
- 实现接口的类有：`HashSet, LinkedHashSet, TreeSet`

## Set介绍

- `Set` 继承了 `Collection` 的接口。实际上 `Set` 就是 `Collection`，只是行为略有不同：`Set` 集合不允许有重复元素。
- `SortedSet` 继承了 `Set` 的接口。`SortedSet` 中的内容是排序的唯一值，排序的方法是通过比较器(Comparator)。
- `NavigableSet` 继承了 `SortedSet` 的接口。它提供了丰富的查找方法：如"获取大于/等于某值的元素"、“获取小于/等于某值的元素”等等。
- `AbstractSet` 是一个抽象类，它继承于 `AbstractCollection`，`AbstractCollection` 实现了 Set 中的绝大部分方法，为实现 `Set` 的实例类提供了便利。
- `HashSet` 类依赖于 `HashMap`，它实际上是通过 `HashMap` 实现的。`HashSet` 中的元素是无序的、散列的。
- `TreeSet` 类依赖于 `TreeMap`，它实际上是通过 `TreeMap` 实现的。`TreeSet` 中的元素是有序的，它是按自然排序或者用户指定比较器排序的 Set。
- `LinkedHashSet` 是按插入顺序排序的 Set。
- `EnumSet` 是只能存放 Emum 枚举类型的 Set。

## 1. `SortedSet`接口

继承了 `Set` 的接口。`SortedSet` 中的内容是排序的唯一值，排序的方法是通过比较器(Comparator)。

`SortedSet` 接口定义如下：

```java
public interface SortedSet<E> extends Set<E> {}
```

扩展的方法：

- `Comparator<? super E> comparator()`：返回一个比较器用来排序集合中的元素，如果为`null`则按照自然顺序
- `SortedSet<E> subSet(E fromElement,E toElement)`：返回指定区间的子集(如果`fromElement`等于`toElement`，则返回一个空`Set`)
- `SortedSet<E> headSet(E toElement)`：返回小于指定元素的子集
- `SortedSet<E> tailSet(E fromElement)`：返回大于指定元素的子集
- `E first()`：返回第一个（最小）的元素
- `E last()`：返回最后一个（最大）的元素
- `default Spliterator<E> spliterator()`

## 2. `NavigableSet`接口

`NavigableSet` 继承了 `SortedSet`。它提供了丰富的查找方法。

```
public interface NavigableSet<E> extends SortedSet<E> {}
```

扩展的方法：

- `E lower(E e)`：返回小于给定的元素中的最大元素，没有则返回`null`
- `E floor(E e)`：返回小于或等于给定元素中的最大元素，没有则返回`null`
- `E higher(E e)`：返回大于给定元素中的最小元素，没有则返回`null`
- `E ceiling(E e)`：返回大于或等于给定元素中的最小元素，没有则返回`null`
- `E pollFirst()`：检索并移除第一个(最低的)元素，如果该集合为空则返回`null`
- `E pollLast()`：检索并移除最后(最高)元素，如果该集合为空则返回null。
- `NavigableSet<E> descendingSet()`：返回反序排列的 `Set`
- `Iterator<E> descendingIterator()`：返回反序排列的`Set`的迭代器
- `NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive)`：返回指定区间的子集（两个布尔值相当于表示区间）
- `NavigableSet<E> headSet(E toElement, boolean inclusive)`：返回小于（或者等于，当`inclusive`为`true`时）指定元素的子集

- `NavigableSet<E> tailSet(E fromElement,boolean inclusive)`：返回大于（或者等于，当`inclusive`为`true`时）指定元素的子集

## 3. `AbstractSet` 抽象类

`AbstractSet` 类提供 `Set` 接口的核心实现，以最大限度地减少实现 `Set` 接口所需的工作。

```java
public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E> {}
```

主要的实现已经在 `AbstractCollection` 中完成。

## 4. `HashSet`类

`HashSet` 类依赖于 `HashMap`，它实际上是通过 `HashMap` 实现的。`HashSet` 中的元素是无序的、散列的。

```java
public class HashSet<E>
    extends AbstractSet<E>
    implements Set<E>, Cloneable, java.io.Serializable{}
```

### 4.1 `HashSet`要点

- `HashSet` 通过继承 `AbstractSet` 实现了 `Set` 接口中的骨干方法。
- `HashSet` 实现了 `Cloneable`，所以支持克隆。
- `HashSet` 实现了 `Serializable`，所以支持序列化。
- `HashSet` 中存储的元素是无序的。
- `HashSet` 允许 null 值的元素。
- `HashSet` 不是线程安全的。

### 4.2 `HashSet`原理

**`HashSet` 是基于 `HashMap` 实现的。**

```java
private transient HashMap<E,Object> map;

// Dummy value to associate with an Object in the backing Map
private static final Object PRESENT = new Object();
```

- `HashSet`中维护了一个`HashMap`对象 map，`HashSet`的重要方法，如`add`、`remove`、`iterator`、`clear`、`size`等都是围绕 map 实现的。
  - `HashSet` 类中通过定义 `writeObject()` 和 `readObject()` 方法确定了其序列化和反序列化的机制。
- PRESENT 是用于关联 map 中当前操作元素的一个虚拟值。

> HashSet有一个全局唯一的PRESENT指向的Object对象，add的时候使用其作为map的value。
>
> 两个值都是boolean类型，map.put和remove方法的处理逻辑均是，与key有关联的值则返回对应的值，否则返回null。

```java
public boolean add(E e) {
	return map.put(e, PRESENT)==null;
}

public boolean remove(Object o) {
    return map.remove(o)==PRESENT;
}
```

## 5. `TreeSet`类

`TreeSet` 类依赖于 `TreeMap`，它实际上是通过 `TreeMap` 实现的。`TreeSet` 中的元素是有序的，它是按自然排序或者用户指定比较器排序的 Set。

```java
public class TreeSet<E> extends AbstractSet<E>
    implements NavigableSet<E>, Cloneable, java.io.Serializable{}
```

### 5.1 `TreeSet`要点

- `TreeSet` 通过继承 `AbstractSet` 实现了 `NavigableSet` 接口中的骨干方法。
- `TreeSet` 实现了 `Cloneable`，所以支持克隆。
- `TreeSet` 实现了 `Serializable`，所以支持序列化。
- `TreeSet` 中存储的元素是有序的。排序规则是**自然顺序**或比较器（`Comparator`）中提供的顺序规则。
- `TreeSet` 不是线程安全的。

### 5.2 `TreeSet`原理

```java
// TreeSet 的核心，通过维护一个 NavigableMap 实体来实现 TreeSet 方法
private transient NavigableMap<E,Object> m;

// Dummy value to associate with an Object in the backing Map
private static final Object PRESENT = new Object();
```

- `TreeSet` 中维护了一个 `NavigableMap` 对象 map（实际上是一个 TreeMap 实例），`TreeSet` 的重要方法，如 `add`、`remove`、`iterator`、`clear`、`size` 等都是围绕 map 实现的。
- `PRESENT` 是用于关联 `map` 中当前操作元素的一个虚拟值。`TreeSet` 中的元素都被当成 `TreeMap` 的 key 存储，而 value 都填的是 `PRESENT`。

## 6. `LinkedHashSet`类

`LinkedHashSet` 是按**插入顺序**排序的 Set。

```java
public class LinkedHashSet<E>
    extends HashSet<E>
    implements Set<E>, Cloneable, java.io.Serializable {}
```

### 6.1 `LinkedHashSet`要点

- `LinkedHashSet` 通过继承 `HashSet` 实现了 `Set` 接口中的骨干方法。
- `LinkedHashSet` 实现了 `Cloneable`，所以支持克隆。
- `LinkedHashSet` 实现了 `Serializable`，所以支持序列化。
- `LinkedHashSet` 中存储的元素是**按照插入顺序**保存的。
- `LinkedHashSet` 不是线程安全的。

### `6.2` `LinkedHashSet`原理

`LinkedHashSet` 有三个构造方法，无一例外，都是调用父类 `HashSet` 的构造方法。

```java
public LinkedHashSet(int initialCapacity, float loadFactor) {
	super(initialCapacity, loadFactor, true);
}

public LinkedHashSet(int initialCapacity) {
	super(initialCapacity, .75f, true);
}

public LinkedHashSet() {
	super(16, .75f, true);
}
```

需要强调的是：**LinkedHashSet 构造方法实际上调用的是父类 HashSet 的非 public 构造方法。**

```java
HashSet(int initialCapacity, float loadFactor, boolean dummy) {
	map = new LinkedHashMap<>(initialCapacity, loadFactor);
}
```

不同于 `HashSet` `public` 构造方法中初始化的 `HashMap` 实例，这个构造方法中，初始化了 `LinkedHashMap` 实例。

也就是说，实际上，`LinkedHashSet` 维护了一个双链表。由双链表的特性可以知道，它是按照元素的插入顺序保存的。所以，这就是 `LinkedHashSet` 中存储的元素是按照插入顺序保存的原理。

## 7. `EnumSet`类

```java
public abstract class EnumSet<E extends Enum<E>> extends AbstractSet<E>
    implements Cloneable, java.io.Serializable {}
```

### 7.1 `EnumSet`要点

- `EnumSet` 继承了 `AbstractSet`，所以有 `Set` 接口中的骨干方法。

- `EnumSet` 实现了 `Cloneable`，所以支持克隆。

- `EnumSet` 实现了 `Serializable`，所以支持序列化。

- `EnumSet` 通过 `>` 限定了存储元素必须是枚举值。

- `EnumSet` 没有构造方法，只能通过类中的 `static` 方法来创建 `EnumSet` 对象。

- `EnumSet` 是有序的。以枚举值在 `EnumSet` 类中的定义顺序来决定集合元素的顺序。

- `EnumSet` 不是线程安全的。
