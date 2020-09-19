package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class PointDefenseTurret extends Block{
    public final int timerTarget = timers++;
    public float retargetTime = 5f;

    public @Load("block-@size") TextureRegion baseRegion;

    public Color color = Color.white;
    public Effect beamEffect = Fx.pointBeam;
    public Effect hitEffect = Fx.pointHit;
    public Effect shootEffect = Fx.sparkShoot;

    public float range = 80f;
    public float reloadTime = 30f;
    public float rotateSpeed = 20;
    public float shootCone = 5f;
    public float bulletDamage = 10f;
    public float shootLength = 3f;
    /** How much reload is lowered by for each unit of liquid of heat capacity. */
    public float coolantMultiplier = 5f;
    /** Effect displayed when coolant is used. */
    public Effect coolEffect = Fx.fuelburn;

    public PointDefenseTurret(String name){
        super(name);

        outlineIcon = true;
        update = true;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.accent);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, region};
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.shootRange, range / tilesize, StatUnit.blocks);
        stats.add(BlockStat.reload, 60f / reloadTime, StatUnit.none);

        stats.add(BlockStat.booster, new BoosterListValue(reloadTime, consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount, coolantMultiplier, true, l -> consumes.liquidfilters.get(l.id)));
    }

    @Override
    public void init(){
        hasLiquids = true;
        consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 0.2f)).update(false).boost();

        super.init();
    }

    public class PointDefenseBuild extends Building{
        public float rotation = 90, reload;
        public @Nullable Bullet target;

        @Override
        public void updateTile(){

            //retarget
            if(timer(timerTarget, retargetTime)){
                target = Groups.bullet.intersect(x - range, y - range, range*2, range*2).min(b -> b.team == team || !b.type().hittable ? Float.MAX_VALUE : b.dst2(this));
            }

            //pooled bullets
            if(target != null && !target.isAdded()){
                target = null;
            }

            //look at target
            if(target != null && target.within(this, range) && target.team != team && target.type() != null && target.type().hittable){
                float dest = angleTo(target);
                rotation = Angles.moveToward(rotation, dest, rotateSpeed * edelta());
                reload -= edelta();

                //shoot when possible
                if(Angles.within(rotation, dest, shootCone) && reload <= 0f){
                    if(target.damage() > bulletDamage){
                        target.damage(target.damage() - bulletDamage);
                    }else{
                        target.remove();
                    }

                    Tmp.v1.trns(rotation, shootLength);

                    beamEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, color, new Vec2().set(target));
                    shootEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, color);
                    hitEffect.at(target.x, target.y, color);
                    reload = reloadTime;
                }
            }else{
                target = null;
            }

            updateCooling();
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, range, Pal.accent);
        }

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);
            Draw.rect(region, x, y, rotation - 90);
        }

        protected void updateCooling(){
            float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;

            Liquid liquid = liquids.current();

            float used = Math.min(Math.min(liquids.get(liquid), maxUsed * Time.delta), Math.max(0, ((reloadTime - reload) / coolantMultiplier) / liquid.heatCapacity));
            reload -= used * liquid.heatCapacity * coolantMultiplier;
            liquids.remove(liquid, used);

            if(Mathf.chance(0.06 * used)){
                coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(rotation);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            rotation = read.f();
        }
    }
}
