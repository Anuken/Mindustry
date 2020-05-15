package mindustry.game;

import arc.*;
import arc.math.*;
import arc.struct.ObjectFloatMap.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.io.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** Updates the campaign universe. Has no relevance to other gamemodes. */
public class Universe{
    private long seconds;
    private float secondCounter;
    private int turn;
    private float turnCounter;

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

        //update turn state - happens only in-game
        turnCounter += Time.delta();

        if(turnCounter >= turnDuration){
            turn ++;
            turnCounter = 0;
            onTurn();
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

    public int[] getTotalExports(){
        int[] exports = new int[Vars.content.items().size];

        for(Planet planet : content.planets()){
            for(Sector sector : planet.sectors){

                //ignore the current sector if the player is in it right now
                if(sector.hasSave() && (state.isMenu() || sector != state.rules.sector)){
                    SaveMeta meta = sector.save.meta;

                    for(Entry<Item> entry : meta.exportRates){
                        //total is calculated by  items/sec (value) * turn duration in seconds
                        int total = (int)(entry.value * turnDuration / 60f);

                        exports[entry.key.id] += total;
                    }
                }
            }
        }

        return exports;
    }

    private void onTurn(){
        //TODO run waves on hostile sectors, damage them

        //calculate passive item generation
        int[] exports = getTotalExports();
        for(int i = 0; i < exports.length; i++){
            data.addItem(content.item(i), exports[i]);
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
        Core.settings.put("turn", turn);
        Core.settings.put("turntime", turnCounter);
        Core.settings.save();
    }

    private void load(){
        seconds = Core.settings.getLong("utime");
        turn = Core.settings.getInt("turn");
        turnCounter = Core.settings.getFloat("turntime");
    }

}
