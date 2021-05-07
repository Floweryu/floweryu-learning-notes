Java有四种创建线程的方式，分别为实现`Runnable`接口的`run`方法，继承`Thread`类并重写`run`方法，使用`FutureTask`方式，利用线程池`ExecutorService`、`Callable`、`future`来实现。

# 1. 前三种创建方式对比

采用实现`Runnable`、`Callable`接口的方式创见多线程时，优势是：

- 线程类只是实现了`Runnable`接口或`Callable`接口，还可以继承其他类。
- 在这种方式下，多个线程可以共享同一个`target`对象，所以非常适合多个相同线程来处理同一份资源的情况，从而可以将CPU、代码和数据分开，形成清晰的模型，较好地体现了面向对象的思想。

劣势是：

- 如果要访问当前线程，则必须使用Thread.currentThread()方法。

****

使用继承`Thread`类的方式创建多线程时.

优势是：

- 编写简单，如果需要访问当前线程，则无需使用`Thread.currentThread()`方法，直接使用this即可获得当前线程。

劣势是：

- 线程类已经继承了`Thread`类，所以不能再继承其他父类。

# 2. 继承`Thread`类方式的实现。

```java
import java.lang.*;

public class App {
    public static void main(String[] args) throws Exception {
        // 创建线程
        MyThread myThread = new MyThread();
        // 启动线程
        myThread.start();
    }

    public static class MyThread extends Thread {
        @Override
        public void run() {
            System.out.println("I am a children thread");
        }
    }
}

```

# 3.  实现`Runnable`接口的`run`方法形式

```java
import java.lang.*;

public class App {
    public static void main(String[] args) throws Exception {
        // 创建线程
        RunableTest runableTest = new RunableTest();
        // 启动线程
        new Thread(runableTest).start();
        new Thread(runableTest).start();
    }

    public static class RunableTest implements Runnable {
        @Override
        public void run() {
            System.out.println("I am a children thread");
        }
    }
}

```

# 4. 使用`FutureTask+Callable`方式

```java
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class App {
    public static void main(String[] args) throws InterruptedException {
        // 创建异步任务
        FutureTask<String> futureTask = new FutureTask<String>(new CallerTask());
        // 启动线程
        new Thread(futureTask).start();
        try {
            // 等待任务执行完毕，并返回结果
            String result = futureTask.get();
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class CallerTask implements Callable<String> {
        @Override
        public String call() throws Exception {
            return "hello";
        }
    }
}
```

# 5. 线程池方式创建

创建固定大小的线程池，提交Callable任务，利用Future获取返回的值：

```java
public class AddPool implements Callable<Integer> {
    private int start, end;

    public AddPool(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public Integer call() throws Exception {
        int sum = 0;
        System.out.println(Thread.currentThread().getName() + " 开始执行!");
        for (int i = start; i <= end; i++) {
            sum += i;
        }
        System.out.println(Thread.currentThread().getName() + " 执行完毕! sum=" + sum);
        return sum;
    }

    public static void main(String[] arg) throws ExecutionException, InterruptedException {
        int start=0, mid=500, end=1000;
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Integer> future1 = executorService.submit(new AddPool(start, mid));
        Future<Integer> future2 = executorService.submit(new AddPool(mid+1, end));

        int sum = future1.get() + future2.get();
        System.out.println("sum: " + sum);
    }
}

// pool-1-thread-1 开始执行!
// pool-1-thread-2 开始执行!
// pool-1-thread-1 执行完毕! sum=125250
// pool-1-thread-2 执行完毕! sum=375250
// sum: 500500
```



# 6. `Runnable`和`Callable`区别

- `Callable`规定的方法是`call()`，`Runnable`规定的方法是`run()`
- `call()`方法可以抛出异常，`run()`方法不可以
- `Callable`的任务执行后可返回值，而`Runnable`的任务是不能返回值(是`void`)
- 运行`Callable`任务可以拿到一个`Future或FutureTask`对象，表示异步计算的结果。它提供了检查计算是否完成的方法，以等待计算的完成，并检索计算的结果。通过`Future`对象可以了解任务执行情况，可取消任务的执行，还可获取执行结果。
- 线程池运行，`Runnable`使用`ExecutorService`的`execute`方法，`Callable`使用`submit`方法。

