# 1. 接口改善

现在接口里已经完全可以定义静态方法了. 举一个比较普遍的例子就是在java类库中, 对于一些接口如Foo, 都会有一个有静态方法的工具类Foos 来生成或者配合Foo对象实例来使用. 既然静态方法可以存在于接口当中, 那么大多数情况下 Foos工具类完全可以使用接口中的公共方法来代理 (或者将Foos置成package-private).

除此之外更重要的就是, Java 8中接口可以定义默认的方法了.举个例子,一个for-each循环的方法就可以加入到java.lang.Iterable中:

```java
public default void forEach(Consumer<? super T> action) {
    Objects.requireNonNull(action); for (T t : this) {
        action.accept(t);
    }
}
```

在过去,java类库的接口中添加方法基本上是不可能的. 在接口中添加方法意味着破坏了实现了这个接口的代码. 但是现在, 只要能够提供一个正确明智的默认的方法的实现, java类库的维护者就可以在接口中添加方法.

# 2. 为什么不能用默认方法来重载equals、hashCode和toString？

接口不能提供对Object类的任何方法的默认实现。特别是，这意味着从接口里不能提供对equals，hashCode或toString的默认实现。

这刚看起来挺奇怪的，但考虑到一些接口实际上是在文档里定义他们的equals行为的。List接口就是一个例子了。因此，为什么不允许这样呢？

Brian Goetz在这个问题上的冗长的回复里给出了4个原因。我这里只说其中一个，因为那个已经足够说服我了：

它会变得更困难来推导什么时候该调用默认的方法。现在它变得很简单了：如果一个类实现了一个方法，那总是优先于默认的实现的。一旦所有接口的实例都是Object的子类，所有接口实例都已经有对equals/hashCode/toString的非默认实现。因此，一个在接口上这些的默认版本都是没用的，它也不会被编译。

# 3. 函数式接口

Java 8 引入的一个核心概念是函数式接口。如果一个接口定义个唯一一个抽象方法，那么这个接口就成为函数式接口。比如，java.lang.Runnable就是一个函数式接口，因为它只顶一个一个抽象方法:

```
public abstract void run();
```

留意到“abstract”修饰词在这里是隐含的，因为这个方法缺少方法体。为了表示一个函数式接口，并非想这段代码一样一定需要“abstract”关键字。

默认方法不是abstract的，所以一个函数式接口里可以定义任意多的默认方法，这取决于你。

同时，引入了一个新的Annotation：[@FunctionalInterface](http://javadocs.techempower.com/jdk18/api/java/lang/FunctionalInterface.html)。可以把他它放在一个接口前，表示这个接口是一个函数式接口。加上它的接口不会被编译，除非你设法把它变成一个函数式接口。它有点像[@Override](http://javadocs.techempower.com/jdk18/api/java/lang/Override.html)，都是声明了一种使用意图，避免你把它用错。

# 4. Lambdas

一个函数式接口非常有价值的属性就是他们能够用lambdas来实例化。这里有一些lambdas的例子：

左边是指定类型的逗号分割的输入列表，右边是带有return的代码块：

```
(int x, int y) -> { return x + y; }
```

左边是推导类型的逗号分割的输入列表，右边是返回值：

```
(x, y) -> x + y
```

左边是推导类型的单一参数，右边是一个返回值：

```
x -> x * x
```

左边没有输入 (官方名称: ["burger arrow"](http://mail.openjdk.java.net/pipermail/lambda-dev/2012-September/005767.html))，在右边返回一个值：

```
() -> x
```

左边是推导类型的单一参数，右边是没返回值的代码块（返回void）：

```
x -> { System.out.println(x); }
```

静态方法引用：

```
String::valueOf
```

非静态方法引用：

```
Object::toString
```

继承的函数引用：

```
x::toString
```

构造函数引用：

```
ArrayList::new 
```

你可以想出一些函数引用格式作为其他lambda格式的简写。

| 方法引用         | **等价的lambda表达式**  |
| ---------------- | ----------------------- |
| String::valueOf  | x -> String.valueOf(x)  |
| Object::toString | x -> x.toString()       |
| x::toString      | () -> x.toString()      |
| ArrayList::new   | () -> new ArrayList<>() |

当然，在Java里方法能被重载。类可以有多个同名但不同参数的方法。这同样对构造方法有效。ArrayList::new能够指向它的3个构造方法中任何一个。决定使用哪个方法是根据在使用的函数式接口。

一个lambda和给定的函数式接口在“外型”匹配的时候兼容。通过“外型”，我指向输入、输出的类型和声明检查异常。

给出两个具体有效的例子：

```
Comparator<String> c = (a, b) -> Integer.compare(a.length(), b.length());
```

一个Comparator<String>的compare方法需要输入两个阐述，然后返回一个int。这和lambda右侧的一致，因此这个任务是有效的。

```
Runnable r = () -> { System.out.println("Running!"); }
```

一个Runnable的run方法不需要参数也不会返回值。这和lambda右侧一致，所以任务有效。

在抽象方法的签名里的受检查异常（如果存在）也很重要。如果函数式接口在它的签名里声明了异常，lambda只能抛出受检查异常。

## 4.1 捕获和非捕获的Lambda表达式

当Lambda表达式访问一个定义在Lambda表达式体外的非静态变量或者对象时，这个Lambda表达式称为“捕获的”。比如，下面这个lambda表达式捕捉了变量x：

```
int x = 5; return y -> x + y;
```

为了保证这个lambda表达式声明是正确的，被它捕获的变量必须是“有效final”的。所以要么它们需要用final修饰符号标记，要么保证它们在赋值后不能被改变。

Lambda表达式是否是捕获的和性能悄然相关。一个非不捕获的lambda通常比捕获的更高效，虽然这一点没有书面的规范说明（据我所知），而且也不能为了程序的正确性指望它做什么，[非捕获的lambda只需要计算一次](http://mail.openjdk.java.net/pipermail/lambda-dev/2012-November/006867.html). 然后每次使用到它都会返回一个唯一的实例。而捕获的lambda表达式每次使用时都需要重新计算一次，而且从目前实现来看，它很像实例化一个匿名内部类的实例。

**lambdas不做的事**

你应该记住，有一些lambdas不提供的特性。为了Java 8它们被考虑到了，但是没有被包括进去，由于简化以及时间限制的原因。

**Non-final\**\**\* 变量捕获** - 如果一个变量被赋予新的数值，它将不能被用于lambda之中。"final"关键字不是必需的，但变量必须是“有效final”的（前面讨论过）。这个代码不会被编译：

```
int count = 0;
List<String> strings = Arrays.asList("a", "b", "c");
strings.forEach(s -> {
    count++; // error: can't modify the value of count });
```

**例外的透明度** - 如果一个已检测的例外可能从lambda内部抛出，功能性的接口也必须声明已检测例外可以被抛出。这种例外不会散布到其包含的方法。这个代码不会被编译：

```
void appendAll(Iterable<String> values, Appendable out) throws IOException { // doesn't help with the error values.forEach(s -> {
        out.append(s); // error: can't throw IOException here // Consumer.accept(T) doesn't allow it });
}
```

有绕过这个的办法，你能定义自己的功能性接口，扩展Consumer的同时通过像RuntimeException之类抛出 IOException。我试图用代码写出来，但发现它令人困惑是否值得。

**控制流程 (break, early return)** -在上面的 forEach例子中，传统的继续方式有可能通过在lambda之内放置 "return;"来实现。但是，没有办法中断循环或者从lambda中通过包含方法的结果返回一个数值。例如：

```
final String secret = "foo"; boolean containsSecret(Iterable<String> values) {
    values.forEach(s -> { if (secret.equals(s)) {
            ??? // want to end the loop and return true, but can't }
    });
}
```

## 4.2 **为什么抽象类不能通过利用lambda实例化**

抽象类，哪怕只声明了一个抽象方法，也不能使用lambda来实例化。

下面有两个类 [Ordering](http://docs.guava-libraries.googlecode.com/git-history/release/javadoc/com/google/common/collect/Ordering.html) 和 [CacheLoader](http://docs.guava-libraries.googlecode.com/git-history/release/javadoc/com/google/common/cache/CacheLoader.html)的例子，都带有一个抽象方法，摘自于Guava 库。那岂不是很高兴能够声明它们的实例，像这样使用lambda表达式？

```
Ordering<String> order = (a, b) -> ...;
CacheLoader<String, String> loader = (key) -> ...;
```

这样做引发的最常见的争论就是会增加阅读lambda的难度。以这种方式实例化一段抽象类将导致隐藏代码的执行：抽象类的构造方法。

另一个原因是，它抛出了lambda表达式可能的优化。在未来，它可能是这种情况，lambda表达式[都不会计算到对象实例](http://mail.openjdk.java.net/pipermail/lambda-dev/2012-November/006875.html)。放任用户用lambda来声明抽象类将妨碍像这样的优化。

此外，有一个简单地解决方法。事实上，上述两个摘自Guava 库的实例类已经证明了这种方法。增加工厂方法将lambda转换成实例。

```
Ordering<String> order = Ordering.from((a, b) -> ...);
CacheLoader<String, String> loader = CacheLoader.from((key) -> ...);
```

# 5. java.util.function

包概要：[java.util.function](http://javadocs.techempower.com/jdk18/api/java/util/function/package-summary.html)

作为Comparator 和Runnable早期的证明，在JDK中已经定义的接口恰巧作为函数接口而与lambdas表达式兼容。同样方式可以在你自己的代码中定义任何函数接口或第三方库。

但有特定形式的函数接口，且广泛的，通用的，在之前的JD卡中并不存在。大量的接口被添加到新的java.util.function 包中。下面是其中的一些：

- Function<T, R> -T作为输入，返回的R作为输出
- Predicate<T> -T作为输入，返回的boolean值作为输出
- Consumer<T> - T作为输入，执行某种动作但没有返回值
- Supplier<T> - 没有任何输入，返回T
- BinaryOperator<T> -两个T作为输入，返回一个T作为输出，对于“reduce”操作很有用

这些最原始的特征同样存在。他们以int，long和double的方式提供。例如：

- IntConsumer -以int作为输入，执行某种动作，没有返回值

这里存在性能上的一些原因，主要释在输入或输出的时候避免装箱和拆箱操作。

# 6. java.util.stream

包汇总: [java.util.stream](http://javadocs.techempower.com/jdk18/api/java/util/stream/package-summary.html)

新的java.util.stream包提供了“支持在流上的函数式风格的值操作”（引用javadoc）的工具。可能活动一个流的最常见方法是从一个collection获取：

```
Stream<T> stream = collection.stream();
```

一个流就像一个地带器。这些值“流过”（模拟水流）然后他们离开。一个流可以只被遍历一次，然后被丢弃。流也可以无限使用。

流能够是 **串行的** 或者 **并行的**。 它们可以使用其中一种方式开始，然后切换到另外的一种方式，使用stream.[sequential()](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#sequential())或stream.[parallel()](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#parallel())来达到这种切换。串行流在一个线程上连续操作。而并行流就可能一次出现在多个线程上。

所以，你想用一个流来干什么？这里是在javadoc包里给出的例子：

```
int sumOfWeights = blocks.stream().filter(b -> b.getColor() == RED)
                                  .mapToInt(b -> b.getWeight())
                                  .sum();
```

注意：上面的代码使用了一个原始的流，以及一个只能用在原始流上的sum()方法。下面马上就会有更多关于原始流的细节。

流提供了流畅的API，可以进行数据转换和对结果执行某些操作。流操作既可以是“中间的”也可以是“末端的”。

- **中****间****的** -中间的操作保持流打开状态，并允许后续的操作。上面例子中的filter和map方法就是中间的操作。这些操作的返回数据类型是流；它们返回当前的流以便串联更多的操作。
- **末端的** - 末端的操作必须是对流的最终操作。当一个末端操作被调用，流被“消耗”并且不再可用。上面例子中的sum方法就是一个末端的操作。

通常，处理一个流涉及了这些步骤：

1. 从某个源头获得一个流。
2. 执行一个或更多的中间的操作。
3. 执行一个末端的操作。

可能你想在一个方法中执行所有那些步骤。那样的话，你就要知道源头和流的属性，而且要可以保证它被正确的使用。你可能不想接受任意的Stream<T>实例作为你的方法的输入，因为它们可能具有你难以处理的特性，比如并行的或无限的。

有几个更普通的关于流操作的特性需要考虑：

- **有状态的** - 有状态的操作给流增加了一些新的属性，比如元素的唯一性，或者元素的最大数量，或者保证元素以排序的方式被处理。这些典型的要比无状态的中间操作代价大。
- **短路** - 短路操作潜在的允许对流的操作尽早停止，而不去检查所有的元素。这是对无限流的一个特殊设计的属性；如果对流的操作没有短路，那么代码可能永远也不会终止。

对每个Sttream方法这里有一些简短的，一般的描述。查阅javadoc获取更详尽的解释。下面给出了每个操作的重载形式的链接。

中间的操作：

- **filter** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#filter(java.util.function.Predicate)) - 排除所有与断言不匹配的元素。
- **map** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#map(java.util.function.Function)) [2](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#mapToInt(java.util.function.ToIntFunction)) [3](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#mapToLong(java.util.function.ToLongFunction)) [4](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#mapToDouble(java.util.function.ToDoubleFunction)) - 通过Function对元素执行一对一的转换。
- **flatMap** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#flatMap(java.util.stream.FlatMapper)) [2](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#flatMapToInt(java.util.stream.FlatMapper.ToInt)) [3](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#flatMapToLong(java.util.stream.FlatMapper.ToLong)) [4](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#flatMapToDouble(java.util.stream.FlatMapper.ToDouble)) [5](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#flatMap(java.util.function.Function)) - 通过FlatMapper将每个元素转变为无或更多的元素。
- **peek** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#peek(java.util.function.Consumer)) - 对每个遇到的元素执行一些操作。主要对调试很有用。
- **distinct** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#distinct()) - 根据.equals行为排除所有重复的元素。这是一个有状态的操作。
- **sorted** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#sorted()) [2](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#sorted(java.util.Comparator)) - 确保流中的元素在后续的操作中，按照比较器（Comparator）决定的顺序访问。这是一个有状态的操作。
- **limit** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#limit(long)) - 保证后续的操作所能看到的最大数量的元素。这是一个有状态的短路的操作。
- **substream** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#substream(long)) [2](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#substream(long, long)) - 确保后续的操作只能看到一个范围的（根据index）元素。像不能用于流的String.substring一样。也有两种形式，一种有一个开始索引，一种有一个结束索引。二者都是有状态的操作，有一个结束索引的形式也是一个短路的操作。

末端的操作：

- **forEach** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#forEach(java.util.function.Consumer)) - 对流中的每个元素执行一些操作。
- **toArray** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#toArray()) [2](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#toArray(java.util.function.IntFunction)) - 将流中的元素倾倒入一个数组。
- **reduce** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#reduce(java.util.function.BinaryOperator)) [2](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#reduce(T, java.util.function.BinaryOperator)) [3](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#reduce(U, java.util.function.BiFunction, java.util.function.BinaryOperator)) - 通过一个二进制操作将流中的元素合并到一起。
- **collect** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#collect(java.util.stream.Collector)) [2](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#collect(java.util.function.Supplier, java.util.function.BiConsumer, java.util.function.BiConsumer)) - 将流中的元素倾倒入某些容器，例如一个Collection或Map.
- **min** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#min(java.util.Comparator)) - 根据一个比较器找到流中元素的最小值。
- **max** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#max(java.util.Comparator)) -根据一个比较器找到流中元素的最大值。
- **count** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#count()) - 计算流中元素的数量。
- **anyMatch** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#anyMatch(java.util.function.Predicate)) - 判断流中是否至少有一个元素匹配断言。这是一个短路的操作。
- **allMatch** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#allMatch(java.util.function.Predicate)) - 判断流中是否每一个元素都匹配断言。这是一个短路的操作。
- **noneMatch** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#noneMatch(java.util.function.Predicate)) - 判断流中是否没有一个元素匹配断言。这是一个短路的操作。
- **findFirst** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#findFirst()) - 查找流中的第一个元素。这是一个短路的操作。
- **findAny** [1](http://javadocs.techempower.com/jdk18/api/java/util/stream/Stream.html#findAny()) - 查找流中的任意元素，可能对某些流要比findFirst代价低。这是一个短路的操作。

如 [javadocs中提到的](http://javadocs.techempower.com/jdk18/api/java/util/stream/package-summary.html) , 中间的操作是延迟的（lazy）。只有末端的操作会立即开始流中元素的处理。在那个时刻，不管包含了多少中间的操作，元素会在一个传递中处理（通常，但并不总是）。（有状态的操作如sorted() 和distinct()可能需要对元素的二次传送。） 



流试图尽可能做很少的工作。有一些细微优化，如当可以判定元素已经有序的时候，省略一个sorted()操作。在包含limit(x) 或 substream(x,y)的操作中，有些时候对一些不会决定结果的元素，流可以避免执行中间的map操作。在这里我不准备实现公平判断；它通过许多细微的但却很重要的方法表现得很聪明，而且它仍在进步。

回到并行流的概念，重要的是要意识到并行不是毫无代价的。从性能的立场它不是无代价的，你不能简单的将顺序流替换为并行流，且不做进一步思考就期望得到相同的结果。在你能（或者应该）并行化一个流以前，需要考虑很多特性，关于流、它的操作以及数据的目标方面。例如：访问顺序确实对我有影响吗？我的函数是无状态的吗？我的流有足够大，并且我的操作有足够复杂，这些能使得并行化是值得的吗？

有针对int,long和double的专业原始的Stream版本：

- [IntStream](http://javadocs.techempower.com/jdk18/api/java/util/stream/IntStream.html)
- [LongStream](http://javadocs.techempower.com/jdk18/api/java/util/stream/LongStream.html)
- [DoubleStream](http://javadocs.techempower.com/jdk18/api/java/util/stream/DoubleStream.html)

可以在众多函数中，通过专业原始的map和flatMap函数，在一个stream对象与一个原始stream对象之间来回转换。给几个虚设例子：

```
List<String> strings = Arrays.asList("a", "b", "c");
strings.stream() // 
Stream<String> .mapToInt(String::length) // IntStream .longs() // 
LongStream .mapToDouble(x -> x / 10.0) // DoubleStream .boxed() // 
Stream<Double> .mapToLong(x -> 1L) // LongStream .mapToObj(x -> "") // 
Stream<String> ...
```

原始的stream也为获得关于stream的基础数据统计提供方法，那些stream是指作为数据结构的。你可以发现count, sum, min, max, 以及元素平均值全部是来自于一个终端的操作。

原始类型的剩余部分没有原始版本，因为这需要一个不可接受的JDK数量的膨胀。IntStream, LongStream, 和 DoubleStream被认为非常有用应当被包含进去，其他的数字型原始stream可以由这三个通过扩展的原始转换来表示。

在flatMap操作中使用的 [FlatMapper](http://javadocs.techempower.com/jdk18/api/java/util/stream/FlatMapper.html) 接口是具有一个抽象方法的功能性接口：

```
void flattenInto(T element, Consumer<U> sink);
```

在一个flatMap操作的上下文中，stream为你提供element和 sink，然后你定义该用element 和 sink做什么。element是指在stream中的当前元素，而sink代表当flatMap操作结束之后在stream中应该显示些什么。例如：

```
Set<Color> colors = ...;
List<Person> people = ...;
Stream<Color> stream = people.stream().flatMap(
    (Person person, Consumer<Color> sink) -> { // Map each person to the colors they like. for (Color color : colors) { if (person.likesColor(color)) {
                sink.accept(color);
            }
        }
    });
```

注意上面lambda中的参数类型是指定的。在大多数其它上下文中，你可以不需要指定类型，但这里由于FlatMapper的自然特性，编译器需要你帮助判定类型。如果你在使用flatMap又迷惑于它为什么不编译，可能是因为你没有指定类型。

最令人感到困惑，复杂而且有用的终端stream操作之一是collect。它引入了一个称为[Collector](http://javadocs.techempower.com/jdk18/api/java/util/stream/Collector.html)的新的非功能性接口。这个接口有些难理解，但幸运的是有一个[Collectors](http://javadocs.techempower.com/jdk18/api/java/util/stream/Collectors.html)工具类可用来产生所有类型的有用的Collectors。例如：

```
List<String> strings = values.stream()
                             .filter(...)
                             .map(...)
                             .collect(Collectors.toList());
```

如果你想将你的stream元素放进一个Collection,Map或String，那么Collectors可能具有你需要的。在javadoc中浏览那个类绝对是值得的。

# 7. 泛型接口改进

这是一个以前不能做到的，对编译器判定泛型能力的努力改进。在以前版本的Java中有许多情形编译器不能给某个方法计算出泛型，当方法处于嵌套的或串联方法调用这样的上下文的时候，即使有时候对程序员来说它看起来“很明显”。那些情况需要程序员明确的指定一个“类型见证”（type witness）。它是一种通用的特性，但吃惊的是很少有Java程序员知道（我这么说是基于私下的交流并且阅读了一些StackOverflow的问题）。它看起来像这样：

```
// In Java 7: foo(Utility.<Type>bar());
Utility.<Type>foo().bar();
```

如果没有类型见证，编译器可能会将<Object>替代为泛型，而且如果需要的是一个更具体的类型，代码将编译失败。

Java 8 极大的改进了这个状况。在更多的案例中，它可以基于上下文计算出更多的特定的泛型类型。

```
// In Java 8: foo(Utility.bar());
Utility.foo().bar();
```

这项工作仍在发展中，所以我不确定建议中列出的例子有多少能真正包含进Java 8。希望是它们全部。

# 8. java.time

在Java8中新的 date/timeAPI存在于 java.time包中。如果你熟悉Joda Time，它将很容易掌握。事实上，我认为如此好的设计，以至于从未听说过 Joda Time的人也能很容易的掌握。

几乎在API中的任何东西都是永恒的，包括值类型和格式化 。对于Date域或者处理或处理本地线程日期格式化不必太过担心。

与传统的date/timeAPI的交叉是最小的。有一个清晰的分段：

- [Date.toInstant()](http://javadocs.techempower.com/jdk18/api/java/util/Date.html#toInstant())
- [Date.from(Instant)](http://javadocs.techempower.com/jdk18/api/java/util/Date.html#from(java.time.Instant))
- [Calendar.toInstant()](http://javadocs.techempower.com/jdk18/api/java/util/Calendar.html#toInstant())

新API对于像月份和每周的天数，喜欢枚举类型更胜过整形常量。

那么，那是什么呢？包级别的javadocs 对额外类型的做出了非常好的阐述。我将对一些值得注意的部分做一些简短的纲要。

非常有用的值类型：

- [Instant](http://javadocs.techempower.com/jdk18/api/java/time/Instant.html) - 与java.util.Date相似
- [ZonedDateTime](http://javadocs.techempower.com/jdk18/api/java/time/ZonedDateTime.html), [ZoneId](http://javadocs.techempower.com/jdk18/api/java/time/ZoneId.html) -时区很重要的时候使用
- [OffsetDateTime](http://javadocs.techempower.com/jdk18/api/java/time/OffsetDateTime.html), [OffsetTime](http://javadocs.techempower.com/jdk18/api/java/time/OffsetTime.html), [ZoneOffset](http://javadocs.techempower.com/jdk18/api/java/time/ZoneOffset.html) -对UTC的偏移处理
- [Duration](http://javadocs.techempower.com/jdk18/api/java/time/Duration.html), [Period](http://javadocs.techempower.com/jdk18/api/java/time/Period.html) - 但如果你想找到两个日期之间的时间量，你可能会寻找ChronoUnit代替（见下文）

其他有用的类型：

- [DateTimeFormatter](http://javadocs.techempower.com/jdk18/api/java/time/format/DateTimeFormatter.html) - 将日期类型转换成字符串类型
- [ChronoUnit](http://javadocs.techempower.com/jdk18/api/java/time/temporal/ChronoUnit.html) - 计算出两点之间的时间量，例如ChronoUnit.DAYS.between(t1, t2)
- [TemporalAdjuster](http://javadocs.techempower.com/jdk18/api/java/time/temporal/TemporalAdjuster.html) - 例如date.with(TemporalAdjuster.firstDayOfMonth())

大多数情况下，新的值类型由JDBC提供支持。有一小部分异常，如ZonedDateTime在SQL中没有对应的（类型）。

# 9. 集合API附件

实际上接口能够定义默认方法允许了JDK作者加入大量的附件到集合API接口中。默认实现在核心接口里提供，而其他更有效或更好的重载实现被加入到可适用的具体类中。

这里是新方法的列表：

- [Iterable.forEach(Consumer)](http://javadocs.techempower.com/jdk18/api/java/lang/Iterable.html#forEach(java.util.function.Consumer))
- [Iterator.forEach(Consumer)](http://javadocs.techempower.com/jdk18/api/java/util/Iterator.html#forEach(java.util.function.Consumer))
- [Collection.removeAll(Predicate)](http://javadocs.techempower.com/jdk18/api/java/util/Collection.html#removeAll(java.util.function.Predicate))
- [Collection.spliterator()](http://javadocs.techempower.com/jdk18/api/java/util/Collection.html#spliterator())
- [Collection.stream()](http://javadocs.techempower.com/jdk18/api/java/util/Collection.html#stream())
- [Collection.parallelStream()](http://javadocs.techempower.com/jdk18/api/java/util/Collection.html#parallelStream())
- [List.sort(Comparator)](http://javadocs.techempower.com/jdk18/api/java/util/List.html#sort(java.util.Comparator))
- [Map.forEach(BiConsumer)](http://javadocs.techempower.com/jdk18/api/java/util/Map.html#forEach(java.util.function.BiConsumer))
- [Map.replaceAll(BiFunction)](http://javadocs.techempower.com/jdk18/api/java/util/Map.html#replaceAll(java.util.function.BiFunction))
- [Map.putIfAbsent(K, V)](http://javadocs.techempower.com/jdk18/api/java/util/Map.html#putIfAbsent(K, V))
- [Map.remove(Object, Object)](http://javadocs.techempower.com/jdk18/api/java/util/Map.html#remove(java.lang.Object, java.lang.Object))
- [Map.replace(K, V, V)](http://javadocs.techempower.com/jdk18/api/java/util/Map.html#replace(K, V, V))
- [Map.replace(K, V)](http://javadocs.techempower.com/jdk18/api/java/util/Map.html#replace(K, V))
- [Map.computeIfAbsent(K, Function)](http://javadocs.techempower.com/jdk18/api/java/util/Map.html#computeIfAbsent(K, java.util.function.Function))
- [Map.computeIfPresent(K, BiFunction)](http://javadocs.techempower.com/jdk18/api/java/util/Map.html#computeIfPresent(K, java.util.function.BiFunction))
- [Map.compute(K, BiFunction)](http://javadocs.techempower.com/jdk18/api/java/util/Map.html#compute(K, java.util.function.BiFunction))
- [Map.merge(K, V, BiFunction)](http://javadocs.techempower.com/jdk18/api/java/util/Map.html#merge(K, V, java.util.function.BiFunction))
- [Map.getOrDefault(Object, V)](http://javadocs.techempower.com/jdk18/api/java/util/Map.html#getOrDefault(Object, V))

同样， [Iterator.remove()](http://javadocs.techempower.com/jdk18/api/java/util/Iterator.html#remove()) 现在有一个默认的, 会抛出异常的实现，使得它稍微容易地去定义不可修改的迭代器。

Collection.stream()和Collection.parallelStream()是流API的主要门户。有其他方法去生成流，但这些在以后会更为长用。

List.sort(Comparator)的附件有点怪异。以前排序一个ArrayList的方法是：

```
Collections.sort(list, comparator);
```

这代码是你在Java7里唯一可选的，非常低效。它会复制list到一个数组里，排序这个数组，然后使用ListIterator来把数组插入到新list的新位置上。

List.sort(比较器)的默认实现仍然会做这个，但是具体的实现类可以自由的优化。例如，ArrayList.sort在ArrayList内部数组上调用了Arrays.sort。CopyOnWriteArrayList做了同样的事情。

从这些新方法中获得的不仅仅是性能。它们也具有更多的令人满意的语义。例如， 对Collections.synchronizedList()排序是一个使用了list.sort的原子操作。你可以使用list.forEach对它的所有元素进行迭代，使之成为原子操作。

Map.computeIfAbsent使得操作类似多重映射的结构变得容易了：

```
// Index strings by length: 
Map<Integer, List<String>> map = new HashMap<>(); for (String s : strings) {
    map.computeIfAbsent(s.length(),
                        key -> new ArrayList<String>())
       .add(s);
} // Although in this case the stream API may be a better choice:
 Map<Integer, List<String>> map = strings.stream()
    .collect(Collectors.groupingBy(String::length));
```

# 10. 增加并发API

- [ForkJoinPool.commonPool()](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/ForkJoinPool.html#commonPool())
- [ConcurrentHashMap(v8)](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/ConcurrentHashMap.html)
- 下面的形式有并行，顺序，对象，整型，长整型和double型。

有太多的链接可以点击，因此参看[ConcurrentHashMap ](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/ConcurrentHashMap.html)javadocs文档以获得更多信息。

- ConcurrentHashMap.reduce...
- ConcurrentHashMap.search...
- ConcurrentHashMap.forEach...

- [ConcurrentHashMap.newKeySet()](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/ConcurrentHashMap.html#newKeySet())
- [ConcurrentHashMap.newKeySet(int)](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/ConcurrentHashMap.html#newKeySet(int))
- [CompletableFuture](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/CompletableFuture.html)
- [StampedLock](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/locks/StampedLock.html)
- [LongAdder](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/atomic/LongAdder.html)
- [LongAccumulator](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/atomic/LongAccumulator.html)
- [DoubleAdder](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/atomic/DoubleAdder.html)
- [DoubleAccumulator](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/atomic/DoubleAccumulator.html)
- [CountedCompleter](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/CountedCompleter.html)
- [Executors.newWorkStealingPool()](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/Executors.html#newWorkStealingPool())

- [Executors.newWorkStealingPool(int)](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/Executors.html#newWorkStealingPool(int))
- 下面的形式有AtomicReference, AtomicInteger, AtomicLong, 和每一个原子数组的版本。 

- [AtomicReference.getAndUpdate(UnaryOperator)](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/atomic/AtomicReference.html#getAndUpdate(java.util.function.UnaryOperator))
- [AtomicReference.updateAndGet(UnaryOperator)](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/atomic/AtomicReference.html#updateAndGet(java.util.function.UnaryOperator))
- [AtomicReference.getAndAccumulate(V, UnaryOperator)](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/atomic/AtomicReference.html#getAndAccumulate(V, java.util.function.BinaryOperator))
- [AtomicReference.accumulateAndGet(V, UnaryOperator)](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/atomic/AtomicReference.html#accumulateAndGet(V, java.util.function.BinaryOperator))

ForkJoinPool.commonPool()是处理所有并行流操作的结构。当你 需要的时候，它是一个好而简单的方式去获得一个ForkJoinPool/ExecutorService/Executor对象。ConcurrentHashMap<K, V>完全重写。内部看起来它一点不像是Java7版本。从外部来看几乎相同，除了它有大量批量操作方法：多种形式的减少搜索和forEach。
ConcurrentHashMap.newKeySet()提供了一个并发的java.util.Set实现。它基本上是Collections.newSetFromMap(new ConcurrentHashMap<T, Boolean>())的另一种方式的重写。

StampedLock是一种新型锁的实现，很可能在大多数场景都可以替代[ReentrantReadWriteLock](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/locks/ReentrantReadWriteLock.html)。当作为一个简单的读写锁的时候，它比RRWL的性能要好。它也为“读优化”提供了API，通过它你获得了一个功能有点弱，但代价很小的读操作锁的版本，执行读操作，然后检查锁是否被一个写操作设定为无效。在Heinz Kabutz汇总的一系列幻灯片中，有更多关于这个类及其性能的细节（在这个系列幻灯片大约一半的地方开始的）：["移相器和](http://javaspecialists.eu/talks/jfokus13/PhaserAndStampedLock.pdf)[StampedLock演示](http://javaspecialists.eu/talks/jfokus13/PhaserAndStampedLock.pdf)"

CompletableFuture<T>是[Future](http://javadocs.techempower.com/jdk18/api/java/util/concurrent/Future.html)接口的一个非常棒的实现，它提供了无数执行（和串接）异步任务的方法。它特别依赖功能性的接口；lambdas是值得增加这个类的一个重要原因。如果你正在使用Guava的 Future工具，例如[Futures](http://docs.guava-libraries.googlecode.com/git-history/release/javadoc/com/google/common/util/concurrent/Futures.html), [ListenableFuture](http://docs.guava-libraries.googlecode.com/git-history/release/javadoc/com/google/common/util/concurrent/ListenableFuture.html), 和 [SettableFuture](http://docs.guava-libraries.googlecode.com/git-history/release/javadoc/com/google/common/util/concurrent/SettableFuture.html)，那么你可能会希望校验CompletableFuture能否作为潜在的替代选择。

# 11. IO/NIO API的新增内容

- [BufferedReader.lines()](http://javadocs.techempower.com/jdk18/api/java/io/BufferedReader.html#lines())
- [Files.list(Path)](http://javadocs.techempower.com/jdk18/api/java/nio/file/Files.html#list(java.nio.file.Path))
- [Files.walk(Path, int, FileVisitOption...)](http://javadocs.techempower.com/jdk18/api/java/nio/file/Files.html#walk(java.nio.file.Path, int, java.nio.file.FileVisitOption...))
- [Files.walk(Path, FileVisitOption...)](http://javadocs.techempower.com/jdk18/api/java/nio/file/Files.html#walk(java.nio.file.Path, java.nio.file.FileVisitOption...))
- [Files.find(Path, int, BiPredicate, FileVisitOption...)](http://javadocs.techempower.com/jdk18/api/java/nio/file/Files.html#find(java.nio.file.Path, int, java.util.function.BiPredicate, java.nio.file.FileVisitOption...))
- [Files.lines(Path, Charset)](http://javadocs.techempower.com/jdk18/api/java/nio/file/Files.html#lines(java.nio.file.Path, java.nio.charset.Charset))
- [DirectoryStream.entries()](http://javadocs.techempower.com/jdk18/api/java/nio/file/DirectoryStream.html#entries())

简单的说，这些API用于从文件和InputStreams获取java.util.stream.Stream对象。不过它们与直接从常规的collection得到的流有些不同，它们引入了两个概念：

- [UncheckedIOException](http://javadocs.techempower.com/jdk18/api/java/io/UncheckedIOException.html) - 当有IO错误时抛出这个异常，不过由于Iterator/Stream的签名中不允许有IOException，所以它只能借助于unchecked异常。
- [CloseableStream](http://javadocs.techempower.com/jdk18/api/java/util/stream/CloseableStream.html) - 可以（并且应该）定义在 try-with-resources 语句里面的流。

# 12. 反射和annotation的改动

- 类型annotation (JSR 308)
- [AnnotatedType](http://javadocs.techempower.com/jdk18/api/java/lang/reflect/AnnotatedType.html)
- [Repeatable](http://javadocs.techempower.com/jdk18/api/java/lang/annotation/Repeatable.html)
- [Method.getAnnotatedReturnType()](http://javadocs.techempower.com/jdk18/api/java/lang/reflect/Method.html#getAnnotatedReturnType())
- [Field.getAnnotationsByType(Class)](http://javadocs.techempower.com/jdk18/api/java/lang/reflect/Field.html#getAnnotationsByType(java.lang.Class))
- [Field.getAnnotatedType()](http://javadocs.techempower.com/jdk18/api/java/lang/reflect/Field.html#getAnnotatedType())
- [Constructor.getAnnotatedReturnType()](http://javadocs.techempower.com/jdk18/api/java/lang/reflect/Constructor.html#getAnnotatedReturnType())
- [Parameter](http://javadocs.techempower.com/jdk18/api/java/lang/reflect/Parameter.html) - 支持 [parameter.getName()](http://javadocs.techempower.com/jdk18/api/java/lang/reflect/Parameter.html#getName())，等等。

Annotation允许在更多的地方被使用，例如List<@Nullable String>。受此影响最大的可能是那些静态分析工具，如Sonar和FindBugs。

JSR 308的网站解释了增加这些改动的动机，介绍的不错： ["类型Annotation (JSR 308) 和 Checker框架"](http://types.cs.washington.edu/jsr308/)