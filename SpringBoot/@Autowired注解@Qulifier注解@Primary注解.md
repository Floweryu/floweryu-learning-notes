### 1. @Autowired注解相关注解

1. 默认优先按照类型去容器中寻找组件：`context.getBean(Bean.Class)`

2. 如果找到多个类型相同的注解，再将@Autowired标注的属性的名称作为组件的id去容器中查找：`context.getBean(id)`

3. `@Qualifier("id")`使用该注解可以指定需要装配的组件的id

4. 自动装配需要将组件的属性设置好，不然会报错，可以设置`@Autowired(required=false)`来解决错误

   

`@Primary`：让String进行自动装配的时候，默认装配该注解的bean，也可以使用`@Qualifier`指定bean，这时@Primary失效

### 2. @Autowired可以作用在：构造器、方法、参数、属性

> 都是从容器中获取值

#### a. 方法上

```java
@Component
public class Blue {
    
    
    private Car car;

    @Autowired
    public void setBook(Car car) {
       this.car = car;
    }
}
```

**标注在方法上，Spring容器创建当前对象，就会调用方法完成赋值，方法使用的参数从容器中获取**

**Bean标注的方法创建对象的时候，方法参数的值从容器中获取**

#### b. 构造器

```java
@Component
public class Blue {
    
    private Car car;

    @Autowired
    public Blue(Car car) {
        this.car = car;
    }
}
```

**默认加载到IOC中的组件，容器会调用无参构造器创建对象，再进行初始化赋值操作。构造器用的组件也是从容器中获取**

#### c. 参数上——和作用在方法上类似

```java
@Component
public class Blue {
    
    
    private Car car;

    public void setBook(@Autowired Car car) {
       this.car = car;
    }
}
```

