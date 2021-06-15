package xyz.szy.zephyr.dubbo.provider;

import java.util.concurrent.TimeUnit;

/**
 * @author ZiYue·Sun
 */
public class HoldProcessor {
    private volatile boolean stopAwait = false;
    /**
     * Thread that currently is inside our await() method.
     */
    private volatile Thread awaitThread = null;

    /**
     * 开始等待
     */
    public void startAwait() {
        Thread awaitThread = new Thread(this::await, "hold-process-thread");
        awaitThread.setContextClassLoader(getClass().getClassLoader());
        //这一步很关键，保证至少一个前台进程
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    /**
     * 停止等待，退出程序，一般放在shutdown hook中执行
     *
     * @see Runtime#addShutdownHook(Thread)
     */
    public void stopAwait() {
        //此变量
        stopAwait = true;
        Thread t = awaitThread;
        if (null != t) {
            t.interrupt();
            try {
                t.join(1000);
            } catch (InterruptedException ignored) { }
        }
    }

    private void await() {
        try {
            awaitThread = Thread.currentThread();
            while (!stopAwait) {
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ex) {
                    // continue and check the flag
                }
            }
        } finally {
            awaitThread = null;
        }

    }
}
