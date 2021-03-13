import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class SingleThreadPoolDemo {
    public static void main (String[] args) throws Exception{
        // 创建一个使用单个 worker 线程的 Executor，以无界队列方式来运行该线程。
        ExecutorService pool = Executors.newSingleThreadExecutor();

        Runnable task1 = new SingleTasks();
        Runnable task2 = new SingleTasks();
        Runnable task3 = new SingleTasks();

        pool.execute(task1);
        pool.execute(task2);
        pool.execute(task3);

        pool.shutdown();
    }
}

class SingleTasks implements Runnable {
    @Override
    public void run () {
        System.out.println(Thread.currentThread().getName() + "正在执行");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(Thread.currentThread().getName() + "执行完毕");
    }
}
