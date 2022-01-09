### IOC过程

1. xml配置文件，配置创建的对象

```xml
<bean id="myTestBean" class="com.floweryu.example.bean.MyTestBean" />
```

2. 有service类和dao类，创建工厂类

```java
class Factory {
    public static MyTestBean getBean() {
        String classValue = class属性值;	// 1. xml解析
        Class clazz = Class.forName(classValue);	// 2. 通过反射创建对象
        return (MyTestBean)clazz.newInstance();		// 3. f
    }
}
```

