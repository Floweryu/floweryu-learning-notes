## 常见错误案例：

#### 1. 非互质关系导致的数据偏斜问题

```java
public static ShardCfg shard(String userId) { 
    int hash = userId.hashCode(); 
    // 对库数量取余结果为库序号 
    int dbIdx = Math.abs(hash % DB_CNT); 
    // 对表数量取余结果为表序号 
    int tblIdx = Math.abs(hash % TBL_CNT); 
 
    return new ShardCfg(dbIdx, tblIdx); 
} 

```

**上面就是用哈希值分别对分库数和分表数取余，得到库序号和表序号**。

假设以10库100表为例，如果一个哈希值对100取值余为0，那么他对10取值余也必然为0。这样意味着只有0库0表才可能有数据，其他库中的0表永远为空。

这样就会带来数据偏斜的问题，因为某些库的某些表中永远不可能有数据，最大数据偏斜率达到无穷大。

**事实上，只要库数量和表数量非互质关系，都会出现某些表中无数据的问题**。

那么是不是只要库数量和表数量互质就可用用这种分库分表方案呢? 比如我用 11 库 100 表的方案，是不是就合理了呢?

答案是否定的，我们除了要考虑数据偏斜的问题，还需要考虑可持续性扩容的问题，一般这种 Hash 分库分表的方案后期的扩容方式都是通过翻倍扩容法，那 11 库翻倍后，和 100 又不再互质。

#### 2. 扩容难以持续

**把 10 库 100 表看成总共 1000 个逻辑表，将求得的 Hash 值对 1000 取余，得到一个介于[0，999)中的数，然后再将这个数二次均分到每个库和每个表中。**

```java
public static ShardCfg shard(String userId) { 
        // ① 算Hash 
        int hash = userId.hashCode(); 
        // ② 总分片数 
        int sumSlot = DB_CNT * TBL_CNT; 
        // ③ 分片序号 
        int slot = Math.abs(hash % sumSlot); 
        // ④ 计算库序号和表序号的错误案例 
        int dbIdx = slot % DB_CNT ; 
        int tblIdx = slot / DB_CNT ; 
 
        return new ShardCfg(dbIdx, tblIdx); 
    } 
```

该方案有个比较大的问题，那就是**在计算表序号的时候，依赖了总库的数量**，那么后续翻倍扩容法进行扩容时，会出现扩容前后数据不在同一个表中，从而无法实施。

例如扩容前 Hash 为 1986 的数据应该存放在 6 库 98 表，但是翻倍扩容成 20 库 100 表后，它分配到了 6 库 99 表，表序号发生了偏移。

这样的话，我们在后续在扩容的时候，不仅要基于库迁移数据，还要基于表迁移数据，非常麻烦且易错。

## 常用分库分表方法

#### 1. 一致性哈希算法

算法详情可以参考这篇文章：[一致性哈希算法](https://blog.csdn.net/weixin_43207025/article/details/125359530?csdn_share_tail=%7B%22type%22%3A%22blog%22%2C%22rType%22%3A%22article%22%2C%22rId%22%3A%22125359530%22%2C%22source%22%3A%22weixin_43207025%22%7D&ctrtid=AHVLX)



#### 2. 二分分片法

```java
public static ShardCfg shard2(String userId) { 
        //  算Hash 
        int hash = userId.hashCode(); 
        //  总分片数 
        int sumSlot = DB_CNT * TBL_CNT; 
        //  分片序号 
        int slot = Math.abs(hash % sumSlot); 
        //  重新修改二次求值方案 
        int dbIdx = slot / TBL_CNT ; 
        int tblIdx = slot % TBL_CNT ; 
 
        return new ShardCfg(dbIdx, tblIdx); 
    } 
```

证明如下：

![img](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202206191521838.jpg)

通过上面结论我们知道，通过翻倍扩容后，我们的表序号一定维持不变，库序号可能还是在原来库，也可能平移到了新库中(原库序号加上原分库数)，完全符合我们需要的扩容持久性方案。

缺点：

- 连续的分片键 Hash 值大概率会散落在相同的库中，某些业务可能容易存在库热点(例如新生成的用户 Hash 相邻且递增，且新增用户又是高概率的活跃用户，那么一段时间内生成的新用户都会集中在相邻的几个库中)

## 常见扩容方法

#### 1. Hash扩容法

主要步骤如下：

- 时间点 t1：针对需要扩容的数据库节点增加从节点，开启主从同步进行数据同步。
- 时间点 t2：完成主从同步后，对原主库进行禁写。此处原因和翻倍扩容法类似，需要保证新的从库和原来主库中数据的一致性。
- 时间点 t3：同步完全完成后，断开主从关系，理论上此时从库和主库有着完全一样的数据集。
- 时间点 t4：修改一致性 Hash 范围的配置，并使应用服务重新读取并生效。
- 时间点 t5：确定所有的应用均接受到新的一致性 Hash 范围配置后，放开原主库的禁写操作，此时应用完全恢复服务。

## 参考文章

https://www.51cto.com/article/687055.html

