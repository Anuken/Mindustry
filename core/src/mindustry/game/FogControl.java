package mindustry.game;

import arc.*;
import arc.math.*;
import arc.struct.Bits;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.io.SaveFileReader.*;
import mindustry.io.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.*;

public final class FogControl implements CustomChunk{
    private static volatile int ww, wh;
    private static final int dynamicUpdateInterval = 1000 / 25; //25 FPS
    private static final Object notifyStatic = new Object(), notifyDynamic = new Object();

    /** indexed by team */
    private volatile @Nullable FogData[] fog;

    private final LongSeq staticEvents = new LongSeq();
    private final LongSeq dynamicEventQueue = new LongSeq(), unitEventQueue = new LongSeq();
    /** access must be synchronized; accessed from both threads */
    private final LongSeq dynamicEvents = new LongSeq(100);

    private @Nullable Thread staticFogThread;
    private @Nullable Thread dynamicFogThread;

    private boolean justLoaded = false;
    private boolean loadedStatic = false;

    public FogControl(){
        Events.on(ResetEvent.class, e -> {
            stop();
        });

        Events.on(WorldLoadEvent.class, e -> {
            stop();

            loadedStatic = false;
            justLoaded = true;
            ww = world.width();
            wh = world.height();

            //all old buildings have static light scheduled around them
            if(state.rules.fog && state.rules.staticFog){
                pushStaticBlocks(true);
                //force draw all static stuff immediately
                updateStatic();

                loadedStatic = true;
            }
        });

        Events.on(TileChangeEvent.class, event -> {
            if(state.rules.fog && event.tile.build != null && event.tile.isCenter() && !event.tile.build.team.isOnlyAI() && event.tile.block().flags.contains(BlockFlag.hasFogRadius)){
                var data = data(event.tile.team());
                if(data != null){
                    data.dynamicUpdated = true;
                }

                if(state.rules.staticFog){
                    synchronized(staticEvents){
                        //TODO event per team?
                        pushEvent(FogEvent.get(event.tile.x, event.tile.y, Mathf.round(event.tile.build.fogRadius()), event.tile.build.team.id), false);
                    }
                }
            }
        });

        //on tile removed, dynamic fog goes away
        Events.on(TilePreChangeEvent.class, e -> {
            if(state.rules.fog && e.tile.build != null && !e.tile.build.team.isOnlyAI() && e.tile.block().flags.contains(BlockFlag.hasFogRadius)){
                var data = data(e.tile.team());
                if(data != null){
                    data.dynamicUpdated = true;
                }
            }
        });

        //unit dead -> fog updates
        Events.on(UnitDestroyEvent.class, e -> {
            if(state.rules.fog && fog[e.unit.team.id] != null){
                fog[e.unit.team.id].dynamicUpdated = true;
            }
        });

        SaveVersion.addCustomChunk("static-fog-data", this);
    }

    public @Nullable Bits getDiscovered(Team team){
        return fog == null || fog[team.id] == null ? null : fog[team.id].staticData;
    }

    public boolean isDiscovered(Team team, int x, int y){
        if(!state.rules.staticFog || team.isAI()) return true;

        var data = getDiscovered(team);
        if(data == null) return false;
        if(x < 0 || y < 0 || x >= ww || y >= wh) return false;
        return data.get(x + y * ww);
    }

    public boolean isVisible(Team team, float x, float y){
        return isVisibleTile(team, World.toTile(x), World.toTile(y));
    }

    public boolean isVisibleTile(Team team, int x, int y){
        if(!state.rules.fog || team.isAI()) return true;

        var data = data(team);
        if(data == null) return false;
        if(x < 0 || y < 0 || x >= ww || y >= wh) return false;
        return data.read.get(x + y * ww);
    }

    public void resetFog(){
        fog = null;
    }

    @Nullable FogData data(Team team){
        return fog == null || fog[team.id] == null ? null : fog[team.id];
    }

    void stop(){
        fog = null;
        //I don't care whether the fog thread crashes here, it's about to die anyway
        staticEvents.clear();
        if(staticFogThread != null){
            staticFogThread.interrupt();
            staticFogThread = null;
        }

        dynamicEvents.clear();
        if(dynamicFogThread != null){
            dynamicFogThread.interrupt();
            dynamicFogThread = null;
        }
    }

    /** @param initial whether this is the initial update; if true, does not update renderer */
    void pushStaticBlocks(boolean initial){
        if(fog == null) fog = new FogData[256];

        synchronized(staticEvents){
            for(var build : Groups.build){
                if(build.block.flags.contains(BlockFlag.hasFogRadius)){
                    if(fog[build.team.id] == null){
                        fog[build.team.id] = new FogData();
                    }

                    pushEvent(FogEvent.get(build.tile.x, build.tile.y, Mathf.round(build.fogRadius()), build.team.id), initial);
                }
            }
        }
    }

    /** @param skipRender whether the event is passed to the fog renderer */
    void pushEvent(long event, boolean skipRender){
        if(!state.rules.staticFog) return;

        staticEvents.add(event);
        if(!skipRender && !headless && FogEvent.team(event) == Vars.player.team().id){
            renderer.fog.handleEvent(event);
        }
    }

    public void forceUpdate(Team team, Building build){
        if(state.rules.fog && fog[team.id] != null){
            fog[team.id].dynamicUpdated = true;

            if(state.rules.staticFog){
                synchronized(staticEvents){
                    pushEvent(FogEvent.get(build.tile.x, build.tile.y, Mathf.round(build.fogRadius()), build.team.id), false);
                }
            }
        }
    }

    public void update(){
        if(fog == null){
            fog = new FogData[256];
        }

        //force update static
        if(state.rules.staticFog && !loadedStatic){
            pushStaticBlocks(false);
            updateStatic();
            loadedStatic = true;
        }

        if(staticFogThread == null){
            staticFogThread = new StaticFogThread();
            staticFogThread.setPriority(Thread.NORM_PRIORITY - 1);
            staticFogThread.setDaemon(true);
            staticFogThread.start();
        }

        if(dynamicFogThread == null){
            dynamicFogThread = new DynamicFogThread();
            dynamicFogThread.setPriority(Thread.NORM_PRIORITY - 1);
            dynamicFogThread.setDaemon(true);
            dynamicFogThread.start();
        }

        //clear to prepare for queuing fog radius from units and buildings
        dynamicEventQueue.clear();

        for(var team : state.teams.present){
            //AI teams do not have fog
            if(!team.team.isOnlyAI()){
                //separate for each team
                unitEventQueue.clear();

                FogData data = fog[team.team.id];

                if(data == null){
                    data = fog[team.team.id] = new FogData();
                }

                synchronized(staticEvents){
                    //TODO slow?
                    for(var unit : team.units){
                        int tx = unit.tileX(), ty = unit.tileY(), pos = tx + ty * ww;
                        if(unit.type.fogRadius <= 0f) continue;
                        long event = FogEvent.get(tx, ty, (int)unit.type.fogRadius, team.team.id);

                        //always update the dynamic events, but only *flush* the results when necessary?
                        unitEventQueue.add(event);

                        if(unit.lastFogPos != pos){
                            pushEvent(event, false);
                            unit.lastFogPos = pos;
                            data.dynamicUpdated = true;
                        }
                    }
                }

                //if it's time for an update, flush *everything* onto the update queue
                if(data.dynamicUpdated && Time.timeSinceMillis(data.lastDynamicMs) > dynamicUpdateInterval){
                    data.dynamicUpdated = false;
                    data.lastDynamicMs = Time.millis();

                    //add building updates
                    for(var build : indexer.getFlagged(team.team, BlockFlag.hasFogRadius)){
                        dynamicEventQueue.add(FogEvent.get(build.tile.x, build.tile.y, Mathf.round(build.fogRadius()), build.team.id));
                    }

                    //add unit updates
                    dynamicEventQueue.addAll(unitEventQueue);
                }
            }
        }

        if(dynamicEventQueue.size > 0){
            //flush unit events over when something happens
            synchronized(dynamicEvents){
                dynamicEvents.clear();
                dynamicEvents.addAll(dynamicEventQueue);
            }
            dynamicEventQueue.clear();

            //force update so visibility doesn't have a pop-in
            if(justLoaded){
                updateDynamic(new Bits(256));
                justLoaded = false;
            }

            //notify that it's time for rendering
            //TODO this WILL block until it is done rendering, which is inherently problematic.
            synchronized(notifyDynamic){
                notifyDynamic.notify();
            }
        }

        //wake up, it's time to draw some circles
        if(state.rules.staticFog && staticEvents.size > 0 && staticFogThread != null){
            synchronized(notifyStatic){
                notifyStatic.notify();
            }
        }
    }

    class StaticFogThread extends Thread{

        StaticFogThread(){
            super("StaticFogThread");
        }

        @Override
        public void run(){
            while(true){
                try{
                    synchronized(notifyStatic){
                        try{
                            //wait until an event happens
                            notifyStatic.wait();
                        }catch(InterruptedException e){
                            //end thread
                            return;
                        }
                    }

                    updateStatic();
                    //ignore, don't want to crash this thread
                }catch(Exception e){}
            }
        }
    }

    void updateStatic(){

        //I really don't like synchronizing here, but there should be *some* performance benefit at least
        synchronized(staticEvents){
            int size = staticEvents.size;
            for(int i = 0; i < size; i++){
                long event = staticEvents.items[i];
                int x = FogEvent.x(event), y = FogEvent.y(event), rad = FogEvent.radius(event), team = FogEvent.team(event);
                var data = fog[team];
                if(data != null){
                    circle(data.staticData, x, y, rad);
                }
            }
            staticEvents.clear();
        }
    }

    class DynamicFogThread extends Thread{
        final Bits cleared = new Bits();

        DynamicFogThread(){
            super("DynamicFogThread");
        }

        @Override
        public void run(){

            while(true){
                try{
                    synchronized(notifyDynamic){
                        try{
                            //wait until an event happens
                            notifyDynamic.wait();
                        }catch(InterruptedException e){
                            //end thread
                            return;
                        }
                    }

                    updateDynamic(cleared);

                    //ignore, don't want to crash this thread
                }catch(Exception e){
                    //log for debugging
                    e.printStackTrace();
                }
            }
        }
    }

    void updateDynamic(Bits cleared){
        cleared.clear();

        //ugly sync
        synchronized(dynamicEvents){
            int size = dynamicEvents.size;

            //draw step
            for(int i = 0; i < size; i++){
                long event = dynamicEvents.items[i];
                int x = FogEvent.x(event), y = FogEvent.y(event), rad = FogEvent.radius(event), team = FogEvent.team(event);

                if(rad <= 0) continue;

                var data = fog[team];
                if(data != null){

                    //clear the buffer, since it is being re-drawn
                    if(!cleared.get(team)){
                        cleared.set(team);

                        data.write.clear();
                    }

                    //radius is always +1 to keep up with visuals
                    circle(data.write, x, y, rad + 1);
                }
            }
            dynamicEvents.clear();
        }

        //swap step, no need for synchronization or anything
        for(int i = 0; i < 256; i++){
            if(cleared.get(i)){
                var data = fog[i];

                //swap buffers, flushing the data that was just drawn
                Bits temp = data.read;
                data.read = data.write;
                data.write = temp;
            }
        }
    }

    @Override
    public void write(DataOutput stream) throws IOException{
        int used = 0;
        for(int i = 0; i < 256; i++){
            if(fog[i] != null) used ++;
        }

        stream.writeByte(used);
        stream.writeShort(world.width());
        stream.writeShort(world.height());

        for(int i = 0; i < 256; i++){
            if(fog[i] != null){
                stream.writeByte(i);
                Bits data = fog[i].staticData;
                int size = ww * wh;

                int pos = 0;
                while(pos < size){
                    int consecutives = 0;
                    boolean cur = data.get(pos);
                    while(consecutives < 127 && pos < size){
                        if(cur != data.get(pos)){
                            break;
                        }

                        consecutives ++;
                        pos ++;
                    }
                    int mask = (cur ? 0b1000_0000 : 0);
                    stream.write(mask | (consecutives));
                }
            }
        }
    }

    @Override
    public void read(DataInput stream) throws IOException{
        if(fog == null) fog = new FogData[256];

        int teams = stream.readUnsignedByte();
        int w = stream.readShort(), h = stream.readShort();
        int len = w * h;

        ww = w;
        wh = h;

        for(int ti = 0; ti < teams; ti++){
            int team = stream.readUnsignedByte();
            fog[team] = new FogData();

            int pos = 0;
            Bits bools = fog[team].staticData;

            while(pos < len){
                int data = stream.readByte() & 0xff;
                boolean sign = (data & 0b1000_0000) != 0;
                int consec = data & 0b0111_1111;

                if(sign){
                    bools.set(pos, pos + consec);
                    pos += consec;
                }else{
                    pos += consec;
                }
            }
        }

    }

    @Override
    public boolean shouldWrite(){
        return state.rules.fog && state.rules.staticFog && fog != null;
    }

    static void circle(Bits arr, int x, int y, int radius){
        int f = 1 - radius;
        int ddFx = 1, ddFy = -2 * radius;
        int px = 0, py = radius;

        hline(arr, x, x, y + radius);
        hline(arr, x, x, y - radius);
        hline(arr, x - radius, x + radius, y);

        while(px < py){
            if(f >= 0){
                py--;
                ddFy += 2;
                f += ddFy;
            }
            px++;
            ddFx += 2;
            f += ddFx;
            hline(arr, x - px, x + px, y + py);
            hline(arr, x - px, x + px, y - py);
            hline(arr, x - py, x + py, y + px);
            hline(arr, x - py, x + py, y - px);
        }
    }

    static void hline(Bits arr, int x1, int x2, int y){
        if(y < 0 || y >= wh) return;
        int tmp;

        if(x1 > x2){
            tmp = x1;
            x1 = x2;
            x2 = tmp;
        }

        if(x1 >= ww) return;
        if(x2 < 0) return;

        if(x1 < 0) x1 = 0;
        if(x2 >= ww) x2 = ww - 1;
        x2++;
        int off = y * ww;

        arr.set(off + x1, off + x2);
    }

    static class FogData{
        /** dynamic double-buffered data for dynamic (live) coverage */
        volatile Bits read, write;
        /** static map exploration fog*/
        final Bits staticData;

        /** last dynamic update timestamp. */
        long lastDynamicMs = 0;
        /** if true, a dynamic fog update must be scheduled. */
        boolean dynamicUpdated = true;

        FogData(){
            int len = ww * wh;

            read = new Bits(len);
            write = new Bits(len);
            staticData = new Bits(len);
        }
    }

    @Struct
    class FogEventStruct{
        @StructField(16)
        int x;
        @StructField(16)
        int y;
        @StructField(16)
        int radius;
        @StructField(8)
        int team;
    }
}
