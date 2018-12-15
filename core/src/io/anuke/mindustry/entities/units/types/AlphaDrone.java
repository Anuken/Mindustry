package io.anuke.mindustry.entities.units.types;

import com.badlogic.gdx.math.Vector2;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.fx.UnitFx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.FlyingUnit;
import io.anuke.mindustry.entities.units.UnitCommand;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class AlphaDrone extends FlyingUnit {
    static final float followDistance = 80f;

    public Player leader;

    public final UnitState attack = new UnitState() {
        @Override
        public void update() {
            if(leader == null || leader.isDead() || !leader.isAdded()){
                damage(99999f);
                return;
            }
            TargetTrait last = target;
            target = leader;

            if(last == null){
                circle(leader.isShooting ? 60f : 0f);
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

            if(!leader.isShooting && distanceTo(leader) < 7f){
                Call.onAlphaDroneFade(AlphaDrone.this);
            }
        }
    };

    @Remote(called = Loc.server)
    public static void onAlphaDroneFade(BaseUnit drone){
        if(drone == null) return;
        Effects.effect(UnitFx.pickup, drone);
        //must run afterwards so the unit's group is not null when sending the removal packet
        threads.runDelay(drone::remove);
    }

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
    public void write(DataOutput stream) throws IOException {
        super.write(stream);
        stream.writeInt(leader == null ? -1 : leader.id);
    }

    @Override
    public void read(DataInput stream, long time) throws IOException {
        super.read(stream, time);
        leader = Vars.playerGroup.getByID(stream.readInt());
    }

    @Override
    public void readSave(DataInput stream) throws IOException{
        super.readSave(stream);

        if(!Net.active() && !headless){
            leader = players[0];
        }
    }
}
