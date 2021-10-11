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
    protected static final int timerTarget = 0, timerTarget2 = 1, timerTarget3 = 2, timerTarget4 = 3;

    protected Unit unit;
    protected Interval timer = new Interval(4);
    protected AIController fallback;

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

    @Nullable
    public AIController fallback(){
        return null;
    }

    public boolean useFallback(){
        return false;
    }

    public UnitCommand command(){
        return unit.team.data().command;
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
            if(!Units.invalidateTarget(target, unit, unit.range()) && unit.type.rotateShooting && unit.type.hasWeapons()){
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

        if(invalid(target)){
            target = null;
        }

        unit.isShooting = false;

        for(var mount : unit.mounts){
            Weapon weapon = mount.weapon;

            //let uncontrollable weapons do their own thing
            if(!weapon.controllable) continue;

            float mountX = unit.x + Angles.trnsx(rotation, weapon.x, weapon.y),
                mountY = unit.y + Angles.trnsy(rotation, weapon.x, weapon.y);

            if(unit.type.singleTarget){
                mount.target = target;
            }else{
                if(ret){
                    mount.target = findTarget(mountX, mountY, weapon.bullet.range(), weapon.bullet.collidesAir, weapon.bullet.collidesGround);
                }

                if(checkTarget(mount.target, mountX, mountY, weapon.bullet.range())){
                    mount.target = null;
                }
            }

            boolean shoot = false;

            if(mount.target != null){
                shoot = mount.target.within(mountX, mountY, weapon.bullet.range() + (mount.target instanceof Sized s ? s.hitSize()/2f : 0f)) && shouldShoot();

                Vec2 to = Predict.intercept(unit, mount.target, weapon.bullet.speed);
                mount.aimX = to.x;
                mount.aimY = to.y;
            }

            unit.isShooting |= (mount.shoot = mount.rotate = shoot);

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
        Tile target = Geometry.findClosest(x, y, enemy ? indexer.getEnemy(unit.team, flag) : indexer.getAllied(unit.team, flag));
        return target == null ? null : target.build;
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
        if(target == null) return;

        vec.set(target).sub(unit);

        float length = circleLength <= 0.001f ? 1f : Mathf.clamp((unit.dst(target) - circleLength) / smooth, -1f, 1f);

        vec.setLength(unit.speed() * length);
        if(length < -0.5f){
            vec.rotate(180f);
        }else if(length < 0){
            vec.setZero();
        }

        unit.moveAt(vec);
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
