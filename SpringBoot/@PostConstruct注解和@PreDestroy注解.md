### @PostConstruct注解作用：

>  在bean创建完成并且属性复制完成后，执行初始化方法

### @PreDestroy注解使用：

> 在bean移除之前使用

上面两个注解不属于Spring，在下面依赖中：

```xml
<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>1.3.2</version>
</dependency>
```

#### 【示例】：

```java
@Component
public class Dog {
    public Dog() {
        System.out.println("Dog Construct....");
    }

    /**
     * 对象创建并赋值后使用
     */
    @PostConstruct
    public void init() {
        System.out.println("Dog @PostConstruct....");
    }

    /**
     * 容器移除对象之前使用
     */
    @PreDestroy
    public void destroy() {
        System.out.println("Dog @PreDestroy");
    }
}

// 输出：
Dog Construct....
Dog @PostConstruct....
org.springframework.context.annotation.AnnotationConfigApplicationContext@52af6cff, started on Sun Mar 06 16:46:48 CST 2022
Dog @PreDestroy
```

