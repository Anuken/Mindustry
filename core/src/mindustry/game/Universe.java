package mindustry.game;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class Universe{
    private long seconds;
    private float secondCounter;

    public Universe(){
        load();
    }

    public void updateGlobal(){
        //currently only updates one solar system
        updatePlanet(Planets.sun);
    }

    private void updatePlanet(Planet planet){
        planet.position.setZero();
        planet.addParentOffset(planet.position);
        if(planet.parent != null){
            planet.position.add(planet.parent.position);
        }
        for(Planet child : planet.children){
            updatePlanet(child);
        }
    }

    public void update(){
        secondCounter += Time.delta() / 60f;
        if(secondCounter >= 1){
            seconds += (int)secondCounter;
            secondCounter %= 1f;

            //save every few seconds
            if(seconds % 10 == 1){
                save();
            }
        }

        if(state.hasSector()){
            //update sector light
            float light = state.getSector().getLight();
            float alpha = Mathf.clamp(Mathf.map(light, 0f, 0.8f, 0.1f, 1f));
            //assign and map so darkness is not 100% dark
            state.rules.ambientLight.a = 1f - alpha;
            state.rules.lighting = !Mathf.equal(alpha, 1f);
        }
    }

    public float secondsMod(float mod, float scale){
        return (seconds / scale) % mod;
    }

    public long seconds(){
        return seconds;
    }

    public float secondsf(){
        return seconds + secondCounter;
    }

    private void save(){
        Core.settings.put("utime", seconds);
        Core.settings.save();
    }

    private void load(){
        seconds = Core.settings.getLong("utime");
    }

}
