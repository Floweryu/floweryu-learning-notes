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

## 2. 反射检测抽象类、接口、实现类

```java
public abstract class AbstractExample {
    public abstract LocalDate getLocalDate();

    public abstract LocalTime getLocalTime();
}
```

```java
public interface InterfaceExample {
}
```

#### 检测抽象类

```java
public void givenAbstractClass_whenCheckModifierIsAbstract_thenTrue() throws Exception {
    Class<AbstractExample> clazz = AbstractExample.class;

    System.out.println(Modifier.isAbstract(clazz.getModifiers())); // true
}

public void givenAbstractClass_whenCheckIsAbstractClass_thenTrue() {
    Class<AbstractExample> clazz = AbstractExample.class;
    int mod = clazz.getModifiers();

    System.out.println(Modifier.isInterface(mod));  // false
    System.out.println(Modifier.isAbstract(mod));   // true
}
```

#### 检测接口

```java
public void givenInterface_whenCheckModifierIsAbstract_thenTrue() {
    Class<InterfaceExample> clazz = InterfaceExample.class;
    int mod = clazz.getModifiers();
    System.out.println(Modifier.isInterface(mod));  // true
    System.out.println(Modifier.isAbstract(clazz.getModifiers()));  // true 注意该方法检测接口也是抽象类
}
```

#### 检测实现类

```java
public void givenConcreteClass_whenCheckIsAbstractClass_thenFalse() {
    Class<Date> clazz = Date.class;
    int mod = clazz.getModifiers();

    System.out.println(Modifier.isInterface(mod));  // false
    System.out.println(Modifier.isAbstract(mod));   // false
}
```

## 3. 判断静态方法

```java
    /**
     * 判断是不是静态方法
     */
    @Test
    public void whenCheckStaticMethod_ThenSuccess() throws Exception {
        Method method = StaticUtility.class.getMethod("getAuthorName", null);
        System.out.println(Modifier.isStatic(method.getModifiers()));   // true
    }

    /**
     * 获取类中的静态方法
     */
    @Test
    public void whenCheckAllStaticMethods_thenSuccess() {
        List<Method> methodList = Arrays.asList(StaticUtility.class.getMethods())
                .stream()
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .collect(Collectors.toList());
        System.out.println(methodList);
    }
```



## 方法详解

#### 1. `getMethod()`

可以使用寻找类中所有的`public`的方法，包括从基类继承的、从接口实现的所有public方法。

#### 2. `getDeclaredMethod()`

获取类中定义的任何方法，包括`public, protected, default, private`，但是不包括继承的方法。

#### 3. `setAccessible()`

设置访问控制检查。当调用`private`或`protected`方法时，需要`method.setAccessible(true);`，否者会抛出`IllegalAccessException`异常。