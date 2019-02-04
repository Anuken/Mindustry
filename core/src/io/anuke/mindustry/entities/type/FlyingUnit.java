package io.anuke.mindustry.entities.type;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Fill;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Time;
import io.anuke.arc.util.Tmp;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;

import static io.anuke.mindustry.Vars.world;

public abstract class FlyingUnit extends BaseUnit{
    protected float[] weaponAngles = {0, 0};

    protected final UnitState

    idle = new UnitState(){
        public void update(){
            retarget(() -> {
                targetClosest();
                targetClosestEnemyFlag(BlockFlag.target);

                if(target != null){
                    setState(attack);
                }
            });

            target = getClosestCore();
            if(target != null){
                circle(50f);
            }
            velocity.scl(0.8f);
        }
    },

    attack = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            if(Units.invalidateTarget(target, team, x, y)){
                target = null;
            }

            if(target == null){

                retarget(() -> {
                    targetClosest();

                    if(target == null){
                        setState(patrol);
                        return;
                    }

                    if(target == null) targetClosestEnemyFlag(BlockFlag.target);
                    if(target == null) targetClosestEnemyFlag(BlockFlag.producer);
                    if(target == null) targetClosestEnemyFlag(BlockFlag.turret);

                    if(target == null){
                        setState(idle);
                    }
                });
            }else{
                attack(type.attackLength);

                if((Angles.near(angleTo(target), rotation, type.shootCone) || getWeapon().ignoreRotation) //bombers and such don't care about rotation
                && dst(target) < Math.max(getWeapon().bullet.range(), type.range)){
                    BulletType ammo = getWeapon().bullet;

                    if(type.rotateWeapon){
                        for(boolean left : Mathf.booleans){
                            int wi = Mathf.num(left);
                            float wx = x + Angles.trnsx(rotation - 90, getWeapon().width * Mathf.sign(left));
                            float wy = y + Angles.trnsy(rotation - 90, getWeapon().width * Mathf.sign(left));

                            weaponAngles[wi] = Mathf.slerpDelta(weaponAngles[wi], Angles.angle(wx, wy, target.getX(), target.getY()), 0.1f);

                            Tmp.v2.trns(weaponAngles[wi], getWeapon().length);
                            getWeapon().update(FlyingUnit.this, wx + Tmp.v2.x, wy + Tmp.v2.y, weaponAngles[wi], left);
                        }
                    }else{
                        Vector2 to = Predict.intercept(FlyingUnit.this, target, ammo.speed);
                        getWeapon().update(FlyingUnit.this, to.x, to.y);
                    }
                }
            }
        }
    },
    patrol = new UnitState(){
        public void update(){
            retarget(() -> {
                targetClosest();

                if(target != null){
                    setState(attack);
                }

                target = getClosestCore();
            });

            if(target != null){
                circle(60f + Mathf.absin(Time.time() + id * 23525, 70f, 1200f));
            }
        }
    },
    retreat = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            if(health >= maxHealth()){
                state.set(attack);
            }else if(!targetHasFlag(BlockFlag.repair)){
                retarget(() -> {
                    Tile target = Geometry.findClosest(x, y, world.indexer.getAllied(team, BlockFlag.repair));
                    if(target != null) FlyingUnit.this.target = target.entity;
                });
            }else{
                circle(20f);
            }
        }
    };

    @Override
    public void move(float x, float y){
        moveBy(x, y);
    }

    @Override
    public void update(){
        super.update();

        if(!Net.client()){
            updateRotation();
            wobble();
        }
    }

    @Override
    public void drawUnder(){
        drawEngine();
    }

    @Override
    public void draw(){
        Draw.alpha(Draw.getShader() != Shaders.mix ? 1f : hitTime / hitDuration);
        Draw.rect(type.region, x, y, rotation - 90);

        drawWeapons();
        drawItems();

        Draw.alpha(1f);
    }

    public void drawWeapons(){

    }

    public void drawEngine(){
        Draw.color(Palette.engine);
        Fill.circle(x + Angles.trnsx(rotation + 180, type.engineOffset), y + Angles.trnsy(rotation + 180, type.engineOffset),
                type.engineSize + Mathf.absin(Time.time(), 2f, type.engineSize/4f));

        Draw.color(Color.WHITE);
        Fill.circle(x + Angles.trnsx(rotation + 180, type.engineOffset-1f), y + Angles.trnsy(rotation + 180, type.engineOffset-1f),
        (type.engineSize + Mathf.absin(Time.time(), 2f, type.engineSize/4f)) / 2f);
        Draw.color();
    }

    @Override
    public void behavior(){
        if(Units.invalidateTarget(target, this)){
            for(boolean left : Mathf.booleans){
                int wi = Mathf.num(left);
                weaponAngles[wi] = Mathf.slerpDelta(weaponAngles[wi],rotation, 0.1f);
            }
        }

        if(health <= health * type.retreatPercent &&
         Geometry.findClosest(x, y, world.indexer.getAllied(team, BlockFlag.repair)) != null){
            setState(retreat);
        }
    }

    @Override
    public UnitState getStartState(){
        return attack;
    }

    protected void wobble(){
        if(Net.client()) return;

        x += Mathf.sin(Time.time() + id * 999, 25f, 0.05f)*Time.delta();
        y += Mathf.cos(Time.time() + id * 999, 25f, 0.05f)*Time.delta();

        if(velocity.len() <= 0.05f){
            //rotation += Mathf.sin(Time.time() + id * 99, 10f, 2f * type.speed)*Time.delta();
        }
    }

    protected void updateRotation(){
        rotation = velocity.angle();
    }

    protected void circle(float circleLength){
        circle(circleLength, type.speed);
    }

    protected void circle(float circleLength, float speed){
        if(target == null) return;

        Tmp.v1.set(target.getX() - x, target.getY() - y);

        if(Tmp.v1.len() < circleLength){
            Tmp.v1.rotate((circleLength - Tmp.v1.len()) / circleLength * 180f);
        }

        Tmp.v1.setLength(speed * Time.delta());

        velocity.add(Tmp.v1);
    }

    protected void moveTo(float circleLength){
        if(target == null) return;

        Tmp.v1.set(target.getX() - x, target.getY() - y);

        float length = circleLength <= 0.001f ? 1f : Mathf.clamp((dst(target) - circleLength) / 100f, -1f, 1f);

        Tmp.v1.setLength(type.speed * Time.delta() * length);
        if(length < -0.5f){
            Tmp.v1.rotate(180f);
        }else if(length < 0){
            Tmp.v1.setZero();
        }

        velocity.add(Tmp.v1);
    }

    protected void attack(float circleLength){
        Tmp.v1.set(target.getX() - x, target.getY() - y);

        float ang = angleTo(target);
        float diff = Angles.angleDist(ang, rotation);

        if(diff > 100f && Tmp.v1.len() < circleLength){
            Tmp.v1.setAngle(velocity.angle());
        }else{
            Tmp.v1.setAngle(Mathf.slerpDelta(velocity.angle(), Tmp.v1.angle(), 0.44f));
        }

        Tmp.v1.setLength(type.speed * Time.delta());

        velocity.add(Tmp.v1);
    }
}
