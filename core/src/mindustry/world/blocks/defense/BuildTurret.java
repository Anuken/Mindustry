package mindustry.world.blocks.defense;

import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class BuildTurret extends BaseTurret{
    public final int timerTarget = timers++;
    public int targetInterval = 20;

    public @Load(value = "@-base", fallback = "block-@size") TextureRegion baseRegion;
    public float buildSpeed = 1f;
    //created in init()
    public @Nullable UnitType unitType;
    public float elevation = -1f;

    public BuildTurret(String name){
        super(name);
        group = BlockGroup.turrets;
    }

    @Override
    public void init(){
        super.init();

        if(elevation < 0) elevation = size / 2f;

        //this is super hacky, but since blocks are initialized before units it does not run into init/concurrent modification issues
        unitType = new UnitType("turret-unit-" + name){{
            speed = 0f;
            hitSize = 0f;
            health = 1;
            rotateSpeed = 360f;
            itemCapacity = 0;
            commandLimit = 0;
            constructor = BlockUnitUnit::create;
            buildRange = BuildTurret.this.range;
            buildSpeed = BuildTurret.this.buildSpeed;
        }};
    }

    public class BuildTurretBuild extends BaseTurretBuild implements ControlBlock{
        public BlockUnitc unit = (BlockUnitc)unitType.create(team);

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

            unit.buildSpeedMultiplier(efficiency() * timeScale);

            if(!isControlled()){
                unit.updateBuilding(true);

                if(unit.buildPlan() == null && timer(timerTarget, targetInterval)){
                    Queue<BlockPlan> blocks = team.data().blocks;
                    for(int i = 0; i < blocks.size; i++){
                        var block = blocks.get(i);
                        if(within(block.x * tilesize, block.y * tilesize, range)){
                            if(Build.validPlace(content.block(block.block), unit.team(), block.x, block.y, block.rotation)){
                                unit.addBuild(new BuildPlan(block.x, block.y, block.rotation, content.block(block.block), block.config));
                                //shift build plan to tail so next unit builds something else
                                blocks.addLast(blocks.removeIndex(i));
                                break;
                            }
                        }
                    }
                }
            }

            //please do not commit suicide
            unit.plans().remove(b -> b.build() == this);

            unit.updateBuildLogic();
        }

        @Override
        public void draw(){
            super.draw();

            Draw.rect(baseRegion, x, y);
            Draw.color();

            Draw.z(Layer.turret);

            Drawf.shadow(region, x - elevation, y - elevation, rotation - 90);
            Draw.rect(region, x, y, rotation - 90);

            unit.drawBuilding();
        }

        /*
        @Override
        public void write(Writes write){
            super.write(write);
            write.f(rotation);
            //TODO build queue
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            rotation = read.f();
            unit.rotation(rotation);
        }*/
    }
}
