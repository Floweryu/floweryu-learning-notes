```java
    public static CuratorFramework createSimple(String connectString) {
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
        return CuratorFrameworkFactory.newClient(connectString, retryPolicy);
    }
```

## 1. 只监听一次

```java
public  void watcher() {
    CuratorFramework client = ClientFactory.createSimple(IdMakeUtil.ZK_ADDRESS);
    try {
        client.start();
        Stat stat = client.checkExists().forPath(workPath);
        if (stat == null) {
            String data = "hello";
            byte[] payload = data.getBytes(StandardCharsets.UTF_8);
            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(workPath, payload);
        }
        Watcher watcher = new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println("监听到的变化, watchedEvent = " + event);
            }
        };
        byte[] bytes = client.getData().usingWatcher(watcher).forPath(workPath);
        log.info("监听节点的内容：" + new String(bytes));
        client.setData().forPath(workPath, "第1次更改内容".getBytes(StandardCharsets.UTF_8));
        client.setData().forPath(workPath, "第2次更改内容".getBytes(StandardCharsets.UTF_8));
    } catch(Exception e) {
        e.printStackTrace();
    }

}
```

## 2. 反复监听当前节点

```java
public void watcherCache() {
    CuratorFramework client = ClientFactory.createSimple(IdMakeUtil.ZK_ADDRESS);
    try {
        client.start();
        Stat stat = client.checkExists().forPath(workPath);
        if (stat == null) {
            String data = "cache";
            byte[] payload = data.getBytes(StandardCharsets.UTF_8);
            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(workPath, payload);
        }

        CuratorCache cache = CuratorCache.builder(client, workPath).build();
        CuratorCacheListener listener = CuratorCacheListener.builder()
            .forCreates(node -> log.info("改变节点, node={}", node))
            .forChanges(((oldNode, node) -> log.info("改变节点, old={}, new={}", oldNode, node)))
            .build();
        cache.listenable().addListener(listener);
        cache.start();
        client.setData().forPath(workPath, "第1次更改内容".getBytes(StandardCharsets.UTF_8));
        Thread.sleep(1000);
        client.setData().forPath(workPath, "第2次更改内容".getBytes(StandardCharsets.UTF_8));
        Thread.sleep(1000);
        client.setData().forPath(workPath, "第3次更改内容".getBytes(StandardCharsets.UTF_8));
        Thread.sleep(1000);
    } catch(Exception e) {
        e.printStackTrace();
    }

}
```

## 3. 反复监听当前节点子节点

```java
public void childrenCache() {
        CuratorFramework client = ClientFactory.createSimple(IdMakeUtil.ZK_ADDRESS);
        try {
            client.start();
            Stat stat = client.checkExists().forPath(workPath);
            if (stat == null) {
                String data = "cache";
                byte[] payload = data.getBytes(StandardCharsets.UTF_8);
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(workPath, payload);
            }
            CuratorCache cache = CuratorCache.builder(client, workPath).build();

            CuratorCacheListener listener = CuratorCacheListener.builder().forPathChildrenCache(workPath, client, new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                    ChildData data = event.getData();
                    switch (event.getType()) {
                        case CHILD_ADDED:
                            log.info("子节点增加, path={}, data={}", data.getPath(), new String(data.getData()));
                            break;
                        case CHILD_UPDATED:
                            log.info("子节点更新, path={}, data={}", data.getPath(), new String(data.getData()));
                            break;
                        case CHILD_REMOVED:
                            log.info("子节点删除, path={}, data={}", data.getPath(), new String(data.getData()));
                            break;
                        default:
                            break;
                    }
                }
            }).build();
            cache.listenable().addListener(listener);
            cache.start();
            
            for (int i = 0; i < 3; i++) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(subWorkPath + i, "children".getBytes(StandardCharsets.UTF_8));
            }
            
            for (int i = 0; i < 3; i++) {
                client.delete().forPath(subWorkPath + i);
            }
        } catch(Exception e) {
            log.error("监听失败");
        }
    }
```

