以前的spring是通过写xml文件来注入bean，现在springboot提供了更简单的方式注入bean.

## 使用

> @Configuration标注在类上，相当于把该类作为spring的xml配置文件，作用为：配置spring容器(应用上下文)

```java
/**
 * @author Floweryu
 * @date 2022/1/3 19:29
 * 这是一个配置类
 */
@Configuration 
public class MyConfig {

    /**
     * 向容器中添加组件，以方法名作为组件名
     */
    @Bean("Dogger")
    public User user1() {
        return new User("zhanglei", 18);
    }
}
```

上面代码将向容器中注入一个id为`Dogger`的类，如果没有在`@Bean`中自定义名称，则会向容器中注入`user1`类

在启动类中执行下面代码：

输出为true，说明是从容器中获取的bean，是单例的

```java
User user1 = context.getBean("Dogger", User.class);
User user2 = context.getBean("Dogger", User.class);
System.out.println("siglon: " + (user1 == user2));
```

## 内部原理

查看**@Configuration**内部：

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {
    @AliasFor(
        annotation = Component.class
    )
    String value() default "";

    boolean proxyBeanMethods() default true;
}
```

#### proxyBeanMethods配置

**`proxyBeanMethods()`作用：指定@Bean注解标注的方法是否使用代理.**

> - 默认是true，即使用代理。这时获取组件，springboot会检查该组件是否在IOC容器中，返回的是同一个组件，保证组件单例
> - 如果是false，则每次调用@Bean方法获取到的对象和IOC容器中的都不一样，是一个新的对象
>
> 由于false在启动时不用检查组件是否在容器中，所以启动会比true快

```java
ConfigurableApplicationContext context =  SpringApplication.run(FloweryuApplication.class);
MyConfig myConfig = context.getBean(MyConfig.class);
System.out.println(myConfig);

User user3 = myConfig.user1();
User user4 = myConfig.user1();
System.out.println("proxyBeanMethods check: " + (user3 == user4));
// 如果proxyBeanMethods为true，则输出true
// 如果proxyBeanMethods为false，则输出false
```

#### Full和Lite两种模式

**由配置proxyBeanMethods可以引出Configuration的两种模式：**

>- Full(proxyBeanMethods = true)  如果组件有依赖，使用该模式
>- Lite(proxyBeanMethods = false)  组件无依赖，可以使用该模式，加速启动

下面组件User依赖于组件Pet

```java
@Configuration(proxyBeanMethods = false)
public class MyConfig {

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
    
    @Bean("Tom")
    public Pet petBean() {
        return new Pet("cat");
    }
}
```

```java
User user5 = context.getBean("Dogger", User.class);
Pet pet1 = context.getBean("Tom", Pet.class);
System.out.println("User's pet is equal pet?? " + (user5.getPet() == pet1));
// 如果proxyBeanMethods为true，则输出true
// 如果proxyBeanMethods为false，则输出false
```

