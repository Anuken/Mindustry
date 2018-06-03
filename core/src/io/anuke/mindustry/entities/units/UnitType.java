package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.content.fx.ExplosionFx;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Item;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public abstract class UnitType {
    private static byte lastid = 0;
    private static Array<UnitType> types = new Array<>();

    private static int timerIndex = 0;

    protected static final int timerTarget = timerIndex++;
    protected static final int timerBoost = timerIndex++;
    protected static final int timerReload = timerIndex++;

    public final String name;
    public final byte id;

    protected float health = 60;
    protected float hitsize = 5f;
    protected float hitsizeTile = 4f;
    protected float speed = 0.4f;
    protected float range = 160;
    protected float rotatespeed = 0.1f;
    protected float baseRotateSpeed = 0.1f;
    protected float mass = 1f;
    protected boolean isFlying;
    protected float drag = 0.1f;
    protected float maxVelocity = 5f;
    protected float reload = 40f;
    protected float retreatPercent = 0.2f;
    protected float armor = 0f;
    protected ObjectMap<Item, AmmoType> ammo = new ObjectMap<>();

    public UnitType(String name){
        this.id = lastid++;
        this.name = name;
        types.add(this);
    }

    protected void setAmmo(AmmoType... types){
        for(AmmoType type : types){
            ammo.put(type.item, type);
        }
    }

    public abstract void draw(BaseUnit unit);

    public void drawUnder(BaseUnit unit){}

    public void drawOver(BaseUnit unit){}

    public UnitState getStartState(){
        return null;
    }

    public boolean isFlying(){
        return isFlying;
    }

    public void update(BaseUnit unit){
        if(unit.hitTime > 0){
            unit.hitTime -= Timers.delta();
        }

        if(unit.hitTime < 0) unit.hitTime = 0;

        if(Net.client()){
            unit.interpolate();
            return;
        }

        updateTargeting(unit);

        unit.state.update(unit);
        unit.updateVelocityStatus(drag, maxVelocity);

        if(unit.target != null) behavior(unit);

        unit.x = Mathf.clamp(unit.x, 0, world.width() * tilesize);
        unit.y = Mathf.clamp(unit.y, 0, world.height() * tilesize);
    }

    /**Only runs when the unit has a target.*/
    public abstract void behavior(BaseUnit unit);

    public void updateTargeting(BaseUnit unit){
        if(unit.target == null || (unit.target instanceof Unit && (unit.target.isDead() || ((Unit)unit.target).team == unit.team))
                || (unit.target instanceof TileEntity && ((TileEntity) unit.target).tile.entity == null)){
            unit.target = null;
        }
    }

    public void shoot(BaseUnit unit, AmmoType type, float rotation, float translation){
        Bullet.create(type.bullet, unit,
                unit.x + Angles.trnsx(rotation, translation),
                unit.y + Angles.trnsy(rotation, translation), rotation);
        Effects.effect(type.shootEffect, unit.x + Angles.trnsx(rotation, translation),
                unit.y + Angles.trnsy(rotation, translation), rotation, unit);
        Effects.effect(type.smokeEffect, unit.x + Angles.trnsx(rotation, translation),
                unit.y + Angles.trnsy(rotation, translation), rotation, unit);
    }

    public void onDeath(BaseUnit unit){
        //TODO other things, such as enemies?
        Effects.effect(ExplosionFx.explosion, unit);
        Effects.shake(2f, 2f, unit);

        if(Net.server()){
            NetEvents.handleUnitDeath(unit);
        }

        unit.remove();
    }

    public void onRemoteDeath(BaseUnit unit){
        onDeath(unit);
    }

    public void removed(BaseUnit unit){
        //TODO
    }

    public static UnitType getByID(byte id){
        return types.get(id);
    }

    public static Array<UnitType> getAllTypes(){
        return types;
    }
}
