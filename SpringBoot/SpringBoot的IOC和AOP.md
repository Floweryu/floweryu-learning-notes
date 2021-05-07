# 1. IOC

控制反转(IOC)和依赖注入(DI)是同一个概念，目的在于降低系统耦合，将类的实例化工作交给Spring代理，主要用到的设计模式为工厂模式，通过Java反射机制实现类的自动注入。

## 1.2 方式

1. 接口注入
2. Construct注入
3. Setter注入

## 1.3 目的

1. 降低类之间的耦合
2. 倡导面向接口编程、实施依赖倒换原则
3. 提高系统可插入、可测试、可修改特性

## 1.4 具体做法

1. 将bean之间的依赖关系尽可能地转换为关联关系
2. 将对具体类的关联尽可能地转换为对Java 接口的关联，而不是与具体的服务对象相关联
3. bean实例具体关联相关Java 接口的哪个实现类的实例，在配置信息的元数据中描述
4. 由IOC组件（或称容器）根据配置信息，实例化具体bean类，将bean之间的依赖关系注入进来

## 1.5 所属包

`org.springframework.beans`及`org.springframework.context`包是`Spring IoC`容器的基础。

`BeanFactory`提供的高级配置机制，使得管理任何性质的对象成为可能。

`ApplicationContext`是`BeanFactory`的扩展，功能得到了进一步增强，比如更易与`Spring AOP`集成、消息资源处理(国际化处理)、事件传递及各种不同应用层的`context`实现(如针对`web`应用的`WebApplicationContext`)。

简而言之，`BeanFactory`提供了配制框架及基本功能，而`ApplicationContext`则增加了更多支持企业核心内容的功能。`ApplicationContext`完全由`BeanFactory`扩展而来，因而`BeanFactory`所具备的能力和行为也适用于`ApplicationContext`。

IoC容器负责容纳bean，并对`bean`进行管理。在Spring中，`BeanFactory`是IoC容器的核心接口。它的职责包括：实例化、定位、配置应用程序中的对象及建立这些对象间的依赖。Spring为我们提供了许多易用的`BeanFactory`实现，`XmlBeanFactory`就是最常用的一个。该实现将以XML方式描述组成应用的对象以及对象间的依赖关系。`XmlBeanFactory`类将持有此XML配置元数据，并用它来构建一个完全可配置的系统或应用。

```java
Resource resource = new FileSystemResource("beans.xml");
BeanFactory factory = new XmlBeanFactory(resource);
... 或...
ClassPathResource resource = new ClassPathResource("beans.xml");
BeanFactory factory = new XmlBeanFactory(resource);
... 或...
ApplicationContext context = new ClassPathXmlApplicationContext(
        new String[] {"applicationContext.xml", "applicationContext-part2.xml"});
// of course, an ApplicationContext is just a BeanFactory
BeanFactory factory = (BeanFactory) context;
```

将XML配置文件分拆成多个部分是非常有用的。为了加载多个XML文件生成一个ApplicationContext实例，可以将文件路径作为字符串数组传给`ApplicationContext`构造器。而bean factory将通过调用bean defintion reader从多个文件中读取bean定义。

通常情况下，Spring团队倾向于上述做法，因为这样各个配置并不会查觉到它们与其他配置文件的组合。另外一种方法是使用一个或多个的 `<import/>`元素来从另外一个或多个文件加载bean定义。所有的 `<import/>`元素必须放在` <bean/>`元素之前以完成bean定义的导入。 让我们看个例子：

```xml
<beans>
	<import resource="services.xml"/>
    <import resource="resources/messageSource.xml"/>
    <import resource="/resources/themeSource.xml"/>
    <bean id="bean1" class="..."/>
    <bean id="bean2" class="..."/>
</beans>
```

在上面的例子中，我们从3个外部文件：` services.xml、 messageSource.xml及 themeSource.xml`来加载bean定义。这里采用的都是相对路径，因此，此例中的 `services.xml`一定要与导入文件放在同一目录或类路径，而 `messageSource.xml`和 `themeSource.xml`的文件位置必须放在导入文件所在目录下的 `resources`目录中。正如你所看到的那样，开头的斜杠`/`实际上可忽略。因此不用斜杠`/`可能会更好一点。

## 1.6 BeanFactory和FactoryBean的区别

BeanFactory是加载的容器，加载一切的Bean，而FactoryBean用于创建代理类。

BeanFactory它的职责包括：实例化、定位、配置应用程序中的对象及建立这些对象间的依赖。

FactoryBean(通常情况下，bean无须自己实现工厂模式，Spring容器担任工厂角色；但少数情况下，容器中的bean本身就是工厂，其作用是产生其它bean实例),作用是产生其他bean实例。通常情况下，这种bean没有什么特别的要求，仅需要提供一个工厂方法，该方法用来返回其他bean实例。由工厂bean产生的其他bean实例，不再由Spring容器产生，因此与普通bean的配置不同，不再需要提供class元素。

****

`ProxyFactoryBean`用于创建代理(根据Advisor生成的Bean，也就是TargetBean的代理)

# 2. AOP

面向切面编程，通过预编译方式和运行期动态代理实现程序功能的统一维护的一种技术。

利用AOP可以对业务逻辑的各个部分进行隔离，从而使得业务逻辑各部分之间的耦合度降低，提高程序的可重用性，同时提高了开发的效率。

## 2.1 概念

1. 方面（Aspect）：一个关注点的模块化，这个关注点实现可能另外横切多个对象。事务管理是J2EE应用中一个很好的横切关注点例子。方面用Spring的Advisor或拦截器实现。将那些影响了 多个类的公共行为封装到一个可重用模块。简单地说，就是将那些与业务无关，却为业务模块所共同调用的 逻辑或责任封装起来，比如日志记录，便于减少系统的重复代码，降低模块间的耦合度，并有利于未来的可操作性和可维护性。
2. 切入点（Pointcut）：指定一个通知将被引发的一系列连接点的集合。
3. 连接点（Joinpoint）：程序执行过程中明确的点，如方法的调用或特定的异常被抛出。
4. 通知（Advice）：在特定的连接点，AOP框架执行的动作。
5. 目标对象（Target Object）：包含连接点的对象，也被称作被通知或被代理对象。
6. AOP代理（AOP Proxy）：AOP框架创建的对象，包含通知。在Spring中，AOP代理可以是JDK动态代理或CGLIB代理。
7. 引入（Introduction）：添加方法或字段到被通知的类。Spring允许引入新的接口到任何被通知的对象。
8. 编织（Weaving）：组装方面来创建一个被通知对象。

## 2.2 使用场景

-  权限检查 
- 缓存
- 内容传递
- 错误处理
- 日志记录，跟踪，优化，校准
- 性能优化，效率检查

# 3. 设计模式

在使用Spring框架的过程中，其实就是为了使用IOC，依赖注入，和AOP，面向切面编程，这两个是Spring的灵魂。

主要用到的设计模式有工厂模式和代理模式。

IOC就是典型的工厂模式，通过sessionfactory去注入实例。

AOP就是典型的代理模式的体现。

# 4. Spring优点

1. 降低了组件之间的耦合性 ，实现了软件各层之间的解耦
2. 可以使用容易提供的众多服务，如事务管理，消息服务等
3. 容器提供单例模式支持
4. 容器提供了AOP技术，利用它很容易实现如权限拦截，运行期监控等功能
5. 容器提供了众多的辅助类，能加快应用的开发
6. spring对于主流的应用框架提供了集成支持，如hibernate，JPA，Struts等

# 参考文章

- https://blog.csdn.net/MarkSorin/article/details/79640085
- https://blog.csdn.net/baidu_20876831/article/details/78956220
- https://blog.csdn.net/luoshenfu001/article/details/5816408?utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromMachineLearnPai2%7Edefault-2.control&dist_request_id=&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromMachineLearnPai2%7Edefault-2.control