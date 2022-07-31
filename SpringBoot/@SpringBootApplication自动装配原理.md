查看@SpringBootApplication源码，可以看到，主要由下面三个注解组成：

```java
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
    .......
}
```

## 1. @SpringBootConfiguration注解

底层实际还是`@Configuration`注解：**代表当前类是一个配置类**

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration  // 主要是这个注解
@Indexed
public @interface SpringBootConfiguration {
    @AliasFor(
        annotation = Configuration.class
    )
    boolean proxyBeanMethods() default true;
}
```

## 2. @ComponentScan注解

指定扫描哪些包

## 3. @EnableAutoConfiguration注解

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigurationPackage	// 3.1
@Import({AutoConfigurationImportSelector.class})	// 3.2
public @interface EnableAutoConfiguration {
    String ENABLED_OVERRIDE_PROPERTY = "spring.boot.enableautoconfiguration";

    Class<?>[] exclude() default {};

    String[] excludeName() default {};
}
```

### 3.1 @AutoConfigurationPackage注解

> 将注解标注的类（MainApplication）所在包下的所有组件导入容器

字面意思：自动配置包。

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({Registrar.class})	//　给容器导入组件
public @interface AutoConfigurationPackage {
    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};
}
```

**利用`Registrar`给容器导入一系列组件**

```java
static class Registrar implements ImportBeanDefinitionRegistrar, DeterminableImports {
    Registrar() {
    }

    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        AutoConfigurationPackages.register(registry, (String[])(new AutoConfigurationPackages.PackageImports(metadata)).getPackageNames().toArray(new String[0]));
    }

    public Set<Object> determineImports(AnnotationMetadata metadata) {
        return Collections.singleton(new AutoConfigurationPackages.PackageImports(metadata));
    }
}
```

利用debug可以查看一下信息：

![image-20220108231528364](https://s2.loli.net/2022/07/31/C45pMbozcs3Ojwi.png)

注解的`metadata`就是启动的应用程序（MainApplication），通过该元信息，可以获取到主类所在的包名。因为该注解标注在主类上。 然后将包下面所有组件都导入容器。

### 3.2 @Import({AutoConfigurationImportSelector.class})

主要在于下面这个方法：

```java
public String[] selectImports(AnnotationMetadata annotationMetadata) {
    if (!this.isEnabled(annotationMetadata)) {
        return NO_IMPORTS;
    } else {
        AutoConfigurationImportSelector.AutoConfigurationEntry autoConfigurationEntry = this.getAutoConfigurationEntry(annotationMetadata);
        return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
    }
}
```

利用`this.getAutoConfigurationEntry(annotationMetadata)`给容器批量导入组件：

在此方法中，获取到了所有的组件，这些组件是需要默认导入到容器中的。

```java
protected AutoConfigurationImportSelector.AutoConfigurationEntry getAutoConfigurationEntry(AnnotationMetadata annotationMetadata) {
    if (!this.isEnabled(annotationMetadata)) {
        return EMPTY_ENTRY;
    } else {
        AnnotationAttributes attributes = this.getAttributes(annotationMetadata);
        List<String> configurations = this.getCandidateConfigurations(annotationMetadata, attributes); // 获取所有需要导入容器的配置类
        configurations = this.removeDuplicates(configurations);  // 移除重复的配置
        Set<String> exclusions = this.getExclusions(annotationMetadata, attributes);
        this.checkExcludedClasses(configurations, exclusions);
        configurations.removeAll(exclusions);
        configurations = this.getConfigurationClassFilter().filter(configurations);
        this.fireAutoConfigurationImportEvents(configurations, exclusions);
        return new AutoConfigurationImportSelector.AutoConfigurationEntry(configurations, exclusions);
    }
}
```

![image-20220108232823402](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202201082328906.png)

那么，`List<String> configurations = this.getCandidateConfigurations(annotationMetadata, attributes);`是怎么获取到所有需要导入的类的呢？**利用Spring的工厂加载器`Map<String, List<String>> loadSpringFactories(ClassLoader classLoader)`获取到所有组件**

```java
protected List<String> getCandidateConfigurations(AnnotationMetadata metadata, AnnotationAttributes attributes) {
    List<String> configurations = SpringFactoriesLoader.loadFactoryNames(this.getSpringFactoriesLoaderFactoryClass(), this.getBeanClassLoader());
    Assert.notEmpty(configurations, "No auto configuration classes found in META-INF/spring.factories. If you are using a custom packaging, make sure that file is correct.");
    return configurations;
}
```

下面看看**`loadSpringFactories`**方法源码：

> 默认扫描所有包下面**META-INF/spring.factories**位置的文件
>
> 核心是spring-boot-autoconfigure-2.6.2.jar包下的**META-INF/spring.factories**文件，将*.EnableAutoConfiguration下所有组件都加载，最终会按需配置。

```java
private static Map<String, List<String>> loadSpringFactories(ClassLoader classLoader) {
    Map<String, List<String>> result = (Map)cache.get(classLoader);
    if (result != null) {
        return result;
    } else {
        HashMap result = new HashMap();

        try {
            Enumeration urls = classLoader.getResources("META-INF/spring.factories"); // 默认扫描该系统所有包下此文件
            .......
        }
    }
}
```

**如何按需配置？**

以下面包为例，只有在导入了所需要的包后，才会导入batch包。

![image-20220108235036230](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202201082350379.png)

## 4. 总结

1. SpringBoot先加载所有的自动配置类`xxxAutoConfiguration`

2. 每个自动配置类按照条件生效，默认会绑定配置文件指定的值，`xxxProperties`文件里面拿

3. 生效的配置类会向容器中装配很多组件

4. 组件装配了，就相当于这些功能就有了

5. 用户可以定制配置：

   - 可以直接用@Bean替换底层组件

   - 可以去包里面查看这个组件获取的配置文件什么值，针对修改

总体流程：`xxxAutoConfiguration`---> 组件 ---> `xxxProperties`拿值 ---> `application.properties`

 
