BeanFactoryPostProcessor接口作用：

​	



如果自定义实现BeanFactoryPostProcessor接口，想让Spring识别到，有两种方式：

1. 定义在Spring的配置文件中，让Spring自动识别。
2. 调用具体的`addBeanFactoryPostProcessor`方法。

![image-20230313221134923](./BeanFactoryPostProcessor的执行.assets/image-20230313221134923.png)
