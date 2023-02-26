### 1. 自定义一个注解

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAnnotation {
    
}
```

### 2. 然后使用AOP特性实现注解

```java
@Aspect
@Component
public class MyAnnotationAop {
    @Pointcut("@annotation(com.floweryu.example.annotation.MyAnnotation)")
    public void myAnnotation() {
        
    }
    
    @Before("myAnnotation()")
    public void before() {
        System.out.println("MyAnnotation 开始前");
    }
    
    @AfterReturning(value = "myAnnotation()",returning = "res")
    public Object dochange(JoinPoint joinPoint, Object res){
        System.out.println("AfterReturning 执行前, res: " + res);
        // 获取数据
        Map<String,String> map= (Map<String, String>) res;
        // 添加新值
        map.put("s1","我是在AOP中添加的新值");
        return map;
    }
}
```

### 3. 相关介绍

#### @Target注解

该注解有以下值可赋：

- TYPE：作用在类、接口（包括注解类型），枚举类
- FIELD：字段声明（包含枚举常量）
- METHOD：作用于方法
- PARAMETER：作用于参数
- CONSTRUCTOR：作用于构造器

......

#### @Retention注解

规定了我们**自定义注解的生命周期**

- SOURCE：编译后丢弃
- CLASS：编译后保留，但JVM运行时会忽略
- RUNTIME：运行期保留，在Class中存在，JVM运行时保留，可以通过反射机制来读取该注解信息

