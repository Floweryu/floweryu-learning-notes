`TransactionDefinition` 接⼝中定义了五个表示隔离级别的常量：

- `TransactionDefinition.ISOLATION_DEFAULT`: 使⽤后端数据库默认的隔离级别，Mysql 默认采⽤的 `REPEATABLE_READ`隔离级别 Oracle 默认采⽤的 `READ_COMMITTED`隔离级别.
- `TransactionDefinition.ISOLATION_READ_UNCOMMITTED`: 最低的隔离级别，允许读取 尚未提交的数据变更，可能会导致脏读、幻读或不可重复读
- `TransactionDefinition.ISOLATION_READ_COMMITTED`: 允许读取并发事务已经提交的 数据，可以阻⽌脏读，但是幻读或不可重复读仍有可能发⽣

- `TransactionDefinition.ISOLATION_REPEATABLE_READ:` 对同⼀字段的多次读取结果 都是⼀致的，除⾮数据是被本身事务⾃⼰所修改，可以阻⽌脏读和不可重复读，但幻读仍有 可能发⽣
- `TransactionDefinition.ISOLATION_SERIALIZABLE: `最⾼的隔离级别，完全服从ACID的 隔离级别。所有的事务依次逐个执⾏，这样事务之间就完全不可能产⽣⼲扰，也就是说，该 级别可以防⽌脏读、不可重复读以及幻读。但是这将严重影响程序的性能。通常情况下也不 会⽤到该级别。