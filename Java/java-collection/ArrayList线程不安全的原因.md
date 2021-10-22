以下面代码为测试用例：

```java
public class ArrayListTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArrayListTest.class);

    private static final ExecutorService threadPoolService = new ThreadPoolExecutor(100, 200, 5, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(200000, true),
            new ThreadFactoryBuilder().setNameFormat("videoCountThread-%d").build(),
            new ThreadPoolExecutor.AbortPolicy());

    public static void main(String[] args) {
        List<Integer> idList = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            idList.add(i);
        }

        for (int i = 0; i < 20; i++) {
            long time = System.currentTimeMillis();
            List<String> res = getCount(idList);
            long end = System.currentTimeMillis() - time;
            System.out.println(i + " exec, " + "res size:  " + res.size());
            System.out.println("time :" + end);
            System.out.println();
        }
    }

    public static List<String> getCount(List<Integer> idList) {
        final CountDownLatch countDownLatch = new CountDownLatch(idList.size());
        List<String> res = new ArrayList<>(idList.size());
        
        try {
            for (Integer id : idList) {
                threadPoolService.submit(() -> {
                    try {
                        String node = id + "vvv";
                        res.add(node);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }
            countDownLatch.await();
            LOGGER.debug("批量执行结束 res size: {}, idList size: {} ", res.size(), idList.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }
}
```

![image-20211022152652709](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/20211022152654.png)

查看ArrayList的add方法的源码，发现线程不安全的地方是下面两个点：

1. 扩容过程
2. 在elementData对应位置上设置对应的值

```java
public boolean add(E e) {
    ensureCapacityInternal(size + 1);  // Increments modCount!!
    elementData[size++] = e;
    return true;
}
```

**扩容过程如何出现线程不安全?**

假设数组目前容量为10，当前的元素个数也是10，这时有A，B两个线程：

A线程进来发现size为10，需要扩容，调用ensureCapacityInternal(size + 1)方法

B线程进来发现size为10，需要扩容，调用ensureCapacityInternal(size + 1)方法

A线程发现可以容纳一个元素，不再扩容

B线程发现可以容纳一个元素，也不再扩容

A线程执行elementData[size++] = e方法，此时size=11

B线程执行elementData[size++] = e方法，即elementData[11] = e，但是A线程已经把扩容的拿一个位置占用了，所以B线程这时会报数组越界异常。

**设置对应的值出现线程不安全?**

设置值过程可以分为下面两步：

```java
elementData[size] = e
size = size + 1
```

假设数组大小为0，有A和B两个线程

A线程执行代码1处操作，设置elementData[0] = A，

接着，B线程也执行到代码1处的操作，此时B获取到的size = 0，设置elementData[0] = B

然后，线程A执行代码2处操作，将size + 1 = 1

线程B执行代码2处操作，将size + 1 = 2

最后数组中的结果变为：B，null

而理想情况下数组结果应该是：A，B

**就会发生一个线程的值覆盖另一个线程添加的值**

