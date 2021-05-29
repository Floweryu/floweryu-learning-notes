# 1.  为什么Cookie ⽆法防⽌CSRF攻击，⽽token可以？

CSRF（Cross Site Request Forgery）⼀般被翻译为 跨站请求伪造 。那么什么是 跨站请求伪造 呢？说简单⽤你的身份去发送⼀些对你不友好的请求。

> 举个简单的例⼦： ⼩壮登录了某⽹上银⾏，他来到了⽹上银⾏的帖⼦区，看到⼀个帖⼦下⾯有⼀个链接写着“科学理 财，年盈利率过万”，⼩壮好奇的点开了这个链接，结果发现⾃⼰的账户少了10000元。这是这么 回事呢？原来⿊客在链接中藏了⼀个请求，这个请求直接利⽤⼩壮的身份给银⾏发送了⼀个转账 请求,也就是通过你的 Cookie 向银⾏发出请求。

```html
<a src=http://www.mybank.com/Transfer?bankId=11&money=10000>科学理财，年盈利率过万</>
```

进⾏Session 认证的时候，我们⼀般使⽤ Cookie 来存储 SessionId,当我们登陆 后后端⽣成⼀个SessionId放在Cookie中返回给客户端，服务端通过Redis或者其他存储⼯具记录 保存着这个Sessionid，客户端登录以后每次请求都会带上这个SessionId，服务端通过这个 SessionId来标示你这个⼈。如果别⼈通过 cookie拿到了 SessionId 后就可以代替你的身份访问 系统了。

Session 认证中 Cookie 中的 SessionId是由浏览器发送到服务端的，借助这个特性，攻击者就 可以通过让⽤户误点攻击链接，达到攻击效果。

但是，我们使⽤ token 的话就不会存在这个问题，在我们登录成功获得 token 之后，⼀般会选择 存放在 local storage 中。然后我们在前端通过某些⽅式会给每个发到后端的请求加上这个 token, 这样就不会出现 CSRF 漏洞的问题。因为，即使有个你点击了⾮法链接发送了请求到服务端，这 个⾮法请求是不会携带 token 的，所以这个请求将是⾮法的。

需要注意的是不论是 Cookie 还是 token 都⽆法避免跨站脚本攻击（Cross Site Scripting） XSS。