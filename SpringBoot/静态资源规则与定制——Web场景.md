### 1. 静态资源目录

![image-20220109213129727](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202201092131265.png)

[官方文档](https://docs.spring.io/spring-boot/docs/2.6.2/reference/html/web.html#web.servlet.spring-mvc.static-content)中详细写了SpringBoot的静态资源目录：`/static` (or `/public` or `/resources` or `/META-INF/resources`)，即当前项目根路径(这里也是resources包)下的这些文件夹

访问：当前项目的根路径/ + 静态资源名

原理：静态映射/**

请求进来后，先去到Controller看能不能处理，不能处理的请求会交给静态资源处理器，静态资源也找不到就返回404

#### 1.1 改变默认的静态资源目录

```yaml
spring:
  web:
    resources:
      static-locations: classpath:/other/
```

### 2. 静态资源访问前缀

默认无前缀，可以自定义前缀。如下所示：

```yaml
spring:
  mvc:
    static-path-pattern: /res/**
```

访问：当前项目根路径/ + `static-path-pattern` + /静态资源名





