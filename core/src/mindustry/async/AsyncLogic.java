package mindustry.async;

import arc.*;
import arc.struct.*;
import mindustry.game.EventType.*;

import java.util.concurrent.*;

public class AsyncLogic{
    //all processes to be executed each frame
    private Array<AsyncProcess> processes = Array.with(new PhysicsProcess());

    //futures to be awaited
    private Array<Future<?>> futures = new Array<>();

    private ExecutorService executor = Executors.newFixedThreadPool(processes.size, r -> {
        Thread thread = new Thread(r, "AsyncLogic-Thread");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> Core.app.post(() -> { throw new RuntimeException(e); }));
        return thread;
    });

    public AsyncLogic(){
        Events.on(WorldLoadEvent.class, e -> {
            complete();
            for(AsyncProcess p : processes){
                p.init();
            }
        });

        Events.on(ResetEvent.class, e -> {
            complete();
            for(AsyncProcess p : processes){
                p.reset();
            }
        });
    }

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
        complete();

        //sync end (flush data)
        for(AsyncProcess p : processes){
            p.end();
        }
    }

    private void complete(){
        //wait for all threads to stop processing
        for(Future future : futures){
            try{
                future.get();
            }catch(Throwable t){
                throw new RuntimeException(t);
            }
        }

        //clear processed futures
        futures.clear();
    }
}
