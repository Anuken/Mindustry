package io.anuke.mindustry.entities.type.base;

import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.bullet.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.units.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.*;

public class FlyingUnit extends BaseUnit{
    protected float[] weaponAngles = {0,0};

    protected final UnitState

    attack = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){

            if(Units.invalidateTarget(target, team, x, y)){
                target = null;
            }

            if(retarget()){
                targetClosest();

                if(target == null) targetClosestEnemyFlag(BlockFlag.producer);
                if(target == null) targetClosestEnemyFlag(BlockFlag.turret);

                if(target == null && isCommanded() && getCommand() != UnitCommand.attack){
                    onCommand(getCommand());
                }
            }

            if(getClosestSpawner() == null && getSpawner() != null && target == null){
                target = getSpawner();
                circle(80f + Mathf.randomSeed(id) * 120);
            }else if(target != null){
                attack(type.attackLength);

                if((Angles.near(angleTo(target), rotation, type.shootCone) || getWeapon().ignoreRotation) //bombers and such don't care about rotation
                && dst(target) < getWeapon().bullet.range()){
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
            }else{
                target = getClosestSpawner();
                moveTo(Vars.state.rules.dropZoneRadius + 120f);
            }
        }
    },
    rally = new UnitState(){
        public void update(){
            if(retarget()){
                targetClosestAllyFlag(BlockFlag.rally);
                targetClosest();

                if(target != null && !Units.invalidateTarget(target, team, x, y)){
                    setState(attack);
                    return;
                }

                if(target == null) target = getSpawner();
            }

            if(target != null){
                circle(65f + Mathf.randomSeed(id) * 100);
            }
        }
    },
    retreat = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            if(retarget()){
                target = getSpawner();

                Tile repair = Geometry.findClosest(x, y, indexer.getAllied(team, BlockFlag.repair));
                if(repair != null && damaged()) FlyingUnit.this.target = repair.entity;
                if(target == null) target = getClosestCore();
            }

            circle(targetHasFlag(BlockFlag.repair) ? 20f : 60f + Mathf.randomSeed(id) * 50, 0.65f * type.speed);
        }
    };;

    @Override
    public void onCommand(UnitCommand command){
        state.set(command == UnitCommand.retreat ? retreat :
        command == UnitCommand.attack ? attack :
        command == UnitCommand.rally ? rally :
        null);
    }

    @Override
    public void move(float x, float y){
        moveBy(x, y);
    }

    @Override
    public void update(){
        super.update();

        if(!net.client()){
            updateRotation();
        }
        wobble();
    }

    @Override
    public void drawUnder(){
        drawEngine();
    }

    @Override
    public void draw(){
        Draw.mixcol(Color.white, hitTime / hitDuration);
        Draw.rect(type.region, x, y, rotation - 90);

        drawWeapons();

        Draw.mixcol();
    }

    public void drawWeapons(){

    }

    public void drawEngine(){
        Draw.color(Pal.engine);
        Fill.circle(x + Angles.trnsx(rotation + 180, type.engineOffset), y + Angles.trnsy(rotation + 180, type.engineOffset),
        type.engineSize + Mathf.absin(Time.time(), 2f, type.engineSize / 4f));

        Draw.color(Color.white);
        Fill.circle(x + Angles.trnsx(rotation + 180, type.engineOffset - 1f), y + Angles.trnsy(rotation + 180, type.engineOffset - 1f),
        (type.engineSize + Mathf.absin(Time.time(), 2f, type.engineSize / 4f)) / 2f);
        Draw.color();
    }

    @Override
    public void behavior(){

        if(Units.invalidateTarget(target, this)){
            for(boolean left : Mathf.booleans){
                int wi = Mathf.num(left);
                weaponAngles[wi] = Mathf.slerpDelta(weaponAngles[wi], rotation, 0.1f);
            }
        }
    }

    @Override
    public UnitState getStartState(){
        return attack;
    }

    protected void wobble(){
        if(net.client()) return;

        x += Mathf.sin(Time.time() + id * 999, 25f, 0.05f) * Time.delta();
        y += Mathf.cos(Time.time() + id * 999, 25f, 0.05f) * Time.delta();

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
