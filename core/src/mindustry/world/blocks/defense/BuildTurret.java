package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class BuildTurret extends BaseTurret{
    public final int timerTarget = timers++, timerTarget2 = timers++;
    public int targetInterval = 15;

    public @Load(value = "@-base", fallback = "block-@size") TextureRegion baseRegion;
    public @Load("@-glow") TextureRegion glowRegion;
    public float buildSpeed = 1f;
    public float buildBeamOffset = 5f;
    //created in init()
    public @Nullable UnitType unitType;
    public float elevation = -1f;
    public Color heatColor = Pal.accent.cpy().a(0.9f);

    public BuildTurret(String name){
        super(name);
        group = BlockGroup.turrets;
        sync = false;
        rotateSpeed = 10f;
        suppressable = true;
    }

    @Override
    public void init(){
        super.init();

        if(elevation < 0) elevation = size / 2f;

        //this is super hacky, but since blocks are initialized before units it does not run into init/concurrent modification issues
        unitType = new UnitType("turret-unit-" + name){{
            hidden = true;
            internal = true;
            speed = 0f;
            hitSize = 0f;
            health = 1;
            itemCapacity = 0;
            rotateSpeed = BuildTurret.this.rotateSpeed;
            buildBeamOffset = BuildTurret.this.buildBeamOffset;
            buildRange = BuildTurret.this.range;
            buildSpeed = BuildTurret.this.buildSpeed;
            constructor = BlockUnitUnit::create;
        }};
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.addPercent(Stat.buildSpeed, buildSpeed);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, region};
    }

    public class BuildTurretBuild extends BaseTurretBuild implements ControlBlock{
        public BlockUnitc unit = (BlockUnitc)unitType.create(team);
        public @Nullable Unit following;
        public @Nullable BlockPlan lastPlan;
        public float warmup;

        {
            unit.rotation(90f);
        }

        @Override
        public boolean canControl(){
            return true;
        }

        @Override
        public Unit unit(){
            //make sure stats are correct
            unit.tile(this);
            unit.team(team);
            return (Unit)unit;
        }

        @Override
        public void updateTile(){
            unit.tile(this);
            unit.team(team);

            //only cares about where the unit itself is looking
            rotation = unit.rotation();

            if(unit.activelyBuilding()){
                unit.lookAt(angleTo(unit.buildPlan()));
            }

            if(checkSuppression()){
                efficiency = potentialEfficiency = 0f;
            }

            unit.buildSpeedMultiplier(potentialEfficiency * timeScale);
            unit.speedMultiplier(potentialEfficiency * timeScale);

            warmup = Mathf.lerpDelta(warmup, unit.activelyBuilding() ? efficiency : 0f, 0.1f);

            if(!isControlled()){
                unit.updateBuilding(true);

                if(following != null){
                    //validate follower
                    if(!following.isValid() || !following.activelyBuilding()){
                        following = null;
                        unit.plans().clear();
                    }else{
                        //set to follower's first build plan, whatever that is
                        unit.plans().clear();
                        unit.plans().addFirst(following.buildPlan());
                        lastPlan = null;
                    }

                }else if(unit.buildPlan() == null && timer(timerTarget, targetInterval)){ //search for new stuff
                    Queue<BlockPlan> blocks = team.data().plans;
                    for(int i = 0; i < blocks.size; i++){
                        var block = blocks.get(i);
                        if(within(block.x * tilesize, block.y * tilesize, range)){
                            var btype = content.block(block.block);

                            if(Build.validPlace(btype, unit.team(), block.x, block.y, block.rotation) && (state.rules.infiniteResources || team.rules().infiniteResources || team.items().has(btype.requirements, state.rules.buildCostMultiplier))){
                                unit.addBuild(new BuildPlan(block.x, block.y, block.rotation, content.block(block.block), block.config));
                                //shift build plan to tail so next unit builds something else
                                blocks.addLast(blocks.removeIndex(i));
                                lastPlan = block;
                                break;
                            }
                        }
                    }

                    //still not building, find someone to mimic
                    if(unit.buildPlan() == null){
                        following = null;
                        Units.nearby(team, x, y, range, u -> {
                            if(following  != null) return;

                            if(u.canBuild() && u.activelyBuilding()){
                                BuildPlan plan = u.buildPlan();

                                Building build = world.build(plan.x, plan.y);
                                if(build instanceof ConstructBuild && within(build, range)){
                                    following = u;
                                }
                            }
                        });
                    }
                }else if(unit.buildPlan() != null){ //validate building
                    BuildPlan req = unit.buildPlan();

                    //clear break plan if another player is breaking something
                    if(!req.breaking && timer.get(timerTarget2, 30f)){
                        for(Player player : team.data().players){
                            if(player.isBuilder() && player.unit().activelyBuilding() && player.unit().buildPlan().samePos(req) && player.unit().buildPlan().breaking){
                                unit.plans().removeFirst();
                                //remove from list of plans
                                team.data().plans.remove(p -> p.x == req.x && p.y == req.y);
                                return;
                            }
                        }
                    }

                    boolean valid =
                        !(lastPlan != null && lastPlan.removed) &&
                        ((req.tile() != null && req.tile().build instanceof ConstructBuild cons && cons.current == req.block) ||
                        (req.breaking ?
                        Build.validBreak(unit.team(), req.x, req.y) :
                        Build.validPlace(req.block, unit.team(), req.x, req.y, req.rotation)));

                    if(!valid){
                        //discard invalid request
                        unit.plans().removeFirst();
                        lastPlan = null;
                    }
                }
            }else{ //is being controlled, forget everything
                following = null;
                lastPlan = null;
            }

            //please do not commit suicide
            unit.plans().remove(b -> b.build() == this);

            unit.updateBuildLogic();
        }

        @Override
        public boolean shouldConsume(){
            return unit.plans().size > 0 && !isHealSuppressed();
        }

        @Override
        public void draw(){
            super.draw();

            Draw.rect(baseRegion, x, y);
            Draw.color();

            Draw.z(Layer.turret);

            Drawf.shadow(region, x - elevation, y - elevation, rotation - 90);
            Draw.rect(region, x, y, rotation - 90);

            if(glowRegion.found()){
                Drawf.additive(glowRegion, heatColor, warmup, x, y, rotation - 90f, Layer.turretHeat);
            }

            if(efficiency > 0){
                unit.drawBuilding();
            }
        }

        @Override
        public float warmup(){
            return warmup;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(rotation);
            //TODO queue can be very large due to logic?
            TypeIO.writePlans(write, unit.plans().toArray(BuildPlan.class));
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            rotation = read.f();
            unit.rotation(rotation);
            unit.plans().clear();
            var reqs = TypeIO.readPlans(read);
            if(reqs != null){
                for(var req : reqs){
                    unit.plans().add(req);
                }
            }
        }
    }
}
