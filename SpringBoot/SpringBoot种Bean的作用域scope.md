# 1.五种模式

## 1.1 `Singleton`单例模式

Spring默认的scope，表示Spring容器只创建一个bean的实例，Spring在创建第一次后会缓存起来，之后不再创建，就是设计模式中的单例模式。

### 单例Bean优势

- 减少了新生成实例的消耗
- 减少jvm垃圾回收
- 可以快速获取到bean

### 单例Bean的劣势

> 在并发环境下线程不安全？

单例 bean 存在线 程问题，主要是因为当多个线程操作同⼀个对象的时候，对这个对象的⾮静态成员变量的写操作 会存在线程安全问题。

常⻅的有两种解决办法：

1. 在Bean对象中尽量避免定义可变的成员变量（不太现实）。
2. 在类中定义⼀个ThreadLocal成员变量，将需要的可变成员变量保存在 ThreadLocal 中（推 荐的⼀种⽅式）。

## 1.2 `Prototype`

每次请求都会创建⼀个新的 bean 实例。

## 1.3 `Request`

每⼀次HTTP请求都会产⽣⼀个新的bean，该bean仅在当前HTTP request内有效。

## 1.4 `Session`

每⼀次HTTP请求都会产⽣⼀个新的 bean，该bean仅在当前 HTTP session 内有效。

## 1.5 `GlobalSession`

全局session作⽤域，仅仅在基于portlet的web应⽤中才有意义，Spring5已 经没有了。Portlet是能够⽣成语义代码(例如：HTML)⽚段的⼩型Java Web插件。它们基于 portlet容器，可以像servlet⼀样处理HTTP请求。但是，与 servlet 不同，每个 portlet 都有不 同的会话

# 2. 在创建bean的时候如何指定呢？

## 2.1 xml方式

```xml
<bean id="student" class="Student" scope="prototype" />
```

## 2.2 注解方式

```java
@Component

@Scope("prototype")

public class Student{

}
```

