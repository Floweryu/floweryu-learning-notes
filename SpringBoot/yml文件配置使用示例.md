类
```java
@ConfigurationProperties(prefix = "person")
@Component
@Data
@ToString
public class Person {
    private String username;
    private List<String> animal;
    private Map<String, Object> score;
    private Set<Double> salarys;
    private Map<String, List<Pet>> allPets;
}
```
对应的配置文件
```yaml
person:
  username: zhangsan
#  animal: [cat, dog]
  animal:
    - cat
    - dog
#  score: {english: 80, math: 90}
  score:
    english: 80
    math: 90
  salarys: [999.9, 888.8]
  allPets:
    sick:
      - name: cat
        age: 8
      - {name: dog, age: 9}
    health: [{name: pig, age: 7}, {name: rabbit, age: 4}]
```

