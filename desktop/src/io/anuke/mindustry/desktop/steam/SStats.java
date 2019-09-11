package io.anuke.mindustry.desktop.steam;

import com.codedisaster.steamworks.*;
import io.anuke.arc.util.*;

public class SStats implements SteamUserStatsCallback{
    public final SteamUserStats stats = new SteamUserStats(this);

    //todo store stats periodically
    private boolean updated = false;

    public SStats(){
        stats.requestCurrentStats();
    }

    public void onUpdate(){
        this.updated = true;
    }

    private void registerEvents(){

    }

    @Override
    public void onUserStatsReceived(long gameID, SteamID steamID, SteamResult result){
        if(result == SteamResult.OK){
            registerEvents();
        }else{
            Log.err("Failed to recieve steam stats: {0}", result);
        }
    }

    @Override
    public void onUserStatsStored(long l, SteamResult steamResult){

    }

    @Override
    public void onUserStatsUnloaded(SteamID steamID){

    }

    @Override
    public void onUserAchievementStored(long l, boolean b, String s, int i, int i1){

    }

    @Override
    public void onLeaderboardFindResult(SteamLeaderboardHandle steamLeaderboardHandle, boolean b){

    }

    @Override
    public void onLeaderboardScoresDownloaded(SteamLeaderboardHandle steamLeaderboardHandle, SteamLeaderboardEntriesHandle steamLeaderboardEntriesHandle, int i){

    }

    @Override
    public void onLeaderboardScoreUploaded(boolean b, SteamLeaderboardHandle steamLeaderboardHandle, int i, boolean b1, int i1, int i2){

    }

    @Override
    public void onGlobalStatsReceived(long l, SteamResult steamResult){

    }
}
