package mindustry.world.blocks.power;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

public class Battery extends PowerDistributor{
    public DrawBlock drawer;

    public Color emptyLightColor = Color.valueOf("f8c266");
    public Color fullLightColor = Color.valueOf("fb9567");

    @Deprecated
    public @Load("@-top") TextureRegion topRegion;

    public Battery(String name){
        super(name);
        outputsPower = true;
        consumesPower = true;
        canOverdrive = false;
        flags = EnumSet.of(BlockFlag.battery);
        //TODO could be supported everywhere...
        envEnabled |= Env.space;
        destructible = true;
        //batteries don't need to update
        update = false;
    }

    @Override
    public void init(){
        super.init();

        checkDrawDefault();
    }

    void checkDrawDefault(){
        if(drawer == null){
            drawer = new DrawMulti(new DrawDefault(), new DrawPower(){{
                emptyLightColor = Battery.this.emptyLightColor;
                fullLightColor = Battery.this.fullLightColor;
            }}, new DrawRegion("-top"));
        }
    }

    @Override
    public void load(){
        checkDrawDefault();

        super.load();
        drawer.load(this);
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        drawer.drawPlan(this, plan, list);
    }

    @Override
    public TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }

    @Override
    public void getRegionsToOutline(Seq<TextureRegion> out){
        drawer.getRegionsToOutline(this, out);
    }

    public class BatteryBuild extends Building{
        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLight(this);
        }

        @Override
        public float warmup(){
            return power.status;
        }

        @Override
        public void overwrote(Seq<Building> previous){
            for(Building other : previous){
                if(other.power != null && other.block.consPower != null && other.block.consPower.buffered){
                    float amount = other.block.consPower.capacity * other.power.status;
                    power.status = Mathf.clamp(power.status + amount / consPower.capacity);
                }
            }
        }

        @Override
        public BlockStatus status(){
            if(Mathf.equal(power.status, 0f, 0.001f)) return BlockStatus.noInput;
            if(Mathf.equal(power.status, 1f, 0.001f)) return BlockStatus.active;
            return BlockStatus.noOutput;
        }
    }
}
