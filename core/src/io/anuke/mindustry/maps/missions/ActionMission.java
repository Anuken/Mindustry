package io.anuke.mindustry.maps.missions;

import io.anuke.arc.Core;

/**A mission which simply runs a single action and is completed instantly.*/
public class ActionMission extends Mission{
    protected Runnable runner;

    public ActionMission(Runnable runner){
        this.runner = runner;
    }

    public ActionMission(){
    }

    @Override
    public void onComplete(){
        runner.run();
    }

    @Override
    public boolean isComplete(){
        return true;
    }

    @Override
    public String displayString(){
        return Core.bundle.get("text.loading");
    }
}
