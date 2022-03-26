AOP：动态代理

**指程序在运行期间动态的将某段代码切入到指定方法，指定位置进行运行的编程方式**

#### 使用例子

需要引入下面依赖

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
    <version>5.3.14</version>
</dependency>
```

添加切面类：

```java
@Aspect
public class LogAspects {

    /**
     * 1. 本类引用，只需要写方法名
     * 2. 其他类引用，需要写路径
     */
    @Pointcut("execution(public int com.floweryu.example.aop.MathCalculator.*(..))")
    public void pointCut() {}

    /**
     * 前置通知：在目标方法被调用之前调用通知功能
     * JoinPoint一定要在参数l的第一位
     */
    @Before("pointCut()")
    public void logStart(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        System.out.println(joinPoint.getSignature().getName() + " 运行开始.......参数列表是: {" + Arrays.toString(args) + "}");
    }

    /**
     * 后置通知：在目标方法调用之后调用通知，此时不关心方法的输出结果
     */
    @After("pointCut()")
    public void logEnd() {
        System.out.println("除法结束.......");
    }

    /**
     * 返回通知：在目标方法执行成功后调用通知
     */
    @AfterReturning(value = "pointCut()", returning = "returning")
    public void logReturn(Object returning) {
        System.out.println("除法正常返回.......返回值: {" + returning + "}");
    }

    /**
     * 异常通知：在目标方法抛出异常后调用通知
     */
    @AfterThrowing(value = "pointCut()", throwing = "ex")
    public void logException(Exception ex) {
        System.out.println("除法异常.......异常: {" + ex +"}");
    }
}
```

添加目标类：

```java
public class MathCalculator {
    
    public int div(int i, int j) {
        System.out.println("MathCalculator is running...");
        return i / j;
    }
}
```

将两个类注入到容器中，不然不会生效

```java
@Configuration
@EnableAspectJAutoProxy  // 开启AOP支持
public class MainConfigOfAop {
    
    @Bean
    public MathCalculator mathCalculator() {
        return new MathCalculator();
    }
    
    
    @Bean
    public LogAspects logAspects() {
        return new LogAspects();
    }
}
```

最后，从容器中获取 `MathCalculator`类，因为AOP只对容器中的类有效

```java
@Test
public void aopTest() {
    MathCalculator bean = context.getBean(MathCalculator.class);
    bean.div(8, 2);
    context.close();
}

// 输出：
除法开始.......参数列表是: {}
MathCalculator is running...
除法正常返回.......
除法结束.......
```

