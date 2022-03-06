#### 1. 指定初始化和销毁方法

在xml文件中可以使用`init-method="" destroy-method=""`来自定义Bean的初始化方法和销毁方法。

也可以在@Bean注解中使用上述字段

**构造**：

- 单实例：在容器启动时创建对象
- 多实例：在每次获取时创建对象

**初始化**：

- 对象创建完成，并且复制完成后，调用初始化方法

**销毁**：

- 如果是单实例，则在容器关闭时销毁实例
- 如果是多实例，则容器关闭不会销毁，需要手动销毁

##### 1.1【示例】：

定义一个Car的类：

```java
public class Car {
    
    public Car() {
        System.out.println("construct car...");
    }
    
    public void init() {
        System.out.println("Car init....");
    }
    
    public void destory() {
        System.out.println("Car destory....");
    }
}
```

然后在注入Bean的时候指定初始化和销毁方法：

```java
@Configuration
public class MainConfigOfLifeCycle {
    
    @Bean(initMethod = "init", destroyMethod = "destory")
    public Car car() {
        return new Car();
    }
}
```

最后测试：

```java
public class ConfigTest {

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfigOfLifeCycle.class);
    
    @Test
    public void lifeCycleTest() {
        System.out.println(context);
        context.close();
    }
}

// 单例模式下输出：因为单例模式下bean在容器关闭时会销毁
construct car...
Car init....
org.springframework.context.annotation.AnnotationConfigApplicationContext@3551a94, started on Sun Mar 06 14:19:48 CST 2022
Car destory....
// 多例模式下输出: 可以看到初始化在调用时进行的，并且容器关闭时没有调用销毁方法
org.springframework.context.annotation.AnnotationConfigApplicationContext@3551a94, started on Sun Mar 06 14:27:25 CST 2022
construct car...
Car init....
```

#### 2. 通过实现InitializingBean(定义初始化)，DisposableBean(定义销毁逻辑)

```java
@Component
public class Cat implements InitializingBean, DisposableBean {
    @Override
    public void destroy() throws Exception {
        System.out.println("Cat destroy....");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("Cat afterPropertiesSet....");
    }
}

//输出：
Cat afterPropertiesSet....
org.springframework.context.annotation.AnnotationConfigApplicationContext@3551a94, started on Sun Mar 06 16:13:13 CST 2022
Cat destroy....
```

