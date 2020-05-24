package mindustry.entities.comp;

import arc.util.io.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

import java.nio.*;

@Component
abstract class SyncComp implements Entityc{
    transient long lastUpdated, updateSpacing;

    //all these method bodies are internally generated
    void snapSync(){}
    void readSync(Reads read){}
    void writeSync(Writes write){}
    void readSyncManual(FloatBuffer buffer){}
    void writeSyncManual(FloatBuffer buffer){}
    void interpolate(){}

    @Override
    public void update(){
        if(Vars.net.client() && !isLocal()){
            interpolate();
        }
    }
}
