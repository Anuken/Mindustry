package mindustry.entities.units;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class AIController implements UnitController{
    protected static final Vec2 vec = new Vec2();
    protected static final float rotateBackTimer = 60f * 5f;
    protected static final int timerTarget = 0, timerTarget2 = 1, timerTarget3 = 2, timerTarget4 = 3;

    protected Unit unit;
    protected Interval timer = new Interval(4);
    protected AIController fallback;
    protected float noTargetTime;

    /** main target that is being faced */
    protected Teamc target;

    {
        timer.reset(0, Mathf.random(40f));
        timer.reset(1, Mathf.random(60f));
    }

    @Override
    public void updateUnit(){
        //use fallback AI when possible
        if(useFallback() && (fallback != null || (fallback = fallback()) != null)){
            if(fallback.unit != unit) fallback.unit(unit);
            fallback.updateUnit();
            return;
        }

        updateVisuals();
        updateTargeting();
        updateMovement();
    }

    /**
     * @return whether controller state should not be reset after reading.
     * Do not override unless you know exactly what you are doing.
     * */
    public boolean keepState(){
        return false;
    }

    @Override
    public boolean isLogicControllable(){
        return true;
    }

    public void stopShooting(){
        for(var mount : unit.mounts){
            //ignore mount controllable stats too, they should not shoot either
            mount.shoot = false;
        }
    }

    @Nullable
    public AIController fallback(){
        return null;
    }

    public boolean useFallback(){
        return false;
    }

    public void updateVisuals(){
        if(unit.isFlying()){
            unit.wobble();

            unit.lookAt(unit.prefRotation());
        }
    }

    public void updateMovement(){

    }

    public void updateTargeting(){
        if(unit.hasWeapons()){
            updateWeapons();
        }
    }

    /** For ground units: Looks at the target, or the movement position. Does not apply to non-omni units. */
    public void faceTarget(){
        if(unit.type.omniMovement || unit instanceof Mechc){
            if(!Units.invalidateTarget(target, unit, unit.range()) && unit.type.faceTarget && unit.type.hasWeapons()){
                unit.lookAt(Predict.intercept(unit, target, unit.type.weapons.first().bullet.speed));
            }else if(unit.moving()){
                unit.lookAt(unit.vel().angle());
            }
        }
    }

    public void faceMovement(){
        if((unit.type.omniMovement || unit instanceof Mechc) && unit.moving()){
            unit.lookAt(unit.vel().angle());
        }
    }

    public boolean invalid(Teamc target){
        return Units.invalidateTarget(target, unit.team, unit.x, unit.y);
    }

    public void pathfind(int pathTarget){
        int costType = unit.pathType();

        Tile tile = unit.tileOn();
        if(tile == null) return;
        Tile targetTile = pathfinder.getTargetTile(tile, pathfinder.getField(unit.team, costType, pathTarget));

        if(tile == targetTile || (costType == Pathfinder.costNaval && !targetTile.floor().isLiquid)) return;

        unit.movePref(vec.trns(unit.angleTo(targetTile.worldx(), targetTile.worldy()), unit.speed()));
    }

    public void updateWeapons(){
        float rotation = unit.rotation - 90;
        boolean ret = retarget();

        if(ret){
            target = findMainTarget(unit.x, unit.y, unit.range(), unit.type.targetAir, unit.type.targetGround);
        }

        noTargetTime += Time.delta;

        if(invalid(target)){
            target = null;
        }else{
            noTargetTime = 0f;
        }

        unit.isShooting = false;

        for(var mount : unit.mounts){
            Weapon weapon = mount.weapon;
            float wrange = weapon.range();

            //let uncontrollable weapons do their own thing
            if(!weapon.controllable || weapon.noAttack) continue;

            if(!weapon.aiControllable){
                mount.rotate = false;
                continue;
            }

            float mountX = unit.x + Angles.trnsx(rotation, weapon.x, weapon.y),
                mountY = unit.y + Angles.trnsy(rotation, weapon.x, weapon.y);

            if(unit.type.singleTarget){
                mount.target = target;
            }else{
                if(ret){
                    mount.target = findTarget(mountX, mountY, wrange, weapon.bullet.collidesAir, weapon.bullet.collidesGround);
                }

                if(checkTarget(mount.target, mountX, mountY, wrange)){
                    mount.target = null;
                }
            }

            boolean shoot = false;

            if(mount.target != null){
                shoot = mount.target.within(mountX, mountY, wrange + (mount.target instanceof Sized s ? s.hitSize()/2f : 0f)) && shouldShoot();

                Vec2 to = Predict.intercept(unit, mount.target, weapon.bullet.speed);
                mount.aimX = to.x;
                mount.aimY = to.y;
            }

            unit.isShooting |= (mount.shoot = mount.rotate = shoot);

            if(mount.target == null && !shoot && !Angles.within(mount.rotation, mount.weapon.baseRotation, 0.01f) && noTargetTime >= rotateBackTimer){
                mount.rotate = true;
                Tmp.v1.trns(unit.rotation + mount.weapon.baseRotation, 5f);
                mount.aimX = mountX + Tmp.v1.x;
                mount.aimY = mountY + Tmp.v1.y;
            }

            if(shoot){
                unit.aimX = mount.aimX;
                unit.aimY = mount.aimY;
            }
        }
    }

    public boolean checkTarget(Teamc target, float x, float y, float range){
        return Units.invalidateTarget(target, unit.team, x, y, range);
    }

    public boolean shouldShoot(){
        return true;
    }

    public Teamc targetFlag(float x, float y, BlockFlag flag, boolean enemy){
        if(unit.team == Team.derelict) return null;
        return Geometry.findClosest(x, y, enemy ? indexer.getEnemy(unit.team, flag) : indexer.getFlagged(unit.team, flag));
    }

    public Teamc target(float x, float y, float range, boolean air, boolean ground){
        return Units.closestTarget(unit.team, x, y, range, u -> u.checkTarget(air, ground), t -> ground);
    }

    public boolean retarget(){
        return timer.get(timerTarget, target == null ? 40 : 90);
    }

    public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground){
        return findTarget(x, y, range, air, ground);
    }

    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        return target(x, y, range, air, ground);
    }

    public void commandTarget(Teamc moveTo){}

    public void commandPosition(Vec2 pos){}

    /** Called after this controller is assigned a unit. */
    public void init(){

    }

    public @Nullable Tile getClosestSpawner(){
        return Geometry.findClosest(unit.x, unit.y, Vars.spawner.getSpawns());
    }

    public void unloadPayloads(){
        if(unit instanceof Payloadc pay && pay.hasPayload() && target instanceof Building && pay.payloads().peek() instanceof UnitPayload){
            if(target.within(unit, Math.max(unit.type().range + 1f, 75f))){
                pay.dropLastPayload();
            }
        }
    }

    public void circleAttack(float circleLength){
        vec.set(target).sub(unit);

        float ang = unit.angleTo(target);
        float diff = Angles.angleDist(ang, unit.rotation());

        if(diff > 70f && vec.len() < circleLength){
            vec.setAngle(unit.vel().angle());
        }else{
            vec.setAngle(Angles.moveToward(unit.vel().angle(), vec.angle(), 6f));
        }

        vec.setLength(unit.speed());

        unit.moveAt(vec);
    }

    public void circle(Position target, float circleLength){
        circle(target, circleLength, unit.speed());
    }

    public void circle(Position target, float circleLength, float speed){
        if(target == null) return;

        vec.set(target).sub(unit);

        if(vec.len() < circleLength){
            vec.rotate((circleLength - vec.len()) / circleLength * 180f);
        }

        vec.setLength(speed);

        unit.moveAt(vec);
    }

    public void moveTo(Position target, float circleLength){
        moveTo(target, circleLength, 100f);
    }

    public void moveTo(Position target, float circleLength, float smooth){
        moveTo(target, circleLength, smooth, unit.isFlying(), null);
    }

    public void moveTo(Position target, float circleLength, float smooth, boolean keepDistance, @Nullable Vec2 offset){
        moveTo(target, circleLength, smooth, keepDistance, offset, false);
    }

    public void moveTo(Position target, float circleLength, float smooth, boolean keepDistance, @Nullable Vec2 offset, boolean arrive){
        if(target == null) return;

        vec.set(target).sub(unit);

        float length = circleLength <= 0.001f ? 1f : Mathf.clamp((unit.dst(target) - circleLength) / smooth, -1f, 1f);

        vec.setLength(unit.speed() * length);

        if(arrive){
            Tmp.v3.set(-unit.vel.x / unit.type.accel * 2f, -unit.vel.y / unit.type.accel * 2f).add((target.getX() - unit.x), (target.getY() - unit.y));
            vec.add(Tmp.v3).limit(unit.speed() * length);
        }

        if(length < -0.5f){
            if(keepDistance){
                vec.rotate(180f);
            }else{
                vec.setZero();
            }
        }else if(length < 0){
            vec.setZero();
        }

        if(offset != null){
            vec.add(offset);
            vec.setLength(unit.speed() * length);
        }

        //do not move when infinite vectors are used or if its zero.
        if(vec.isNaN() || vec.isInfinite() || vec.isZero()) return;

        if(!unit.type.omniMovement && unit.type.rotateMoveFirst){
            float angle = vec.angle();
            unit.lookAt(angle);
            if(Angles.within(unit.rotation, angle, 3f)){
                unit.movePref(vec);
            }
        }else{
            unit.movePref(vec);
        }
    }

    @Override
    public void unit(Unit unit){
        if(this.unit == unit) return;

        this.unit = unit;
        init();
    }

    @Override
    public Unit unit(){
        return unit;
    }
}
