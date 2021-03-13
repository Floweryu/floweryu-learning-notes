# 一、IO多路复用之`select`, `poll`, `epoll`详解

`select`，`poll`，`epoll`都是IO多路复用的机制。I/O多路复用就是通过一种机制，**一个进程可以监视多个描述符，一旦某个描述符就绪（一般是读就绪或者写就绪），能够通知程序进行相应的读写操作**。但`select，poll，epoll`本质上都是同步I/O，因为他们都需要在读写事件就绪后自己负责进行读写，也就是说这个读写过程是阻塞的，而异步I/O则无需自己负责进行读写，异步I/O的实现会负责把数据从内核拷贝到用户空间。
## 1. `select`

```cpp
int select (int n, fd_set *readfds, fd_set *writefds, fd_set *exceptfds, struct timeval *timeout);
```
`select` 函数监视的文件描述符分3类，分别是`writefds`、`readfds`、和`exceptfds`。调用后`select`函数会阻塞，**直到有描述符就绪**（有数据 可读、可写、或者有except），或者超时（`timeout`指定等待时间，如果立即返回设为`null`即可），函数返回。当`select`函数返回后，可以 通过遍历`fdset`，来找到就绪的描述符。

select目前几乎在所有的平台上支持，其良好跨平台支持也是它的一个优点。select的一 个缺点在于单个进程能够监视的文件描述符的数量存在最大限制，在Linux上一般为1024，可以通过修改宏定义甚至重新编译内核的方式提升这一限制，但 是这样也会造成效率的降低。\
### `select`的优点

> 跨平台。（几乎所有的平台都支持）
时间精度高。（ns级别）
### `select`的缺点
1. **最大限制**：单个进程能够监视的文件描述符的数量存在最大限制。(基于数组存储的赶脚)一般来说这个数目和系统内存关系很大，具体数目可以cat /proc/sys/fs/file-max察看。它由FD_SETSIZE设置，32位机默认是1024个。64位机默认是2048.
2. **时间复杂度**： 对socket进行扫描时是线性扫描，即采用轮询的方法，效率较低，时间复杂度O(n)。
当套接字比较多的时候，每次select()都要通过遍历FD_SETSIZE个Socket来完成调度，不管哪个Socket是活跃的，都遍历一遍。这会浪费很多CPU时间。
它仅仅知道有I/O事件发生了，却并不知道是哪那几个流（可能有一个，多个，甚至全部），我们只能无差别轮询所有流，找出能读出数据，或者写入数据的流，对他们进行操作。所以select具有O(n)的无差别轮询复杂度，同时处理的流越多，无差别轮询时间就越长。
3. **内存拷贝**：需要维护一个用来存放大量fd的数据结构，这样会使得用户空间和内核空间在传递该结构时复制开销大。
## 2. `poll`

```cpp
int poll (struct pollfd *fds, unsigned int nfds, int timeout);
```
不同与`select`使用三个位图来表示三个`fdset`的方式，`poll`使用一个` pollfd`的指针实现：

```cpp
struct pollfd {
    int fd; /* file descriptor */
    short events; /* requested events to watch */
    short revents; /* returned events witnessed */
};

```
`pollfd`结构包含了要监视的`event`和发生的`event`，不再使用`select`“参数-值”传递的方式。同时，`pollfd`并没有最大数量限制（但是数量过大后性能也是会下降）。 和`select`函数一样，`poll`返回后，需要轮询`pollfd`来获取就绪的描述符。

从上面看，`select`和`poll`都需要在返回后，通过遍历文件描述符来获取已经就绪的`socket`。事实上，同时连接的大量客户端在一时刻可能只有很少的处于就绪状态，因此随着监视的描述符数量的增长，其效率也会线性下降。
### `poll优点`
- 没有最大连接数的限制。（基于链表来存储的）
### `poll缺点`
1. **时间复杂度**： 对socket进行扫描时是线性扫描，即采用轮询的方法，效率较低，时间复杂度O(n)。
　　它将用户传入的数组拷贝到内核空间，然后查询每个fd对应的设备状态，如果设备就绪则在设备等待队列中加入一项并继续遍历，如果遍历完所有fd后没有发现就绪设备，则挂起当前进程，直到设备就绪或者主动超时，被唤醒后它又要再次遍历fd。这个过程经历了多次无谓的遍历。
2. **内存拷贝**：大量的fd数组被整体复制于用户态和内核地址空间之间，而不管这样的复制是不是有意义。
大量的fd数组被整体复制于用户态和内核地址空间之间，而不管这样的复制是不是有意义。
3. **水平触发**：如果报告了fd后，没有被处理，那么下次poll时会再次报告该fd。
## 3. `epoll`
相对于`select`和`poll`来说，`epoll`更加灵活，没有描述符限制。`epoll`使用一个文件描述符管理多个描述符，将用户关系的文件描述符的事件存放到内核的一个事件表中，这样在用户空间和内核空间的`copy`只需一次。
### `epoll`的操作过程
`epoll`操作过程需要三个接口，分别如下：

```cpp
int epoll_create(int size)；//创建一个epoll的句柄，size用来告诉内核这个监听的数目一共有多大
int epoll_ctl(int epfd, int op, int fd, struct epoll_event *event)；
int epoll_wait(int epfd, struct epoll_event * events, int maxevents, int timeout);
```
#### a. `int epoll_create(int size);`
创建一个epoll的句柄，`size`用来告诉内核这个监听的数目一共有多大，这个参数不同于`select()`中的第一个参数，给出最大监听的fd+1的值，`参数size并不是限制了epoll所能监听的描述符最大个数，只是对内核初始分配内部数据结构的一个建议。`

当创建好`epoll`句柄后，它就会占用一个`fd`值，在`linux`下如果查看`/proc/进程id/fd/`，是能够看到这个`fd`的，所以在使用完`epoll`后，必须调用`close()`关闭，否则可能导致`fd`被耗尽。

#### b. `int epoll_ctl(int epfd, int op, int fd, struct epoll_event *event)；`
函数是对指定描述`fd`执行`op`操作:

 - `epfd`：是`epoll_create()`的返回值。
 - `op`：表示op操作，用三个宏来表示：添加`EPOLL_CTL_ADD`，删除`EPOLL_CTL_DEL`，修改`EPOLL_CTL_MOD`。分别添加、删除和修改对fd的监听事件。
 - ` fd`：是需要监听的`fd`（文件描述符）
-  `epoll_event`：是告诉内核需要监听什么事，`struct epoll_event`结构如下

```cpp
struct epoll_event {
  __uint32_t events;  /* Epoll events */
  epoll_data_t data;  /* User data variable */
};

//events可以是以下几个宏的集合：
EPOLLIN ：表示对应的文件描述符可以读（包括对端SOCKET正常关闭）；
EPOLLOUT：表示对应的文件描述符可以写；
EPOLLPRI：表示对应的文件描述符有紧急的数据可读（这里应该表示有带外数据到来）；
EPOLLERR：表示对应的文件描述符发生错误；
EPOLLHUP：表示对应的文件描述符被挂断；
EPOLLET： 将EPOLL设为边缘触发(Edge Triggered)模式，这是相对于水平触发(Level Triggered)来说的。
EPOLLONESHOT：只监听一次事件，当监听完这次事件之后，如果还需要继续监听这个socket的话，需要再次把这个socket加入到EPOLL队列里

```
#### c. `int epoll_wait(int epfd, struct epoll_event * events, int maxevents, int timeout);`
等待`epfd`上的`io`事件，最多返回`maxevents`个事件。

参数`events`用来从内核得到事件的集合，`maxevents`告之内核这个`events`有多大，这个`maxevents`的值不能大于创建`epoll_create()`时的`size`，参数`timeout`是超时时间（毫秒，0会立即返回，-1将不确定，也有说法说是永久阻塞）。该函数返回需要处理的事件数目，如返回0表示已超时。

### `工作模式`
　`epoll`对文件描述符的操作有两种模式：`LT（level trigger）`和`ET（edge trigger）`。**LT模式是默认模式**，LT模式与ET模式的区别如下：
　
- `LT模式`：当`epoll_wait`检测到描述符事件发生并将此事件通知应用程序，应用程序可以**不立即**处理该事件。下次调用`epoll_wait`时，会再次响应应用程序并通知此事件。
- `ET模式`：当`epoll_wait`检测到描述符事件发生并将此事件通知应用程序，应用程序必须**立即**处理该事件。如果不处理，下次调用`epoll_wait`时，不会再次响应应用程序并通知此事件。

#### LT模式
`LT(level triggered)`是缺省的工作方式，并且同时支持`block`和`no-block socket`.在这种做法中，内核告诉你一个文件描述符是否就绪了，然后你可以对这个就绪的`fd`进行`IO`操作。如果你不作任何操作，内核还是会继续通知你的。

#### ET模式
`ET(edge-triggered)`是高速工作方式，只支持`no-block socket`。在这种模式下，当描述符从未就绪变为就绪时，内核通过`epoll`告诉你。然后它会假设你知道文件描述符已经就绪，并且不会再为那个文件描述符发送更多的就绪通知，直到你做了某些操作导致那个文件描述符不再为就绪状态了(比如，你在发送，接收或者接收请求，或者发送接收的数据少于一定量时导致了一个EWOULDBLOCK 错误）。但是请注意，如果一直不对这个`fd`作`IO操作`(从而导致它再次变成未就绪)，内核不会发送更多的通知(only once)

`ET模式`在很大程度上**减少了epoll事件被重复触发的次数**，因此**效率要比LT模式高**。`epoll`工作在ET模式的时候，**必须使用非阻塞套接口**，以避免由于一个文件句柄的阻塞读/阻塞写操作把处理多个文件描述符的任务饿死。
### `epoll优点`
1. **没有最大连接数的限制**。（基于 红黑树+双链表 来存储的:1G的内存上能监听约10万个端口）
2. **时间复杂度低**： 边缘触发和事件驱动，监听回调，时间复杂度O(1)。
只有活跃可用的fd才会调用callback函数；即epoll最大的优点就在于它只管“活跃”的连接，而跟连接总数无关，因此实际网络环境中，Epoll的效率就会远远高于select和poll。
3. **内存拷贝**：利用mmap()文件映射内存加速与内核空间的消息传递，减少拷贝开销。

### `epoll使用场景`
#### 适合用epoll的应用场景：
- 对于连接特别多，活跃的连接特别少
- 典型的应用场景为一个需要处理上万的连接服务器，例如各种app的入口服务器，例如qq
#### 不适合epoll的场景：
- 连接比较少，数据量比较大，例如ssh
#### epoll 的惊群问题：
因为epoll 多用于多个连接，只有少数活跃的场景，但是万一某一时刻，epoll 等的上千个文件描述符都就绪了，这时候epoll 要进行大量的I/O，此时压力太大。
### 例子
假如有这样一个例子：
1. 我们已经把一个用来从管道中读取数据的文件句柄(RFD)添加到epoll描述符
2. 这个时候从管道的另一端被写入了2KB的数据
3. 调用epoll_wait(2)，并且它会返回RFD，说明它已经准备好读取操作
4. 然后我们读取了1KB的数据
5. 调用epoll_wait(2)......

#### LT模式：
如果是LT模式，那么在第5步调用epoll_wait(2)之后，仍然能受到通知。

#### ET模式：
如果我们在第1步将RFD添加到epoll描述符的时候使用了`EPOLLET`标志，那么在第5步调用`epoll_wait(2)`之后将有可能会挂起，因为剩余的数据还存在于文件的输入缓冲区内，而且数据发出端还在等待一个针对已经发出数据的反馈信息。只有在监视的文件句柄上发生了某个事件的时候 ET 工作模式才会汇报事件。因此在第5步的时候，调用者可能会放弃等待仍在存在于文件输入缓冲区内的剩余数据。

当使用`epoll`的ET模型来工作时，当产生了一个`EPOLLIN`事件后，
读数据的时候需要考虑的是当`recv()`返回的大小如果等于请求的大小，那么很有可能是缓冲区还有数据未读完，也意味着该次事件还没有处理完，所以还需要再次读取：

```cpp
while(rs){
  buflen = recv(activeevents[i].data.fd, buf, sizeof(buf), 0);
  if(buflen < 0){
    // 由于是非阻塞的模式,所以当errno为EAGAIN时,表示当前缓冲区已无数据可读
    // 在这里就当作是该次事件已处理处.
    if(errno == EAGAIN){
        break;
    }
    else{
        return;
    }
  }
  else if(buflen == 0){
     // 这里表示对端的socket已正常关闭.
  }

 if(buflen == sizeof(buf){
      rs = 1;   // 需要再次读取
 }
 else{
      rs = 0;
 }
}

```
# 二、总结
## 1. 最大连接数
|  |  |
|--|--|
| select |单个进程所能打开的最大连接数有FD_SETSIZE宏定义，其大小是32个整数的大小（在32位的机器上，大小就是3232，同理64位机器上FD_SETSIZE为3264），当然我们可以对进行修改，然后重新编译内核，但是性能可能会受到影响，这需要进一步的测试。  |
|poll|poll本质上和select没有区别，但是它没有最大连接数的限制，原因是它是基于链表来存储的|
|epoll|虽然连接数有上限，但是很大，1G内存的机器上可以打开10万左右的连接，2G内存的机器可以打开20万左右的连接|

## 2. IO效率问题
|  |  |
|--|--|
| select |因为每次调用时都会对连接进行线性遍历，所以随着FD的增加会造成遍历速度慢的“线性下降性能问题”。  |
|poll|同上|
|epoll|因为epoll内核中实现是根据每个fd上的callback函数来实现的，只有活跃的socket才会主动调用callback，所以在活跃socket较少的情况下，使用epoll没有前面两者的线性下降的性能问题，但是所有socket都很活跃的情况下，可能会有性能问题。|

## 3. 消息传递方式
|  |  |
|--|--|
| select | 内核需要将消息传递到用户空间，都需要内核拷贝动作|
|poll|同上|
|epoll|	epoll通过mmap把对应设备文件片断映射到用户空间上, 消息传递不通过内核, 内存与设备文件同步数据.|


![在这里插入图片描述](https://img-blog.csdnimg.cn/20201224114704694.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MzIwNzAyNQ==,size_16,color_FFFFFF,t_70)
***
### 参考来自：
[https://www.cnblogs.com/yungyu16/p/13066744.html#%E4%BA%94%E3%80%81select%E3%80%81poll%E3%80%81epoll%E5%8C%BA%E5%88%AB](https://www.cnblogs.com/yungyu16/p/13066744.html#%E4%BA%94%E3%80%81select%E3%80%81poll%E3%80%81epoll%E5%8C%BA%E5%88%AB)
