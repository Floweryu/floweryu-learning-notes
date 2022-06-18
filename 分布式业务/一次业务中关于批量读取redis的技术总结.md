## 一、业务背景

根据视频docId批量去redis中查询视频的各种数，由于redis使用的是集群模式，不支持管道查询（或者使用redis管道比较麻烦）。故在此使用了线程池去查询。

```java
private ExecutorService executorService = new ThreadPoolExecutor(10, 20, 5, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(2000, true),
        new ThreadFactoryBuilder().setNameFormat("videoCountThread-%d").build(),
        new ThreadPoolExecutor.AbortPolicy());

public List<FeedbackInfo> getVideoCount(List<String> docIdList) {
    
    final CountDownLatch countDownLatch = new CountDownLatch(docIdList.size());
    List<FeedbackInfo> countList = Collections.synchronizedList(new ArrayList<>(docIdList.size()));
    
    try {
        for (String docId : docIdList) {
            executorService.submit(() -> {
                try {
                    // 此处省略.....
					countList.add(feedbackInfo);
                } catch (Exception e) {
                    LOGGER.error("getVideoCount error. param: docId = {}", docId, e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        LOGGER.info("批量执行结束 countList size: {}, docIdList size: {} ", countList.size(), docIdList.size());
    } catch (Exception e) {
        LOGGER.error("getVideoCount error. param: docIdList = ", e);
    }
    return countList;
}
```

## 二、两个核心问题

#### 1. 线程获取数据后如何同步返回

这里使用了**CountDownLatch**来完成同步

#### 2. 线程并发List的选择

有以下List可以供选择：ArrayList、CopyOnWriteArrayList、ConcurrentLinkedQueue、Collections.synchronizedList。

其中，ArrayList不是线程安全的，CopyOnWriteArrayList的写操作效率太低，所以在ConcurrentLinkedQueue、Collections.synchronizedList两个中进行选择。

经过一些测试，这两个并发安全的List在本业务场景中效率差不多，考虑到使用ConcurrentLinkedQueue在最后异步需要转化为List，所以最终选用了Collections.synchronizedList。

