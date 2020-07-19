package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class TractorBeamTurret extends Block{
    public final int timerTarget = timers++;
    public float retargetTime = 5f;

    public @Load("block-$size") TextureRegion baseRegion;
    public @Load("@-laser") TextureRegion laser;
    public @Load("@-laser-end") TextureRegion laserEnd;

    public float range = 80f;
    public float rotateSpeed = 10;
    public float shootCone = 6f;
    public float laserWidth = 0.6f;
    public float force = 0.3f;
    public float scaledForce = 0f;
    public float damage = 0f;
    public boolean targetAir = true, targetGround = false;

    public TractorBeamTurret(String name){
        super(name);

        update = true;
        solid = true;
        outlineIcon = true;
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
        stats.add(BlockStat.targetsAir, targetAir);
        stats.add(BlockStat.targetsGround, targetGround);
        stats.add(BlockStat.damage, damage * 60f, StatUnit.perSecond);
    }

    public class TractorBeamEntity extends Building{
        public float rotation = 90;
        public @Nullable Unit target;
        public float lastX, lastY, strength;
        public boolean any;

        @Override
        public void updateTile(){

            //retarget
            if(timer(timerTarget, retargetTime)){
                target = Units.closestEnemy(team, x, y, range, u -> u.checkTarget(targetAir, targetGround));
            }

            //look at target
            if(target != null && target.within(this, range) && target.team() != team && target.type().flying){
                any = true;
                float dest = angleTo(target);
                rotation = Angles.moveToward(rotation,dest, rotateSpeed * edelta());
                lastX = target.x;
                lastY = target.y;
                strength = Mathf.lerpDelta(strength, 1f, 0.1f);

                if(damage > 0){
                    target.damageContinuous(damage);
                }

                //shoot when possible
                if(Angles.within(rotation, dest, shootCone)){
                    target.impulse(Tmp.v1.set(this).sub(target).limit((force + (1f - target.dst(this) / range) * scaledForce) * efficiency() * timeScale));
                }
            }else{
                target = null;
                strength = Mathf.lerpDelta(strength, 0, 0.1f);
            }
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, range, Pal.accent);
        }

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);
            Draw.rect(region, x, y, rotation - 90);

            //draw laser if applicable
            if(any){
                Draw.z(Layer.bullet);
                float ang = angleTo(lastX, lastY);
                float len = 5f;

                Draw.mixcol(Color.white, Mathf.absin(4f, 0.6f));

                Drawf.laser(team, laser, laserEnd,
                x + Angles.trnsx(ang, len), y + Angles.trnsy(ang, len),
                lastX, lastY, strength * efficiency() * laserWidth);

                Draw.mixcol();
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
