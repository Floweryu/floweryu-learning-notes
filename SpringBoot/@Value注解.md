### 1. 作用

**可以直接赋值：**

```java
@Value("string value")
private String stringValue;
```

**可以使用SpEL表达式：**

```java
@Value("#{systemProperties['priority']}")
private String spelValue;

@Value("#{systemProperties['unknown'] ?: 'some default'}")
private String spelSomeDefault;

@Value("#{someBean.someValue}")
private Integer someBeanValue;

@Value("#{'${listOfValues}'.split(',')}")
private List<String> valuesList;
```

**可以使用`${}`取值:**

```java
@Value("${value.from.file}")
private String valueFromFile;
```

 ### 2. 需要配合@PropertySource注解使用

需要使用该注解在配置类上扫描到配置文件

```
@PropertySource(value = {"classpath:/person.properties"})
```

然后再使用@Value注解的`${}`取值。

也可以使用下面这种方式取值：

```java
AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfigOfProperty.class);
ConfigurableEnvironment environment = context.getEnvironment();
String property = environment.getProperty("person.nickName");
System.out.println(property);
```

