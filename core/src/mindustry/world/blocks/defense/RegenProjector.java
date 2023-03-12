package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class RegenProjector extends Block{
    private static final IntSet taken = new IntSet();
    //map building pos to mend amount (TODO just use buildings as keys? no lookup)
    private static final IntFloatMap mendMap = new IntFloatMap();
    private static long lastUpdateFrame = -1;

    public int range = 14;
    //per frame
    public float healPercent = 12f / 60f;
    public float optionalMultiplier = 2f;
    public float optionalUseTime = 60f * 8f;

    public DrawBlock drawer = new DrawDefault();

    public float effectChance = 0.003f;
    public Color baseColor = Pal.accent;
    public Effect effect = Fx.regenParticle;

    public RegenProjector(String name){
        super(name);
        solid = true;
        update = true;
        group = BlockGroup.projectors;
        hasPower = true;
        hasItems = true;
        emitLight = true;
        suppressable = true;
        envEnabled |= Env.space;
        rotateDraw = false;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        x *= tilesize;
        y *= tilesize;

        Drawf.dashSquare(baseColor, x, y, range * tilesize);
        indexer.eachBlock(Vars.player.team(), Tmp.r1.setCentered(x, y, range * tilesize), b -> true, t -> {
            Drawf.selected(t, Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f)));
        });
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        drawer.drawPlan(this, plan, list);
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }

    @Override
    public void load(){
        super.load();
        drawer.load(this);
    }

    @Override
    public void setStats(){
        stats.timePeriod = optionalUseTime;
        super.setStats();

        stats.add(Stat.repairTime, (int)(1f / (healPercent / 100f) / 60f), StatUnit.seconds);
        stats.add(Stat.range, range, StatUnit.blocks);
        stats.add(Stat.boostEffect, optionalMultiplier, StatUnit.timesSpeed);
    }

    public class RegenProjectorBuild extends Building{
        public Seq<Building> targets = new Seq<>();
        public int lastChange = -2;
        public float warmup, totalTime, optionalTimer;
        public boolean anyTargets = false;
        public boolean didRegen = false;

        public void updateTargets(){
            targets.clear();
            taken.clear();
            indexer.eachBlock(team, Tmp.r1.setCentered(x, y, range * tilesize), b -> true, targets::add);
        }

        @Override
        public void updateTile(){
            if(lastChange != world.tileChanges){
                lastChange = world.tileChanges;
                updateTargets();
            }

            //TODO should warmup depend on didRegen?
            warmup = Mathf.approachDelta(warmup, didRegen ? 1f : 0f, 1f / 70f);
            totalTime += warmup * Time.delta;
            didRegen = false;
            anyTargets = false;

            //no healing when suppressed
            if(checkSuppression()){
                return;
            }

            anyTargets = targets.contains(b -> b.damaged());

            if(efficiency > 0){
                if((optionalTimer += Time.delta * optionalEfficiency) >= optionalUseTime){
                    consume();
                    optionalTimer = 0f;
                }

                float healAmount = Mathf.lerp(1f, optionalMultiplier, optionalEfficiency) * healPercent;

                //use Math.max to prevent stacking
                for(var build : targets){
                    if(!build.damaged() || build.isHealSuppressed()) continue;

                    didRegen = true;

                    int pos = build.pos();
                    //TODO periodic effect
                    float value = mendMap.get(pos);
                    mendMap.put(pos, Math.min(Math.max(value, healAmount * edelta() * build.block.health / 100f), build.block.health - build.health));

                    if(value <= 0 && Mathf.chanceDelta(effectChance * build.block.size * build.block.size)){
                        effect.at(build.x + Mathf.range(build.block.size * tilesize/2f - 1f), build.y + Mathf.range(build.block.size * tilesize/2f - 1f));
                    }
                }
            }

            if(lastUpdateFrame != state.updateId){
                lastUpdateFrame = state.updateId;

                for(var entry : mendMap.entries()){
                    var build = world.build(entry.key);
                    if(build != null){
                        build.heal(entry.value);
                        build.recentlyHealed();
                    }
                }
                mendMap.clear();
            }
        }

        @Override
        public boolean shouldConsume(){
            return anyTargets;
        }

        @Override
        public void drawSelect(){
            super.drawSelect();

            Drawf.dashSquare(baseColor, x, y, range * tilesize);
            for(var target : targets){
                Drawf.selected(target, Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f)));
            }
        }

        @Override
        public float warmup(){
            return warmup;
        }

        @Override
        public float totalProgress(){
            return totalTime;
        }

        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLight(this);
        }
    }
}
