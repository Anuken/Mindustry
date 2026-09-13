package mindustry.desktop.steam;

import arc.*;
import arc.util.*;
import mindustry.game.EventType.*;
import steamworks.*;

import static mindustry.Vars.*;

public class SStats implements SteamUserStatsCallback{
    public final SteamUserStats stats = new SteamUserStats(this);

    private boolean updated = false;
    private int statSavePeriod = 2; //in minutes

    public SStats(){
        service.init();

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
    public void onUserStatsStored(long gameID, SteamResult result){
        Log.info("Stored stats: @", result);

        if(result == SteamResult.OK){
            updated = false;
        }
    }
}
