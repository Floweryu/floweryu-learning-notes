### 作用

> 满足Conditional指定的条件，则进行组件注入

- ConditionalOnBean：当容器中有Bean才注入
- ConditionalOnMissingBean：当容器中没有Bean才注入
- ConditionalOnClass：当容器中有Class才注入
- ConditionalOnMissingClass：当容器中没有Class才注入
- ConditionalOnResource：存在资源才注入
- ......

![image-20220103204543723](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202201032045798.png)

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
// 注意：如果这里将name修改为Tom，这该类永远不会注入成功，因为Tom需要在此类中注入，而Tom没注入，此类也不会进行注入，类似于si'shu
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

