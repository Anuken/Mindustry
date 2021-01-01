package mindustry.desktop.steam;

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
        return SVars.stats.stats.getStatI(name(), 0);
    }

    public void max(int amount){
        if(amount > get()){
            set(amount);
        }
    }

    public void set(int amount){
        SVars.stats.stats.setStatI(name(), amount);
        SVars.stats.onUpdate();

        for(SAchievement a : SAchievement.all){
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
