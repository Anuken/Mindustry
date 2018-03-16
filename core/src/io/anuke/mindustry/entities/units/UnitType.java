package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class UnitType {
    private static byte lastid = 0;
    private static Array<UnitType> types = new Array<>();

    public final String name;
    public final byte id;

    protected int health = 60;
    protected float hitsize = 5f;
    protected float hitsizeTile = 4f;
    protected float speed = 0.4f;
    protected float range = 60;
    protected float rotatespeed = 0.1f;
    protected float mass = 1f;
    protected boolean isFlying;

    public UnitType(String name){
        this.id = lastid++;
        this.name = name;
        types.add(this);
    }

    public void draw(BaseUnit enemy){
        //TODO
    }

    public void drawOver(BaseUnit enemy){
        //TODO
    }

    public void update(BaseUnit enemy){
        //TODO
        enemy.x = Mathf.clamp(enemy.x, 0, world.width() * tilesize);
        enemy.y = Mathf.clamp(enemy.y, 0, world.height() * tilesize);
    }

    public void move(BaseUnit enemy){
        //TODO
    }

    public void behavior(BaseUnit enemy){
        //TODO
    }

    public void updateTargeting(BaseUnit enemy){
        //TODO
    }

    public void updateShooting(BaseUnit enemy){
        //TODO
    }

    public void shoot(BaseUnit enemy){
        //TODO
    }

    public void onShoot(BaseUnit enemy, BulletType type, float rotation){
        //TODO
    }

    public void onDeath(BaseUnit enemy){
        //TODO
    }

    public void onRemoteDeath(BaseUnit enemy){
        //TODO
    }

    public void removed(BaseUnit enemy){
        //TODO
    }

    public static UnitType getByID(byte id){
        return types.get(id);
    }
}
