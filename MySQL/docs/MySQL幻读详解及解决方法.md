# 1. 什么是幻读？

建立一张表如下：

![image-20210402161531899](https://i.loli.net/2021/04/02/w36CHgQMjhiqptu.png)

按照下面步骤执行两个事务。

**事务1**：

![image-20210402161856788](https://i.loli.net/2021/04/02/vnUuD5L8mpcBE3e.png)

**事务2**：

![image-20210402162014354](https://i.loli.net/2021/04/02/g9Fub3mO6wDPTcB.png)

由此，可以把**幻读理解**为：select 某记录是否存在，不存在，准备插入此记录，但执行 insert 时发现此记录已存在，无法插入，此时就发生了幻读。

# 2. 解决幻读

## 2.1 RR(可重复读)级别下防止幻读

RR级别下只要对`SELECT`操作手动加**排他锁(X锁)**即可实现类似`SERIALIZABLE`级别（会对SELECT隐式加锁），代码如下：

```mysql
# 这里需要用 X锁， 用 LOCK IN SHARE MODE 拿到 S锁 后我们没办法做 写操作
SELECT `id` FROM `users` WHERE `id` = 1 FOR UPDATE;
```

这里`id=3`的记录不存在，`FOR UPDATE`也对此记录加锁。InnoDB 的行锁（gap锁是范围行锁，一样的）锁定的是记录所对应的索引，且聚簇索引同记录是直接关系在一起的。

**T1事务**

![image-20210402163829717](https://i.loli.net/2021/04/02/4DwsYB5E3Umvk12.png)

**T2事务**

![image-20210402163942322](https://i.loli.net/2021/04/02/zTfBFv4yCXNUELe.png)

> id = 3 的记录不存在，开始执行事务
>
> step.1 ：T1事务 查询id = 3的记录，并对其加X锁
>
> step.2 ：T2事务 插入id = 3的记录，但是会被阻塞
>
> step.3 ：T1事务 插入 id= 3的记录，成功执行，但此时T2事务依然被阻塞，直到T1提交事务后，T2才被唤醒，但主键冲突报错。
>
> T1事务最终被成功执行，T2事务干扰T1事务失败。

## 2.2 `SERIALIZABLE`级别杜绝幻读

在此级别下，我们便不需要对 SELECT 操作显式加锁，InnoDB会自动加锁，事务安全，但性能很低。

先修改数据库隔离级别：MySQL8查询事务应该使用`transaction_isolation`，`tx_isolation`在MySQL 5.7.20后被弃用。

![image-20210403093544328](https://i.loli.net/2021/04/03/PtQpDrB7ymOJv8o.png)

> 设置数据库隔离级别为SERIALIZABLE，左边是事务T1，右边是事务T2.
>
> step.1：T1查询id=4的记录是空， Innodb隐式对其加X锁。
>
> step.2：T2向表中插入id=4的记录，被阻塞
>
> step.3：T1向表中插入id=4的记录，成功执行（此时T1仍然被阻塞）
>
> 接着，提交T1事务，此时T2唤醒但是提示冲突错误。
>
> T1事务成功执行，T2干扰T1事务失败。