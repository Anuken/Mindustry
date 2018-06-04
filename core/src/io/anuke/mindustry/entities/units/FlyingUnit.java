package io.anuke.mindustry.entities.units;

import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Trail;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.world.BlockFlag;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.world;

public class FlyingUnit extends BaseUnit {
    protected static Translator vec = new Translator();
    protected static float maxAim = 30f;
    protected static float wobblyness = 0.6f;

    protected Trail trail = new Trail(16);

    public FlyingUnit(UnitType type, Team team) {
        super(type, team);
    }

    @Override
    public void update() {
        super.update();

        rotation = velocity.angle();
        trail.update(x + Angles.trnsx(rotation + 180f, 6f) + Mathf.range(wobblyness),
                y + Angles.trnsy(rotation + 180f, 6f) + Mathf.range(wobblyness));
    }

    @Override
    public void draw() {
        Draw.alpha(hitTime / hitDuration);

        Draw.rect(type.name, x, y, rotation - 90);

        Draw.alpha(1f);
    }

    @Override
    public void drawOver() {
        trail.draw(Palette.lightFlame, Palette.lightOrange, 5f);
    }

    @Override
    public void behavior() {
        if(health <= health * type.retreatPercent && !isWave &&
                Geometry.findClosest(x, y, world.indexer().getAllied(team, BlockFlag.repair)) != null){
            setState(retreat);
        }
    }


    @Override
    public UnitState getStartState(){
        return attack;
    }

    @Override
    public float drawSize() {
        return 60;
    }

    protected void circle(float circleLength){
        vec.set(target.getX() - x, target.getY() - y);

        if(vec.len() < circleLength){
            vec.rotate((circleLength-vec.len())/circleLength * 180f);
        }

        vec.setLength(type.speed * Timers.delta());

        velocity.add(vec);
    }

    protected void attack(float circleLength){
        vec.set(target.getX() - x, target.getY() - y);

        float ang = angleTo(target);
        float diff = Angles.angleDist(ang, rotation);

        if(diff > 100f && vec.len() < circleLength){
            vec.setAngle(velocity.angle());
        }else{
            vec.setAngle(Mathf.slerpDelta(velocity.angle(), vec.angle(),  0.44f));
        }

        vec.setLength(type.speed*Timers.delta());

        velocity.add(vec);
    }

    public final UnitState

    resupply = new UnitState(){
        public void entered() {
            target = null;
        }

        public void update() {
            if(inventory.totalAmmo() + 10 >= inventory.ammoCapacity()){
                state.set(attack);
            }else if(!targetHasFlag(BlockFlag.resupplyPoint)){
                retarget(() -> targetClosestAllyFlag(BlockFlag.resupplyPoint));
            }else{
                circle(20f);
            }
        }
    },
    idle = new UnitState() {
        public void update() {
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
        public void entered() {
            target = null;
        }

        public void update() {
            if(Units.invalidateTarget(target, team, x, y)){
                target = null;
            }

            if(!inventory.hasAmmo()) {
                state.set(resupply);
            }else if (target == null){
                retarget(() -> {
                    targetClosest();
                    targetClosestEnemyFlag(BlockFlag.target);
                    targetClosestEnemyFlag(BlockFlag.producer);

                    if(target == null){
                        setState(idle);
                    }
                });
            }else{
                attack(150f);

                if (timer.get(timerReload, type.reload) && Mathf.angNear(angleTo(target), rotation, 13f)
                        && distanceTo(target) < inventory.getAmmo().getRange()) {
                    AmmoType ammo = inventory.getAmmo();
                    inventory.useAmmo();

                    shoot(ammo, Angles.moveToward(rotation, angleTo(target), maxAim), 4f);
                }
            }
        }
    },
    retreat = new UnitState() {
        public void entered() {
            target = null;
        }

        public void update() {
            if(health >= maxHealth()){
                state.set(attack);
            }else if(!targetHasFlag(BlockFlag.repair)){
                retarget(() -> {
                    Tile target = Geometry.findClosest(x, y, world.indexer().getAllied(team, BlockFlag.repair));
                    if (target != null) FlyingUnit.this.target = target.entity;
                });
            }else{
                circle(20f);
            }
        }
    };
}
