package mindustry.maps;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
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
    public static final int maxRetWave = 110, maxWavesSimulated = 111;

    //direct damage is for testing only
    private static final boolean rubble = true;

    /** @return calculated capture progress of the enemy */
    public static float getDamage(Sector sector){
        return getDamage(sector, sector.info.wavesPassed);
    }

    /** @return calculated capture progress of the enemy */
    public static float getDamage(Sector sector, int wavesPassed){
        return getDamage(sector, wavesPassed, false);
    }

    /** @return maximum waves survived, up to maxRetWave. */
    public static int getWavesSurvived(Sector sector){
        return (int)getDamage(sector, maxRetWave, true);
    }

    /** @return calculated capture progress of the enemy if retWave is false, otherwise return the maximum waves survived as int.
     * if it survives all the waves, returns maxRetWave. */
    public static float getDamage(Sector sector, int wavesPassed, boolean retWave){
        var info = sector.info;
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

            int groundSpawns = Math.max(spawner.countFlyerSpawns(), 1), airSpawns = Math.max(spawner.countGroundSpawns(), 1);

            for(int i = waveBegin; i <= waveEnd; i++){
                float enemyDps = 0f, enemyHealth = 0f;

                if(sector.save != null || sector.isBeingPlayed()){
                    for(SpawnGroup group : (sector.isBeingPlayed() ? state.rules.spawns : sector.save.meta.rules.spawns)){
                        //calculate the amount of spawn points used
                        //if there's a spawn position override, there is only one potential place they spawn
                        //assume that all overridden positions are valid, should always be true in properly designed campaign maps
                        int spawnCount = group.spawn != -1 ? 1 : group.type.flying ? airSpawns : groundSpawns;

                        float healthMult = 1f + Mathf.clamp(group.type.armor / 20f);
                        StatusEffect effect = (group.effect == null ? StatusEffects.none : group.effect);
                        int spawned = group.getSpawned(i) * spawnCount;
                        if(spawned <= 0) continue;
                        enemyHealth += spawned * (group.getShield(i) + group.type.health * effect.healthMultiplier * healthMult);
                        enemyDps += spawned * group.type.dpsEstimate * effect.damageMultiplier;
                    }
                }

                float efficiency = health / info.sumHealth;
                float dps = info.sumDps * efficiency;
                float rps = info.sumRps * efficiency;

                if(info.bossWave == i){
                    enemyDps += info.bossDps;
                    enemyHealth += info.bossHealth;
                }

                if(i == waveBegin){
                    enemyDps += info.curEnemyDps;
                    enemyHealth += info.curEnemyHealth;
                }

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
                    if(retWave) return Math.max(i - waveBegin - 1, waveBegin);
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
        float damage = getDamage(state.rules.sector);

        //scaled damage has a power component to make it seem a little more realistic (as systems fail, enemy capturing gets easier and easier)
        float scaled = Mathf.pow(damage, 1.2f);

        Tile spawn = spawner.getFirstSpawn();

        //damage only units near the spawn point
        if(spawn != null){
            Seq<Unit> allies = new Seq<>();
            float sumUnitHealth = 0f;
            for(Unit ally : Groups.unit){
                if(ally.team == state.rules.defaultTeam && ally.within(spawn, state.rules.dropZoneRadius * 2.5f)){
                    allies.add(ally);
                    sumUnitHealth += ally.health;
                }
            }

            allies.sort(u -> u.dst2(spawn));

            //apply damage to units
            float unitDamage = damage * sumUnitHealth;

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
    public static void writeParameters(Sector sector){
        var info = sector.info;
        Building core = state.rules.defaultTeam.core();
        Seq<Tile> spawns = new Seq<>();
        spawner.eachGroundSpawn((x, y) -> spawns.add(world.tile(x, y)));

        if(spawns.isEmpty() && state.rules.waveTeam.core() != null){
            spawns.add(state.rules.waveTeam.core().tile);
        }

        if(core == null || spawns.isEmpty()) return;

        boolean airOnly = !state.rules.spawns.contains(g -> !g.type.flying);

        Tile start = spawns.first();
        Seq<Tile> path = new Seq<>();

        //TODO would be nice if this worked in a more generic way, with two different calculations and paths
        if(airOnly){
            World.raycastEach(start.x, start.y, core.tileX(), core.tileY(), (x, y) -> {
                path.add(world.rawTile(x, y));
                return false;
            });
        }else{
            var field = pathfinder.getField(state.rules.waveTeam, Pathfinder.costGround, Pathfinder.fieldCore);
            boolean found = false;

            if(field != null && field.weights != null){
                int[] weights = field.weights;
                int count = 0;
                Tile current = start;
                while(count < weights.length){
                    int minCost = Integer.MAX_VALUE;
                    int cx = current.x, cy = current.y;
                    for(Point2 p : Geometry.d4){
                        int nx = cx + p.x, ny = cy + p.y, packed = world.packArray(nx, ny);

                        Tile other = world.tile(nx, ny);
                        if(other != null && weights[packed] < minCost && weights[packed] != -1){
                            minCost = weights[packed];
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
                path.clear();
                path.addAll(Astar.pathfind(start, core.tile, SectorDamage::cost, t -> !(t.block().isStatic() && t.solid())));
            }
        }

        //create sparse tile array for fast range query
        int sparseSkip = 5, sparseSkip2 = 3;
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
        int radius = 6;
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
                            sumHealth += tile.build.health / (tile.block().size * tile.block().size);
                            totalPathBuild += 1f / (tile.block().size * tile.block().size);
                        }
                    }
                }
            }
        }

        float avgHealth = totalPathBuild <= 1 ? sumHealth : sumHealth / totalPathBuild;

        //block dps + regen + extra health/shields
        for(Building build : state.rules.defaultTeam.data().buildings){
            float e = build.potentialEfficiency;
            if(e > 0.08f){
                if(build instanceof Ranged ranged && sparse.contains(t -> t.within(build, ranged.range() + 4*tilesize))){
                    //TODO make sure power turret network supports the turrets?
                    if(build instanceof TurretBuild b && b.hasAmmo()){
                        sumDps += b.estimateDps();
                    }

                    if(build.block instanceof MendProjector m){
                        sumRps += m.healPercent / m.reload * avgHealth * 60f / 100f * e * build.timeScale();
                    }

                    //point defense turrets act as flat health right now
                    if(build.block instanceof PointDefenseTurret){
                        sumHealth += 150f * build.timeScale() * build.potentialEfficiency;
                    }

                    if(build.block instanceof ForceProjector f){
                        sumHealth += f.shieldHealth * e * build.timeScale();
                        sumRps += e;
                    }
                }
            }
        }

        float curEnemyHealth = 0f, curEnemyDps = 0f;

        //unit regen + health + dps
        for(Unit unit : Groups.unit){
            //skip player
            if(unit.isPlayer()) continue;

            //scale health based on armor - yes, this is inaccurate, but better than nothing
            float healthMult = 1f + Mathf.clamp(unit.armor / 20f);

            if(unit.team == state.rules.defaultTeam){
                sumHealth += unit.health*healthMult + unit.shield;
                sumDps += unit.type.dpsEstimate;
                sumRps += unit.type.weapons.sumf(w -> w.shotsPerSec() * (w.bullet.healPercent/100f * 20f + w.bullet.healAmount));
                if(unit.controller() instanceof CommandAI ai && ai.command == UnitCommand.rebuildCommand){
                    sumRps += unit.type.buildSpeed * 20f;
                }
            }else{
                float bossMult = unit.isBoss() ? 3f : 1f;
                curEnemyDps += unit.type.dpsEstimate * unit.damageMultiplier() * bossMult;
                curEnemyHealth += unit.health * healthMult * unit.healthMultiplier() * bossMult + unit.shield;
            }
        }

        SpawnGroup bossGroup = state.rules.spawns.find(s -> s.effect == StatusEffects.boss);

        if(bossGroup != null){
            float bossMult = 1.2f;
            //calculate first boss appearance
            for(int wave = state.wave; wave < state.wave + 60; wave++){
                int spawned = bossGroup.getSpawned(wave - 1);
                if(spawned > 0){
                    //set up relevant stats
                    info.bossWave = wave;
                    info.bossDps = spawned * bossGroup.type.dpsEstimate * StatusEffects.boss.damageMultiplier * bossMult;
                    info.bossHealth = spawned * (bossGroup.getShield(wave) + bossGroup.type.health * StatusEffects.boss.healthMultiplier * (1f + Mathf.clamp(bossGroup.type.armor / 20f))) * bossMult;
                    break;
                }
            }
        }

        info.sumHealth = sumHealth * 0.9f;
        info.sumDps = sumDps;
        info.sumRps = sumRps;

        float cmult = 1.6f;

        info.curEnemyDps = curEnemyDps*cmult;
        info.curEnemyHealth = curEnemyHealth*cmult;

        info.wavesSurvived = getWavesSurvived(sector);
    }

    public static void apply(float fraction){
        Tiles tiles = world.tiles;

        Queue<Tile> frontier = new Queue<>();
        float[][] values = new float[tiles.width][tiles.height];

        //phase one: find all spawnpoints
        for(Tile tile : tiles){
            if((tile.block() instanceof CoreBlock && tile.team() == state.rules.waveTeam) || tile.overlay() == Blocks.spawn){
                frontier.add(tile);
                values[tile.x][tile.y] = fraction * 24;
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
                float totalHealth = fraction >= 1f ? 1f : path.sumf(t -> {
                    float s = 0;
                    for(int dx = -radius; dx <= radius; dx++){
                        for(int dy = -radius; dy <= radius; dy++){
                            int wx = dx + t.x, wy = dy + t.y;
                            if(wx >= 0 && wy >= 0 && wx < world.width() && wy < world.height() && Mathf.within(dx, dy, radius)){
                                Tile other = world.rawTile(wx, wy);
                                if(!(other.block() instanceof CoreBlock)){
                                    s += other.team() == state.rules.defaultTeam ? other.build.health / (other.block().size * other.block().size) : 0f;
                                }
                            }
                        }
                    }
                    return s;
                });
                float targetHealth = totalHealth * fraction;
                float healthCount = 0;

                out:
                for(int i = 0; i < path.size && (healthCount < targetHealth || fraction >= 1f); i++){
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

                                    if(healthCount >= targetHealth && fraction < 0.999f){
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

        float falloff = (fraction) / (Math.max(tiles.width, tiles.height) * Mathf.sqrt2);
        int peak = 0;

        if(fraction > 0.15f){
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
                            }else{
                                indexer.notifyHealthChanged(other.build);
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
            (tile.build != null ? tile.build.health / (tile.build.block.size * tile.build.block.size) / 20f : 0f) +
            (tile.floor().isLiquid ? 10f : 0f);
    }
}
