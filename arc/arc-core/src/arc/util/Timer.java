package arc.util;

import arc.Application;
import arc.ApplicationListener;
import arc.Core;
import arc.Files;
import arc.struct.Seq;

/**
 * Executes tasks in the future on the main loop thread.
 * @author Nathan Sweet
 */
// TimerThread access is synchronized using threadLock.
// Timer access is synchronized using the Timer instance.
// Task access is synchronized using the Task instance.
public class Timer{
    static final Object threadLock = new Object();
    static TimerThread thread;

    final Seq<Task> tasks = new Seq<>(false, 8);

    public Timer(){
        start();
    }

    /**
     * Timer instance singleton for general application wide usage. Static methods on {@link Timer} make convenient use of this
     * instance.
     */
    public static Timer instance(){
        synchronized(threadLock){
            TimerThread thread = thread();
            if(thread.instance == null) thread.instance = new Timer();
            return thread.instance;
        }
    }

    private static TimerThread thread(){
        synchronized(threadLock){
            if(thread == null || thread.files != Core.files){
                if(thread != null) thread.dispose();
                thread = new TimerThread();
            }
            return thread;
        }
    }

    /**
     * Schedules a task on {@link #instance}.
     * @see #postTask(Task)
     */
    public static Task post(Task task){
        return instance().postTask(task);
    }

    /**
     * Schedules a task on {@link #instance}.
     * @see #scheduleTask(Task, float)
     */
    public static Task schedule(Task task, float delaySeconds){
        return instance().scheduleTask(task, delaySeconds);
    }

    /**
     * Schedules a task on {@link #instance}.
     * @see #scheduleTask(Task, float, float)
     */
    public static Task schedule(Task task, float delaySeconds, float intervalSeconds){
        return instance().scheduleTask(task, delaySeconds, intervalSeconds);
    }

    /**
     * Schedules a task on {@link #instance}.
     * @see #scheduleTask(Task, float, float, int)
     */
    public static Task schedule(Task task, float delaySeconds, float intervalSeconds, int repeatCount){
        return instance().scheduleTask(task, delaySeconds, intervalSeconds, repeatCount);
    }

    /**
     * Schedules a task on {@link #instance}.
     * @see #scheduleTask(Task, float)
     */
    public static Task schedule(Runnable task, float delaySeconds){
        return instance().scheduleTask(new Task(){
            @Override
            public void run(){
                task.run();
            }
        }, delaySeconds);
    }

    /**
     * Schedules a task on {@link #instance}.
     * @see #scheduleTask(Task, float, float)
     */
    public static Task schedule(Runnable task, float delaySeconds, float intervalSeconds){
        return instance().scheduleTask(new Task(){
            @Override
            public void run(){
                task.run();
            }
        }, delaySeconds, intervalSeconds);
    }

    /**
     * Schedules a task on {@link #instance}.
     * @see #scheduleTask(Task, float, float, int)
     */
    public static Task schedule(Runnable task, float delaySeconds, float intervalSeconds, int repeatCount){
        return instance().scheduleTask(new Task(){
            @Override
            public void run(){
                task.run();
            }
        }, delaySeconds, intervalSeconds, repeatCount);
    }

    /** Schedules a task to occur once as soon as possible, but not sooner than the start of the next frame. */
    public Task postTask(Task task){
        return scheduleTask(task, 0, 0, 0);
    }

    /** Schedules a task to occur once after the specified delay. */
    public Task scheduleTask(Task task, float delaySeconds){
        return scheduleTask(task, delaySeconds, 0, 0);
    }

    /** Schedules a task to occur once after the specified delay and then repeatedly at the specified interval until cancelled. */
    public Task scheduleTask(Task task, float delaySeconds, float intervalSeconds){
        return scheduleTask(task, delaySeconds, intervalSeconds, -1);
    }

    /**
     * Schedules a task to occur once after the specified delay and then a number of additional times at the specified interval.
     * @param repeatCount If negative, the task will repeat forever.
     */
    public Task scheduleTask(Task task, float delaySeconds, float intervalSeconds, int repeatCount){
        synchronized(this){
            synchronized(task){
                if(task.timer != null) throw new IllegalArgumentException("The same task may not be scheduled twice.");
                task.timer = this;
                task.executeTimeMillis = System.nanoTime() / 1000000 + (long)(delaySeconds * 1000);
                task.intervalMillis = (long)(intervalSeconds * 1000);
                task.repeatCount = repeatCount;
                tasks.add(task);
            }
        }
        synchronized(threadLock){
            threadLock.notifyAll();
        }
        return task;
    }

    /** Stops the timer, tasks will not be executed and time that passes will not be applied to the task delays. */
    public void stop(){
        synchronized(threadLock){
            thread().instances.remove(this, true);
        }
    }

    /** Starts the timer if it was stopped. */
    public void start(){
        synchronized(threadLock){
            TimerThread thread = thread();
            Seq<Timer> instances = thread.instances;
            if(instances.contains(this, true)) return;
            instances.add(this);
            threadLock.notifyAll();
        }
    }

    /** Cancels all tasks. */
    public synchronized void clear(){
        for(int i = 0, n = tasks.size; i < n; i++){
            Task task = tasks.get(i);
            synchronized(task){
                task.executeTimeMillis = 0;
                task.timer = null;
            }
        }
        tasks.clear();
    }

    /**
     * Returns true if the timer has no tasks in the queue. Note that this can change at any time. Synchronize on the timer
     * instance to prevent tasks being added, removed, or updated.
     */
    public synchronized boolean isEmpty(){
        return tasks.size == 0;
    }

    synchronized long update(long timeMillis, long waitMillis){
        for(int i = 0, n = tasks.size; i < n; i++){
            Task task = tasks.get(i);
            synchronized(task){
                if(task.executeTimeMillis > timeMillis){
                    waitMillis = Math.min(waitMillis, task.executeTimeMillis - timeMillis);
                    continue;
                }
                if(task.repeatCount == 0){
                    task.timer = null;
                    tasks.remove(i);
                    i--;
                    n--;
                }else{
                    task.executeTimeMillis = timeMillis + task.intervalMillis;
                    waitMillis = Math.min(waitMillis, task.intervalMillis);
                    if(task.repeatCount > 0) task.repeatCount--;
                }
                task.app.post(task);
            }
        }
        return waitMillis;
    }

    /** Adds the specified delay to all tasks. */
    public synchronized void delay(long delayMillis){
        for(int i = 0, n = tasks.size; i < n; i++){
            Task task = tasks.get(i);
            synchronized(task){
                task.executeTimeMillis += delayMillis;
            }
        }
    }

    /**
     * Runnable that can be scheduled on a {@link Timer}.
     * @author Nathan Sweet
     */
    static abstract public class Task implements Runnable{
        final Application app;
        long executeTimeMillis, intervalMillis;
        int repeatCount;
        volatile Timer timer;

        public Task(){
            app = Core.app; // Store which app to post
            if(app == null) throw new IllegalStateException("Core.app not available.");
        }

        /**
         * If this is the last time the task will be ran or the task is first cancelled, it may be scheduled again in this
         * method.
         */
        abstract public void run();

        /** Cancels the task. It will not be executed until it is scheduled again. This method can be called at any time. */
        public void cancel(){
            Timer timer = this.timer;
            if(timer != null){
                synchronized(timer){
                    synchronized(this){
                        executeTimeMillis = 0;
                        this.timer = null;
                        timer.tasks.remove(this, true);
                    }
                }
            }else{
                synchronized(this){
                    executeTimeMillis = 0;
                    this.timer = null;
                }
            }
        }

        /**
         * Returns true if this task is scheduled to be executed in the future by a timer. The execution time may be reached at any
         * time after calling this method, which may change the scheduled state. To prevent the scheduled state from changing,
         * synchronize on this task object, eg:
         *
         * <pre>
         * synchronized (task) {
         * 	if (!task.isScheduled()) { ... }
         * }
         * </pre>
         */
        public boolean isScheduled(){
            return timer != null;
        }

        /** Returns the time in milliseconds when this task will be executed next. */
        public synchronized long getExecuteTimeMillis(){
            return executeTimeMillis;
        }
    }

    /**
     * Manages a single thread for updating timers. Uses application events to pause, resume, and dispose the thread.
     * @author Nathan Sweet
     */
    static class TimerThread implements Runnable, ApplicationListener{
        final Files files;
        final Seq<Timer> instances = new Seq<>(1);
        Timer instance;
        private long pauseMillis;

        public TimerThread(){
            files = Core.files;
            Core.app.addListener(this);
            resume();

            Thread thread = new Thread(this, "Timer");
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public void run(){
            while(true){
                synchronized(threadLock){
                    if(thread != this || files != Core.files) break;

                    long waitMillis = 5000;
                    if(pauseMillis == 0){
                        long timeMillis = System.nanoTime() / 1000000;
                        for(int i = 0, n = instances.size; i < n; i++){
                            try{
                                waitMillis = instances.get(i).update(timeMillis, waitMillis);
                            }catch(Throwable ex){
                                throw new ArcRuntimeException("Task failed: " + instances.get(i).getClass().getName(), ex);
                            }
                        }
                    }

                    if(thread != this || files != Core.files) break;

                    try{
                        if(waitMillis > 0) threadLock.wait(waitMillis);
                    }catch(InterruptedException ignored){
                    }
                }
            }
            dispose();
        }

        @Override
        public void resume(){
            if(Core.app.isDesktop()) return;
            synchronized(threadLock){
                long delayMillis = System.nanoTime() / 1000000 - pauseMillis;
                for(int i = 0, n = instances.size; i < n; i++)
                    instances.get(i).delay(delayMillis);
                pauseMillis = 0;
                threadLock.notifyAll();
            }
        }

        @Override
        public void pause(){
            //allow tasks to run in the background on desktop
            if(Core.app.isDesktop()) return;
            synchronized(threadLock){
                pauseMillis = System.nanoTime() / 1000000;
                threadLock.notifyAll();
            }
        }

        @Override
        public void dispose(){ // OK to call multiple times.
            synchronized(threadLock){
                if(thread == this) thread = null;
                instances.clear();
                threadLock.notifyAll();
            }
            Core.app.removeListener(this);
        }
    }
}
