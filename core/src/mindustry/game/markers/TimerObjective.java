package mindustry.game.markers;

import arc.*;
import arc.util.*;
import mindustry.game.objectives.*;
import mindustry.game.objectives.MapObjectives.*;

import static mindustry.Vars.*;

public class TimerObjective extends MapObjective{
    public @Multiline String text;
    public @Second float duration = 60f * 30f;

    protected float countup;

    public TimerObjective(String text, float duration){
        this.text = text;
        this.duration = duration;
    }

    public TimerObjective(){
    }

    @Override
    public boolean update(){
        return (countup += Time.delta) >= duration * state.rules.objectiveTimerMultiplier;
    }

    @Override
    public void reset(){
        countup = 0f;
    }

    @Nullable
    @Override
    public String text(){
        if(text != null){
            int i = (int)((duration * state.rules.objectiveTimerMultiplier - countup) / 60f);
            StringBuilder timeString = new StringBuilder();

            int m = i / 60;
            int s = i % 60;
            if(m > 0){
                timeString.append(m);
                timeString.append(":");
                if(s < 10){
                    timeString.append("0");
                }
            }
            timeString.append(s);

            if(text.startsWith("@")){
                if(state.mapLocales.containsProperty(text.substring(1))){
                    try{
                        return state.mapLocales.getFormatted(text.substring(1), timeString.toString());
                    }catch(IllegalArgumentException e){
                        //illegal text.
                        text = "";
                    }
                }
                //the 'escelating' typo was fixed in the bundle+maps, but existing saves don't have that fix, so it has to be changed here
                String actualText = text.equals("@objective.enemyescelating") ? "@objective.enemyescalating" : text;
                return Core.bundle.format(actualText.substring(1), timeString.toString());
            }else{
                try{
                    return Core.bundle.formatString(text, timeString.toString());
                }catch(IllegalArgumentException e){
                    //illegal text.
                    text = "";
                }

            }
        }

        return null;
    }

    @Override
    public String toString(){
        return "timer: " + duration;
    }
}
