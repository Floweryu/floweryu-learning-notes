import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class FixedThreadPollDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService poll = Executors.newFixedThreadPool(2);

        Thread t1 = new MyThread();
        Thread t2 = new MyThread();
        Thread t3 = new MyThread();
        Thread t4 = new MyThread();
        Thread t5 = new MyThread();
        
        poll.execute(t1);
        poll.execute(t2);
        poll.execute(t3);
        poll.execute(t4);
        poll.execute(t5);
        
        poll.shutdown();
    }
}

class MyThread extends Thread {
    @Override
    public void run () {
        System.out.println(Thread.currentThread().getName() + "正在执行");
    }
}