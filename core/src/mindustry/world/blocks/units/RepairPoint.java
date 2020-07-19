package mindustry.world.blocks.units;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class RepairPoint extends Block{
    private static final Rect rect = new Rect();

    public int timerTarget = timers++;

    public float repairRadius = 50f;
    public float repairSpeed = 0.3f;
    public float powerUse;

    public @Load("@-base") TextureRegion baseRegion;
    public @Load("laser") TextureRegion laser;
    public @Load("laser-end") TextureRegion laserEnd;

    public Color laserColor = Color.valueOf("e8ffd7");

    public RepairPoint(String name){
        super(name);
        update = true;
        solid = true;
        flags = EnumSet.of(BlockFlag.repair);
        hasPower = true;
        outlineIcon = true;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(BlockStat.range, repairRadius / tilesize, StatUnit.blocks);
    }

    @Override
    public void init(){
        consumes.powerCond(powerUse, entity -> ((RepairPointEntity)entity).target != null);
        super.init();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, repairRadius, Pal.accent);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, region};
    }

    public class RepairPointEntity extends Building{
        public Unit target;
        public float strength, rotation = 90;

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);

            Draw.z(Layer.turret);
            Draw.rect(region, x, y, rotation - 90);

            if(target != null && Angles.angleDist(angleTo(target), rotation) < 30f){
                Draw.z(Layer.power);
                float ang = angleTo(target);
                float len = 5f;

                Draw.color(laserColor);
                Drawf.laser(team, laser, laserEnd,
                x + Angles.trnsx(ang, len), y + Angles.trnsy(ang, len),
                target.x(), target.y(), strength);
                Draw.color();
            }
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, repairRadius, Pal.accent);
        }

        @Override
        public void updateTile(){
            boolean targetIsBeingRepaired = false;
            if(target != null && (target.dead() || target.dst(tile) > repairRadius || target.health() >= target.maxHealth())){
                target = null;
            }else if(target != null && consValid()){
                target.heal(repairSpeed * Time.delta * strength * efficiency());
                rotation = Mathf.slerpDelta(rotation, angleTo(target), 0.5f);
                targetIsBeingRepaired = true;
            }

            if(target != null && targetIsBeingRepaired){
                strength = Mathf.lerpDelta(strength, 1f, 0.08f * Time.delta);
            }else{
                strength = Mathf.lerpDelta(strength, 0f, 0.07f * Time.delta);
            }

            if(timer(timerTarget, 20)){
                rect.setSize(repairRadius * 2).setCenter(x, y);
                target = Units.closest(team, x, y, repairRadius, Unit::damaged);
            }
        }

        @Override
        public boolean shouldConsume(){
            return target != null;
        }
    }
}
