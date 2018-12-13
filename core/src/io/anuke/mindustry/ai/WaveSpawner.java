package io.anuke.mindustry.ai;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.Squad;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Waves;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.util.GridBits;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Structs;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class WaveSpawner{
    private static final int quadsize = 6;

    private GridBits quadrants;

    private Array<SpawnGroup> groups;
    private boolean dynamicSpawn;

    private Array<FlyerSpawn> flySpawns = new Array<>();
    private Array<GroundSpawn> groundSpawns = new Array<>();

    public WaveSpawner(){
        Events.on(WorldLoadEvent.class, this::reset);
    }

    public void write(DataOutput output) throws IOException{
        output.writeShort(flySpawns.size);
        for(FlyerSpawn spawn : flySpawns){
            output.writeFloat(spawn.angle);
        }

        output.writeShort(groundSpawns.size);
        for(GroundSpawn spawn : groundSpawns){
            output.writeShort((short) spawn.x);
            output.writeShort((short) spawn.y);
        }
    }

    public void read(DataInput input) throws IOException{
        short flya = input.readShort();

        for(int i = 0; i < flya; i++){
            FlyerSpawn spawn = new FlyerSpawn();
            spawn.angle = input.readFloat();
            flySpawns.add(spawn);
        }

        short grounda = input.readShort();
        for(int i = 0; i < grounda; i++){
            GroundSpawn spawn = new GroundSpawn();
            spawn.x = input.readShort();
            spawn.y = input.readShort();
            groundSpawns.add(spawn);
        }
    }

    public void spawnEnemies(){
        int flyGroups = 0;
        int groundGroups = 0;

        //count total subgroups spawned by flying/group types
        for(SpawnGroup group : groups){
            int amount = group.getGroupsSpawned(state.wave);
            if(group.type.isFlying){
                flyGroups += amount;
            }else if(dynamicSpawn){
                groundGroups += amount;
            }
        }

        int addGround = groundGroups - groundSpawns.size, addFly = flyGroups - flySpawns.size;

        //add extra groups if the total exceeds it
        if(dynamicSpawn){
            for(int i = 0; i < addGround; i++){
                GroundSpawn spawn = new GroundSpawn();
                findLocation(spawn);
                groundSpawns.add(spawn);
            }
        }

        for(int i = 0; i < addFly; i++){
            FlyerSpawn spawn = new FlyerSpawn();
            findLocation(spawn);
            flySpawns.add(spawn);
        }

        //store index of last used fly/ground spawn locations
        int flyCount = 0, groundCount = 0;

        for(SpawnGroup group : groups){
            int groups = group.getGroupsSpawned(state.wave);
            int spawned = group.getUnitsSpawned(state.wave);

            for(int i = 0; i < groups; i++){
                Squad squad = new Squad();
                float spawnX, spawnY;
                float spread;

                if(!group.type.isFlying && groundCount >= groundSpawns.size) continue;

                if(group.type.isFlying){
                    FlyerSpawn spawn = flySpawns.get(flyCount);

                    float margin = 40f; //how far away from the edge flying units spawn
                    spawnX = world.width() * tilesize / 2f + Mathf.sqrwavex(spawn.angle) * (world.width() / 2f * tilesize + margin);
                    spawnY = world.height() * tilesize / 2f + Mathf.sqrwavey(spawn.angle) * (world.height() / 2f * tilesize + margin);
                    spread = margin / 1.5f;

                    flyCount++;
                }else{ //make sure it works for non-dynamic spawns
                    GroundSpawn spawn = groundSpawns.get(groundCount);

                    if(dynamicSpawn){
                        checkQuadrant(spawn.x, spawn.y);
                        if(!getQuad(spawn.x, spawn.y)){
                            findLocation(spawn);
                        }
                    }

                    spawnX = spawn.x * quadsize * tilesize + quadsize * tilesize / 2f;
                    spawnY = spawn.y * quadsize * tilesize + quadsize * tilesize / 2f;
                    spread = quadsize * tilesize / 3f;

                    groundCount++;
                }

                for(int j = 0; j < spawned; j++){
                    BaseUnit unit = group.createUnit(Team.red);
                    unit.setWave();
                    unit.setSquad(squad);
                    unit.set(spawnX + Mathf.range(spread), spawnY + Mathf.range(spread));
                    unit.add();
                }
            }
        }
    }

    public void checkAllQuadrants(){
        for(int x = 0; x < quadWidth(); x++){
            for(int y = 0; y < quadHeight(); y++){
                checkQuadrant(x, y);
            }
        }
    }

    private void checkQuadrant(int quadx, int quady){
        setQuad(quadx, quady, true);

        outer:
        for(int x = quadx * quadsize; x < world.width() && x < (quadx + 1) * quadsize; x++){
            for(int y = quady * quadsize; y < world.height() && y < (quady + 1) * quadsize; y++){
                Tile tile = world.tile(x, y);

                if(tile == null || tile.solid() || tile.getTeam() == defaultTeam || world.pathfinder.getValueforTeam(Team.red, x, y) == Float.MAX_VALUE || tile.floor().isLiquid){
                    setQuad(quadx, quady, false);
                    break outer;
                }
            }
        }
    }

    private void reset(WorldLoadEvent event){
        dynamicSpawn = false;
        flySpawns.clear();
        groundSpawns.clear();
        quadrants = new GridBits(quadWidth(), quadHeight());

        if(world.getSector() == null){
            groups = Waves.getSpawns();
        }else{
            groups = world.getSector().spawns;
        }

        dynamicSpawn = true;

        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                if(world.tile(x, y).block() == Blocks.spawn){
                    dynamicSpawn = false;
                    GroundSpawn spawn = new GroundSpawn();
                    spawn.x = x/quadsize;
                    spawn.y = y/quadsize;
                    groundSpawns.add(spawn);
                }
            }
        }
    }

    private boolean getQuad(int quadx, int quady){
        return quadrants.get(quadx, quady);
    }

    private void setQuad(int quadx, int quady, boolean valid){
        if(quadrants == null){
            quadrants = new GridBits(quadWidth(), quadHeight());
        }

        if(!Structs.inBounds(quadx, quady, quadWidth(), quadHeight())){
            return;
        }

        quadrants.set(quadx, quady, valid);
    }

    //TODO instead of randomly scattering locations around the map, find spawns close to each other
    private void findLocation(GroundSpawn spawn){
        spawn.x = Mathf.random(quadWidth()-1);
        spawn.y = Mathf.random(quadHeight()-1);

        int shellWidth = quadWidth() * 2 + quadHeight() * 2 * 6;
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

    //TODO instead of randomly scattering locations around the map, find spawns close to each other
    private void findLocation(FlyerSpawn spawn){
        spawn.angle = Mathf.random(360f);
    }

    private int quadWidth(){
        return Mathf.ceil(world.width() / (float) quadsize);
    }

    private int quadHeight(){
        return Mathf.ceil(world.height() / (float) quadsize);
    }

    private class FlyerSpawn{
        //square angle
        float angle;
    }

    private class GroundSpawn{
        //quadrant spawn coordinates
        int x, y;
    }
}
