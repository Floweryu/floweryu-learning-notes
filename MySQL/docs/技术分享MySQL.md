## MySQL的`explain`

#### `id`：查询编号

**id值越大，代表优先级越高，越先执行。如果id一致，则按table的顺序由上向下执行。id为null则最后执行**

如果没有子查询或者关联查询的话，就只有一条。

![image-20210822153748244](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/20210822153750.png)

如果是子查询，可能包含多个`select`关键字，每个关键字都会对应一个唯一的`id`值。

但需要注意的是：**查询优化器可能对涉及子查询的语句进行重写，从而转换为连接查询。**此时执行计划的id值就是唯一的了。

![image-20210822150105007](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/20210822150106.png)

如果是联合查询，那么就会出现一条id为NULL的记录，因为`union`结果会放到临时表中，临时表并不在原SQL中出现，所以这里的`table`是`<union1,2>`这种格式，表示将id为1和2的查询关联。

![image-20210822150953982](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/20210822150955.png)

#### `select_type`：查询类型

| `select_type` **Value** |       **JSON Name**        |                           Meaning                            |                             解释                             |
| :---------------------: | :------------------------: | :----------------------------------------------------------: | :----------------------------------------------------------: |
|         SIMPLE          |            None            |       Simple `SELECT` or (not using `UNION`subqueries)       |               简单查询，不包含子查询和`UNION`                |
|         PRIMARY         |            None            |                       Outermost SELECT                       | 查询若包含任何复杂的子部份，最外层查询就会被标记成`PRIMARY`  |
|          UNION          |            None            |       Second or later `SELECT` statement in a `UNION`        |      在`UNION`中的第二个和后面的select会被标记为`UNION`      |
|     DEPENDENT UNION     |    `dependent` (`true`)    | Second or later `SELECT` statement in a `UNION`, dependent on outer query |     UNION中的第二个或后面的SELECT语句，取决于外面的查询      |
|      UNION RESULT       |        union_result        |                     Result of a `UNION`                      |                         UNION的结果                          |
|        SUBQUERY         |            None            |                  First `SELECT` in subquery                  |                    子查询中的第一个SELECT                    |
|   DEPENDENT SUBQUERY    |    `dependent` (`true`)    |     First `SELECT` in subquery, dependent on outer query     |           子查询中的第一个SELECT，取决于外面的查询           |
|         DERIVED         |            None            |                        Derived table                         | 标记出现在from里的子查询，数字代表是子查询的`id`，这个结果会放进临时表中，也叫派生表。 |
|    DEPENDENT DERIVED    |    `dependent` (`true`)    |           Derived table dependent on another table           |                  依赖其他表的派生表的SELECT                  |
|      MATERIALIZED       | materialized_from_subquery |                    Materialized subquery                     |                         具体化子查询                         |
|  UNCACHEABLE SUBQUERY   |   `cacheable` (`false`)    | A subquery for which the result cannot be cached and must be re-evaluated for each row of the outer query |                      不能被缓存的子查询                      |
|    UNCACHEABLE UNION    |   `cacheable` (`false`)    | The second or later select in a `UNION` that belongs to an uncacheable subquery (see `UNCACHEABLE SUBQUERY`) |           UNION中第二个或后面的不能被缓存的子查询            |

#### `table`：正在访问哪个表

将`id`和`table`结合可以看出SQL的执行顺序。

- `<unionM,N>`：当有 union 时，UNION RESULT 的 table 列的值为 <union1,2>，1和2表示参与 union 的 select 行id。

- `<derivedN>`：id值为N的派生表的结果
- `<subqueryN>`：id值为N的具体化子查询结果

#### `partitions`：匹配的分区

查询时匹配到的分区信息，对于非分区表值为`NULL`，当查询的是分区表时，`partitions`显示分区表命中的分区情况。

#### `type`：查询使用的类型

- system：当表仅有一行记录时(系统表)，数据量很少，往往不需要进行磁盘IO，速度非常快。
- const：表示查询时命中 `primary key` 主键或者 `unique` 唯一索引，或者被连接的部分是一个常量(`const`)值。这类扫描效率极高，返回数据量少，速度非常快，因为只读一次。
- eq_ref：表连接查询，主键索引或者唯一索引全部被命中，是除system和const之外，最好的连接类型，和索引列比较只能使用=号。
- ref：使用最左前缀匹配索引（索引不是主键，也不是唯一索引），和索引列比较可以使用 = 或 <=> 。
- fulltext：使用全文索引的时候才会出现。
- ref_or_null：这个查询类型和ref很像，但是 MySQL 会做一个额外的查询，来看哪些行包含了NULL。
- index_merge：在一个查询里面很有多索引用被用到，可能会触发index_merge的优化机制。
- unique_subquery：unique_subquery和eq_ref不一样的地方是使用了in的子查询：

```mysql
value IN (SELECT primary_key FROM single_table WHERE some_expr)
```

> unique_subquery是一个索引查找函数，代替子查询提高效率。

- index_subquery：index_subquery和unique_subquery很像，区别是它在子查询里使用的是非唯一索引。

```mysql
value IN (SELECT key_column FROM single_table WHERE some_expr)
```

- range：通过索引范围查找多行数据，可以使用=, <>, >, >=, <, <=, IS NULL, <=>, BETWEEN, LIKE, 或 IN() 操作符。
- index：index类型和ALL类型一样，区别就是index类型是扫描的索引树。以下两种情况会触发：
  -  如果索引是查询的覆盖索引，就是说查询的数据在索引中都能找到，只需扫描索引树，不需要回表查询。 在这种情况下，explain 的 Extra 列的结果是 Using index。仅索引扫描通常比ALL快，因为索引的大小通常小于表数据。
  - 全表扫描会按索引的顺序来查找数据行。使用索引不会出现在Extra列中。
- all：全表扫描，效率最低的查询，一般可以通过添加索引避免。

#### `possible_keys`：显示查询可以使用哪些索引，不一定是最终使用的索引

#### `key`：实际使用的索引

#### `key_len`：在索引里使用的字节数

#### `ref`：在key索引中查找值所用的列或常量

- 当使用常量等值查询，显示`const`
- 当关联查询时，会显示相应关联表的**关联字段**
- 如果查询条件使用了`表达式`、`函数`，或者条件列发生内部隐式转换，可能显示为`func`
- 其它为**null**

#### `rows`：估计为找到所需的行而要读取的行数

#### `filtered`：返回的数据在经过过滤后，剩下满足条件的记录数量的比例。

#### `Extra`：不适合展示在其他列的额外信息

- `Using index`：使用了覆盖索引
- `Using where`：查询时未找到可用的索引，进而通过`where`条件过滤获取所需数据
- `Using temporary`：表示查询后结果需要使用临时表来存储，一般在排序或者分组查询时用到。
- `Using filesort`：表示无法利用索引完成的排序操作，也就是`ORDER BY`的字段没有索引，通常这样的SQL都是需要优化的。
- `Using join buffer`：联表查询的时候，如果表的连接条件没有用到索引，需要有一个连接缓冲区来存储中间结果。
- `Impossible where`：用不太正确的`where`语句，导致没有符合条件的行。
- `No tables used`：查询语句中没有`FROM`子句，或者有 `FROM DUAL`子句。

> 还有其它字段：[官方文档](https://dev.mysql.com/doc/refman/8.0/en/explain-output.html#explain_ref)

## 扩展：`show profile`

用来分析当前会话中SQL语句执行的资源消耗情况。

#### 常用命令：

- `show variables like '%profil%';`  查看 profiling 设置

- `set profiling=1;`  开启 profiling 功能。0 或 OFF 表示关闭，1 或 ON 表示开启

- `show profiles;` 查看当前session所有已经产生的profiles

- `show profile;` 查看上一条SQL语句开销，具体命令如下：

  ```bash
  SHOW PROFILE [type [, type] ... ]
      [FOR QUERY n]	// 如果不指定，则默认显示最近执行的语句；如果指定则显示语句n，n是query_id的值，可通过show profiles查看
      [LIMIT row_count [OFFSET offset]]
  
  type: {
      ALL		// 显示所有性能开销信息
    | BLOCK IO	// 显示 I/O 相关开销信息
    | CONTEXT SWITCHES	// 上下文切换开销
    | CPU		// 显示CPU相关信息
    | IPC		// 显示发送和接收消息的数量
    | MEMORY		// 显示内存相关的开销信息
    | PAGE FAULTS		// 显示页面错误相关的开销信息
    | SOURCE		// 显示源代码中函数名称以及该函数所在文件的名称和行号
    | SWAPS		// 显示交换次数的相关信息
  }
  ```

  > :bulb: `show profile`和``show profiles`将会在后续版本删除，使用 [performance_schema ](https://dev.mysql.com/doc/refman/8.0/en/performance-schema-query-profiling.html)来代替，后者没用过，看官方示例感觉使用上没有前者方便

#### 使用示例及解释

```mysql
mysql> show variables like '%profil%';
+------------------------+-------+
| Variable_name          | Value |
+------------------------+-------+
| have_profiling         | YES   |		// 标记是否存在语句分析功能
| profiling              | OFF   |		// 开启SQL语句剖析功能。 0或OFF表示关闭(默认)，1或ON表示开启
| profiling_history_size | 15    |		// 设置保留profiling的数目(即使用show profiles;命令展示的默认是最近执行
+------------------------+-------+		// 的15条sql的记录), 范围是[0, 100]，设置为0时将禁用profiling.
3 rows in set, 1 warning (0.00 sec)
```



```mysql
# 开启session级别的profiling
mysql> set profiling=1;
Query OK, 0 rows affected, 1 warning (0.00 sec)

# 验证修改后的结果
mysql> show variables like '%profil%';
+------------------------+-------+
| Variable_name          | Value |
+------------------------+-------+
| have_profiling         | YES   |
| profiling              | ON    |
| profiling_history_size | 15    |
+------------------------+-------+
3 rows in set, 1 warning (0.00 sec)
```

下面运行几个查询语句示例：

```mysql
mysql> select * from test;
+----+------+
| id | name |
+----+------+
|  1 | a    |
|  2 | b    |
|  3 | c    |
+----+------+
3 rows in set (0.00 sec)

mysql> select name from test;
+------+
| name |
+------+
| a    |
| b    |
| c    |
+------+
3 rows in set (0.00 sec)

# 查看当前session所有已经产生的profiles
mysql> show profiles;
+----------+------------+-----------------------+
| Query_ID | Duration   | Query                 |
+----------+------------+-----------------------+
|        1 | 0.00065825 | select * from test    |
|        2 | 0.00030100 | select name from test |
+----------+------------+-----------------------+
2 rows in set, 1 warning (0.00 sec)

# 可以使用show profile来查看上一条SQL语句开销
# 注：show profile之类的语句不会被profiling
# 查看全部信息
mysql> show profile all;
*************************** 17. row ***************************
             Status: cleaning up	// 状态
           Duration: 0.000023		// 持续时间，单位:s
           CPU_user: 0.000000		// 用户态CPU时间，单位:s
         CPU_system: 0.000000		// 系统态CPU时间，单位:s
  Context_voluntary: NULL			// 自愿上下文切换次数
Context_involuntary: NULL			// 非自愿上下文切换次数
       Block_ops_in: NULL			// 块输入次数
      Block_ops_out: NULL			// 块输出次数
      Messages_sent: NULL			// 发送的消息数量
  Messages_received: NULL			// 接收的消息数量
  Page_faults_major: NULL			// 主要页面错误数量
  Page_faults_minor: NULL			// 次要页面错误数量
              Swaps: NULL			// 交换次数
    Source_function: dispatch_command		// 源代码函数
        Source_file: sql_parse.cc			// 源代码文件
        Source_line: 2252					// 源代码行数
17 rows in set, 1 warning (0.00 sec)

# 查看基本执行过程时间信息
mysql> show profile;
+--------------------------------+----------+
| Status                         | Duration |
+--------------------------------+----------+
| starting                       | 0.000188 |
| Executing hook on transaction  | 0.000060 |
| starting                       | 0.000022 |
| checking permissions           | 0.000011 |	// 检查是否又执行该sql的权限
| Opening tables                 | 0.000292 |
| init                           | 0.000004 |
| System lock                    | 0.000034 |
| optimizing                     | 0.000003 |
| statistics                     | 0.000090 |
| preparing                      | 0.000015 |
| executing                      | 0.000160 |
| end                            | 0.000002 |
| query end                      | 0.000003 |
| waiting for handler commit     | 0.000025 |
| closing tables                 | 0.000008 |	// 将变更的表中的数据刷新到磁盘上并正在关闭使用过的表
| freeing items                  | 0.000064 |	
| cleaning up                    | 0.000023 |	// 释放内存和重置某些状态变量
+--------------------------------+----------+
17 rows in set, 1 warning (0.00 sec)
```

