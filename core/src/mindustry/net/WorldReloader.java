package mindustry.net;

import arc.struct.*;
import arc.struct.Seq.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

/** Handles player state for sending to every connected player*/
public class WorldReloader{
    Seq<Player> players = new Seq<>();
    boolean wasServer = false;
    boolean began = false;

    /** Begins reloading the world. Sends world begin packets to each user and stores player state.
     *  If the current client is not a server, this resets state and disconnects. */
    public void begin(){
        //don't begin twice
        if(began) return;

        if(wasServer = net.server()){
            players.clear();

            for(Player p : Groups.player){
                if(p.isLocal()) continue;

                players.add(p);
                p.clearUnit();
            }

            logic.reset();

            Call.worldDataBegin();
        }else{
            net.reset();
            logic.reset();
        }

        began = true;
    }

    /** Ends reloading the world. Sends world data to each player.
     * If the current client was not a server, does nothing.*/
    public void end(){
        if(wasServer){
            for(Player p : players){
                if(p.con == null) continue;

                boolean wasAdmin = p.admin;
                p.reset();
                p.admin = wasAdmin;
                if(state.rules.pvp){
                    p.team(netServer.assignTeam(p, new SeqIterable<>(players)));
                }
                netServer.sendWorldData(p);
            }
        }
    }
}
