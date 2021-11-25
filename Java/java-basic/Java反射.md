## 1.  反射调用静态方法

> 下面实例使用反射调用静态方法的两种类型：public和private

创建一个类`GreetingAndBye`

```java
public class GreetingAndBye {
    public static String greeting(String name) {
        return String.format("Hey %s, nice to meet you!", name);
    }

    private static String goodBye(String name) {
        return String.format("Bye %s, see you next time.", name);
    }
}
```

这个类包含两个静态方法，一个是`private`，一个是`public`

#### 调用`public static`方法

```java
@Test
public void invokePublicMethod() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<GreetingAndBye> clazz = GreetingAndBye.class;
    Method method = clazz.getMethod("greeting", String.class);

    Object result = method.invoke(null, "Eric");

    System.out.println(result);
}
```

当调用静态方法时，`invoke`的第一个参数传`null`

#### 调用`private static`方法

```java
@Test
public void invokePrivateMethod() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<GreetingAndBye> clazz = GreetingAndBye.class;
    Method method = clazz.getDeclaredMethod("goodBye", String.class);
    method.setAccessible(true);

    Object result = method.invoke(null, "Eric");

    System.out.println(result);
}
```

当调用的方法为`private`时，需要使用`getDeclaredMethod`代替`getMethod`。并且需要调用`method.setAccessible(true)`去调用`private`方法。

## 方法详解

#### 1. `getMethod()`

可以使用寻找类中所有的`public`的方法，包括从基类继承的、从接口实现的所有public方法。

#### 2. `getDeclaredMethod()`

获取类中定义的任何方法，包括`public, protected, default, private`，但是不包括继承的方法。

#### 3. `setAccessible()`

设置访问控制检查。当调用`private`或`protected`方法时，需要`method.setAccessible(true);`，否者会抛出`IllegalAccessException`异常。