先在`application.properties`中预定义配置属性：

```yaml
book.name = springboot-cloud
book.author = zhang
```

然后定义一个组件与之对应：

```java
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class Book {
    @Value("${book.name}")
    private String name;
    
    @Value("${book.author}")
    private String author;
}
```

就可以注入组件使用了：

```java
@SpringBootTest
class SpringbootCloudApplicationTests {

    @Test
    void contextLoads() {
    }
    
    @Autowired
    Book book;

    @Test
    void getBook() {
        System.out.println(book);
    }
}
```

输出：

```java
Book(name=springboot-cloud, author=zhang)

```

也可以在`application.properties`中添加参数引用，使用随机数：

```yaml
book.name = springboot-cloud
book.author = zhang
book.desc = ${book.author} is writting ${book.name}
book.price = ${random.int}
```

在用命令行方式 启 动 Spring Boot 应用时， 连续的两个减号`－－`就 是对 `application.properties` 中的属性值进行赋值 的标识。 所以 ， `java -jar xxx.jar--server.port=8888`命令， 等价于中在`application.properties`添加 属性`server.port= 8888`。

