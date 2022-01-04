## 前言

`@SpringBootApplication` 注解详情：

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
    excludeFilters = {@Filter(
    type = FilterType.CUSTOM,
    classes = {TypeExcludeFilter.class}
), @Filter(
    type = FilterType.CUSTOM,
    classes = {AutoConfigurationExcludeFilter.class}
)}
)
public @interface SpringBootApplication {
    ......
}
```

虽然使用了很多注解，但其中重要的也就下面三个：

- `@Configuration`(`@SpringBootConfiguration`点开查看发现里面还是应用了`@Configuration`)
- `@EnableAutoConfiguration`
- `@ComponentScan`

其实，可以使用这三个注解代替`@SpringBootApplication`注解，但是这样比较麻烦。

本篇文章主要理解`@EnableAutoConfiguration`注解

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigurationPackage
@Import({AutoConfigurationImportSelector.class})
public @interface EnableAutoConfiguration {
    String ENABLED_OVERRIDE_PROPERTY = "spring.boot.enableautoconfiguration";

    Class<?>[] exclude() default {};

    String[] excludeName() default {};
}

```

## 从源码看原理

最关键的是`@Import({AutoConfigurationImportSelector.class})`，借助`AutoConfigurationImportSelector`，`@EnableAutoConfiguration`可以帮助SpringBoot应用将所有符合条件的`@Configuration`配置都加载到当前SpringBoot创建并使用的IOC容器。

在`AutoConfigurationImportSelector`类中可以看到下面这句代码：

```java
List<String> configurations = SpringFactoriesLoader.loadFactoryNames(this.getSpringFactoriesLoaderFactoryClass(), this.getBeanClassLoader());
```

把 `spring-boot-autoconfigure.jar/META-INF/spring.factories`中每一个**xxxAutoConfiguration**文件都加载到容器中，`spring.factories`文件里每一个**xxxAutoConfiguration**文件一般都会有下面的条件注解:

- `@ConditionalOnClass` ： 存在该类时起效
- `@ConditionalOnMissingClass` ：不存在该类时起效
- `@ConditionalOnBean`： 容器中存在该类型Bean时起效
- `@ConditionalOnMissingBean` ： 容器中不存在该类型Bean时起效
- `@ConditionalOnSingleCandidate` ： 容器中该类型Bean只有一个或@Primary的只有一个时起效
- `@ConditionalOnExpression` ： SpEL表达式结果为true时
- `@ConditionalOnProperty `： 参数设置或者值一致时起效
- `@ConditionalOnResource` ： 指定的文件存在时起效
- `@ConditionalOnJndi` ： 指定的JNDI存在时起效
- `@ConditionalOnJava` ： 指定的Java版本存在时起效
- `@ConditionalOnWebApplication` ： Web应用环境下起效
- `@ConditionalOnNotWebApplication` ： 非Web应用环境下起效

![image-20220104230544820](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202201042305654.png)

### SpringFactoriesLoader

`SpringFactoriesLoader`主要功能就是从指定的配置文件`META-INF/spring-factories`加载配置。

`spring-factories`是一个典型的java properties文件，只不过Key和Value都是Java类型的完整类名，比如：

```xml
org.springframework.context.ApplicationListener=\
org.springframework.boot.autoconfigure.BackgroundPreinitializer
```

在`@EnableAutoConfiguration`场景中，`SpringFactoriesLoader`提供了一种配置查找的功能支持，即根据`@EnableAutoConfiguration`的完整类名`org.springframework.boot.autoconfig.EnableAutoConfiguration`作为查找的Key，获得对应的一组`@Configuration`类。


`SpringFactoriesLoader`类中定义的静态属性定义了其加载资源的路径`public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories`，此外还有四个静态方法：

- `loadFactories`：加载指定的factoryClass并进行实例化。
- `loadFactoryNames`：加载指定的factoryClass的名称集合。
- `instantiateFactory`：对指定的factoryClass进行实例化。
- `loadSpringFactories`：加载springFactory

在`loadFactories`方法中调用了`loadFactoryNames`以及`instantiateFactory`方法。

```java
public static <T> List<T> loadFactories(Class<T> factoryType, @Nullable ClassLoader classLoader) {
    Assert.notNull(factoryType, "'factoryType' must not be null");
    ClassLoader classLoaderToUse = classLoader;
    if (classLoader == null) {
        classLoaderToUse = SpringFactoriesLoader.class.getClassLoader();
    }

    List<String> factoryImplementationNames = loadFactoryNames(factoryType, classLoaderToUse);
    if (logger.isTraceEnabled()) {
        logger.trace("Loaded [" + factoryType.getName() + "] names: " + factoryImplementationNames);
    }

    List<T> result = new ArrayList(factoryImplementationNames.size());
    Iterator var5 = factoryImplementationNames.iterator();

    while(var5.hasNext()) {
        String factoryImplementationName = (String)var5.next();
        result.add(instantiateFactory(factoryImplementationName, factoryType, classLoaderToUse));
    }

    AnnotationAwareOrderComparator.sort(result);
    return result;
}
```

`loadFactories`方法首先获取类加载器，然后调用`loadFactoryNames`方法获取所有的指定资源的名称集合、接着调用`instantiateFactory`方法实例化这些资源类并将其添加到result集合中。最后调用`AnnotationAwareOrderComparator.sort`方法进行集合的排序。


