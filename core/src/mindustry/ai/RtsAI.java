package mindustry.ai;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.defense.turrets.Turret.*;
import mindustry.world.meta.*;

public class RtsAI{
    static final Seq<Building> targets = new Seq<>();
    static final int timeUpdate = 0;
    static final float minWeight = 0.9f;

    //in order of priority??
    static final BlockFlag[] flags = {BlockFlag.generator, BlockFlag.factory, BlockFlag.core, BlockFlag.battery};
    static final ObjectFloatMap<Building> weights = new ObjectFloatMap<>();
    static final int minSquadSize = 4;
    static boolean debug = true;

    final Interval timer = new Interval(10);
    final TeamData data;

    public RtsAI(TeamData data){
        this.data = data;

        //TODO remove: debugging!

        Events.run(Trigger.draw, () -> {
            if(!debug) return;

            Draw.draw(Layer.overlayUI, () -> {

                float s = Fonts.outline.getScaleX();
                Fonts.outline.getData().setScale(0.5f);
                for(var target : targets){
                    if(weights.containsKey(target)){
                        float w = weights.get(target, 0f);

                        Fonts.outline.draw("[sky]" + Strings.fixed(w, 2), target.x, target.y, Align.center);
                    }
                }
                Fonts.outline.getData().setScale(s);
            });

        });
    }

    public void update(){
        if(timer.get(timeUpdate, 60f * 2f)){
            var build = findAttackPoint();

            if(build != null){
                for(var unit : data.units){
                    if(unit.isCommandable() && !unit.command().hasCommand()){
                        unit.command().commandTarget(build);
                    }
                }
            }
        }
    }

    @Nullable Building findAttackPoint(){
        float health = 0f, dps = 0f;
        int total = 0;
        for(var unit : data.units){
            if(unit.isCommandable() && !unit.command().hasCommand()){
                health += unit.health;
                dps += unit.type.dpsEstimate;
                total ++;
            }
        }

        //flag priority?
        //1. generator
        //2. factor
        //3. core

        //TODO split into "squads" that each find the best target for them
        if(total < minSquadSize) return null;

        targets.clear();
        for(var flag : flags){
            targets.addAll(Vars.indexer.getEnemy(data.team, flag));
        }

        if(targets.size == 0) return null;

        weights.clear();

        for(var target : targets){
            weights.put(target, estimateStats(target.x, target.y, dps, health));
        }

        var result = targets.min(b -> {
            float w = 1f - weights.get(b, 0f);

            //TODO more weighting, e.g. distance
            return w;
        });

        float weight = weights.get(result, 0f);
        if(weight < minWeight && total < Units.getCap(data.team)){
            return null;
        }

        return result;
    }

    float estimateStats(float x, float y, float selfDps, float selfHealth){
        float[] health = {0f}, dps = {0f};
        float extraRadius = 15f;

        //TODO this does not take into account the path to this object
        for(var turret : Vars.indexer.getEnemy(data.team, BlockFlag.turret)){
            if(turret.within(x, y, ((TurretBuild)turret).range() + extraRadius)){
                health[0] += turret.health;
                dps[0] += ((TurretBuild)turret).estimateDps();
            }
        }

        //add on extra radius, assume unit range is below that...?
        Units.nearbyEnemies(data.team, x, y, extraRadius + 140f, other -> {
            if(other.within(x, y, other.range() + extraRadius)){
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
