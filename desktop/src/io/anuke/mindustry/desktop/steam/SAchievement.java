package io.anuke.mindustry.desktop.steam;

import io.anuke.arc.function.*;

public enum SAchievement{
    ;

    private final BooleanProvider completed;

    public static final SAchievement[] all = values();

    SAchievement(BooleanProvider completed){
        this.completed = completed;
    }

    /** Creates an achievement that is triggered when this stat reaches a number.*/
    SAchievement(SStat stat, int required){
        this(() -> stat.get() >= required);
    }

    public void checkCompletion(){
        if(!achieved() && conditionsMet()){
            SVars.stats.stats.setAchievement(name());
            SVars.stats.stats.storeStats();
        }
    }

    public boolean achieved(){
        return SVars.stats.stats.isAchieved(name(), false);
    }

    public boolean conditionsMet(){
        return completed.get();
    }
}
