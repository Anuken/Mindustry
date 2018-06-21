package io.anuke.mindustry.ai;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.IntArray;
import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.Squad;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class WaveSpawner {
    private static final int quadsize = 4;

    private Bits quadrants;
    private IntArray tmpArray = new IntArray();

    private Array<FlyerSpawn> flySpawns = new Array<>();
    private Array<GroundSpawn> groundSpawns = new Array<>();

    public WaveSpawner(){
        Events.on(WorldLoadEvent.class, this::reset);
    }

    public void spawnEnemies(){
        int spawned = Math.min(state.wave, 6);
        int groundGroups = 1 + state.wave / 20;
        int flyGroups = 1 + state.wave / 20;

        //add extra groups if necessary
        for (int i = 0; i < groundGroups - groundSpawns.size; i++) {
            GroundSpawn spawn = new GroundSpawn();
            findLocation(spawn);
            groundSpawns.add(spawn);
        }

        for (int i = 0; i < flyGroups - flySpawns.size; i++) {
            FlyerSpawn spawn = new FlyerSpawn();
            findLocation(spawn);
            flySpawns.add(spawn);
        }

        if(state.wave % 20 == 0){
            for(FlyerSpawn spawn : flySpawns) findLocation(spawn);
            for(GroundSpawn spawn : groundSpawns) findLocation(spawn);
        }

        for(GroundSpawn spawn : groundSpawns){
            checkQuadrant(spawn.x, spawn.y);
            if(!getQuad(spawn.x, spawn.y)){
                findLocation(spawn);
            }

            Squad squad = new Squad();

            for(int i = 0; i < spawned; i ++){
                BaseUnit unit = UnitTypes.scout.create(Team.red);
                unit.inventory.addAmmo(AmmoTypes.bulletIron);
                unit.setWave();
                unit.setSquad(squad);
                unit.set(spawn.x * quadsize * tilesize + quadsize * tilesize/2f + Mathf.range(quadsize*tilesize/3f),
                        spawn.y * quadsize * tilesize + quadsize * tilesize/2f + Mathf.range(quadsize*tilesize/3));
                unit.add();
            }
        }

        for(FlyerSpawn spawn : flySpawns){
            Squad squad = new Squad();
            float addition = 40f;
            float spread = addition / 1.5f;

            float baseX = world.width() *tilesize/2f + Mathf.sqrwavex(spawn.angle) * (world.width()/2f*tilesize + addition),
                    baseY = world.height() * tilesize/2f + Mathf.sqrwavey(spawn.angle) * (world.height()/2f*tilesize + addition);

            for(int i = 0; i < spawned; i ++){
                BaseUnit unit = UnitTypes.vtol.create(Team.red);
                unit.inventory.addAmmo(AmmoTypes.bulletIron);
                unit.setWave();
                unit.setSquad(squad);
                unit.set(baseX + Mathf.range(spread), baseY + Mathf.range(spread));
                unit.add();
            }
        }
    }

    public void checkAllQuadrants(){
        for(int x = 0; x < quadWidth(); x ++){
            for(int y = 0; y < quadHeight(); y ++){
                checkQuadrant(x, y);
            }
        }
    }
    
    private void checkQuadrant(int quadx, int quady){
        setQuad(quadx, quady, true);

        outer:
        for (int x = quadx * quadsize; x < world.width() && x < (quadx + 1)*quadsize; x++) {
            for (int y = quady * quadsize; y < world.height() && y < (quady + 1)*quadsize; y++) {
                Tile tile = world.tile(x, y);

                if(tile.solid() || world.pathfinder().getValueforTeam(Team.red, x, y) == Float.MAX_VALUE){
                    setQuad(quadx, quady, false);
                    break outer;
                }
            }
        }
    }

    private void reset(){
        flySpawns.clear();
        groundSpawns.clear();
        quadrants = new Bits(quadWidth() * quadHeight());
    }

    private boolean getQuad(int quadx, int quady){
        return quadrants.get(quadx + quady * quadWidth());
    }

    private void setQuad(int quadx, int quady, boolean valid){
        if(valid){
            quadrants.set(quadx + quady * quadWidth());
        }else{
            quadrants.clear(quadx + quady * quadWidth());
        }
    }

    private void findLocation(GroundSpawn spawn){
        spawn.x = -1;
        spawn.y = -1;

        int shellWidth = quadWidth()*2 + quadHeight() * 2 * 6;
        shellWidth = Math.min(quadWidth() * quadHeight() / 4, shellWidth);

        Mathf.traverseSpiral(quadWidth(), quadHeight(), Mathf.random(shellWidth), (x, y) -> {
            if(getQuad(x, y)){
                spawn.x = x;
                spawn.y = y;
                return true;
            }

            return false;
        });
    }

    private void findLocation(FlyerSpawn spawn){
        spawn.angle = Mathf.random(360f);
    }

    private int quadWidth(){
        return Mathf.ceil(world.width() / (float)quadsize);
    }

    private int quadHeight(){
        return Mathf.ceil(world.height() / (float)quadsize);
    }

    private class FlyerSpawn{
        //square angle
        float angle;

        FlyerSpawn(){

        }
    }

    private class GroundSpawn{
        //quadrant spawn coordinates
        int x, y;

        GroundSpawn(){

        }
    }
}
