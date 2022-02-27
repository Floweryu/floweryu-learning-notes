给注解中注册组件：

> a. 包扫描 + 组件标注（@Controller/@Service/@Repository/@Component），在自己写类一般使用
>
> b. @Bean[导入第三方组件]
>
> c. @Import[快速给容器导入组件]

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
