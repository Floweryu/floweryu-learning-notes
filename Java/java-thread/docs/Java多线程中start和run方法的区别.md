`start()`和`run()`方法的主要区别在于：

- 当程序调用`start()`方法，一个新线程将会被创建，并且在`run()`方法中的代码会在**新线程**上运行。

- 当直接调用`run()`方法时，程序并不会创建新线程，`run()`方法内部的代码在**当前线程**上运行。

【示例】

```java
public class DifferentBetweenStartAndRun {
    public static void main (String[] args) {
        ThreadTask startThread = new ThreadTask("start");
        ThreadTask runThread = new ThreadTask("run");

        new Thread(startThread).start();
        new Thread(runThread).run();
    }

    public static class ThreadTask implements Runnable {
        private String caller;

        public ThreadTask(String caller) {
            this.caller = caller;
        }

        @Override
        public void run () {
            System.out.println("Caller: " + caller + ";  Thread is " + Thread.currentThread().getName());
        }
    }
}

// 打印如下
// Caller: run;  Thread is main
// Caller: start;  Thread is Thread-0
```

