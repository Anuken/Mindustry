package io.anuke.mindustry.ai;

import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.function.PositionConsumer;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.arc.util.Tmp;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.BaseUnit;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.*;

public class WaveSpawner{
    private static final float margin = 40f, coreMargin = tilesize * 3; //how far away from the edge flying units spawn

    private Array<FlyerSpawn> flySpawns = new Array<>();
    private Array<Tile> groundSpawns = new Array<>();
    private boolean spawning = false;

    public WaveSpawner(){
        Events.on(WorldLoadEvent.class, e -> reset());
    }

    public int countSpawns(){
        return groundSpawns.size;
    }

    public Array<Tile> getGroundSpawns(){
        return groundSpawns;
    }

    /** @return true if the player is near a ground spawn point. */
    public boolean playerNear(){
        return groundSpawns.contains(g -> Mathf.dst(g.x * tilesize, g.y * tilesize, player.x, player.y) < state.rules.dropZoneRadius);
    }

    public void spawnEnemies(){
        spawning = true;

        for(SpawnGroup group : state.rules.spawns){
            int spawned = group.getUnitsSpawned(state.wave - 1);

            if(group.type.flying){
                float spread = margin / 1.5f;

                eachFlyerSpawn((spawnX, spawnY) -> {
                    for(int i = 0; i < spawned; i++){
                        BaseUnit unit = group.createUnit(waveTeam);
                        unit.set(spawnX + Mathf.range(spread), spawnY + Mathf.range(spread));
                        unit.add();
                    }
                });
            }else{
                float spread = tilesize * 2;

                eachGroundSpawn((spawnX, spawnY, doShockwave) -> {

                    for(int i = 0; i < spawned; i++){
                        Tmp.v1.rnd(spread);

                        BaseUnit unit = group.createUnit(waveTeam);
                        unit.set(spawnX + Tmp.v1.x, spawnY + Tmp.v1.y);

                        Time.run(Math.min(i * 5, 60 * 2), () -> spawnEffect(unit));
                    }
                });
            }
        }

        eachGroundSpawn((spawnX, spawnY, doShockwave) -> {
            if(doShockwave){
                Time.run(20f, () -> Effects.effect(Fx.spawnShockwave, spawnX, spawnY, state.rules.dropZoneRadius));
                Time.run(40f, () -> Damage.damage(waveTeam, spawnX, spawnY, state.rules.dropZoneRadius, 99999999f, true));
            }
        });

        Time.runTask(121f, () -> spawning = false);
    }

    private void eachGroundSpawn(SpawnConsumer cons){
        for(Tile spawn : groundSpawns){
            cons.accept(spawn.worldx(), spawn.worldy(), true);
        }

        if(state.rules.attackMode && state.teams.isActive(waveTeam) && !state.teams.get(defaultTeam).cores.isEmpty()){
            Tile firstCore = state.teams.get(defaultTeam).cores.first();
            for(Tile core : state.teams.get(waveTeam).cores){
                Tmp.v1.set(firstCore).sub(core.worldx(), core.worldy()).limit(coreMargin + core.block().size*tilesize);
                cons.accept(core.worldx() + Tmp.v1.x, core.worldy() + Tmp.v1.y, false);
            }
        }
    }

    private void eachFlyerSpawn(PositionConsumer cons){
        for(FlyerSpawn spawn : flySpawns){
            float trns = (world.width() + world.height()) * tilesize;
            float spawnX = Mathf.clamp(world.width() * tilesize / 2f + Angles.trnsx(spawn.angle, trns), -margin, world.width() * tilesize + margin);
            float spawnY = Mathf.clamp(world.height() * tilesize / 2f + Angles.trnsy(spawn.angle, trns), -margin, world.height() * tilesize + margin);
            cons.accept(spawnX, spawnY);
        }

        if(state.rules.attackMode && state.teams.isActive(waveTeam)){
            for(Tile core : state.teams.get(waveTeam).cores){
                cons.accept(core.worldx(), core.worldy());
            }
        }
    }

    public boolean isSpawning(){
        return spawning && !net.client();
    }

    private void reset(){

        flySpawns.clear();
        groundSpawns.clear();

        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){

                if(world.tile(x, y).overlay() == Blocks.spawn){
                    addSpawns(x, y);
                }
            }
        }
    }

    private void addSpawns(int x, int y){
        groundSpawns.add(world.tile(x, y));

        FlyerSpawn fspawn = new FlyerSpawn();
        fspawn.angle = Angles.angle(world.width() / 2f, world.height() / 2f, x, y);
        flySpawns.add(fspawn);
    }

    private void spawnEffect(BaseUnit unit){
        Effects.effect(Fx.unitSpawn, unit.x, unit.y, 0f, unit);
        Time.run(30f, () -> {
            unit.add();
            Effects.effect(Fx.spawn, unit);
        });
    }

    private interface SpawnConsumer{
        void accept(float x, float y, boolean shockwave);
    }

    private class FlyerSpawn{
        float angle;
    }
}
