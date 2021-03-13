### 切面(Aop)

**一、概念**

> AOP（Aspect OrientedProgramming）：面向切面编程，面向切面编程（也叫面向方面编程），是目前软件开发中的一个热点，也是Spring框架中的一个重要内容。利用AOP可以对业务逻辑的各个部分进行隔离，从而使得业务逻辑各部分之间的耦合度降低，提高程序的可重用性，同时提高了开发的效率。

**二、用途**

> 日志记录，性能统计，安全控制，权限管理，事务处理，异常处理，资源池管理。

**三、详解**

1.切面（Aspect）:
官方的抽象定义为“一个关注点的模块化，这个关注点可能会横切多个对象”，在本例中，“切面”就是类TestAspect所关注的具体行为，例如：AServiceImpl.barA()的调用就是切面TestAspect所关注的行为之一。“切面”在ApplicationContext中aop:aspect来配置。

2.连接点（Joinpoint）:
程序执行过程中的某一行为，例如，AServiceImpl.barA()的调用或者BServiceImpl.barB(String _msg, int _type)抛出异常等行为。

3.通知（Advice）:
“切面”对于某个“连接点”所产生的动作，例如，TestAspect中对com.spring.service包下所有类的方法进行日志记录的动作就是一个Advice。其中，一个“切面”可以包含多个“Advice”，例如TestAspect。Advice共有如下5种类型：

- A 前置通知（Before advice） ：在某连接点（JoinPoint）之前执行的通知，但这个通知不能阻止连接点前的执行。xml中在aop:aspect里面使用aop:before元素进行声明；例如，TestAspect中的doBefore方法。注解中使用@Before声明；例如，TestAnnotationAspect中的doBefore方法。
- B 后通知（After advice）：当某连接点退出的时候执行的通知（不论是正常返回还是异常退出）。xml中在aop:aspect里面使用aop:after元素进行声明。例如，TestAspect中的doAfter方法，所以AOPTest中调用BServiceImpl.barB抛出异常时，doAfter方法仍然执行。注解中使用@After声明。
- C 返回后通知（After return advice）：在某连接点正常完成后执行的通知，不包括抛出异常的情况。xml中在aop:aspect里面使用元素进行声明。注解中使用@AfterReturning声明；
- D 环绕通知（Around advice） ：包围一个连接点的通知，类似Web中Servlet规范中的Filter的doFilter方法。可以在方法的调用前后完成自定义的行为，也可以选择不执行。xml中在aop:aspect里面使用aop:around元素进行声明。例如，TestAspect中的doAround方法。注解中使用@Around声明。
- E 抛出异常后通知（After throwing advice） ： 在方法抛出异常退出时执行的通知。xml中在aop:aspect里面使用aop:after-throwing元素进行声明。例如，TestAspect中的doThrowing方法。注解中使用@AfterThrowing声明。
- 通知执行顺序：前置通知→环绕通知连接点之前→连接点执行→环绕通知连接点之后→返回通知→后通知 →(如果发生异常)异常通知→后通知。

4.切入点（Pointcut）匹配连接点的断言，在AOP中通知和一个切入点表达式关联。例如，TestAspect中的所有通知所关注的连接点，都由切入点表达式execution(* com.spring.service.*.*(..))来决定。

● 切入点表达式

**execution**：用于匹配方法执行的连接点；

**within**：用于匹配指定类型内的方法执行；

**this**：用于匹配当前AOP代理对象类型的执行方法；注意是AOP代理对象的类型匹配，这样就可能包括引入接口也类型匹配；注意this中使用的表达式必须是完整类名，不支持通配符；

**target**：用于匹配当前目标对象类型的执行方法；注意是目标对象的类型匹配，这样就不包括引入接口也类型匹配；注意target中使用的表达式必须是完整类名，不支持通配符；

**args**：用于匹配当前执行的方法传入的参数为指定类型的执行方法；参数类型列表中的参数必须是完整类名，通配符不支持；args属于动态切入点，这种切入点开销非常大，非特殊情况最好不要使用；

**@within**：用于匹配所以持有指定注解类型内的方法；注解类型也必须是完整类名；

**@target**：用于匹配当前目标对象类型的执行方法，其中目标对象持有指定的注解；注解类型也必须是完整类名；

**@args**：用于匹配当前执行的方法传入的参数持有指定注解的执行；注解类型也必须是完整类名；

**@annotation**：用于匹配当前执行方法持有指定注解的方法；注解类型也必须是完整类名；

**bean**：Spring AOP扩展的，AspectJ没有对于指示符，用于匹配特定名称的Bean对象的执行方法；

**reference pointcut**：表示引用其他命名切入点，只有注解风格支持，XML风格不支持。