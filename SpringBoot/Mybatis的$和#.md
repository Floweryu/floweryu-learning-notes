- `${}` 是 Properties ⽂件中的变量占位符，它可以⽤于标签属性值和 sql 内部，**属于静态⽂本替换**，⽐如${driver}会被静态替换为 com.mysql.jdbc.Driver 。
- ` #{}` 是 sql 的参数占位符，Mybatis 会将 sql 中的 #{} 替换为?号，在 sql 执⾏前会使⽤ PreparedStatement 的参数设置⽅法，按序给 sql 的?号占位符设置参数值，⽐如 ps.setInt(0, parameterValue)， #{item.name} 的取值⽅式为使⽤反射从参数对象中获取 item 对象的 name 属性值，相当于 param.getItem().getName() 。

