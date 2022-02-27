给注解中注册组件：

> a. 包扫描 + 组件标注（@Controller/@Service/@Repository/@Component），在自己写类一般使用
>
> b. @Bean[导入第三方组件]
>
> c. @Import[快速给容器导入组件]
>
> d. 使用Spring提供的工厂FactoryBean——默认获取的是工厂Bean调用getObject()创建的对象，在bean的名字前加上&可以获取到工厂Bean本身

## @Import注解作用

```java
@Import({Color.class, Red.class})
```

> 给容器中自动创建这两个类型的组件，默认组件的名称是全类名
>
> com.floweryu.example.config.bean.User

## @ImportSelector注解作用

> 返回需要导入的组件的全类名数组

需要先实现ImportSelector接口`selectImports`：

```java
public class MyImportSelector implements ImportSelector {

    /**
     * 获取导入到容器中的组件全类名
     * @param importingClassMetadata 当前标注@Import注解的类的所有信息
     */
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        // 方法不要返回Null值
        return new String[] {"com.floweryu.example.bean.Blue", "com.floweryu.example.bean.Green"};
    }
}
```

然后在@Import注解中导入该类：

```java
@Configuration
@Import({Color.class, Red.class, MyImportSelector.class})
public class MainConfig2 {
    
    @Lazy
    @Bean("book")
    public Book book() {
        System.out.println("向容器中添加book...");
        return new Book("小王子", "猫儿");
    }
}
```

这样就可以将`Blue和Green`类注入到容器中

## @ImportBeanDefinitionRegistrar注解作用

> 可以手动并且按条件注入bean

先实现ImportBeanDefinitionRegistrar里面的一个接口`registerBeanDefinitions`：

```java
public class MyImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
    /**
     * 
     * @param importingClassMetadata 当前类的注解信息
     * @param registry：BeanDefinition注册类
     *                把所有需要添加到容器中的Bean，
     *                使用BeanDefinitionRegistry.registerBeanDefinition手动注入
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        boolean red = registry.containsBeanDefinition("com.floweryu.example.bean.Red");
        boolean blue = registry.containsBeanDefinition("com.floweryu.example.bean.Blue");
        // 如果容器中有上面的bean，则注入rainBow
        if (red && blue) {
            RootBeanDefinition rootBeanDefinition = new RootBeanDefinition(RainBow.class);
            // 注入一个bean，并且指定名字
            registry.registerBeanDefinition("rainBow", rootBeanDefinition);
        }
    }
}
```

然后在@Import中注入上面类：

```java
@Configuration
@Import({Color.class, Red.class, MyImportSelector.class, MyImportBeanDefinitionRegistrar.class})
public class MainConfig2 {
    
    @Lazy
    @Bean("book")
    public Book book() {
        System.out.println("向容器中添加book...");
        return new Book("小王子", "猫儿");
    }
}
```

这样如果容器中存在符合条件的bean，就会注入自定义的bean.

## FactoryBean

实现`FactoryBean`接口，创建工厂bean

```java
public class ColorFactoryBean implements FactoryBean{
    /**
     * 返回一个color对象，添加到容器中
     */
    @Override
    public Object getObject() throws Exception {
        System.out.println("getObject ...");
        return new Color();
    }

    @Override
    public Class<?> getObjectType() {
        return Color.class;
    }
}
```

然后注入这个工厂bean

```java
@Configuration
public class MainConfig2 {
    @Bean
    public ColorFactoryBean colorFactoryBean() {
        return new ColorFactoryBean();
    }
}
```

最后测试获取bean

```java
@Test
public void importTest() {
    Object colorFactortyBean = context.getBean("colorFactoryBean");
    System.out.println("colorFactoryBean...  " + colorFactortyBean.getClass());
}
// 输出如下
getObject ...
colorFactoryBean...  class com.floweryu.example.bean.Color
```

发现最终获取的Bean是Color

**如果想获取这个工厂bean怎么办？**

在前面添加**&**符号，这个在`BeanFactory`接口中可以查到

```java
@Test
public void importTest() {
    printNames();
    Object colorFactortyBean = context.getBean("&colorFactoryBean");
    System.out.println("colorFactoryBean...  " + colorFactortyBean.getClass());
}
```



