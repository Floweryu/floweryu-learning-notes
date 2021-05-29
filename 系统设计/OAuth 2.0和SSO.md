# 1. Oauth 2.0

OAuth 是⼀个⾏业的标准授权协议，主要⽤来授权第三⽅应⽤获取有限的权限。⽽ OAuth 2.0是 对 OAuth 1.0 的完全重新设计，OAuth 2.0更快，更容易实现，OAuth 1.0 已经被废弃。

实际上它就是⼀种授权机制，它的最终⽬的是为第三⽅应⽤颁发⼀个有时效性的令牌 token，使 得第三⽅应⽤能够通过该令牌获取相关的资源。

OAuth 2.0 ⽐较常⽤的场景就是第三⽅登录，当你的⽹站接⼊了第三⽅登录的时候⼀般就是使⽤ 的 OAuth 2.0 协议。

另外，现在OAuth 2.0也常⻅于⽀付场景（微信⽀付、⽀付宝⽀付）和开发平台（微信开放平 台、阿⾥开放平台等等）。

## 第三方登录的原理

所谓第三方登录，实质就是 OAuth 授权。用户想要登录 A 网站，A 网站让用户提供第三方网站的数据，证明自己的身份。获取第三方网站的身份数据，就需要 OAuth 授权。

举例来说，A 网站允许 GitHub 登录，背后就是下面的流程。

1. A 网站让用户跳转到 GitHub。
2. GitHub 要求用户登录，然后询问"A 网站要求获得 xx 权限，你是否同意？"
3. 用户同意，GitHub 就会重定向回 A 网站，同时发回一个授权码。
4. A 网站使用授权码，向 GitHub 请求令牌。
5. GitHub 返回令牌.
6. A 网站使用令牌，向 GitHub 请求用户数据。

# 2. SSO

SSO(Single Sign On)即单点登录说的是⽤户登陆多个⼦系统的其中⼀个就有权访问与其相关的其他系统。举个例⼦我们在登陆了京东⾦融之后，我们同时也成功登陆京东的京东超市、京东家 电等⼦系统。

OAuth 是⼀个⾏业的标准授权协议，主要⽤来授权第三⽅应⽤获取有限的权限。SSO解决的是⼀ 个公司的多个相关的⾃系统的之间的登陆问题⽐如京东旗下相关⼦系统京东⾦融、京东超市、京 东家电等等。

推荐文章：

- http://www.ruanyifeng.com/blog/2019/04/github-oauth.html
- http://www.ruanyifeng.com/blog/2019/04/oauth_design.html
- http://www.ruanyifeng.com/blog/2019/04/oauth-grant-types.html
- https://deepzz.com/post/what-is-oauth2-protocol.html