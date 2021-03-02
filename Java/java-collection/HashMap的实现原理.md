#### 参考文章（本文图文均来自此）：
- [https://www.cnblogs.com/jing99/p/11330341.html](https://www.cnblogs.com/jing99/p/11330341.html)

> 本文是在参考文章的基础上自己总结，因为觉得不亲手总结一下不好消化。也请多去原文查看，原文内容比本文生动，有图有代码。如果侵权，请联系我删除！！

### 前叙：
先看看其他数据结构在查询、新增和删除这几个方面的性能：

 - **数组**：采用连续的存储空间来存储数据。对于指定下标的查找，时间复杂度为O(1)。对于给定的值的查找，时间复杂度为O(n)，因为要遍历数组，寻找值相等项。但是，对于有序数组的查找，可以使用二分等算法，时间复杂度降为O(logn)；对于一般的插入操作，由于要移动数组元素，所以平均时间复杂度为O(n)。
 - **线性链表**：对于新增和删除操作（这里指在找到新增和删除的位置后），仅需要处理节点即可，时间复杂度为O(1)；对于查询操作，则需要遍历链表，时间复杂度为O(n)。
 - **二叉树**：对于有序的平衡二叉树，查找、删除和新增的时间复杂度为O(logn)。
 - **哈希表**：在哈希表中进行添加，删除，查找等操作，性能很高，不考虑哈希冲突的情况下，仅需一次定位即可完成，时间复杂度为O(1)。

数据存储的物理结构映射到最底层就是两种：**顺序存储结构**和**链式存储结构**。

哈希表的主干是数组，当需要查询某个元素，只需要通过**哈希函数**将当前关键字映射到数组的对应下标处，然后通过数组下标取值一次即可完成。

**哈希冲突**：当两个哈希函数计算的地址相同怎么处理？？这就会产生**哈希冲突**。解决哈希冲突的方法有：**开放定址法**（发生冲突，继续寻找下一块未被占用的存储地址），**再散列函数法**，**链地址法**。而**HashMap即是采用了链地址法，也就是数组+链表的方式**。

### 一、 `HashMap`的实现原理
HashMap的主干是一个Entry数组。Entry是HashMap的基本组成单元，每一个Entry包含一个key-value键值对。

```java
transient Entry<K,V>[] table = (Entry<K,V>[]) EMPTY_TABLE;
```
因此，HashMap的整体结构如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201109082428159.png#pic_center)
HashMap是由数组+链表组成的，数组是HashMap的主体，链表是为了解决哈希冲突而存在的。如果定位到的数组位置不含链表，则查找、添加操作很快，实践复杂度为O(1)。如果定位到数组包含链表，对应于添加操作，时间复杂度为O(1)，对于查找操作时间复杂度就不是O(1)，因为要遍历链表部分。所以，HashMap中链表越少越好，也就是冲突越少越好。
几个重要字段：

```java
//实际存储的key-value键值对的个数
transient int size;
//阈值，当table == {}时，该值为初始容量（初始容量默认为16）；当table被填充了，也就是为table分配内存空间后，threshold一般为 capacity*loadFactory。HashMap在进行扩容时需要参考threshold，后面会详细谈到
int threshold;
//负载因子，代表了table的填充度有多少，默认是0.75
final float loadFactor;
//用于快速失败，由于HashMap非线程安全，在对HashMap进行迭代时，如果期间其他线程的参与导致HashMap的结构发生变化了（比如put，remove等操作），需要抛出异常ConcurrentModificationException
transient int modCount;
```
### 中间看源码那一部分先跳过
贴几个重要源码片段
```java
//这是一个神奇的函数，用了很多的异或，移位等运算，对key的hashcode进一步进行计算以及二进制位的调整等来保证最终获取的存储位置尽量分布均匀
final int hash(Object k) {
        int h = hashSeed;
        if (0 != h && k instanceof String) {
            return sun.misc.Hashing.stringHash32((String) k);
        }
 
        h ^= k.hashCode();
 
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }
```

```java
/**
    * 返回数组下标
    */
   static int indexFor(int h, int length) {
       return h & (length-1);
   }
```

### 二、 为什么HashMap的数组长度一定是2的次幂？
数组的索引位置的计算是通过对key的值的hashcode进行hash扰乱运算后，再通过和length-1进行位运算得到最终数组索引位置。

hashMap的数组长度一定保持2的次幂，比如16的二进制表示为 10000，那么length-1就是15，二进制为01111，同理扩容后的数组长度为32，二进制表示为100000，length-1为31，二进制表示为011111。

从下图可以我们也能看到这样会保证低位全为1，而扩容后只有一位差异，也就是多出了最左位的1，这样在通过 h&(length-1)的时候，只要h对应的最左边的那一个差异位为0，就能保证得到的新的数组索引和老数组索引一致(大大减少了之前已经散列良好的老数组的数据位置重新调换)，这里按作者理解就行。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201109085047937.png#pic_center)
还有，数组长度保持2的次幂，length-1的低位都为1，会使得获得的数组索引index更加均匀：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020110908535925.png#pic_center)
上面的&运算，**高位是不会对结果产生影响**的（hash函数采用各种位运算可能也是为了使得低位更加散列），我们只关注低位bit，如果低位全部为1，那么对于h低位部分来说，任何一位的变化都会对结果产生影响，也就是说，要得到index=21这个存储位置，h的低位只有这一种组合。这也是数组长度设计为必须为2的次幂的原因。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201109085458699.png?#pic_center)
如果不是2的次幂，也就是低位不是全为1此时，要使得index=21，h的低位部分不再具有唯一性了，哈希冲突的几率会变的更大，同时，index对应的这个bit位无论如何不会等于1了，而对应的那些数组位置也就被白白浪费了。

### 参考文章

- [https://blog.csdn.net/javazejian/article/details/51348320](https://blog.csdn.net/javazejian/article/details/51348320)
- [https://www.cnblogs.com/qianguyihao/p/3929585.html](https://www.cnblogs.com/qianguyihao/p/3929585.html)
- [https://www.cnblogs.com/jing99/p/11330341.html](https://www.cnblogs.com/jing99/p/11330341.html)
- [https://www.cnblogs.com/skywang12345/p/3324958.html](https://www.cnblogs.com/skywang12345/p/3324958.html)
