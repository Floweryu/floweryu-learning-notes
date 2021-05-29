1. 作⽤对象不同: @Component 注解作⽤于类，⽽ @Bean 注解作⽤于⽅法
2. @Component 通常是通过类路径扫描来⾃动侦测以及⾃动装配到Spring容器中（我们可以使 ⽤ @ComponentScan 注解定义要扫描的路径从中找出标识了需要装配的类⾃动装配到 Spring 的 bean 容器中）。 @Bean 注解通常是我们在标有该注解的⽅法中定义产⽣这个 bean, @Bean 告诉了Spring这是某个类的示例，当我需要⽤它的时候还给我。
3. @Bean 注解⽐ Component 注解的⾃定义性更强，⽽且很多地⽅我们只能通过 @Bean 注 解来注册bean。⽐如当我们引⽤第三⽅库中的类需要装配到 Spring 容器时，则只能通过 @Bean 来实现

@Bean 注解使⽤示例：

```java
@Configuration
public class AppConfig {
 	 @Bean
     public TransferService transferService() {
        return new TransferServiceImpl();
     }
}
```

上⾯的代码相当于下⾯的 xml 配置:

```xml
<beans>
 <bean id="transferService" class="com.acme.TransferServiceImpl"/>
</beans>
```

下⾯这个例⼦是通过 @Component ⽆法实现的。

```java
@Bean
public OneService getService(status) {
     case (status) {
         when 1:
         return new serviceImpl1();
         when 2:
         return new serviceImpl2();
         when 3:
         return new serviceImpl3();
     }
}
```

