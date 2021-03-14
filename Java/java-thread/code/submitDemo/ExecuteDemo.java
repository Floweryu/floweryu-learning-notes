import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecuteDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);

        Runnable r = () -> System.out.println(1 / 0);
        pool.execute(r);
        pool.shutdown();
    }
}