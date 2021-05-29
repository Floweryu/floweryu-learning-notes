⼀般使⽤ @Autowired 注解⾃动装配 bean，要想把类标识成可⽤于 @Autowired 注解⾃动 装配的 bean 的类,采⽤以下注解可实现：

- @Component ：通⽤的注解，可标注任意类为 Spring 组件。如果⼀个Bean不知道属于哪 个层，可以使⽤ @Component 注解标注。
- @Repository : 对应持久层即 Dao 层，主要⽤于数据库相关操作。
- @Service : 对应服务层，主要涉及⼀些复杂的逻辑，需要⽤到 Dao层。
- @Controller : 对应 Spring MVC 控制层，主要⽤户接受⽤户请求并调⽤ Service 层返回数 据给前端⻚⾯。

