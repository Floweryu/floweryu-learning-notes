运行下面代码，如果不对异常处进行捕获，则结果如下：

```bash
// 线程t1会中断，线程t2会进入代码继续执行
t1 start.
t1 count = 1
t1 count = 2
t1 count = 3
t1 count = 4
t1 count = 5
Exception in thread "t1" java.lang.ArithmeticException: / by zero
	at thread.Catch.m(Catch.java:30)
	at thread.Catch.lambda$main$0(Catch.java:39)
	at java.lang.Thread.run(Thread.java:748)
t2 start.
t2 count = 6
t2 count = 7
t2 count = 8
t2 count = 9
```

加上异常捕获：

```bash
// 线程t1在抛出异常后会继续执行，不会释放自己的锁
t1 start.
t1 count = 1
t1 count = 2
t1 count = 3
t1 count = 4
t1 count = 5
java.lang.ArithmeticException: / by zero
	at thread.Catch.m(Catch.java:23)
	at thread.Catch.lambda$main$0(Catch.java:35)
	at java.lang.Thread.run(Thread.java:748)
t1 count = 6
t1 count = 7
t1 count = 8
```



```java
public class Catch {
    int count = 0;
    synchronized void m() {
        System.out.println(Thread.currentThread().getName() + " start.");
        while (true) {
            count++;
            System.out.println(Thread.currentThread().getName() + " count = " + count);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                if (count == 5) {
                    int i = count / 0;		// 这里人为设置异常
                    System.out.println(i);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

        }
    }

    public static void main(String[] args) {
        Catch catchs = new Catch();
        Runnable r = () -> catchs.m();
        new Thread(r, "t1").start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(r, "t2").start();
    }
}
```

