我们知道 Session 信息需要保存⼀份在服务器端。这种⽅式会带来⼀些麻烦，⽐如 需要我们保证保存 Session 信息服务器的可⽤性、不适合移动端（依赖Cookie）等等。 

有没有⼀种不需要⾃⼰存放 Session 信息就能实现身份验证的⽅式呢？使⽤ Token 即可！JWT （JSON Web Token） 就是这种⽅式的实现，通过这种⽅式服务器端就不需要保存 Session 数据 了，只⽤在客户端保存服务端返回给客户的 Token 就可以了，扩展性得到提升。

**JWT 本质上就⼀段签名的 JSON 格式的数据。由于它是带有签名的，因此接收者便可以验证它 的真实性。**

JWT 由 3 部分构成:

1. Header :描述 JWT 的元数据。定义了⽣成签名的算法以及 Token 的类型。
2. Payload（负载）:⽤来存放实际需要传递的数据
3. Signature（签名）：服务器通过 Payload 、 Header 和⼀个密钥( secret )使⽤ Header ⾥⾯ 指定的签名算法（默认是 HMAC SHA256）⽣成。

**使用token进行身份验证**

在基于 Token 进⾏身份验证的的应⽤程序中，服务器通过 Payload 、 Header 和⼀个密钥 ( secret )创建令牌（ Token ）并将 Token 发送给客户端，客户端将 Token 保存在 Cookie 或 者 localStorage ⾥⾯，以后客户端发出的所有请求都会携带这个令牌。你可以把它放在 Cookie ⾥⾯⾃动发送，但是这样不能跨域，所以更好的做法是放在 HTTP Header 的 Authorization字段 中： Authorization: Bearer Token 。

过程如下：

1. ⽤户向服务器发送⽤户名和密码⽤于登陆系统。
2.  身份验证服务响应并返回了签名的 JWT，上⾯包含了⽤户是谁的内容。
3. ⽤户以后每次向后端发请求都在Header中带上 JWT。
4. 服务端检查 JWT 并从中获取⽤户相关信息