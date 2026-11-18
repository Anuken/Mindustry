package mindustry.service;

import arc.util.*;

import static mindustry.Vars.*;

public enum SStat{
    unitsDestroyed,
    attacksWon,
    pvpsWon,
    timesLaunched,
    blocksDestroyed,
    itemsLaunched,
    reactorsOverheated,
    maxUnitActive,
    unitTypesBuilt,
    unitsBuilt,
    bossesDefeated,
    maxPlayersServer,
    mapsMade,
    mapsPublished,
    maxWavesSurvived,
    blocksBuilt,
    maxProduction,
    sectorsControlled,
    schematicsCreated,
    bouldersDeconstructed,
    totalCampaignItems,
    ;

    public static final SStat[] all = values();

    /** Increments that have not been sent to the game service yet.  */
    private int pending;
    /** Achievements that depend on this stat. Lazily initialized. */
    private @Nullable Achievement[] linked;

    public int get(){
        return service.getStat(name(), 0) + pending;
    }

    public void max(int amount){
        if(amount > get()){
            set(amount);
        }
    }

    public void set(int amount){
        pending = 0;
        service.setStat(name(), amount);
        service.storeStats();
        checkAchievements();
    }

    /** Queues an increment. This does not call into the game service; the value is sent on the next {@link #flushAll()}. */
    public void add(int amount){
        pending += amount;
    }

    public void add(){
        add(1);
    }

    /** Sends any queued increments to the game service. */
    public void flush(){
        if(pending != 0){
            set(get());
        }
    }

    public static void flushAll(){
        for(SStat stat : all){
            stat.flush();
        }
    }

    private void checkAchievements(){
        if(linked == null){
            linked = Structs.filter(Achievement.class, Achievement.all, a -> a.stat() == this);
        }

        for(Achievement a : linked){
            a.checkCompletion();
        }
    }
}
