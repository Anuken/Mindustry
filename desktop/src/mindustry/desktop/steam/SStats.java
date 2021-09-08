package mindustry.desktop.steam;

import arc.*;
import arc.util.*;
import com.codedisaster.steamworks.*;
import mindustry.game.EventType.*;

import static mindustry.Vars.*;

public class SStats implements SteamUserStatsCallback{
    public final SteamUserStats stats = new SteamUserStats(this);

    private boolean updated = false;
    private int statSavePeriod = 4; //in minutes

    public SStats(){
        stats.requestCurrentStats();

        Events.on(ClientLoadEvent.class, e -> {
            Timer.schedule(() -> {
                if(updated){
                    stats.storeStats();
                }
            }, statSavePeriod * 60, statSavePeriod * 60);
        });
    }

    public void onUpdate(){
        this.updated = true;
    }

    @Override
    public void onUserStatsReceived(long gameID, SteamID steamID, SteamResult result){
        service.init();

        if(result != SteamResult.OK){
            Log.err("Failed to receive steam stats: @", result);
        }else{
            Log.info("Received steam stats.");
        }
    }

    @Override
    public void onUserStatsStored(long gameID, SteamResult result){
        Log.info("Stored stats: @", result);

        if(result == SteamResult.OK){
            updated = true;
        }
    }
}
