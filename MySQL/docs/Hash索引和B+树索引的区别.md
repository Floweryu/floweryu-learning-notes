# 1. Hash索引和B+树索引的实现原理

`hash`索引底层就是`hash`表,进行查询时,调用一次`hash`函数就可以获取到相应的键值,之后进行回表查询获得实际数据.

`B+树`底层实现原理是多路平衡查找树,对于每一次的查询都是从根节点出发,查询到叶子节点方可以获得所查键值,然后查询判断是否需要回表查询.

# 2. Hash索引与B+树索引区别

## 2.1 Hash索引

- hash索引进行**等值查询更快**(一般情况下)但是却无法进行范围查询。因为在`hash`索引中经过`hash`函数建立索引之后,索引的顺序与原顺序无法保持一致,不能支持范围查询.
- hash索引**不支持模糊查询**以及多列索引的**最左前缀匹配**。因为hash函数的不可预测。eg:AAAA和AAAAB的索引没有相关性.
- hash索引任何时候都避免不了回表查询数据。

- hash索引虽然在等值上查询较快，但是不稳定，性能不可预测。当某个键值存在大量重复的时候，发生hash碰撞,此时查询效率可能极差。
- hash索引不支持使用索引进行排序，因为hash函数的不可预测

## 2.2 B+树索引

- B+树的所有节点皆遵循(左节点小于父节点，右节点大于父节点，多叉树也类似)，自然支持范围查询.
- 在符合某些条件(聚簇索引,覆盖索引等)的时候可以只通过索引完成查询。不需要回表查询。
- 查询效率比较稳定，对于查询都是从根节点到叶子节点，且树的高度较低。

