### 懒加载@Lazy注解

**单实例Bean，默认在容器启动的时候创建对象。**

**懒加载：容器启动不创建对象，第一次使用（获取）Bean创建对象，并初始化**

### 示例：

在单例模式下，如果不加上@Lazy注解，则会在容器启动时创建对象

```java
@Configuration
public class MainConfig2 {
    
    @Bean("book")
    public Book book() {
        System.out.println("向容器中添加book...");
        return new Book("小王子", "猫儿");
    }
}
```

```java
@Test
public void config2Test() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig2.class);
    System.out.println("容器创建完成");
}
```

输出：

```bash
向容器中添加book...
容器创建完成
```

如果加上@Lazy注解，则会在获取bean时创建bean

```java
@Configuration
public class MainConfig2 {
    
    @Lazy
    @Bean("book")
    public Book book() {
        System.out.println("向容器中添加book...");
        return new Book("小王子", "猫儿");
    }
}
```

```java
@Test
public void config2Test() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig2.class);
    System.out.println("容器创建完成");
    Object bean = context.getBean("book");
}
```

输出：

```bash
容器创建完成
向容器中添加book...
```

