# 1. 什么是事务传播行为

事务传播行为用来描述由**某一个事务传播行为修饰的方法**被嵌套进**另一个方法**时事务如何传播。

用伪代码说明：

```java
 public void methodA(){
    methodB();
    //doSomething
 }
 
 @Transaction(Propagation=XXX)
 public void methodB(){
    //doSomething
 }
```

代码中`methodA()`方法嵌套调用了`methodB()`方法，`methodB()`的事务传播行为由`@Transaction(Propagation=XXX)`设置决定。这里需要注意的是`methodA()`并没有开启事务，某一个事务传播行为修饰的方法并不是必须要在开启事务的外围方法中调用。

## @Transactional(rollbackFor = Exception.class)注解

Exception分为运⾏时异常RuntimeException和⾮运⾏时异常。事务管理对于企业应 ⽤来说是⾄关重要的，即使出现异常情况，它也可以保证数据的⼀致性。

当 @Transactional 注解作⽤于类上时，该类的所有 public ⽅法将都具有该类型的事务属性，同 时，我们也可以在⽅法级别使⽤该标注来覆盖类级别的定义。如果类或者⽅法加了这个注解，那 么这个类⾥⾯的⽅法抛出异常，就会回滚，数据库⾥⾯的数据也会回滚。

在 @Transactional 注解中如果不配置 rollbackFor 属性,那么事物只会在遇到 RuntimeException 的 时候才会回滚,加上 rollbackFor=Exception.class ,可以让事物在遇到⾮运⾏时异常时也回滚。

# 2. Spring中七种事务传播行为

| 事务传播行为类型          | 说明                                                         |
| ------------------------- | ------------------------------------------------------------ |
| PROPAGATION_REQUIRED      | 如果当前没有事务，就新建一个事务，如果已经存在一个事务中，加入到这个事务中。这是最常见的选择。**Spring默认的事务传播类型** |
| PROPAGATION_SUPPORTS      | 支持当前事务，如果当前没有事务，就以非事务方式执行。         |
| PROPAGATION_MANDATORY     | 使用当前的事务，如果当前没有事务，就抛出异常。               |
| PROPAGATION_REQUIRES_NEW  | 新建事务，如果当前存在事务，把当前事务挂起。                 |
| PROPAGATION_NOT_SUPPORTED | 以非事务方式执行操作，如果当前存在事务，就把当前事务挂起。**始终以非事务方式执行,如果当前存在事务，则挂起当前事务** |
| PROPAGATION_NEVER         | 以非事务方式执行，如果当前存在事务，则抛出异常。             |
| PROPAGATION_NESTED        | 如果当前存在事务，则在嵌套事务内执行。如果当前没有事务，则执行与PROPAGATION_REQUIRED类似的操作。 |

定义非常简单，也很好理解，下面我们就进入代码测试部分，验证我们的理解是否正确。

# 3. **REQUIRED**

**如果当前没有事务，则自己新建一个事务，如果当前存在事务，则加入这个事务**。

【示例1】

在`testMain`和`testB`上声明事务，设置传播行为`REQUIRED`，伪代码如下：

```java
@Transactional(propagation = Propagation.REQUIRED)
public void testMain(){
    A(a1);  //调用A入参a1
    testB();    //调用testB
}
@Transactional(propagation = Propagation.REQUIRED)
public void testB(){
    B(b1);  //调用B入参b1
    throw Exception;     //发生异常抛出
    B(b2);  //调用B入参b2
}
```

该场景下执行`testMain`方法结果:

数据库没有插入新的数据，数据库还是保持着执行testMain方法之前的状态，没有发生改变。testMain上声明了事务，在执行`testB`方法时就加入了`testMain`的事务（**当前存在事务，则加入这个事务**），在执行`testB`方法抛出异常后**事务会发生回滚**，又`testMain`和`testB`使用的同一个事务，所以事务回滚后`testMain`和`testB`中的操作都会回滚，也就使得数据库仍然保持初始状态.

【示例2】

```java
public void testMain(){
    A(a1);  //调用A入参a1
    testB();    //调用testB
}
@Transactional(propagation = Propagation.REQUIRED)
public void testB(){
    B(b1);  //调用B入参b1
    throw Exception;     //发生异常抛出
    B(b2);  //调用B入参b2
}
```

这时的执行结果:

数据`a1`存储成功，数据`b1`和`b2`没有存储。由于`testMain`没有声明事务，`testB`有声明事务且传播行为是`REQUIRED`，所以在执行`testB`时会自己新建一个事务（**如果当前没有事务，则自己新建一个事务**），`testB`抛出异常则只有`testB`中的操作发生了回滚，也就是`b1`的存储会发生回滚，但`a1`数据不会回滚，所以最终`a1`数据存储成功，`b1`和`b2`数据没有存储。

# 4. SUPPORTS

**当前存在事务，则加入当前事务，如果当前没有事务，就以非事务方法执行**。

【示例3】

只在testB上声明事务，设置传播行为SUPPORTS，伪代码如下：

```java
public void testMain(){
    A(a1);  //调用A入参a1
    testB();    //调用testB
}
@Transactional(propagation = Propagation.SUPPORTS)
public void testB(){
    B(b1);  //调用B入参b1
    throw Exception;     //发生异常抛出
    B(b2);  //调用B入参b2
}
```

这种情况下，执行`testMain`的最终结果就是，a1，b1存入数据库，b2没有存入数据库。由于`testMain`没有声明事务，且`testB`的事务传播行为是`SUPPORTS`，所以执行`testB`时就是没有事务的（**如果当前没有事务，就以非事务方法执行**），则在`testB`抛出异常时也不会发生回滚，所以最终结果就是a1和b1存储成功，b2没有存储。

那么当在`testMain`上声明事务且使用`REQUIRED`传播方式的时候，这个时候执行`testB`就满足**当前存在事务，则加入当前事务**，在`testB`抛出异常时事务就会回滚，最终结果就是a1，b1和b2都不会存储到数据库.

# 5. **MANDATORY**

**当前存在事务，则加入当前事务，如果当前事务不存在，则抛出异常。**

【示例4】

只在`testB`上声明事务，设置传播行为`MANDATORY`，伪代码如下：

```java
public void testMain(){
    A(a1);  //调用A入参a1
    testB();    //调用testB
}
@Transactional(propagation = Propagation.MANDATORY)
public void testB(){
    B(b1);  //调用B入参b1
    throw Exception;     //发生异常抛出
    B(b2);  //调用B入参b2
}
```

这种情形的执行结果就是a1存储成功，而b1和b2没有存储。b1和b2没有存储，并不是事务回滚的原因，而是因为`testMain`方法没有声明事务，在去执行`testB`方法时就直接抛出事务要求的异常（**如果当前事务不存在，则抛出异常**），所以`testB`方法里的内容就没有执行。

那么如果在`testMain`方法进行事务声明，并且设置为`REQUIRED`，则执行`testB`时就会使用`testMain`已经开启的事务，遇到异常就正常的回滚了。

# 6. **REQUIRES_NEW**

**创建一个新事务，如果存在当前事务，则挂起该事务。**

【示例5】

为了说明设置REQUIRES_NEW的方法会开启新事务，我们把异常发生的位置换到了testMain，然后给testMain声明事务，传播类型设置为REQUIRED，testB也声明事务，设置传播类型为REQUIRES_NEW，伪代码如下：

```java
@Transactional(propagation = Propagation.REQUIRED)
public void testMain(){
    A(a1);  //调用A入参a1
    testB();    //调用testB
    throw Exception;     //发生异常抛出
}
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void testB(){
    B(b1);  //调用B入参b1
    B(b2);  //调用B入参b2
}
```

这种情形的执行结果就是a1没有存储，而b1和b2存储成功，因为`testB`的事务传播设置为`REQUIRES_NEW`,所以在执行`testB`时会开启一个新的事务，`testMain`中发生的异常时在`testMain`所开启的事务中，所以这个异常不会影响`testB`的事务提交，`testMain`中的事务会发生回滚，所以最终a1就没有存储，而b1和b2就存储成功了。

与这个场景对比的一个场景就是`testMain`和testB都设置为`REQUIRED`，那么上面的代码执行结果就是所有数据都不会存储，因为`testMain`和`testMain`是在同一个事务下的，所以事务发生回滚时，所有的数据都会回滚.

# 7. **NOT_SUPPORTED**

**始终以非事务方式执行,如果当前存在事务，则挂起当前事务**。

可以理解为设置事务传播类型为NOT_SUPPORTED的方法，在执行时，不论当前是否存在事务，都会以非事务的方式运行。

【示例6】

`testMain`传播类型设置为`REQUIRED`，`testB`传播类型设置为`NOT_SUPPORTED`，且异常抛出位置在`testB`中，伪代码如下：

```java
@Transactional(propagation = Propagation.REQUIRED)
public void testMain(){
    A(a1);  //调用A入参a1
    testB();    //调用testB
}
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void testB(){
    B(b1);  //调用B入参b1
    throw Exception;     //发生异常抛出
    B(b2);  //调用B入参b2
}
```

执行结果就是a1和b2没有存储，而b1存储成功。`testMain`有事务，而testB不使用事务，所以执行中testB的存储b1成功，然后抛出异常，此时testMain检测到异常事务发生回滚，但是由于testB不在事务中，所以只有testMain的存储a1发生了回滚，最终只有b1存储成功，而a1和b1都没有存储.

# 8. **NEVER**

**不使用事务，如果当前事务存在，则抛出异常**

很容易理解，就是这个方法不使用事务，并且调用我的方法也不允许有事务，如果调用我的方法有事务则我直接抛出异常。

【示例7】

`testMain`设置传播类型为`REQUIRED`，`testB`传播类型设置为`NEVER`，并且把`testB`中的抛出异常代码去掉，则伪代码如下：

```java
@Transactional(propagation = Propagation.REQUIRED)
public void testMain(){
    A(a1);  //调用A入参a1
    testB();    //调用testB
}
@Transactional(propagation = Propagation.NEVER)
public void testB(){
    B(b1);  //调用B入参b1
    B(b2);  //调用B入参b2
}
```

该场景执行，直接抛出事务异常，且不会有数据存储到数据库。由于`testMain`事务传播类型为`REQUIRED`，所以`testMain`是运行在事务中，而`testB`事务传播类型为`NEVER`，所以`testB`不会执行而是直接抛出事务异常，此时`testMain`检测到异常就发生了回滚，所以最终数据库不会有数据存入。

# 9. **NESTED**

**如果当前事务存在，则在嵌套事务中执行，否则REQUIRED的操作一样（开启一个事务）**

- 和REQUIRES_NEW的区别

> REQUIRES_NEW是新建一个事务并且新开启的这个事务与原有事务无关，而NESTED则是当前存在事务时（我们把当前事务称之为父事务）会开启一个嵌套事务（称之为一个子事务）。
> 在NESTED情况下父事务回滚时，子事务也会回滚，而在REQUIRES_NEW情况下，原有事务回滚，不会影响新开启的事务。

- 和REQUIRED的区别

> REQUIRED情况下，调用方存在事务时，则被调用方和调用方使用同一事务，那么被调用方出现异常时，由于共用一个事务，所以无论调用方是否catch其异常，事务都会回滚
> 而在NESTED情况下，被调用方发生异常时，调用方可以catch其异常，这样只有子事务回滚，父事务不受影响

【示例8】

`testMain`设置为`REQUIRED`，`testB`设置为`NESTED`，且异常发生在`testMain`中，伪代码如下：

```java
@Transactional(propagation = Propagation.REQUIRED)
public void testMain(){
    A(a1);  //调用A入参a1
    testB();    //调用testB
    throw Exception;     //发生异常抛出
}
@Transactional(propagation = Propagation.NESTED)
public void testB(){
    B(b1);  //调用B入参b1
    B(b2);  //调用B入参b2
}
```

该场景下，所有数据都不会存入数据库，因为在testMain发生异常时，父事务回滚则子事务也跟着回滚了，可以与*(示例5)*比较看一下，就找出了与REQUIRES_NEW的不同

【示例9】

`testMain`设置为`REQUIRED`，`testB`设置为`NESTED`，且异常发生在`testB`中，伪代码如下：

```java
@Transactional(propagation = Propagation.REQUIRED)
public void testMain(){
    A(a1);  //调用A入参a1
    try{
        testB();    //调用testB
    }catch（Exception e){

    }
    A(a2);
}
@Transactional(propagation = Propagation.NESTED)
public void testB(){
    B(b1);  //调用B入参b1
    throw Exception;     //发生异常抛出
    B(b2);  //调用B入参b2
}
```

这种场景下，结果是a1,a2存储成功，b1和b2存储失败，因为调用方catch了被调方的异常，所以只有子事务回滚了。

同样的代码，如果我们把`testB`的传播类型改为`REQUIRED`，结果也就变成了：没有数据存储成功。就算在调用方`catch`了异常，整个事务还是会回滚，因为，调用方和被调方共用的同一个事务

# 参考资料

- https://juejin.cn/post/6844903566205779982
- https://zhuanlan.zhihu.com/p/148504094