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
    void afterSync(){}
    void interpolate(){}

    @Override
    public void update(){
        //interpolate the player if:
        //- this is a client and the entity is everything except the local player
        //- this is a server and the entity is a remote player
        if((Vars.net.client() && !isLocal()) || isRemote()){
            interpolate();
        }
    }

    @Override
    public void remove(){
        //notify client of removal
        if(Vars.net.client()){
            Vars.netClient.addRemovedEntity(id());
        }
    }
}
