package io.anuke.mindustry.ai;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.Squad;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.WaveCreator;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class WaveSpawner{
    private static final int quadsize = 4;

    private Bits quadrants;

    private Array<SpawnGroup> groups;

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
            }else{
                groundGroups += amount;
            }
        }

        int addGround = groundGroups - groundSpawns.size, addFly = flyGroups - flySpawns.size;

        //add extra groups if the total exceeds it
        for(int i = 0; i < addGround; i++){
            GroundSpawn spawn = new GroundSpawn();
            findLocation(spawn);
            groundSpawns.add(spawn);
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

                if(group.type.isFlying){
                    FlyerSpawn spawn = flySpawns.get(flyCount);
                    //TODO verify flyer spawn

                    float margin = 40f; //how far away from the edge flying units spawn
                    spawnX = world.width() * tilesize / 2f + Mathf.sqrwavex(spawn.angle) * (world.width() / 2f * tilesize + margin);
                    spawnY = world.height() * tilesize / 2f + Mathf.sqrwavey(spawn.angle) * (world.height() / 2f * tilesize + margin);
                    spread = margin / 1.5f;

                    flyCount++;
                }else{
                    GroundSpawn spawn = groundSpawns.get(groundCount);
                    checkQuadrant(spawn.x, spawn.y);
                    if(!getQuad(spawn.x, spawn.y)){
                        findLocation(spawn);
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

                if(tile == null || tile.solid() || world.pathfinder().getValueforTeam(Team.red, x, y) == Float.MAX_VALUE){
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

        if(groups == null){
            groups = WaveCreator.getSpawns();
        }
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

    //TODO instead of randomly scattering locations around the map, find spawns close to each other
    private void findLocation(GroundSpawn spawn){
        spawn.x = -1;
        spawn.y = -1;

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
