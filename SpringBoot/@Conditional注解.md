### 作用

> 满足Conditional指定的条件，则进行组件注入

- ConditionalOnBean：当容器中有Bean才注入
- ConditionalOnMissingBean：当容器中没有Bean才注入
- ConditionalOnClass：当容器中有Class才注入
- ConditionalOnMissingClass：当容器中没有Class才注入
- ConditionalOnResource：存在资源才注入
- ......

![image-20220103204543723](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202201032045798.png)

## 自定义条件

需要实现`Condition`接口的`matches`方法

```java
public class LinuxCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 获取当前环境信息
        Environment environment = context.getEnvironment();
        String property = environment.getProperty("os.name");
        assert property != null;
        return property.contains("Linux");
    }
}
```

```java
public class WindowsCondition implements Condition {

    /**
     * @param context 判断条件能使用的上下文
     * @param metadata 注释信息
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 1. 获取到ioc使用的beanFactory
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        // 2. 获取到类加载器
        ClassLoader classLoader = context.getClassLoader();
        // 3. 获取当前环境信息
        Environment environment = context.getEnvironment();
        // 4. 获取bean定义的注册类
        BeanDefinitionRegistry registry = context.getRegistry();

        String property = environment.getProperty("os.name");
        assert property != null;
        return property.contains("Windows");
    }
}
```

然后作用在类或方法上：

```java
@Configuration
public class MainConfig2 {
    // 如果是windows系统则注入
    @Conditional({WindowsCondition.class})
    @Bean("windows")
    public Book book1() {
        return new Book("Windows", "11");
    }

    // 如果是l系统则注入
    @Conditional({LinuxCondition.class})
    @Bean("linux")
    public Book book2() {
        return new Book("linux", "8");
    }
}
```



### 实例

#### 1. 作用在方法上：对该方法有效

```java
@Import({User.class})
@Configuration(proxyBeanMethods = true)
public class MyConfig {

    @Bean("Tom")
    public Pet petBean() {
        return new Pet("cat");
    }

    /**
     * 向容器中添加组件，以方法名作为组件名
     */
    @ConditionalOnBean(name = "Tom2")  // 如果不存在Tom2这个bean，则不注入User
    @Bean("Dogger")
    public User user1() {
        User user2 = new User("zhanglei", 18);
        // User组件依赖了Pet组件
        user2.setPet(petBean());
        return user2;
    }
}

// 测试类
boolean b = context.containsBean("Tom");
System.out.println("is Tom exist : " + b);

boolean b1 = context.containsBean("Dogger");
System.out.println("is Dogger exist : " + b1);
// is Tom exist : true
// is Dogger exist : false
```

#### 2. 作用在类上：对整个类有效

注意：如果对整个类使用，则下面情况将永远不会注入成功：

```java
@ConditionalOnBean(name = "Tom2")  // 如果不存在Tom2这个bean，则不注入类里面的所有组件
// 注意：如果这里将name修改为Tom，这该类永远不会注入成功，因为Tom需要在此类中注入，而Tom没注入，此类也不会进行注入，类似于死锁
@Import({User.class})
@Configuration(proxyBeanMethods = true)
public class MyConfig {

    @Bean("Tom")
    public Pet petBean() {
        return new Pet("cat");
    }

    /**
     * 向容器中添加组件，以方法名作为组件名
     */
    @Bean("Dogger")
    public User user1() {
        User user2 = new User("zhanglei", 18);
        // User组件依赖了Pet组件
        user2.setPet(petBean());
        return user2;
    }
}

// 测试类
boolean b = context.containsBean("Tom");
System.out.println("is Tom exist : " + b);

boolean b1 = context.containsBean("Dogger");
System.out.println("is Dogger exist : " + b1);
// is Tom exist : false
// is Dogger exist : false
```

