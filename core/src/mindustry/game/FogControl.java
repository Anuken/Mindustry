package mindustry.game;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.io.SaveFileReader.*;
import mindustry.io.*;

import java.io.*;

import static mindustry.Vars.*;

public class FogControl implements CustomChunk{
    private static volatile int ww, wh;
    private static final int buildLight = 0;

    private final Object sync = new Object();
    /** indexed by [team] [packed array tile pos] */
    private @Nullable boolean[][] fog;

    private final LongSeq events = new LongSeq();
    private @Nullable Thread fogThread;

    private boolean read = false;

    public FogControl(){
        Events.on(ResetEvent.class, e -> {
            clear();
        });

        Events.on(WorldLoadEvent.class, e -> {
            clear();

            ww = world.width();
            wh = world.height();

            //all old buildings have light around them
            if(state.rules.fog && read){
                for(var build : Groups.build){
                    synchronized(events){
                        events.add(FogEvent.get(build.tile.x, build.tile.y, buildLight + build.block.size, build.team.id));
                    }
                }

                read = false;
            }
        });

        Events.on(TileChangeEvent.class, event -> {
            if(state.rules.fog && event.tile.build != null && event.tile.isCenter()){
                synchronized(events){
                    //TODO event per team?
                    pushEvent(FogEvent.get(event.tile.x, event.tile.y, buildLight + event.tile.block().size, event.tile.build.team.id));
                }
            }
        });

        SaveVersion.addCustomChunk("fogdata", this);
    }

    public @Nullable boolean[] getData(Team team){
        return fog == null ? null : fog[team.id];
    }

    public boolean isCovered(Team team, int x, int y){
        var data = getData(team);
        if(data == null || x < 0 || y < 0 || x >= ww || y >= wh) return false;
        return !data[x + y * ww];
    }

    void clear(){
        fog = null;
        //I don't care whether the fog thread crashes here, it's about to die anyway
        events.clear();
        if(fogThread != null){
            fogThread.interrupt();
            fogThread = null;
        }
    }

    void pushEvent(long event){
        events.add(event);
        if(!headless && FogEvent.team(event) == Vars.player.team().id){
            renderer.fog.handleEvent(event);
        }
    }

    public void update(){
        if(fog == null){
            fog = new boolean[256][];
        }

        if(fogThread == null && !net.client()){
            fogThread = new FogThread();
            fogThread.setDaemon(true);
            fogThread.start();
        }

        for(var team : state.teams.present){
            if(!team.team.isAI()){

                if(fog[team.team.id] == null){
                    fog[team.team.id] = new boolean[world.width() * world.height()];
                }

                synchronized(events){
                    //TODO slow?
                    for(var unit : team.units){
                        int tx = unit.tileX(), ty = unit.tileY(), pos = tx + ty * ww;
                        if(unit.lastFogPos != pos){
                            pushEvent(FogEvent.get(tx, ty, (int)unit.type.fogRadius, team.team.id));
                            unit.lastFogPos = pos;
                        }
                    }
                }
            }
        }

        //wake up, it's time to draw some circles
        if(events.size > 0 && fogThread != null){
            synchronized(sync){
                sync.notify();
            }
        }
    }

    public class FogThread extends Thread{

        @Override
        public void run(){
            while(true){
                try{
                    synchronized(sync){
                        try{
                            //wait until an event happens
                            sync.wait();
                        }catch(InterruptedException e){
                            //end thread
                            return;
                        }
                    }

                    //I really don't like synchronizing here, but there should be some performance benefit at least
                    synchronized(events){
                        int size = events.size;
                        for(int i = 0; i < size; i++){
                            long event = events.items[i];
                            int x = FogEvent.x(event), y = FogEvent.y(event), rad = FogEvent.radius(event), team = FogEvent.team(event);
                            var arr = fog[team];
                            if(arr != null){
                                circle(arr, x, y, rad);
                            }
                        }
                        events.clear();
                    }
                    //ignore, don't want to crash this thread
                }catch(Exception e){}
            }
        }

        void circle(boolean[] arr, int x, int y, int radius){
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

        void hline(boolean[] arr, int x1, int x2, int y){
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

            while(x1 != x2){
                arr[off + x1++] = true;
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
                boolean[] data = fog[i];

                int pos = 0, size = data.length;
                while(pos < size){
                    int consecutives = 0;
                    boolean cur = data[pos];
                    while(consecutives < 127 && pos < size){
                        boolean next = data[pos];
                        if(cur != next){
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
        if(fog == null) fog = new boolean[256][];

        int teams = stream.readUnsignedByte();
        int w = stream.readShort(), h = stream.readShort();
        int len = w * h;

        for(int ti = 0; ti < teams; ti++){
            int team = stream.readUnsignedByte();
            int pos = 0;
            boolean[] bools = fog[team] = new boolean[w * h];

            while(pos < len){
                int data = stream.readByte() & 0xff;
                boolean sign = (data & 0b1000_0000) != 0;
                int consec = data & 0b0111_1111;

                if(sign){
                    for(int i = 0; i < consec; i++){
                        bools[pos ++] = true;
                    }
                }else{
                    pos += consec;
                }
            }
        }

        read = true;

    }

    @Override
    public boolean shouldWrite(){
        return state.rules.fog && fog != null;
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
