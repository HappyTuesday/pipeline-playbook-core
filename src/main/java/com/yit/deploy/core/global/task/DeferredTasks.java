package com.yit.deploy.core.global.task;

import com.yit.deploy.core.function.Action;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeferredTasks {

    private static DeferredTasks instance = new DeferredTasks();

    public static DeferredTasks getInstance() {
        return instance;
    }

    private static final Logger LOGGER = Logger.getLogger(DeferredTasks.class.getName());

    private DelayQueue<DeferredTask> queue = new DelayQueue<>();

    public DeferredTasks() {
        Executors.newSingleThreadExecutor().submit(this::process);
    }

    public DeferredTasks submit(long delay, @Nonnull Action action) {
        return submit(delay, action, 0);
    }

    public DeferredTasks submit(long delay, @Nonnull Action action, int retries) {
        queue.put(new DeferredTask(delay, action, retries));
        return this;
    }

    private void process() {
        LOGGER.info("start deferred tasks");
        while (true) {
            DeferredTask task = queue.poll();
            if (task == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }

                continue;
            }

            try {
                task.action.run();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "failed to execute deferred task" + ( task.retries > 0 ? ". retrying..." : "."), e);
                if (task.retries > 0) {
                    submit(task.delay, task.action, task.retries - 1);
                }
            }
        }
        LOGGER.info("exit deferred tasks");
    }

    private static class DeferredTask implements Delayed {
        private long delay;
        private long expire;
        private Action action;
        private int retries = 0;

        public DeferredTask(long delay, Action action, int retries) {
            this.delay = delay;
            this.retries = retries;
            this.expire = new Date().getTime() + delay;
            this.action = action;
        }

        /**
         * Returns the remaining delay associated with this object, in the
         * given time unit.
         *
         * @param unit the time unit
         * @return the remaining delay; zero or negative values indicate
         * that the delay has already elapsed
         */
        @Override
        public long getDelay(@Nonnull TimeUnit unit) {
            return TimeUnit.MILLISECONDS.convert(expire - new Date().getTime(), unit);
        }

        @Override
        public int compareTo(@Nonnull Delayed o) {
            if (o == this) return 0;
            if (o instanceof DeferredTask) {
                return Long.compare(expire, ((DeferredTask) o).expire);
            }
            return 0;
        }
    }
}
