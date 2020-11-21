package mindustry.maps;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.Turret.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

public class SectorDamage{
    public static final int maxRetWave = 30, maxWavesSimulated = 50;

    //direct damage is for testing only
    private static final boolean direct = false, rubble = true;

    /** @return calculated capture progress of the enemy */
    public static float getDamage(SectorInfo info){
        return getDamage(info, info.wavesPassed);
    }

    /** @return calculated capture progress of the enemy */
    public static float getDamage(SectorInfo info, int wavesPassed){
        return getDamage(info, wavesPassed, false);
    }

    /** @return maximum waves survived, up to maxRetWave. */
    public static int getWavesSurvived(SectorInfo info){
        return (int)getDamage(info, maxRetWave, true);
    }

    /** @return calculated capture progress of the enemy if retWave if false, otherwise return the maximum waves survived as int.
     * if it survives all the waves, returns maxRetWave. */
    public static float getDamage(SectorInfo info, int wavesPassed, boolean retWave){
        float health = info.sumHealth;
        int wave = info.wave;
        float waveSpace = info.waveSpacing;

        //this approach is O(n), it simulates every wave passing.
        //other approaches can assume all the waves come as one, but that's not as fair.
        if(wavesPassed > 0){
            int waveBegin = wave;
            int waveEnd = wave + wavesPassed;

            //do not simulate every single wave if there's too many
            if(wavesPassed > maxWavesSimulated && !retWave){
                waveBegin = waveEnd - maxWavesSimulated;
            }

            for(int i = waveBegin; i <= waveEnd; i++){

                float efficiency = health / info.sumHealth;
                float dps = info.sumDps * efficiency;
                float rps = info.sumRps * efficiency;

                float enemyDps = info.waveDpsBase + info.waveDpsSlope * (i);
                float enemyHealth = info.waveHealthBase + info.waveHealthSlope * (i);

                //happens due to certain regressions
                if(enemyHealth < 0 || enemyDps < 0) continue;

                //calculate time to destroy both sides
                float timeDestroyEnemy = dps <= 0.0001f ? Float.POSITIVE_INFINITY : enemyHealth / dps; //if dps == 0, this is infinity
                float timeDestroyBase = health / (enemyDps - rps); //if regen > enemyDps this is negative

                //regenerating faster than the base can be damaged
                if(timeDestroyBase < 0) continue;

                //sector is lost, enemy took too long.
                if(timeDestroyEnemy > timeDestroyBase){
                    health = 0f;
                    //return current wave if simulating
                    if(retWave) return i - waveBegin;
                    break;
                }

                //otherwise, the enemy shoots for timeDestroyEnemy seconds, so calculate damage taken
                float damageTaken = timeDestroyEnemy * (enemyDps - rps);

                //damage the base.
                health -= damageTaken;

                //regen health after wave.
                health = Math.min(health + rps / 60f * waveSpace, info.sumHealth);
            }
        }

        //survived everything
        if(retWave){
            return maxRetWave;
        }

        return 1f - Mathf.clamp(health / info.sumHealth);
    }

    /** Applies wave damage based on sector parameters. */
    public static void applyCalculatedDamage(){
        //calculate base damage fraction
        float damage = getDamage(state.rules.sector.info);

        //scaled damage has a power component to make it seem a little more realistic (as systems fail, enemy capturing gets easier and easier)
        float scaled = Mathf.pow(damage, 1.6f);

        //apply damage to units
        float unitDamage = damage * state.rules.sector.info.sumHealth;
        Tile spawn = spawner.getFirstSpawn();

        //damage only units near the spawn point
        if(spawn != null){
            Seq<Unit> allies = new Seq<>();
            for(Unit ally : Groups.unit){
                if(ally.team == state.rules.defaultTeam && ally.within(spawn, state.rules.dropZoneRadius * 2.5f)){
                    allies.add(ally);
                }
            }

            allies.sort(u -> u.dst2(spawn));

            //damage units one by one, not uniformly
            for(var u : allies){
                if(u.health < unitDamage){
                    u.remove();
                    unitDamage -= u.health;
                }else{
                    u.health -= unitDamage;
                    break;
                }
            }
        }

        if(state.rules.sector.info.wavesPassed > 0){
            //simply remove each block in the spawner range if a wave passed
            for(Tile spawner : spawner.getSpawns()){
                spawner.circle((int)(state.rules.dropZoneRadius / tilesize), tile -> {
                    if(tile.team() == state.rules.defaultTeam){
                        if(rubble && tile.floor().hasSurface() && Mathf.chance(0.4)){
                            Effect.rubble(tile.build.x, tile.build.y, tile.block().size);
                        }

                        tile.remove();
                    }
                });
            }
        }

        //finally apply scaled damage
        apply(scaled);
    }

    /** Calculates damage simulation parameters before a game is saved. */
    public static void writeParameters(SectorInfo info){
        Building core = state.rules.defaultTeam.core();
        Seq<Tile> spawns = new Seq<>();
        spawner.eachGroundSpawn((x, y) -> spawns.add(world.tile(x, y)));

        if(spawns.isEmpty() && state.rules.waveTeam.core() != null){
            spawns.add(state.rules.waveTeam.core().tile);
        }

        if(core == null || spawns.isEmpty()) return;

        Tile start = spawns.first();

        Time.mark();
        var field = pathfinder.getField(state.rules.waveTeam, Pathfinder.costGround, Pathfinder.fieldCore);
        Seq<Tile> path = new Seq<>();
        boolean found = false;

        if(field != null && field.weights != null){
            int[][] weights = field.weights;
            int count = 0;
            Tile current = start;
            while(count < world.width() * world.height()){
                int minCost = Integer.MAX_VALUE;
                int cx = current.x, cy = current.y;
                for(Point2 p : Geometry.d4){
                    int nx = cx + p.x, ny = cy + p.y;

                    Tile other = world.tile(nx, ny);
                    if(other != null && weights[nx][ny] < minCost && weights[nx][ny] != -1){
                       minCost = weights[nx][ny];
                       current = other;
                    }
                }

                path.add(current);

                if(current.build == core){
                    found = true;
                    break;
                }

                count ++;
            }
        }

        if(!found){
            path = Astar.pathfind(start, core.tile, SectorDamage::cost, t -> !(t.block().isStatic() && t.solid()));
        }

        //create sparse tile array for fast range query
        int sparseSkip = 5, sparseSkip2 = 3;
        //TODO if this is slow, use a quadtree
        Seq<Tile> sparse = new Seq<>(path.size / sparseSkip + 1);
        Seq<Tile> sparse2 = new Seq<>(path.size / sparseSkip2 + 1);

        for(int i = 0; i < path.size; i++){
            if(i % sparseSkip == 0){
                sparse.add(path.get(i));
            }
            if(i % sparseSkip2 == 0){
                sparse2.add(path.get(i));
            }
        }

        //regen is in health per second
        //dps is per second
        float sumHealth = 0f, sumRps = 0f, sumDps = 0f;
        float totalPathBuild = 0;

        //first, calculate the total health of blocks in the path

        //radius around the path that gets counted
        int radius = 8;
        IntSet counted = new IntSet();

        for(Tile t : sparse2){

            //radius is square.
            for(int dx = -radius; dx <= radius; dx++){
                for(int dy = -radius; dy <= radius; dy++){
                    int wx = dx + t.x, wy = dy + t.y;
                    if(wx >= 0 && wy >= 0 && wx < world.width() && wy < world.height()){
                        Tile tile = world.rawTile(wx, wy);

                        if(tile.build != null && tile.team() == state.rules.defaultTeam && counted.add(tile.pos())){
                            //health is divided by block size, because multiblocks are counted multiple times.
                            sumHealth += tile.build.health / tile.block().size;
                            totalPathBuild += 1f / tile.block().size;
                        }
                    }
                }
            }
        }

        float avgHealth = totalPathBuild <= 1 ? sumHealth : sumHealth / totalPathBuild;

        //block dps + regen + extra health/shields
        for(Building build : Groups.build){
            float e = build.efficiency();
            if(e > 0.08f){
                if(build.team == state.rules.defaultTeam && build instanceof Ranged ranged && sparse.contains(t -> t.within(build, ranged.range() + radius*tilesize))){
                    if(build.block instanceof Turret t && build instanceof TurretBuild b && b.hasAmmo()){
                        sumDps += t.shots / t.reloadTime * 60f * b.peekAmmo().estimateDPS() * e;
                    }

                    if(build.block instanceof MendProjector m){
                        sumRps += m.healPercent / m.reload * avgHealth * 60f / 100f * e;
                    }

                    if(build.block instanceof ForceProjector f){
                        sumHealth += f.shieldHealth * e;
                        sumRps += 1f * e;
                    }
                }
            }
        }

        float curEnemyHealth = 0f, curEnemyDps = 0f;

        //unit regen + health + dps
        for(Unit unit : Groups.unit){
            //skip player
            if(unit.isPlayer()) continue;

            if(unit.team == state.rules.defaultTeam){
                //scale health based on armor - yes, this is inaccurate, but better than nothing
                float healthMult = 1f + Mathf.clamp(unit.armor / 20f);

                sumHealth += unit.health*healthMult + unit.shield;
                sumDps += unit.type.dpsEstimate;
                if(unit.abilities.find(a -> a instanceof RepairFieldAbility) instanceof RepairFieldAbility h){
                    sumRps += h.amount / h.reload * 60f;
                }
            }else{
                curEnemyDps += unit.type.dpsEstimate;
                curEnemyHealth += unit.health;
            }
        }

        //calculate DPS and health for the next few waves and store in list
        var reg = new LinearRegression();
        Seq<Vec2> waveDps = new Seq<>(), waveHealth = new Seq<>();

        for(int wave = state.wave; wave < state.wave + 10; wave ++){
            float sumWaveDps = 0f, sumWaveHealth = 0f;

            //first wave has to take into account current dps
            if(wave == state.wave){
                sumWaveDps += curEnemyDps;
                sumWaveHealth += curEnemyHealth;
            }

            for(SpawnGroup group : state.rules.spawns){
                float healthMult = 1f + Mathf.clamp(group.type.armor / 20f);
                StatusEffect effect = (group.effect == null ? StatusEffects.none : group.effect);
                int spawned = group.getSpawned(wave);
                if(spawned <= 0) continue;
                sumWaveHealth += spawned * (group.getShield(wave) + group.type.health * effect.healthMultiplier * healthMult);
                sumWaveDps += spawned * group.type.dpsEstimate * effect.damageMultiplier;
            }
            waveDps.add(new Vec2(wave, sumWaveDps));
            waveHealth.add(new Vec2(wave, sumWaveHealth));
        }

        //calculate linear regression of the wave data and store it
        reg.calculate(waveHealth);
        info.waveHealthBase = reg.intercept;
        info.waveHealthSlope = reg.slope;

        reg.calculate(waveDps);
        info.waveDpsBase = reg.intercept;
        info.waveDpsSlope = reg.slope;

        //enemy units like to aim for a lot of non-essential things, so increase resulting health slightly
        info.sumHealth = sumHealth * 1.3f;
        //players tend to have longer range units/turrets, so assume DPS is higher
        info.sumDps = sumDps * 1.3f;
        info.sumRps = sumRps;

        info.wavesSurvived = getWavesSurvived(info);
    }

    public static void apply(float fraction){
        Tiles tiles = world.tiles;

        Queue<Tile> frontier = new Queue<>();
        float[][] values = new float[tiles.width][tiles.height];
        float damage = fraction*80; //arbitrary damage value

        //phase one: find all spawnpoints
        for(Tile tile : tiles){
            if((tile.block() instanceof CoreBlock && tile.team() == state.rules.waveTeam) || tile.overlay() == Blocks.spawn){
                frontier.add(tile);
                values[tile.x][tile.y] = damage;
            }
        }

        Building core = state.rules.defaultTeam.core();
        if(core != null && !frontier.isEmpty()){
            for(Tile spawner : frontier){
                //find path from spawn to core
                Seq<Tile> path = Astar.pathfind(spawner, core.tile, SectorDamage::cost, t -> !(t.block().isStatic() && t.solid()));
                Seq<Building> removal = new Seq<>();

                int radius = 3;

                //only penetrate a certain % by health, not by distance
                float totalHealth = damage >= 1f ? 1f : path.sumf(t -> {
                    float s = 0;
                    for(int dx = -radius; dx <= radius; dx++){
                        for(int dy = -radius; dy <= radius; dy++){
                            int wx = dx + t.x, wy = dy + t.y;
                            if(wx >= 0 && wy >= 0 && wx < world.width() && wy < world.height() && Mathf.within(dx, dy, radius)){
                                Tile other = world.rawTile(wx, wy);
                                s += other.team() == state.rules.defaultTeam ? other.build.health / other.block().size : 0f;
                            }
                        }
                    }
                    return s;
                });
                float targetHealth = totalHealth * fraction;
                float healthCount = 0;

                out:
                for(int i = 0; i < path.size && (healthCount < targetHealth || damage >= 1f); i++){
                    Tile t = path.get(i);

                    for(int dx = -radius; dx <= radius; dx++){
                        for(int dy = -radius; dy <= radius; dy++){
                            int wx = dx + t.x, wy = dy + t.y;
                            if(wx >= 0 && wy >= 0 && wx < world.width() && wy < world.height() && Mathf.within(dx, dy, radius)){
                                Tile other = world.rawTile(wx, wy);

                                //just remove all the buildings in the way - as long as they're not cores
                                if(other.build != null && other.team() == state.rules.defaultTeam && !(other.block() instanceof CoreBlock)){
                                    if(rubble && !other.floor().solid && !other.floor().isLiquid && Mathf.chance(0.4)){
                                        Effect.rubble(other.build.x, other.build.y, other.block().size);
                                    }

                                    //since the whole block is removed, count the whole health
                                    healthCount += other.build.health;

                                    removal.add(other.build);

                                    if(healthCount >= targetHealth && damage < 0.999f){
                                        break out;
                                    }
                                }
                            }
                        }
                    }
                }

                for(Building r : removal){
                    if(r.tile.build == r){
                        r.addPlan(false);
                        r.tile.remove();
                    }
                }
            }
        }

        //kill every core if damage is maximum
        if(fraction >= 1){
            for(Building c : state.rules.defaultTeam.cores().copy()){
                c.tile.remove();
            }
        }

        float falloff = (damage) / (Math.max(tiles.width, tiles.height) * Mathf.sqrt2);
        int peak = 0;

        if(damage > 0.1f){
            //phase two: propagate the damage
            while(!frontier.isEmpty()){
                peak = Math.max(peak, frontier.size);
                Tile tile = frontier.removeFirst();
                float currDamage = values[tile.x][tile.y] - falloff;

                for(int i = 0; i < 4; i++){
                    int cx = tile.x + Geometry.d4x[i], cy = tile.y + Geometry.d4y[i];

                    //propagate to new tiles
                    if(tiles.in(cx, cy) && values[cx][cy] < currDamage){
                        Tile other = tiles.getn(cx, cy);
                        float resultDamage = currDamage;

                        //damage the tile if it's not friendly
                        if(other.build != null && other.team() != state.rules.waveTeam){
                            resultDamage -= other.build.health();

                            if(direct){
                                other.build.damage(currDamage);
                            }else{ //indirect damage happens at game load time
                                other.build.health -= currDamage;
                                //don't kill the core!
                                if(other.block() instanceof CoreBlock) other.build.health = Math.max(other.build.health, 1f);

                                //remove the block when destroyed
                                if(other.build.health < 0){
                                    //rubble
                                    if(rubble && !other.floor().solid && !other.floor().isLiquid && Mathf.chance(0.4)){
                                        Effect.rubble(other.build.x, other.build.y, other.block().size);
                                    }

                                    other.build.addPlan(false);
                                    other.remove();
                                }
                            }

                        }else if(other.solid() && !other.synthetic()){ //skip damage propagation through solid blocks
                            continue;
                        }

                        if(resultDamage > 0 && values[cx][cy] < resultDamage){
                            frontier.addLast(other);
                            values[cx][cy] = resultDamage;
                        }
                    }
                }
            }
        }

    }

    static float cost(Tile tile){
        return 1f +
            (tile.block().isStatic() && tile.solid() ? 200f : 0f) +
            (tile.build != null ? tile.build.health / 40f : 0f) +
            (tile.floor().isLiquid ? 10f : 0f);
    }
}
