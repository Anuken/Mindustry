package mindustry.ai;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.BaseTurret.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.meta.*;

public class RtsAI{
    static final Seq<Building> targets = new Seq<>();
    static final Seq<Unit> squad = new Seq<>(false);
    static final IntSet used = new IntSet();
    static final IntSet assignedTargets = new IntSet();
    static final float squadRadius = 120f;
    static final int timeUpdate = 0, timerSpawn = 1;

    //in order of priority??
    static final BlockFlag[] flags = {BlockFlag.generator, BlockFlag.factory, BlockFlag.core, BlockFlag.battery};
    static final ObjectFloatMap<Building> weights = new ObjectFloatMap<>();
    static final boolean debug = OS.hasProp("mindustry.debug");

    final Interval timer = new Interval(10);
    final TeamData data;
    final ObjectSet<Building> damagedSet = new ObjectSet<>();
    final Seq<Building> damaged = new Seq<>(false);

    //must be static, as this class can get instantiated many times; event listeners are hard to clean up
    static{
        Events.on(BuildDamageEvent.class, e -> {
            if(e.build.team.rules().rtsAi){
                var ai = e.build.team.data().rtsAi;
                if(ai != null){
                    ai.damagedSet.add(e.build);
                }
            }
        });
    }

    public RtsAI(TeamData data){
        this.data = data;
        timer.reset(0, Mathf.random(60f * 2f));

        //TODO remove: debugging!

        if(debug){
            Events.run(Trigger.draw, () -> {

                Draw.draw(Layer.overlayUI, () -> {

                    float s = Fonts.outline.getScaleX();
                    Fonts.outline.getData().setScale(0.5f);
                    for(var target : weights){
                        Fonts.outline.draw("[sky]" + Strings.fixed(target.value, 2), target.key.x, target.key.y, Align.center);
                    }
                    Fonts.outline.getData().setScale(s);
                });

            });
        }
    }

    public void update(){
        if(timer.get(timeUpdate, 60f * 2f)){
            assignSquads();
            checkBuilding();
        }
    }

    //TODO atrocious implementation
    void checkBuilding(){
        if(data.team.rules().aiCoreSpawn && timer.get(timerSpawn, 60 * 7f) && data.hasCore()){
            CoreBlock block = (CoreBlock)data.core().block;
            int coreUnits = data.countType(block.unitType);

            //create AI core unit(s) at random cores
            if(coreUnits < data.cores.size){
                Unit unit = block.unitType.create(data.team);
                unit.set(data.cores.random());
                unit.add();
                Fx.spawn.at(unit);
            }
        }
    }

    void assignSquads(){
        assignedTargets.clear();
        used.clear();
        damaged.addAll(damagedSet);
        damagedSet.clear();

        boolean didDefend = false;

        for(var unit : data.units){
            if(unit.isCommandable() && !unit.command().hasCommand() && used.add(unit.id)){
                squad.clear();
                data.tree().intersect(unit.x - squadRadius/2f, unit.y - squadRadius/2f, squadRadius, squadRadius, squad);
                //remove overlapping squads
                squad.removeAll(u -> (u != unit && used.contains(u.id)) || !u.isCommandable() || u.command().hasCommand());
                //mark used so other squads can't steal them
                for(var item : squad){
                    used.add(item.id);
                }

                //TODO flawed, squads
                if(handleSquad(squad, !didDefend)){
                    didDefend = true;
                }
            }
        }

        damaged.clear();
    }

    boolean handleSquad(Seq<Unit> units, boolean noDefenders){
        float health = 0f, dps = 0f;
        float ax = 0f, ay = 0f;

        for(var unit : units){
            ax += unit.x;
            ay += unit.y;
            health += unit.health;
            dps += unit.type.dpsEstimate;
        }
        ax /= units.size;
        ay /= units.size;

        if(debug){
            Vars.ui.showLabel("Squad: " + units.size, 2f, ax, ay);
        }

        Building defend = null;

        //there is something to defend, see if it's worth the time
        if(damaged.size > 0){
            //TODO do the weights matter at all?
            //for(var build : damaged){
                //float w = estimateStats(ax, ay, dps, health);
                //weights.put(build, w);
            //}

            //screw you java
            float aax = ax, aay = ay;

            Building best = damaged.min(b -> {
                //rush to core IMMEDIATELY
                if(b instanceof CoreBuild){
                    return -999999f;
                }

                return b.dst(aax, aay);
            });

            //defend when close, or this is the only squad defending
            //TODO will always rush to defense no matter what
            if(best instanceof CoreBuild || units.size >= data.team.rules().rtsMinSquad || best.within(ax, ay, 500f)){
                defend = best;

                if(debug){
                    Vars.ui.showLabel("Defend, dst = " + (int)(best.dst(ax, ay)), 8f, best.x, best.y);
                }
            }
        }

        //find aggressor, or else, the thing being attacked
        Vec2 defendPos = null;
        Teamc defendTarget = null;
        if(defend != null){
            float checkRange = 260f;

            //TODO could be made faster by storing bullet shooter
            Unit aggressor = Units.closestEnemy(data.team, defend.x, defend.y, checkRange, u -> true);
            if(aggressor != null){
                defendTarget = aggressor;
            }else if(false){ //TODO currently ignored, no use defending against nothing
                //should it even go there if there's no aggressor found?
                Tile closest = defend.findClosestEdge(units.first(), Tile::solid);
                if(closest != null){
                    defendPos = new Vec2(closest.worldx(), closest.worldy());
                }
            }else{
                float mindst = Float.MAX_VALUE;
                Building build = null;

                //find closest turret to attack.
                for(var turret : Vars.indexer.getEnemy(data.team, BlockFlag.turret)){
                    if(turret.within(defend, ((Ranged)turret).range())){
                        float dst = turret.dst2(defend);
                        if(dst < mindst){
                            mindst = dst;
                            build = turret;
                        }
                    }
                }

                if(build != null){
                    defendTarget = build;
                }
            }
        }

        boolean anyDefend = defendPos != null || defendTarget != null;

        var build = anyDefend ? null : findTarget(ax, ay, units.size, dps, health);

        if(build != null || anyDefend){
            for(var unit : units){
                if(unit.isCommandable() && !unit.command().hasCommand()){
                    if(defendPos != null){
                        unit.command().commandPosition(defendPos);
                    }else{
                        //TODO stopAtTarget parameter could be false, could be tweaked
                        unit.command().commandTarget(defendTarget == null ? build : defendTarget, defendTarget != null);
                    }
                }
            }
        }

        return anyDefend;
    }

    @Nullable Building findTarget(float x, float y, int total, float dps, float health){
        if(total < data.team.rules().rtsMinSquad) return null;

        //flag priority?
        //1. generator
        //2. factory
        //3. core
        targets.clear();
        for(var flag : flags){
            targets.addAll(Vars.indexer.getEnemy(data.team, flag));
        }
        targets.removeAll(b -> assignedTargets.contains(b.id));

        if(targets.size == 0) return null;

        weights.clear();

        for(var target : targets){
            weights.put(target, estimateStats(x, y, target.x, target.y, dps, health));
        }

        var result = targets.min(
            Structs.comps(
                //weight is most important?
                Structs.comparingFloat(b -> (1f - weights.get(b, 0f)) + b.dst(x, y)/10000f),
                //then distance TODO why weight above
                Structs.comparingFloat(b -> b.dst2(x, y))
            )
        );

        float weight = weights.get(result, 0f);
        if(weight < data.team.rules().rtsMinWeight && total < Units.getCap(data.team)){
            return null;
        }

        assignedTargets.add(result.id);
        return result;
    }

    float estimateStats(float fromX, float fromY, float x, float y, float selfDps, float selfHealth){
        float[] health = {0f}, dps = {0f};
        float extraRadius = 50f;

        for(var turret : Vars.indexer.getEnemy(data.team, BlockFlag.turret)){
            if(turret instanceof BaseTurretBuild t && Intersector.distanceSegmentPoint(fromX, fromY,  x, y, t.x, t.y) <= t.range() + extraRadius){
                health[0] += t.health;
                dps[0] += t.estimateDps();
            }
        }

        Tmp.r1.set(fromX, fromY, x - fromX, y - fromY).normalize().grow(140f * 2f);

        //add on extra radius, assume unit range is below that...?
        Units.nearbyEnemies(data.team, Tmp.r1, other -> {
            if(Intersector.distanceSegmentPoint(fromX, fromY, x, y, other.x, other.y) <= other.range() + extraRadius){
                health[0] += other.health;
                dps[0] += other.type.dpsEstimate;
            }
        });

        float hp = health[0], dp = dps[0];

        float timeDestroyOther = Mathf.zero(selfDps, 0.001f) ? Float.POSITIVE_INFINITY : hp / selfDps;
        float timeDestroySelf = Mathf.zero(dp) ? Float.POSITIVE_INFINITY : selfHealth / dp;

        //other can never be destroyed | other destroys self instantly
        if(Float.isInfinite(timeDestroyOther) | Mathf.zero(timeDestroySelf)) return 0f;
        //self can never be destroyed | self destroys other instantly
        if(Float.isInfinite(timeDestroySelf) | Mathf.zero(timeDestroyOther)) return 1f;

        //examples:
        // self 10 sec / other 10 sec -> can destroy target with 100 % losses -> returns 1
        // self 5 sec / other 10 sec -> can destroy about half of other -> returns 0.5 (needs to be 2x stronger to defeat)
        return timeDestroySelf / timeDestroyOther;
    }
}
