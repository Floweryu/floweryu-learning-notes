MyBatis 的自动映射功能是建立在 resultMap 基础之上的。resultType 属性自动映射的原理是，当 sql 映射输出配置为 resultType 时，MyBatis 会生成一个空的 resultMap，然后指定这个 resultMap 的 type 为指定的 resultType 的类型，接着 MyBatis 检测查询结果集中字段与指定 type 类型中属性的映射关系，对结果进行自动映射。

