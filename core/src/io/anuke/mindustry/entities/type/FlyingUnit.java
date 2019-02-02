package io.anuke.mindustry.entities.type;

import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;

import static io.anuke.mindustry.Vars.world;

public abstract class FlyingUnit extends BaseUnit{
    protected static Vector2 vec = new Vector2();

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
                attack(150f);

                if((Angles.near(angleTo(target), rotation, 15f) || !getWeapon().getAmmo().keepVelocity) //bombers don't care about rotation
                && dst(target) < Math.max(getWeapon().getAmmo().range(), type.range)){
                    BulletType ammo = getWeapon().getAmmo();

                    Vector2 to = Predict.intercept(FlyingUnit.this, target, ammo.speed);

                    getWeapon().update(FlyingUnit.this, to.x, to.y);
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
    public void draw(){
        Draw.alpha(Draw.getShader() != Shaders.mix ? 1f : hitTime / hitDuration);

        Draw.rect(type.name, x, y, rotation - 90);

        drawItems();

        Draw.alpha(1f);
    }

    @Override
    public void behavior(){
        if(health <= health * type.retreatPercent &&
         Geometry.findClosest(x, y, world.indexer.getAllied(team, BlockFlag.repair)) != null){
            setState(retreat);
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

        x += Mathf.sin(Time.time() + id * 999, 25f, 0.08f)*Time.delta();
        y += Mathf.cos(Time.time() + id * 999, 25f, 0.08f)*Time.delta();

        if(velocity.len() <= 0.05f){
            rotation += Mathf.sin(Time.time() + id * 99, 10f, 2.5f)*Time.delta();
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

        vec.setLength(speed * Time.delta());

        velocity.add(vec);
    }

    protected void moveTo(float circleLength){
        if(target == null) return;

        vec.set(target.getX() - x, target.getY() - y);

        float length = circleLength <= 0.001f ? 1f : Mathf.clamp((dst(target) - circleLength) / 100f, -1f, 1f);

        vec.setLength(type.speed * Time.delta() * length);
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

        vec.setLength(type.speed * Time.delta());

        velocity.add(vec);
    }
}
