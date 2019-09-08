package io.anuke.mindustry.desktop.steam;

public enum SStat{
    unitsDestroyed;

    public int get(){
        return SVars.stats.stats.getStatI(name(), 0);
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
