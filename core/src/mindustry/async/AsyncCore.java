package mindustry.async;

import arc.*;
import arc.struct.*;
import mindustry.*;
import mindustry.game.EventType.*;

import java.util.concurrent.*;

import static mindustry.Vars.state;

public class AsyncCore{
    //all processes to be executed each frame
    private final Seq<AsyncProcess> processes = Seq.with(
        new PhysicsProcess(),
        Vars.teamIndex = new TeamIndexProcess()
    );

    //futures to be awaited
    private final Seq<Future<?>> futures = new Seq<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(processes.size, r -> {
        Thread thread = new Thread(r, "AsyncLogic-Thread");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> Core.app.post(() -> { throw new RuntimeException(e); }));
        return thread;
    });

    public AsyncCore(){
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
        if(state.isPlaying()){
            //sync begin
            for(AsyncProcess p : processes){
                p.begin();
            }

            futures.clear();

            //submit all tasks
            for(AsyncProcess p : processes){
                if(p.shouldProcess()){
                    futures.add(executor.submit(p::process));
                }
            }
        }
    }

    public void end(){
        if(state.isPlaying()){
            complete();

            //sync end (flush data)
            for(AsyncProcess p : processes){
                p.end();
            }
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
