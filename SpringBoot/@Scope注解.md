### 1. 作用

- `ConfigurableBeanFactory.SCOPE_PROTOTYPE`：`prototype`——多实例，IOC容器启动不会调用方法创建对象，在获取时才会创建对象
- `ConfigurableBeanFactory.SCOPE_SINGLETON`：`singleton`——单实例（默认值），IOC启动就会调用方法将对象放入IOC容器中
- `org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST`：`request`——同一次请求创建一个实例
- `org.springframework.web.context.WebApplicationContext.SCOPE_SESSION`：`session`——同一个session创建一个实例

### 2. 示例

#### 2.1 多实例模式

```java
@Configuration
public class MainConfig2 {
    
    @Bean("book")
    public Book book() {
        return new Book("小王子", "猫儿");
    }
}
```

```java
@Test
public void config2Test() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig2.class);
    String[] names = context.getBeanDefinitionNames();
    for (String name : names) {
        System.out.println(name);
    }
    Object book1 = context.getBean("book");
    Object book2 = context.getBean("book");
    System.out.println(book1 == book2);
}
```

默认情况下，@Scope值是单例的，所以上述测试类输出为**true**.

```java
@Configuration
public class MainConfig2 {
    
    @Scope("prototype")
    @Bean("book")
    public Book book() {
        return new Book("小王子", "猫儿");
    }
}
```

将book的作用域设置为多例模式后（即`@Scope("prototype")`），测试类输出为**false**.

#### 2.2 单实例/多实例模式下对象加载

**单实例模式：**

```java
@Configuration
public class MainConfig2 {
    
    @Bean("book")
    public Book book() {
        System.out.println("book is load...");
        return new Book("小王子", "猫儿");
    }
}
```

```java
@Test
public void config2Test() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig2.class);
}
```

测试类输出：`book is load...`

> 单实例模式下，IOC加载时就会把对象存入IOC容器中，以后再用时直接从容器中获取

**多实例模式：**

```java
@Configuration
public class MainConfig2 {
    
    @Scope("prototype")
    @Bean("book")
    public Book book() {
        System.out.println("book is load...");
        return new Book("小王子", "猫儿");
    }
}
```

```java
@Test
public void config2Test() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig2.class);
    Object book1 = context.getBean("book");
    Object book2 = context.getBean("book");
}

// 输出：
// book is load...
// book is load...
```

> 多实例模式下，IOC加载时不会将对象存入IOC容器中，只有在获取bean时才会调用对象