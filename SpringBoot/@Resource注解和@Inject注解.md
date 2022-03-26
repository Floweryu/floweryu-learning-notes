> @Resource注解是JSR250规定的，@Inject注解是JSR330规定的，是java规范

### @Resource注解

在Service中注入一个`configDao`

```java
@Service
public class ConfigService {
    
    @Resource
    private ConfigDao configDao;
    
}
```

在配置文件中再注入一个`configDao2`：注意这里使用了`@Primary`

```java
@Configuration
@ComponentScan({"com.floweryu.example.service", "com.floweryu.example.dao", "com.floweryu.example.controller"})
public class MainConfigOfAutowired {
    
    @Primary
    @Bean("configDao2")
    public ConfigDao configDao() {
        ConfigDao configDao = new ConfigDao();
        configDao.setLabel("2");
        return configDao;
    }
}
```

最终容器中注入的是`@Resource`注解注入的`configDao`

**`@Resource`类似于`@Autowired`可以进行组件装配，但是它是按照属性名称进行装配，这也是上面例子中`@Primary`失效的原因。可以使用name属性指定要装配的bean**

**`@Resource`不支持@Primary和@Autowired(required=false)功能**

```java
public @interface Resource {
    String name() default "";

    String lookup() default "";

    Class<?> type() default Object.class;

    Resource.AuthenticationType authenticationType() default Resource.AuthenticationType.CONTAINER;

    boolean shareable() default true;

    String mappedName() default "";

    String description() default "";

    public static enum AuthenticationType {
        CONTAINER,
        APPLICATION;

        private AuthenticationType() {
        }
    }
}
```

### @Inject注解

需要引入依赖包：

```xml
<!-- https://mvnrepository.com/artifact/javax.inject/javax.inject -->
<dependency>
    <groupId>javax.inject</groupId>
    <artifactId>javax.inject</artifactId>
    <version>1</version>
</dependency>

```
将上面例子使用`@Resource`改为`@Inject`
```java
@Service
public class ConfigService {
    
    @Inject
    private ConfigDao configDao;
    
}
```

输出的是`ConfigService{configDao=ConfigDao{label='2'}}`，这次可以支持@Primary

**`@Inject`注解支持@Primary注解，功能和@Autowired一样，但没有required=false**

