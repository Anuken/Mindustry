package mindustry.ai;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class WaveSpawner{
    private static final float margin = 0f, coreMargin = tilesize * 2f, maxSteps = 30;

    private int tmpCount;
    private Seq<Tile> spawns = new Seq<>();
    private boolean spawning = false;
    private boolean any = false;
    private Tile firstSpawn = null;

    public WaveSpawner(){
        Events.on(WorldLoadEvent.class, e -> reset());
    }

    @Nullable
    public Tile getFirstSpawn(){
        firstSpawn = null;
        eachGroundSpawn((cx, cy) -> {
            firstSpawn = world.tile(cx, cy);
        });
        return firstSpawn;
    }

    public int countSpawns(){
        return spawns.size;
    }

    public Seq<Tile> getSpawns(){
        return spawns;
    }

    /** @return true if the player is near a ground spawn point. */
    public boolean playerNear(){
        return state.hasSpawns() && !player.dead() && spawns.contains(g -> Mathf.dst(g.x * tilesize, g.y * tilesize, player.x, player.y) < state.rules.dropZoneRadius && player.team() != state.rules.waveTeam);
    }

    public void spawnEnemies(){
        spawning = true;

        eachGroundSpawn(-1, (spawnX, spawnY, doShockwave) -> {
            if(doShockwave){
                doShockwave(spawnX, spawnY);
            }
        });

        for(SpawnGroup group : state.rules.spawns){
            if(group.type == null) continue;

            int spawned = group.getSpawned(state.wave - 1);

            if(group.type.flying){
                float spread = margin / 1.5f;

                eachFlyerSpawn(group.spawn, (spawnX, spawnY) -> {
                    for(int i = 0; i < spawned; i++){
                        Unit unit = group.createUnit(state.rules.waveTeam, state.wave - 1);
                        unit.set(spawnX + Mathf.range(spread), spawnY + Mathf.range(spread));
                        spawnEffect(unit);
                    }
                });
            }else{
                float spread = tilesize * 2;

                eachGroundSpawn(group.spawn, (spawnX, spawnY, doShockwave) -> {

                    for(int i = 0; i < spawned; i++){
                        Tmp.v1.rnd(spread);

                        Unit unit = group.createUnit(state.rules.waveTeam, state.wave - 1);
                        unit.set(spawnX + Tmp.v1.x, spawnY + Tmp.v1.y);
                        spawnEffect(unit);
                    }
                });
            }
        }

        Time.run(121f, () -> spawning = false);
    }

    public void doShockwave(float x, float y){
        Fx.spawnShockwave.at(x, y, state.rules.dropZoneRadius);
        Damage.damage(state.rules.waveTeam, x, y, state.rules.dropZoneRadius, 99999999f, true);
    }

    public void eachGroundSpawn(Intc2 cons){
        eachGroundSpawn(-1, (x, y, shock) -> cons.get(World.toTile(x), World.toTile(y)));
    }

    private void eachGroundSpawn(int filterPos, SpawnConsumer cons){
        if(state.hasSpawns()){
            for(Tile spawn : spawns){
                if(filterPos != -1 && filterPos != spawn.pos()) continue;

                cons.accept(spawn.worldx(), spawn.worldy(), true);
            }
        }

        if(state.rules.attackMode && state.teams.isActive(state.rules.waveTeam) && !state.teams.playerCores().isEmpty()){
            Building firstCore = state.teams.playerCores().first();
            for(Building core : state.rules.waveTeam.cores()){
                if(filterPos != -1 && filterPos != core.pos()) continue;

                Tmp.v1.set(firstCore).sub(core).limit(coreMargin + core.block.size * tilesize /2f * Mathf.sqrt2);

                boolean valid = false;
                int steps = 0;

                //keep moving forward until the max step amount is reached
                while(steps++ < maxSteps){
                    int tx = World.toTile(core.x + Tmp.v1.x), ty = World.toTile(core.y + Tmp.v1.y);
                    any = false;
                    Geometry.circle(tx, ty, world.width(), world.height(), 3, (x, y) -> {
                        if(world.solid(x, y)){
                            any = true;
                        }
                    });

                    //nothing is in the way, spawn it
                    if(!any){
                        valid = true;
                        break;
                    }else{
                        //make the vector longer
                        Tmp.v1.setLength(Tmp.v1.len() + tilesize*1.1f);
                    }
                }

                if(valid){
                    cons.accept(core.x + Tmp.v1.x, core.y + Tmp.v1.y, false);
                }
            }
        }
    }

    private void eachFlyerSpawn(int filterPos, Floatc2 cons){
        for(Tile tile : spawns){
            if(filterPos != -1 && filterPos != tile.pos()) continue;

            float angle = Angles.angle(world.width() / 2f, world.height() / 2f, tile.x, tile.y);
            float trns = Math.max(world.width(), world.height()) * Mathf.sqrt2 * tilesize;
            float spawnX = Mathf.clamp(world.width() * tilesize / 2f + Angles.trnsx(angle, trns), -margin, world.width() * tilesize + margin);
            float spawnY = Mathf.clamp(world.height() * tilesize / 2f + Angles.trnsy(angle, trns), -margin, world.height() * tilesize + margin);
            cons.get(spawnX, spawnY);
        }

        if(state.rules.attackMode && state.teams.isActive(state.rules.waveTeam)){
            for(Building core : state.rules.waveTeam.data().cores){
                if(filterPos != -1 && filterPos != core.pos()) continue;

                cons.get(core.x, core.y);
            }
        }
    }

    public int countGroundSpawns(){
        tmpCount = 0;
        eachGroundSpawn((x, y) -> tmpCount ++);
        return tmpCount;
    }

    public int countFlyerSpawns(){
        tmpCount = 0;
        eachFlyerSpawn(-1, (x, y) -> tmpCount ++);
        return tmpCount;
    }

    public boolean isSpawning(){
        return spawning && !net.client();
    }

    public void reset(){
        spawning = false;
        spawns.clear();

        for(Tile tile : world.tiles){
            if(tile.overlay() == Blocks.spawn){
                spawns.add(tile);
            }
        }
    }

    /** Applies the standard wave spawn effects to a unit - invincibility, unmoving. */
    public void spawnEffect(Unit unit){
        spawnEffect(unit, unit.angleTo(world.width()/2f * tilesize, world.height()/2f * tilesize));
    }

    /** Applies the standard wave spawn effects to a unit - invincibility, unmoving. */
    public void spawnEffect(Unit unit, float rotation){
        unit.rotation = rotation;
        unit.apply(StatusEffects.unmoving, 30f);
        unit.apply(StatusEffects.invincible, 60f);
        unit.add();
        unit.unloaded();

        Events.fire(new UnitSpawnEvent(unit));
        Call.spawnEffect(unit.x, unit.y, unit.rotation, unit.type);
    }

    private interface SpawnConsumer{
        void accept(float x, float y, boolean shockwave);
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void spawnEffect(float x, float y, float rotation, UnitType u){

        Fx.unitSpawn.at(x, y, rotation, u);

        Time.run(30f, () -> Fx.spawn.at(x, y));
    }
}
