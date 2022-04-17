反应器模式由Reactor反应器线程、Handlers处理器两大角色组成：

- Reactor反应器线程的职责：负责响应IO事件，并且分发到Handlers处理器
- Handlers处理器职责：非阻塞的执行业务逻辑

## 1. 单线程Reactor反应器模式

![image-20220410214209076](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202204102142602.png)

