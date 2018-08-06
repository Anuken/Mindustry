package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.CarriableTrait;
import io.anuke.mindustry.entities.traits.CarryTrait;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Trail;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.world;

public abstract class FlyingUnit extends BaseUnit implements CarryTrait{
    protected static Translator vec = new Translator();
    protected static float wobblyness = 0.6f;

    protected Trail trail = new Trail(8);
    protected CarriableTrait carrying;
    protected final UnitState

    resupply = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            if(inventory.totalAmmo() + 10 >= inventory.ammoCapacity()){
                state.set(attack);
            }else if(!targetHasFlag(BlockFlag.resupplyPoint)){
                retarget(() -> targetClosestAllyFlag(BlockFlag.resupplyPoint));
            }else{
                circle(20f);
            }
        }
    },

    idle = new UnitState(){
        public void update(){
            if(!isCommanded()){
                retarget(() -> {
                    targetClosest();
                    targetClosestEnemyFlag(BlockFlag.target);

                    if(target != null){
                        setState(attack);
                    }
                });
            }

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

            if(!inventory.hasAmmo()){
                state.set(resupply);
            }else if(target == null){
                retarget(() -> {
                    targetClosest();
                    targetClosestEnemyFlag(BlockFlag.target);
                    targetClosestEnemyFlag(BlockFlag.producer);
                    targetClosestEnemyFlag(BlockFlag.turret);

                    if(target == null && !isCommanded()){
                        setState(idle);
                    }
                });
            }else{
                attack(150f);

                if((Mathf.angNear(angleTo(target), rotation, 15f) || !inventory.getAmmo().bullet.keepVelocity) //bombers don't care about rotation
                && distanceTo(target) < inventory.getAmmo().getRange()){
                    AmmoType ammo = inventory.getAmmo();
                    inventory.useAmmo();

                    Vector2 to = Predict.intercept(FlyingUnit.this, target, ammo.bullet.speed);

                    getWeapon().update(FlyingUnit.this, to.x, to.y);
                }
            }
        }
    },
    retreat = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            if(health >= maxHealth() && !isCommanded()){
                state.set(attack);
            }else if(!targetHasFlag(BlockFlag.repair)){
                retarget(() -> {
                    Tile target = Geometry.findClosest(x, y, world.indexer().getAllied(team, BlockFlag.repair));
                    if(target != null) FlyingUnit.this.target = target.entity;
                });
            }else{
                circle(20f);
            }
        }
    };

    //instantiation only
    public FlyingUnit(){

    }

    @Override
    public void onCommand(UnitCommand command){
        state.set(command == UnitCommand.retreat ? retreat :
                 (command == UnitCommand.attack ? attack :
                 (null)));
    }

    @Override
    public CarriableTrait getCarry(){
        return carrying;
    }

    @Override
    public void setCarry(CarriableTrait unit){
        this.carrying = unit;
    }

    @Override
    public float getCarryWeight(){
        return type.carryWeight;
    }

    @Override
    public void update(){
        super.update();

        updateRotation();
        trail.update(x + Angles.trnsx(rotation + 180f, 6f) + Mathf.range(wobblyness),
        y + Angles.trnsy(rotation + 180f, 6f) + Mathf.range(wobblyness));

        wobble();
    }

    @Override
    public void draw(){
        Draw.alpha(hitTime / hitDuration);

        Draw.rect(type.name, x, y, rotation - 90);

        drawItems();

        Draw.alpha(1f);
    }

    @Override
    public void drawOver(){
        trail.draw(Palette.lightTrail, 5f);
    }

    @Override
    public void behavior(){
        if(health <= health * type.retreatPercent && !isCommanded() &&
         Geometry.findClosest(x, y, world.indexer().getAllied(team, BlockFlag.repair)) != null){
            setState(retreat);
        }

        if(squad != null){
            squad.direction.add(velocity.x / squad.units, velocity.y / squad.units);
            velocity.setAngle(Mathf.slerpDelta(velocity.angle(), squad.direction.angle(), 0.3f));
        }
    }

    @Override
    public UnitState getStartState(){
        return attack;
    }

    @Override
    public float drawSize(){
        return 60;
    }

    protected void wobble(){
        if(Net.client()) return;

        x += Mathf.sin(Timers.time() + id * 999, 25f, 0.07f);
        y += Mathf.cos(Timers.time() + id * 999, 25f, 0.07f);

        if(velocity.len() <= 0.2f){
            rotation += Mathf.sin(Timers.time() + id * 99, 10f, 8f);
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

        vec.set(target.getX() - x, target.getY() - y);

        if(vec.len() < circleLength){
            vec.rotate((circleLength - vec.len()) / circleLength * 180f);
        }

        vec.setLength(speed * Timers.delta());

        velocity.add(vec);
    }

    protected void moveTo(float circleLength){
        if(target == null) return;

        vec.set(target.getX() - x, target.getY() - y);

        float length = Mathf.clamp((distanceTo(target) - circleLength) / 100f, -1f, 1f);

        vec.setLength(type.speed * Timers.delta() * length);
        if(length < 0) vec.rotate(180f);

        velocity.add(vec);
    }

    protected void attack(float circleLength){
        vec.set(target.getX() - x, target.getY() - y);

        float ang = angleTo(target);
        float diff = Angles.angleDist(ang, rotation);

        if(diff > 100f && vec.len() < circleLength){
            vec.setAngle(velocity.angle());
        }else{
            vec.setAngle(Mathf.slerpDelta(velocity.angle(), vec.angle(), 0.44f));
        }

        vec.setLength(type.speed * Timers.delta());

        velocity.add(vec);
    }
}
