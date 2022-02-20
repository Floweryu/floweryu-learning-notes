### 1. 作用

**定义扫描的路径**，从中找出标识了**需要装配的类**，自动将这些类装配到Spring的容器中。

### 2. @ComponentScan注解详细作用

下面这些注解：

- @Component
- @Repository
- @Service
- @Controller
- @Configuration

查看它们的源码可以知道：都有一个共同的注解**@Component**，而**@ComponentScan**就会默认装配标识了上述注解的类到容器中。

### 3. 源码解释

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Repeatable(ComponentScans.class)
public @interface ComponentScan {
    // 包扫描的路径，可以是单个，也可以是数组. 如果不设置该值, 则默认为该注解所在类的包
    @AliasFor("basePackages")
    String[] value() default {};

    // 和上述方法一致，可以是单个，也可以是数组. 如果不设置该值, 则默认为该注解所在类的包
    @AliasFor("value")
    String[] basePackages() default {};

    // 指定的具体类所在包下面的所有组件
    Class<?>[] basePackageClasses() default {};

    // Bean的id生成策略，自定义配置后可以自定义Bean的id，默认策略是首字母小写
    Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

    // 解析bean的scope属性
    Class<? extends ScopeMetadataResolver> scopeResolver() default AnnotationScopeMetadataResolver.class;

    ScopedProxyMode scopedProxy() default ScopedProxyMode.DEFAULT;

    String resourcePattern() default "**/*.class";

    boolean useDefaultFilters() default true;

    // 包含bean
    ComponentScan.Filter[] includeFilters() default {};

    // 排除bean
    ComponentScan.Filter[] excludeFilters() default {};

    boolean lazyInit() default false;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    public @interface Filter {
        FilterType type() default FilterType.ANNOTATION;

        @AliasFor("classes")
        Class<?>[] value() default {};

        @AliasFor("value")
        Class<?>[] classes() default {};

        String[] pattern() default {};
    }
}
```

### 4. 使用方法示例

#### 4.1 扫描路径

如下面图片，这里只设置读取`ConfigService`类所在包的所有组件，但是最终输出也有`mainConfig`，是因为该注解作用于MainConfig类，默认会扫描config包。

> 所以，包的路径扫描是多种设置累加的结果

![image-20220220195322567](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202202201953137.png)

#### 4.2 排除bean

FilterType的类型：

- `FilterType.ANNOTATION`：指定注解类型
- `FilterType.ASSIGNABLE_TYPE`：指定类
- `FilterType.ASPECTJ`：指定切入点类型
- `FilterType.REGEX`：指定正则表达式
- `FilterType.CUSTOM`：自定义类型

下面按照注解排除了Service和Controller

![image-20220220210435192](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202202202104277.png)