package arc.packer;

import arc.*;
import arc.struct.*;
import arc.util.*;

import java.util.concurrent.*;

/**
 * Small helper for running leaf tasks on {@link Core#executor}.
 * Leaf tasks must never wait on other tasks themselves.
 */
final class Tasks{

    private Tasks(){
    }

    /** @return whether there is any point in scheduling work on other threads. */
    static boolean parallel(){
        return OS.cores > 1;
    }

    /** Schedules a task. It must be {@link #join(FutureTask) joined} to get the result. */
    static <T> FutureTask<T> submit(Callable<T> callable){
        FutureTask<T> task = new FutureTask<>(callable);
        if(parallel()){
            try{
                Core.executor.execute(task);
            }catch(RejectedExecutionException ignored){
                //executor was shut down; join() will simply run the task on the calling thread
            }
        }
        return task;
    }

    /** Waits for a task (running it on this thread if it hasn't started yet) and returns its result. */
    static <T> T join(FutureTask<T> task){
        task.run(); //no-op if already started or finished by another thread
        try{
            return task.get();
        }catch(ExecutionException e){
            Throwable cause = e.getCause();
            if(cause instanceof RuntimeException) throw (RuntimeException)cause;
            if(cause instanceof Error) throw (Error)cause;
            throw new ArcRuntimeException(cause);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new ArcRuntimeException(e);
        }
    }

    /** Joins every task, so nothing is still running when this returns, then rethrows the first failure (in order), if any. */
    static void joinAll(Seq<? extends FutureTask<?>> tasks){
        RuntimeException first = null;
        for(FutureTask<?> task : tasks){
            try{
                join(task);
            }catch(RuntimeException e){
                if(first == null) first = e;
            }
        }
        if(first != null) throw first;
    }

    /** Waits until every task is finished, one way or another, ignoring failures. */
    static void awaitAll(Seq<? extends FutureTask<?>> tasks){
        for(FutureTask<?> task : tasks){
            try{
                task.run(); //no-op unless it never started, in which case it was cancelled and this does nothing either
                task.get();
            }catch(Exception ignored){
            }
        }
    }

    /** Prevents tasks that have not started yet from running. */
    static void cancelAll(Seq<? extends FutureTask<?>> tasks){
        for(FutureTask<?> task : tasks){
            task.cancel(false);
        }
    }
}
