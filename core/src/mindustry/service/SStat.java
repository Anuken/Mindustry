package mindustry.service;

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
    ;

    public int get(){
        return service.getStat(name(), 0);
    }

    public void max(int amount){
        if(amount > get()){
            set(amount);
        }
    }

    public void set(int amount){
        service.setStat(name(), amount);
        service.storeStats();

        for(Achievement a : Achievement.all){
            a.checkCompletion();
        }
    }

    public void add(int amount){
        set(get() + amount);
    }

    public void add(){
        add(1);
    }
}
