package io.anuke.mindustry.maps.missions;

import io.anuke.ucore.function.BooleanProvider;

public class ConditionMission extends Mission{
    private final BooleanProvider complete;
    private final String display;

    public ConditionMission(String display, BooleanProvider complete){
        this.complete = complete;
        this.display = display;
    }

    @Override
    public boolean isComplete(){
        return complete.get();
    }

    @Override
    public String displayString(){
        return display;
    }
}
