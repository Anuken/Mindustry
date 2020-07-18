package mindustry.ai;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class WaveSpawner{
    private static final float margin = 40f, coreMargin = tilesize * 3; //how far away from the edge flying units spawn

    private Seq<Tile> spawns = new Seq<>();
    private boolean spawning = false;

    public WaveSpawner(){
        Events.on(WorldLoadEvent.class, e -> reset());
    }

    public int countSpawns(){
        return spawns.size;
    }

    public Seq<Tile> getSpawns(){
        return spawns;
    }

    /** @return true if the player is near a ground spawn point. */
    public boolean playerNear(){
        return !player.dead() && spawns.contains(g -> Mathf.dst(g.x * tilesize, g.y * tilesize, player.x, player.y) < state.rules.dropZoneRadius && player.team() != state.rules.waveTeam);
    }

    public void spawnEnemies(){
        spawning = true;

        for(SpawnGroup group : state.rules.spawns){
            int spawned = group.getUnitsSpawned(state.wave - 1);

            if(group.type.flying){
                float spread = margin / 1.5f;

                eachFlyerSpawn((spawnX, spawnY) -> {
                    for(int i = 0; i < spawned; i++){
                        Unit unit = group.createUnit(state.rules.waveTeam, state.wave - 1);
                        unit.set(spawnX + Mathf.range(spread), spawnY + Mathf.range(spread));
                        unit.add();
                    }
                });
            }else{
                float spread = tilesize * 2;

                eachGroundSpawn((spawnX, spawnY, doShockwave) -> {

                    for(int i = 0; i < spawned; i++){
                        Tmp.v1.rnd(spread);

                        Unit unit = group.createUnit(state.rules.waveTeam, state.wave - 1);
                        unit.set(spawnX + Tmp.v1.x, spawnY + Tmp.v1.y);
                        Time.run(Math.min(i * 5, 60 * 2), () -> spawnEffect(unit));
                    }
                });
            }
        }

        eachGroundSpawn((spawnX, spawnY, doShockwave) -> {
            if(doShockwave){
                Time.run(20f, () -> Fx.spawnShockwave.at(spawnX, spawnY, state.rules.dropZoneRadius));
                Time.run(40f, () -> Damage.damage(state.rules.waveTeam, spawnX, spawnY, state.rules.dropZoneRadius, 99999999f, true));
            }
        });

        Time.runTask(121f, () -> spawning = false);
    }

    private void eachGroundSpawn(SpawnConsumer cons){
        for(Tile spawn : spawns){
            cons.accept(spawn.worldx(), spawn.worldy(), true);
        }

        if(state.rules.attackMode && state.teams.isActive(state.rules.waveTeam) && !state.teams.playerCores().isEmpty()){
            Building firstCore = state.teams.playerCores().first();
            for(Building core : state.rules.waveTeam.cores()){
                Tmp.v1.set(firstCore).sub(core).limit(coreMargin + core.block().size * tilesize);
                cons.accept(core.x + Tmp.v1.x, core.y + Tmp.v1.y, false);
            }
        }
    }

    private void eachFlyerSpawn(Floatc2 cons){
        for(Tile tile : spawns){
            float angle = Angles.angle(tile.x, tile.y, world.width() / 2, world.height() / 2);

            float trns = Math.max(world.width(), world.height()) * Mathf.sqrt2 * tilesize;
            float spawnX = Mathf.clamp(world.width() * tilesize / 2f + Angles.trnsx(angle, trns), -margin, world.width() * tilesize + margin);
            float spawnY = Mathf.clamp(world.height() * tilesize / 2f + Angles.trnsy(angle, trns), -margin, world.height() * tilesize + margin);
            cons.get(spawnX, spawnY);
        }

        if(state.rules.attackMode && state.teams.isActive(state.rules.waveTeam)){
            for(Building core : state.teams.get(state.rules.waveTeam).cores){
                cons.get(core.x, core.y);
            }
        }
    }

    public boolean isSpawning(){
        return spawning && !net.client();
    }

    private void reset(){
        spawns.clear();

        for(Tile tile : world.tiles){
            if(tile.overlay() == Blocks.spawn){
                spawns.add(tile);
            }
        }
    }

    private void spawnEffect(Unit unit){
        Fx.unitSpawn.at(unit.x(), unit.y(), 0f, unit);
        Time.run(30f, () -> {
            unit.add();
            Fx.spawn.at(unit);
        });
    }

    private interface SpawnConsumer{
        void accept(float x, float y, boolean shockwave);
    }
}
