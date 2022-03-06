### 1. 作用

`BeanPostProcessor`接口里面有下面两个方法：实现这两个方法可以在bean的初始化前后进行操作。

实现该接口的类必须注入到容器中才能生效

```java
/**
Apply this BeanPostProcessor to the given new bean instance before any bean initialization callbacks (like InitializingBean's  afterPropertiesSet or a custom init-method). The bean will already be populated with property values. The returned bean instance may be a wrapper around the original.
*/
@Nullable
default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
}

/**
Apply this BeanPostProcessor to the given new bean instance after any bean initialization callbacks (like InitializingBean's afterPropertiesSet or a custom init-method). The bean will already be populated with property values. The returned bean instance may be a wrapper around the original.
*/
@Nullable
default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return bean;
}
```

### 2. 使用方法

先实现`BeanPostProcessor`接口，然后将其注入到容器中

```java
/**
 * 初始化前后进行操作
 * 需要注入到容器中
 * @author Floweryu
 * @date 2022/3/6 20:53
 */
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("postProcessBeforeInitialization... " + bean + " -> [" + beanName + "]");
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("postProcessAfterInitialization... " + bean + " -> [" + beanName + "]");
        return bean;
    }
}
```

下面，针对不同的初始化的方法，都会在bean的初始化前后生效：

#### 2.1 对于实现`InitializingBean`接口的类

```java
@Component
public class Cat implements InitializingBean, DisposableBean {
    
    public Cat() {
        System.out.println("Cat construct....");
    }
    
    @Override
    public void destroy() throws Exception {
        System.out.println("Cat destroy....");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("Cat afterPropertiesSet....");
    }
}
```

> 输出：
>
> postProcessBeforeInitialization... com.floweryu.example.bean.Cat@44a7bfbc -> [cat]
> Cat afterPropertiesSet....
> postProcessAfterInitialization... com.floweryu.example.bean.Cat@44a7bfbc -> [cat]

#### 2.2 对于使用`@PostConstruct`初始化的类：

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
```

> 输出：
>
> postProcessBeforeInitialization... com.floweryu.example.bean.Dog@306cf3ea -> [dog]
> Dog @PostConstruct....
> postProcessAfterInitialization... com.floweryu.example.bean.Dog@306cf3ea -> [dog]

#### 2.3 对于使用@Bean自定义的类

```java
@ComponentScan("com.floweryu.example.bean")
@Configuration
public class MainConfigOfLifeCycle {
    
    @Bean(initMethod = "init", destroyMethod = "destory")
    public Car car() {
        return new Car();
    }
}

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

> 输出：
>
> postProcessBeforeInitialization... com.floweryu.example.bean.Car@55536d9e -> [car]
> Car init....
> postProcessAfterInitialization... com.floweryu.example.bean.Car@55536d9e -> [car]
