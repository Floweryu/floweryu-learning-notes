## 设置默认的属性编辑器（可扩展）

核心方法：`org.springframework.beans.support.ResourceEditorRegistrar#registerCustomEditors`

```java
public void registerCustomEditors(PropertyEditorRegistry registry) {
    ResourceEditor baseEditor = new ResourceEditor(this.resourceLoader, this.propertyResolver);
    doRegisterEditor(registry, Resource.class, baseEditor);
    doRegisterEditor(registry, ContextResource.class, baseEditor);
    doRegisterEditor(registry, WritableResource.class, baseEditor);
    doRegisterEditor(registry, InputStream.class, new InputStreamEditor(baseEditor));
    doRegisterEditor(registry, InputSource.class, new InputSourceEditor(baseEditor));
    doRegisterEditor(registry, File.class, new FileEditor(baseEditor));
    doRegisterEditor(registry, Path.class, new PathEditor(baseEditor));
    doRegisterEditor(registry, Reader.class, new ReaderEditor(baseEditor));
    doRegisterEditor(registry, URL.class, new URLEditor(baseEditor));

    ClassLoader classLoader = this.resourceLoader.getClassLoader();
    doRegisterEditor(registry, URI.class, new URIEditor(classLoader));
    doRegisterEditor(registry, Class.class, new ClassEditor(classLoader));
    doRegisterEditor(registry, Class[].class, new ClassArrayEditor(classLoader));

    if (this.resourceLoader instanceof ResourcePatternResolver) {
        doRegisterEditor(registry, Resource[].class,
                         new ResourceArrayPropertyEditor((ResourcePatternResolver) this.resourceLoader, this.propertyResolver));
    }
}
```

> 该方法在后面类填充属性的时候会调用。

如何自定义上面属性编辑器？

1. 自定义一个实现`PropertyEditorSupport`接口的编辑器。可以参考其它编辑器发现都实现了该接口。
2. 为了让spring能识别编辑器，需要自定义一个类实现`PropertyEditorRegistrar`接口，该类的作用是将属性编辑器注册到spring中。参考`ResourceEditorRegistrar`类。
3. 为了让spring识别注册器，需要将注册器注册到spring中。调用`org.springframework.beans.factory.config.CustomEditorConfigurer#setPropertyEditorRegistrars`方法。

### 如何自定义？

#### 1. 首先创建两个包装类：

创建包装类和对应的属性配置文件

```properties
customer.name= zzz
customer.address= 湖北省_襄阳市_谷城县
```

```java
public class Customer {

	@Value("${customer.name}")
	private String name;

	@Value("${customer.address}")
	private Address address;

	//......
}

public class Address {
	private String province;
	private String city;
	private String town;

	// ......
}
```

#### 2. 自定义属性编辑器

实现`PropertyEditorSupport`接口的`setAsText`方法：

```java
public class MyAddressPropertyEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		String[] s = text.split("_");
		Address as = new Address();
		as.setProvince(s[0]);
		as.setCity(s[1]);
		as.setTown(s[2]);
		this.setValue(as);
	}
}
```

#### 3. 向spring中注册属性编辑器

```java
public class MyAddressPropertyEditorRegistrar implements PropertyEditorRegistrar {
	@Override
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		registry.registerCustomEditor(Address.class, new MyAddressPropertyEditor());
	}
}

```

#### 4.让spring识别注册器

```java
@Configuration
@PropertySource("classpath:application.properties")
public class EditorConfig {

	@Bean
	public Customer customer() {
	    return new Customer();
	}

	@Bean
	public CustomEditorConfigurer addressPropertyEditor() {
		CustomEditorConfigurer configurer = new CustomEditorConfigurer();
        // 注入注册器
		configurer.setPropertyEditorRegistrars(new PropertyEditorRegistrar[]{new MyAddressPropertyEditorRegistrar()});
       	// 也可以直接设置编辑器，原因可见下面源码
		// configurer.setCustomEditors(Map.of(Address.class, MyAddressPropertyEditor.class));
		return configurer;
	}
}
```

> org.springframework.beans.factory.config.CustomEditorConfigurer#postProcessBeanFactory源码

```java
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    if (this.propertyEditorRegistrars != null) {
        for (PropertyEditorRegistrar propertyEditorRegistrar : this.propertyEditorRegistrars) {
            beanFactory.addPropertyEditorRegistrar(propertyEditorRegistrar);
        }
    }
    if (this.customEditors != null) {
        this.customEditors.forEach(beanFactory::registerCustomEditor);
    }
}
```



#### 5. 运行测试类

```java
@Test
public void properTest() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(EditorConfig.class);
    Customer bean = context.getBean(Customer.class);
    Address address = bean.getAddress();
    System.out.println(address);
}
```

输出：

> Address{province='湖北省', city='襄阳市', town='谷城县'}

## Spring自定义postProcessBeanFactory

### 源码位置

`org.springframework.context.support.AbstractApplicationContext#postProcessBeanFactory`

可以看到在`AbstractApplicationContext`内部，该接口实现是空的。但spring支持我们自定义该接口。

```java
protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
}
```

### 自定义该接口

**只需要在继承了`AbstractApplicationContext`类的子类中重写该方法即可。**

比如下面自定义一个`MyClassPathXmlApplicaionContext`继承`ClassPathXmlApplicationContext`，就可以实现该方法：

```java
public class MyClassPathXmlApplicaionContext extends ClassPathXmlApplicationContext {
	
	public MyClassPathXmlApplicaionContext(String... configLocations) {
		super(configLocations);
	}
	
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		System.out.println("扩展实现postPrecessBeanFactory");
	}
}
```

## Spring自定义BeanFactoryPostProcessor

在源码中，可以在该方法中`org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors`

中有对BeanFactoryPostProcessor进行处理，但是如果没有自定义BeanFactoryPostProcessor，在该方法中的`beanFactoryPostProcessors`是空的。下面来自定义一个BeanFactoryPostProcessor。

### 自定义

先创建一个自定义类

```java
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("自定义postProcessBeanFactory");
	}
}
```

然后添加到容器中

```java
public class MyClassPathXmlApplicaionContext extends ClassPathXmlApplicationContext {
	
	public MyClassPathXmlApplicaionContext(String... configLocations) {
		super(configLocations);
	}

	@Override
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {

        // 添加自定义BeanFactoryPostProcessor
		super.addBeanFactoryPostProcessor(new MyBeanFactoryPostProcessor());
		super.customizeBeanFactory(beanFactory);
	}
}
```

