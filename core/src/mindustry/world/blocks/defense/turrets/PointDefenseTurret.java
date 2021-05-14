package mindustry.world.blocks.defense.turrets;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.meta.*;

public class PointDefenseTurret extends ReloadTurret{
    public final int timerTarget = timers++;
    public float retargetTime = 5f;

    public @Load("block-@size") TextureRegion baseRegion;
    public @Load("@-heat") TextureRegion heatRegion;
    public float elevation = -1f;

    public Color color = Color.white;
    public Color heatColor = Pal.turretHeat;
    public Effect beamEffect = Fx.pointBeam;
    public Effect hitEffect = Fx.pointHit;
    public Effect shootEffect = Fx.sparkShoot;

    public Sound shootSound = Sounds.lasershoot;

    public float shootCone = 5f;
    public float bulletDamage = 10f;
    public float shootLength = 3f;
    public float cooldown = 0.02f;

    public Cons<PointDefenseBuild> drawer = tile -> Draw.rect(region, tile.x, tile.y, tile.rotation - 90);
    public Cons<PointDefenseBuild> heatDrawer = tile -> {
        if(tile.heat <= 0.00001f) return;

        Draw.color(heatColor, tile.heat);
        Draw.blend(Blending.additive);
        Draw.rect(heatRegion, tile.x, tile.y, tile.rotation - 90);
        Draw.blend();
        Draw.color();
    };

    public PointDefenseTurret(String name){
        super(name);

        rotateSpeed = 20f;
        reloadTime = 30f;

        coolantMultiplier = 2f;
        //disabled due to version mismatch problems
        acceptCoolant = false;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, region};
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.reload, 60f / reloadTime, StatUnit.none);
    }

    @Override
    public void init(){
        super.init();

        if(elevation < 0) elevation = size / 2f;
    }

    public class PointDefenseBuild extends ReloadTurretBuild{
        public @Nullable Bullet target;
        public float heat;

        @Override
        public void updateTile(){
            heat = Mathf.lerpDelta(heat, 0f, cooldown);

            //retarget
            if(timer(timerTarget, retargetTime)){
                target = Groups.bullet.intersect(x - range, y - range, range*2, range*2).min(b -> b.team != team && b.type().hittable, b -> b.dst2(this));
            }

            //pooled bullets
            if(target != null && !target.isAdded()){
                target = null;
            }

            if(acceptCoolant){
                updateCooling();
            }

            //look at target
            if(target != null && target.within(this, range) && target.team != team && target.type() != null && target.type().hittable){
                float dest = angleTo(target);
                rotation = Angles.moveToward(rotation, dest, rotateSpeed * edelta());
                reload += edelta();

                //shoot when possible
                if(Angles.within(rotation, dest, shootCone) && reload >= reloadTime){
                    if(target.damage() > bulletDamage){
                        target.damage(target.damage() - bulletDamage);
                    }else{
                        target.remove();
                    }

                    Tmp.v1.trns(rotation, shootLength);

                    beamEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, color, new Vec2().set(target));
                    shootEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, color);
                    hitEffect.at(target.x, target.y, color);
                    shootSound.at(x + Tmp.v1.x, y + Tmp.v1.y, Mathf.random(0.9f, 1.1f));
                    reload = 0;
                    heat =  1f;
                }
            }
        }

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);

            Drawf.shadow(region, x - elevation, y - elevation, rotation - 90);
            drawer.get(this);

            if(Core.atlas.isFound(heatRegion)){
                heatDrawer.get(this);
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
