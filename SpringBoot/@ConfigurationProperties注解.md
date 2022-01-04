## 使用方式

#### 1. @Component+@ConfigurationProperties

- 需要实现set/get方法

```java
@ConfigurationProperties(prefix = "car")
@Component
public class Car {
    private String name;
    
    private Integer price;

    private Pet pet;

	// standard getters and setters
}
```

#### 2. ＠EnableConfigurationProperties + @ConfigurationProperties

> ＠EnableConfigurationProperties开启对@ConfigurationProperties注解配置Bean的支持
>
> 也就是@EnableConfigurationProperties注解告诉Spring Boot 使能支持@ConfigurationProperties

配置类：

```java
@Configuration
@EnableConfigurationProperties(Car.class)
public class MyConfig {

    @Bean("Tom")
    public Pet petBean() {
        return new Pet("cat");
    }
	......
}
```
普通类（需要读取配置文件）

```java
@ConfigurationProperties(prefix = "car")
public class Car {
    private String name;
    
    private Integer price;

    private Pet pet;

	// standard getters and setters
}
```

## 读取配置时添加校验

```java
@ConfigurationProperties(prefix = "car")
@Validated
public class Car {
    @NotNull
    private String name;
    
    @NotEmpty
    private Integer price;

    private Pet pet;

	// standard getters and setters
}
```

这时，如果配置文件写成下面这样，会抛出异常。

```xml
car:
#  name: timi
  price: 
  pet: 
    name: pig
```



