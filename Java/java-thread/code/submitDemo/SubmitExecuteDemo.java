import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SubmitExecuteDemo {
    
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);

        Runnable r = () -> System.out.println(1 / 0);
        Future<?> f = pool.submit(r);
        f.get();
        pool.shutdown();
    }
}