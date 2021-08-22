## `MySQL`的`explain`

#### `id`：查询编号

如果没有子查询或者关联查询的话，就只有一条。

![image-20210822153748244](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/20210822153750.png)

如果是子查询，可能包含多个`select`关键字，每个关键字都会对应一个唯一的`id`值。

但需要注意的是：**查询优化器可能对涉及子查询的语句进行重写，从而转换为连接查询。**此时执行计划的id值就是唯一的了。

![image-20210822150105007](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/20210822150106.png)

如果是联合查询，那么就会出现一条id为NULL的记录，因为`union`结果会放到临时表中，临时表并不在原SQL中出现，所以这里的`table`是`<union1,2>`这种格式。

![image-20210822150953982](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/20210822150955.png)

#### `select_type`：查询类型

- `SIMPLE`：简单查询，不包含子查询和`UNION`
- `PRIMARY`：如果不是简单查询，最外层查询就会被标记成`PROMARY`
- `UNION`：在`UNION`中的第二个和随后的select会被标记为`UNION`
- `UNION RESULT`：从临时表检索结果的`SELECT`
- `DERIVED`：标记出现在from里的子查询，这个结果会放进临时表中，也叫派生表。

![image-20210822164757090](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/20210822164758.png)

- `SUBQUERY`：不在from里的子查询

![image-20210822165755941](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/20210822165759.png)



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

