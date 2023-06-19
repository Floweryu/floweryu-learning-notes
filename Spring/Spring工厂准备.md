org.springframework.context.support.AbstractApplicationContext#prepareBeanFactory方法



#### 第一步：设置类加载器

#### 第二步：设置表达式解析器

Spel表达式解析类，解析`#{}，${}`等配置

### 第三步：设置默认的属性编辑器（可扩展）

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
3. 为了让spring识别注册器，

