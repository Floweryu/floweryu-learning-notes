# 1. MySQL锁的类型

## 1.1 行锁与表锁

- 行锁就是锁定某行
- 表锁就是对整张表进行加锁

各引擎对锁的支持情况如下：

|        | 行锁 | 表锁 | 页锁 |
| :----: | :--: | :--: | :--: |
| MyISAM |      |  √   |      |
| InnoDB |  √   |  √   |      |
|  BDB   |      |  √   |  √   |

### 行锁

> A record lock is a lock on an index record. Record locks always lock index records, even if a table is defined with no indexes. For such cases, InnoDB creates a hidden clustered index and uses this index for record locking.

每一个InnoDB表都需要一个聚簇索引，有且只有一个。如果你为该表表定义一个主键，那么MySQL将使用主键作为聚簇索引；如果你不为定义一个主键，那么MySQL将会把第一个唯一索引（而且要求NOT NULL）作为聚簇索引；如果上诉两种情况都没有，那么MySQL将自动创建一个名字为`GEN_CLUST_INDEX`的隐藏聚簇索引。

因为是聚簇索引，所以B+树上的叶子节点都存储了数据行，那么如果现在是二级索引呢？InnoDB中的二级索引的叶节点存储的是主键值（或者说聚簇索引的值），所以通过二级索引查询数据时，还需要将对应的主键去聚簇索引中再次进行查询。

![1614350-20201115221104598-1160606998](https://i.loli.net/2021/04/03/YPQXyAuKqF1kOWa.png)

接下来以两条SQL的执行为例，讲解一下InnoDB对于单行数据的加锁原理：

```mysql
update user set age = 10 where id = 49;
update user set age = 10 where name = 'Tom';
```

第一条SQL使用主键查询，只需要在 id = 49 这个主键索引上加上锁。第二条 SQL 使用二级索引来查询，那么首先在 name = Tom 这个索引上加写锁，然后由于使用 InnoDB 二级索引还需再次根据主键索引查询，所以还需要在 id = 49 这个主键索引上加锁。

也就是说使用主键索引需要加一把锁，使用二级索引需要在二级索引和主键索引上各加一把锁。

根据索引对单行数据进行更新的加锁原理了解了，那如果更新操作涉及多个行呢，比如下面 SQL 的执行场景。

```sql
Copyupdate user set age = 10 where id > 49;
```

上述 SQL 的执行过程如下图所示。MySQL Server 会根据 WHERE 条件读取第一条满足条件的记录，然后 InnoDB 引擎会将第一条记录返回并加锁，接着 MySQL Server 发起更新改行记录的 UPDATE 请求，更新这条记录。一条记录操作完成，再读取下一条记录，直至没有匹配的记录为止。



![1614350-20201115221131283-1993522185](https://i.loli.net/2021/04/03/BP1luwCT3jNZ7Xd.png)

### 表锁

InnoDB既支持行锁，也支持表锁，当没有查询列没有索引时，InnoDB就不会去加行锁了，毕竟行锁一定要有索引，所以它现在加表锁，把整张表给锁住了。

表锁使用的是一次性锁技术，也就是说，在会话开始的地方使用 `lock `命令将后续需要用到的表都加上锁，在表释放前，只能访问这些加锁的表，不能访问其他表，直到最后通过 `unlock tables` 释放所有表锁。

除了使用 `unlock tables` 显示释放锁之外，会话持有其他表锁时执行`lock table` 语句会释放会话之前持有的锁；会话持有其他表锁时执行 `start transaction` 或者 `begin `开启事务时，也会释放之前持有的锁。

![1614350-20201115221221527-2019016888](https://i.loli.net/2021/04/03/xsehIgTQj5vG8AS.png)

表锁由 MySQL Server 实现，行锁则是存储引擎实现，不同的引擎实现的不同。在 MySQL 的常用引擎中 InnoDB 支持行锁，而 MyISAM 则只能使用 MySQL Server 提供的表锁。

### 两种锁比较

表锁：加锁过程的开销小，加锁的速度快；不会出现死锁的情况；锁定的粒度大，发生锁冲突的几率大，并发度低。

- 一般在执行DDL语句时会对整个表进行加锁，比如说 ALTER TABLE 等操作；
- 如果对InnoDB的表使用行锁，被锁定字段不是主键，也没有针对它建立索引的话，那么将会锁整张表；
- 表级锁更适合于以查询为主，并发用户少，只有少量按索引条件更新数据的应用，如Web 应用。

行锁：加锁过程的开销大，加锁的速度慢；会出现死锁；锁定粒度最小，发生锁冲突的概率最低，并发度也最高；

- 最大程度的支持并发，同时也带来了最大的锁开销。
- 在 InnoDB 中，除单个 SQL 组成的事务外，锁是逐步获得的，这就决定了在 InnoDB 中发生死锁是可能的。
- 行级锁只在存储引擎层实现，而 MySQL 服务器层没有实现。 行级锁更适合于有大量按索引条件并发更新少量不同数据，同时又有并发查询的应用，如一些在线事务处理（OLTP）系统。

# 2. InnoDB行锁与表锁

## 2.1 InnoDB的行锁

InnoDB实现了以下两种类型的行锁：

- **共享锁（S）**：加了锁的记录，所有事务都能去读取但不能修改，同时阻止其他事务获得相同数据集的排他锁；
- **排他锁（X）**：允许已经获得排他锁的事务去更新数据，阻止其他事务取得相同数据集的共享读锁和排他写锁；

## 2.2 InnoDB表锁——意向锁

由于表锁和行锁虽然锁定范围不同，但是会相互冲突。当你要加表锁时，势必要先遍历该表的所有记录，判断是否有排他锁。这种遍历检查的方式显然是一种低效的方式，MySQL引入了意向锁，来检测表锁和行锁的冲突。

> Intention locks are table-level locks that indicate which type of lock (shared or exclusive) a transaction requires later for a row in a table。
>
> The intention locking protocol is as follows:
>
> - Before a transaction can acquire a shared lock on a row in a table, it must first acquire an IS lock or stronger on the table.
> - Before a transaction can acquire an exclusive lock on a row in a table, it must first acquire an IX lock on the table.

意向锁也是表级锁，分为读意向锁（IS锁）和写意向锁（IX锁）。当事务要在记录上加上行锁时，要首先在表上加上意向锁。这样判断表中是否有记录正在加锁就很简单了，只要看下表上是否有意向锁就行了，从而就能提高效率。

意向锁之间是不会产生冲突的，它只会阻塞表级读锁或写锁。意向锁不于行级锁发生冲突。

## 2.3 InnoDB的加锁方法

- 意向锁是 InnoDB 自动加的，不需要用户干预；
- 对于`UPDATE`、`DELETE`和`INSERT`语句，`InnoDB`会自动给涉及的数据集加上排他锁；
- 对于普通的SELECT语句，InnoDB不会加任何锁；事务可以通过以下语句显示给记录集添加共享锁或排他锁：
  - 共享锁（S）：`select * from table_name where ... lock in share mode`。此时其他 session 仍然可以查询记录，并也可以对该记录加 `share mode` 的共享锁。但是如果当前事务需要对该记录进行更新操作，则很有可能造成死锁。
  - 排他锁（X）：`select * from table_name where ... for update`。其他session可以查询记录，但是不能对该记录加共享锁或排他锁，只能等待锁释放后在加锁。

### 2.3.1 `select for update`

在执行这个 select 查询语句的时候，会将对应的索引访问条目加上排他锁（X锁），也就是说这个语句对应的锁就相当于update带来的效果；

**使用场景**：为了让确保自己查找到的数据一定是最新数据，并且查找到后的数据值允许自己来修改，此时就需要用到select for update语句；

**性能分析**：`select for update`语句相当于一个`update`语句。在业务繁忙的情况下，如果事务没有及时地`commit`或者`rollback`可能会造成事务长时间的等待，从而影响数据库的并发使用效率。

### 2.3.2 `select lock in share mode`

`in share mode` 子句的作用就是将查找的数据加上一个share锁，这个就是表示其他的事务只能对这些数据进行简单的 select 操作，而不能进行 DML 操作。

**使用场景**：为了确保自己查询的数据不会被其他事务正在修改，也就是确保自己查询到的数据是最新的数据，并且不允许其他事务来修改数据。与`select for update`不同的是，本事务在查找完之后不一定能去更新数据，因为有可能其他事务也对同数据集使用了` in share mode` 的方式加上了S锁；

**性能分析**：`select lock in share mode` 语句是一个给查找的数据上一个共享锁（S 锁）的功能，它允许其他的事务也对该数据上S锁，但是不能够允许对该数据进行修改。如果不及时的`commit `或者`rollback `也可能会造成大量的事务等待。

## 2.4 InnoDB的锁争用情况

可以通过检查 InnoDB_row_lock 状态变量来分析系统上的行锁的争夺情况：

```mysql
mysql> show status like 'innodb_row_lock%';
+-------------------------------+-------+
| Variable_name                 | Value |
+-------------------------------+-------+
| Innodb_row_lock_current_waits | 0     |
| Innodb_row_lock_time          | 85677 |
| Innodb_row_lock_time_avg      | 42838 |
| Innodb_row_lock_time_max      | 49289 |
| Innodb_row_lock_waits         | 2     |
+-------------------------------+-------+
5 rows in set (0.52 sec)
```

# 3. MyISAM表锁

## 3.1 MyISAM表级锁模式

- 表共享读锁（Table Read Lock）：不会阻塞其他线程对同一个表的读操作请求，但会阻塞其他线程的写操作请求；
- 表独占写锁（Table Write Lock）：一旦表被加上独占写锁，那么无论其他线程是读操作还是写操作，都会被阻塞；

默认情况下，写锁比读锁具有更高的优先级；当一个锁释放后，那么它会优先相应写锁等待队列中的锁请求，然后再是读锁中等待的获取锁的请求。

> This ensures that updates to a table are not “starved” even when there is heavy SELECT activity for the table. However, if there are many updates for a table, SELECT statements wait until there are no more updates.

这种设定也是MyISAM表不适合于有大量更新操作和查询操作的原因。大量更新操作可能会造成查询操作很难以获取读锁，从而过长的阻塞。同时一些需要长时间运行的查询操作，也会使得线程“饿死”，应用中应尽量避免出现长时间运行的查询操作（在可能的情况下可以通过使用中间表等措施对SQL语句做一定的“分解”，使每一步查询都能在较短的时间内完成，从而减少锁冲突。如果复杂查询不可避免，应尽量安排在数据库空闲时段执行，比如一些定期统计可以安排在夜间执行。）

可以通过一些设置来调节MyISAM的调度行为：

- 通过指定启动参数`low-priority-updates`，使MyISAM引擎默认给予读请求以优先的权利；
- 通过执行命令`SET LOW_PRIORITY_UPDATES=1`，使该连接发出的更新请求优先级降低；
- 通过指定INSERT、UPDATE、DELETE语句的`LOW_PRIORITY`属性，降低该语句的优先级；
- 给系统参数`max_write_lock_count`设置一个合适的值，当一个表的读锁达到这个值后，MySQL就暂时将写请求的优先级降低，给读进程一定获得锁的机会。

## 3.2 MyISAM对表加锁分析

MyISAM在执行查询语句（SELECT）前，会自动给涉及的所有表加读锁，在执行更新操作（UPDATE、DELETE、INSERT等）前，会自动给涉及的表加写锁，这个过程并不需要用户干预，因此用户一般不需要直接用 LOCK TABLE 命令给 MyISAM 表显式加锁。在自动加锁的情况下，MyISAM 总是一次获得 SQL 语句所需要的全部锁，这也正是 MyISAM 表不会出现死锁（Deadlock Free）的原因。

MyISAM存储引擎支持并发插入，以减少给定表的读操作和写操作之间的争用：

如果MyISAM表在数据文件中没有空闲块（由于删除或更新导致的空行），则行始终插入数据文件的末尾。在这种情况下，你可以自由混合并发使用MyISAM表的 INSERT 和 SELECT 语句而不需要加锁（你可以在其他线程进行读操作的情况下，同时将行插入到MyISAM表中）。如果文件中有空闲块，则并发插入会被禁止，但当所有的空闲块重新填充有新数据时，它又会自动启用。 要控制此行为，可以使用MySQL的concurrent_insert系统变量。

- 当concurrent_insert=0时，不允许并发插入功能。
- 当concurrent_insert=1时，允许对没有空闲块的表使用并发插入，新数据位于数据文件结尾（缺省）。
- 当concurrent_insert=2时，不管表有没有空想快，都允许在数据文件结尾并发插入。

## 3.3 显式加表锁的应用

上面已经提及了表锁的加锁方式，一般表锁都是隐式加锁的，不需要我们去主动声明，但是也有需要显式加锁的情况，这里简单做下介绍：

给MyISAM表显式加锁，一般是为了一定程度模拟事务操作，实现对某一时间点多个表的一致性读取。例如，有一个订单表orders，其中记录有订单的总金额total，同时还有一个订单明细表 order_detail，其中记录有订单每一产品的金额小计 subtotal，假设我们需要检查这两个表的金额合计是否相等，可能就需要执行如下两条SQL：

```sql
CopySELECT SUM(total) FROM orders;
SELECT SUM(subtotal) FROM order_detail;
```

这时，如果不先给这两个表加锁，就可能产生错误的结果，因为第一条语句执行过程中，order_detail表可能已经发生了改变。因此，正确的方法应该是：

```sql
CopyLOCK tables orders read local,order_detail read local;
SELECT SUM(total) FROM orders;
SELECT SUM(subtotal) FROM order_detail;
Unlock tables;
```

## 3.4 查看表锁争用情况

可以通过检查 table_locks_waited 和 table_locks_immediate 状态变量来分析系统上的表锁的争夺，如果 Table_locks_waited 的值比较高，则说明存在着较严重的表级锁争用情况：

```sql
Copymysql> SHOW STATUS LIKE 'Table%';
+-----------------------+---------+
| Variable_name | Value |
+-----------------------+---------+
| Table_locks_immediate | 1151552 |
| Table_locks_waited | 15324 |
+-----------------------+---------+
```

# 4. 行锁的类型

根据锁的粒度将锁分为了行锁与表锁，根据使用场景的不同，又可以将行锁进行进一步的划分：`Next-Key Lock`、`Gap Lock`、`Record Lock`以及`插入意向GAP锁`。

不同的锁锁定的位置是不同的，比如说记录锁只锁定对应的记录，而间隙锁锁住记录和记录之间的间隙，`Next-key Lock`则锁住所属记录之间的间隙。不同的锁类型锁定的范围大致如图所示：

![1614350-20201115221250844-767865625](https://i.loli.net/2021/04/03/Q934aj28IrNkueD.png)

## 4.1 记录锁（Record Lock）

记录锁最简单的一种行锁形式，行锁是加在索引上的，如果当你的查询语句不走索引的话，那么它就会升级到表锁，最终造成效率低下。

## 4.2 间隙锁（Gap Lock）

> A gap lock is a lock on a gap between index records, or a lock on the gap before the first or after the last index record。

当我们使用范围条件而不是相等条件去检索，并请求锁时，InnoDB就会给符合条件的记录的索引项加上锁；而对于键值在条件范围内但并不存在（参考上面所说的空闲块）的记录，就叫做间隙，InnoDB在此时也会对间隙加锁，这种记录锁+间隙锁的机制叫`Next-Key Lock`。

可以表明间隙锁是所在两个存在的索引之间，是一个开区间，像最开始的那张索引图，15和18之间，是有（16，17）这个间隙存在的。

> Gap locks in InnoDB are “purely inhibitive”, which means that their only purpose is to prevent other transactions from inserting to the gap. Gap locks can co-exist. A gap lock taken by one transaction does not prevent another transaction from taking a gap lock on the same gap. There is no difference between shared and exclusive gap locks. They do not conflict with each other, and they perform the same function.

上面这段话表明间隙锁是可以共存的，共享间隙锁与独占间隙锁之间是没有区别的，两者之间并不冲突。其存在的目的都是防止其他事务往间隙中插入新的纪录，故而一个事务所采取的间隙锁是不会去阻止另外一个事务在同一个间隙中加锁的。

> Gap locking can be disabled explicitly. This occurs if you change the transaction isolation level to READ COMMITTED. Under these circumstances, gap locking is disabled for searches and index scans and is used only for foreign-key constraint checking and duplicate-key checking.

这段话表明，在 RU 和 RC 两种隔离级别下，即使你使用 `select in share mode `或 `select for update`，也无法防止**幻读**（读后写的场景）。因为这两种隔离级别下只会有**行锁**，而不会有**间隙锁**。而如果是 RR 隔离级别的话，就会在间隙上加上间隙锁。

## 4.3 临键锁（Next-key Lock）

> A next-key lock is a combination of a record lock on the index record and a gap lock on the gap before the index record.

临键锁是记录锁与与间隙锁的结合，所以临键锁与间隙锁是一个同时存在的概念，并且临键锁是个左开右闭的却比如(16, 18]。

关于临键锁与幻读，官方文档有这么一条说明：

> By default, InnoDB operates in REPEATABLE READ transaction isolation level. In this case, InnoDB uses next-key locks for searches and index scans, which prevents phantom rows.

就是说 MySQL 默认隔离级别是RR，在这种级别下，如果你使用 select in share mode 或者 select for update 语句，那么InnoDB会使用临键锁（记录锁 + 间隙锁），因而可以防止幻读；

但是我也在网上看到相关描述：即使你的隔离级别是 RR，如果你这是使用普通的select语句，那么此时 InnoDB 引擎将是使用快照读，而不会使用任何锁，因而还是无法防止幻读。（其实普通读应该是快照读没错，但是快照读有些幻读问题通过MVVC解决，但解决不彻底）。

## 4.4 插入意向锁（Insert Intention Lock）

> An insert intention lock is a type of gap lock set by INSERT operations prior to row insertion. This lock signals the intent to insert in such a way that multiple transactions inserting into the same index gap need not wait for each other if they are not inserting at the same position within the gap. Suppose that there are index records with values of 4 and 7. Separate transactions that attempt to insert values of 5 and 6, respectively, each lock the gap between 4 and 7 with insert intention locks prior to obtaining the exclusive lock on the inserted row, but do not block each other because the rows are nonconflicting.

官方文档已经解释得很清楚了，这里我做个翻译机：

插入意图锁是一种间隙锁，在行执行 INSERT 之前的插入操作设置。如果多个事务 INSERT 到同一个索引间隙之间，但没有在同一位置上插入，则不会产生任何的冲突。假设有值为4和7的索引记录，现在有两事务分别尝试插入值为 5 和 6 的记录，在获得插入行的排他锁之前，都使用插入意向锁锁住 4 和 7 之间的间隙，但两者之间并不会相互阻塞，因为这两行并不冲突。

插入意向锁只会和 间隙或者 Next-key 锁冲突，正如上面所说，间隙锁作用就是防止其他事务插入记录造成幻读，正是由于在执行 INSERT 语句时需要加插入意向锁，而插入意向锁和间隙锁冲突，从而阻止了插入操作的执行。

## 4.5 不同类型锁之间的兼容

不同类型的锁之间的兼容如下表所示：

|          | RECORED | GAP  | NEXT-KEY | II GAP（插入意向锁） |
| -------- | ------- | ---- | -------- | -------------------- |
| RECORED  |         | 兼容 |          | 兼容                 |
| GAP      | 兼容    | 兼容 | 兼容     | 兼容                 |
| NEXT-KEY |         | 兼容 |          | 兼容                 |
| II GAP   | 兼容    |      |          | 兼容                 |

（其中行表示已有的锁，列表示意图加上的锁）

其中，第一行表示已有的锁，第一列表示要加的锁。插入意向锁较为特殊，所以我们先对插入意向锁做个总结，如下：

- 插入意向锁不影响其他事务加其他任何锁。也就是说，一个事务已经获取了插入意向锁，对其他事务是没有任何影响的；
- 插入意向锁与间隙锁和 Next-key 锁冲突。也就是说，一个事务想要获取插入意向锁，如果有其他事务已经加了间隙锁或 Next-key 锁，则会阻塞。

其他类型的锁的规则较为简单：

- 间隙锁不和其他锁（不包括插入意向锁）冲突；
- 记录锁和记录锁冲突，Next-key 锁和 Next-key 锁冲突，记录锁和 Next-key 锁冲突；

# 5. 参考资料

- https://juejin.cn/post/6844903799534911496

- https://www.cnblogs.com/jojop/p/13982679.html#1383854867