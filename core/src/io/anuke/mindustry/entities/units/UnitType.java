package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public abstract class UnitType {
    private static byte lastid = 0;
    private static Array<UnitType> types = new Array<>();

    private static int timerIndex = 0;

    protected static final int timerTarget = timerIndex++;

    public final String name;
    public final byte id;

    protected int health = 60;
    protected float hitsize = 5f;
    protected float hitsizeTile = 4f;
    protected float speed = 0.4f;
    protected float range = 160;
    protected float rotatespeed = 0.1f;
    protected float baseRotateSpeed = 0.1f;
    protected float mass = 1f;
    protected boolean isFlying;
    protected float drag = 0.1f;

    public UnitType(String name){
        this.id = lastid++;
        this.name = name;
        types.add(this);
    }

    public abstract void draw(BaseUnit unit);

    public void drawOver(BaseUnit unit){
        //TODO doesn't do anything
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

        //TODO logic

        unit.x += unit.velocity.x / mass;
        unit.y += unit.velocity.y / mass;

        unit.velocity.scl(Mathf.clamp(1f-drag* Timers.delta()));

        if(unit.target != null) behavior(unit);

        unit.x = Mathf.clamp(unit.x, 0, world.width() * tilesize);
        unit.y = Mathf.clamp(unit.y, 0, world.height() * tilesize);
    }

    /**Only runs when the unit has a target.*/
    public abstract void behavior(BaseUnit unit);

    public void updateTargeting(BaseUnit unit){
        if(unit.target == null || unit.target.isDead()){
            unit.target = null;
        }

        if(unit.timer.get(timerTarget, 30)){
            unit.target = Units.getClosestEnemy(unit.team, unit.x, unit.y, range, e -> true);
        }
    }

    public void onShoot(BaseUnit unit, BulletType type, float rotation){
        //TODO remove?
    }

    public void onDeath(BaseUnit unit){
        //TODO other things, such as enemies?
        Effects.effect(Fx.explosion, unit);

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
}
