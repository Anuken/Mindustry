package mindustry.server;

import arc.*;
import mindustry.entities.type.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class IdleSync implements ApplicationListener{
    @Override
    public void update(){
        for(Player p : playerGroup){

            if(p.idle()){
                p.idle++;
            }else{
                p.idle = 0;
            }

            if(p.idle > 60 * 2.5 && p.syncWhenIdle){
                p.syncWhenIdle = false;
                Call.onWorldDataBegin(p.con);
                netServer.sendWorldData(p);
            }
        }
    }
}
