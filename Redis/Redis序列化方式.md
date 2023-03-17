# 1. JdkSerializationRedisSerializer

这是RestTemplate类默认的序列化方式。

优点：

- 反序列化时不需要提供类型信息(class)，

缺点：

- 需要实现Serializable接口
- 存储的为二进制数据
- 序列化后的结果非常庞大，是JSON格式的5倍左右，这样就会消耗redis服务器的大量内存

# 2. StringRedisSerializer

是StringRedisTemplate默认的序列化方式，key和value都会采用此方式进行序列化，是被推荐使用的，对开发者友好，轻量级，效率也比较高。

# 3. GenericToStringSerializer

需要调用者给传一个对象到字符串互转的Converter

# 4. Jackson2JsonRedisSerializer

优点：

- 速度快，序列化后的字符串短小精悍，不需要实现Serializable接口。

缺点：

- 此类的构造函数中有一个类型参数，必须提供要序列化对象的类型信息(.class对象），其在反序列化过程中用到了类型信息

# 5. GenericJackson2JsonRedisSerializer

与Jackson2JsonRedisSerializer大致相同，会**额外存储**序列化对象的包命和类名