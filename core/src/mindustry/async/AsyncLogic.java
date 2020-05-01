package mindustry.async;

import arc.struct.*;

import java.util.concurrent.*;

public class AsyncLogic{
    //all processes to be executed each frame
    private Array<AsyncProcess> processes = Array.with(new PhysicsProcess());

    //futures to be awaited
    private Array<Future<?>> futures = new Array<>();

    private ExecutorService executor = Executors.newFixedThreadPool(processes.size, r -> {
        Thread thread = new Thread(r, "AsyncExecutor-Thread");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            //TODO crash!
        });
        return thread;
    });

    public void begin(){
        //sync begin
        for(AsyncProcess p : processes){
            p.begin();
        }

        futures.clear();

        //submit all tasks
        for(AsyncProcess p : processes){
            futures.add(executor.submit(p::process));
        }
    }

    public void end(){
        //wait for all threads to stop processing
        for(Future future : futures){
            try{
                future.get();
            }catch(Throwable t){
                throw new RuntimeException(t);
            }
        }

        futures.clear();

        //sync end (flush data)
        for(AsyncProcess p : processes){
            p.end();
        }
    }
}
