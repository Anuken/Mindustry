package io.anuke.mindustry.entities.units.types;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.entities.units.FlyingUnit;
import io.anuke.mindustry.entities.units.UnitCommand;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class AlphaDrone extends FlyingUnit {
    static final float followDistance = 80f;

    public Unit leader;
    public final UnitState attack = new UnitState() {
        @Override
        public void update() {
            if(leader == null || leader.isDead()){
                damage(99999f);
                return;
            }
            TargetTrait last = target;
            target = leader;
            if(last == null){
                circle(50f);
            }
            target = last;
            if(distanceTo(leader) < followDistance){
                targetClosest();
            }else{
                target = null;
            }
            if(target != null){
                attack(50f);

                if((Mathf.angNear(angleTo(target), rotation, 15f) && distanceTo(target) < getWeapon().getAmmo().getRange())){
                    AmmoType ammo = getWeapon().getAmmo();

                    Vector2 to = Predict.intercept(AlphaDrone.this, target, ammo.bullet.speed);
                    getWeapon().update(AlphaDrone.this, to.x, to.y);
                }
            }
        }
    };

    @Override
    public void onCommand(UnitCommand command){
        //nuh
    }

    @Override
    public void behavior(){
        //nope
    }

    @Override
    public UnitState getStartState() {
        return attack;
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException {
        super.writeSave(stream);
    }

    @Override
    public void readSave(DataInput stream) throws IOException {
        super.readSave(stream);
    }
}
