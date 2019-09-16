package io.anuke.mindustry.desktop.steam;

public enum SStat{
    unitsDestroyed,
    attacksWon,
    pvpsWon,
    timesLaunched,
    zoneMechsUsed,
    blocksDestroyed,
    itemsLaunched,
    reactorsOverheated,
    maxUnitActive,
    unitsBuilt,
    bossesDefeated,
    maxPlayersServer,
    mapsMade,
    mapsPublished,
    maxWavesSurvived,
    blocksBuilt,
    ;

    public int get(){
        return SVars.stats.stats.getStatI(name(), 0);
    }

    public void max(int amount){
        if(amount > get()){
            add(amount - get());
        }
    }

    public void add(int amount){
        SVars.stats.stats.setStatI(name(), get() + amount);
        SVars.stats.onUpdate();

        for(SAchievement a : SAchievement.all){
            a.checkCompletion();
        }
    }

    public void add(){
        add(1);
    }
}
