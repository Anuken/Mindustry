package io.anuke.mindustry.entities.bullet;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.impl.BaseBulletType;

public abstract class BulletType extends BaseBulletType<Bullet> implements Content{
    private static int lastid = 0;
    private static Array<BulletType> types = new Array<>();

    public final int id;
    /**Knockback in velocity.*/
    public float knockback;
    /**Whether this bullet hits tiles.*/
    public boolean hitTiles = true;
    /**Status effect applied on hit.*/
    public StatusEffect status = StatusEffects.none;
    /**Intensity of applied status effect in terms of duration.*/
    public float statusIntensity = 0.5f;
    /**What fraction of armor is pierced, 0-1*/
    public float armorPierce = 0f;
    /**Whether to sync this bullet to clients.*/
    public boolean syncable;
    /**Whether this bullet type collides with tiles.*/
    public boolean collidesTiles = true;
    /**Whether this bullet type collides with tiles that are of the same team.*/
    public boolean collidesTeam = false;
    /**Whether this bullet types collides with anything at all.*/
    public boolean collides = true;
    /**Whether velocity is inherited from the shooter.*/
    public boolean keepVelocity = true;

    public BulletType(float speed, float damage){
        this.id = lastid++;
        this.speed = speed;
        this.damage = damage;
        lifetime = 40f;
        hiteffect = BulletFx.hitBulletSmall;
        despawneffect = BulletFx.hitBulletSmall;

        types.add(this);
    }

    public static BulletType getByID(int id){
        return types.get(id);
    }

    public static Array<BulletType> all(){
        return types;
    }

    public void hitTile(Bullet b, Tile tile){
        hit(b);
    }

    @Override
    public void hit(Bullet b, float hitx, float hity){
        Effects.effect(hiteffect, hitx, hity, b.angle());
    }

    @Override
    public void despawned(Bullet b){
        Effects.effect(despawneffect, b.x, b.y, b.angle());
    }

    @Override
    public String getContentTypeName(){
        return "bullettype";
    }

    @Override
    public Array<? extends Content> getAll(){
        return types;
    }
}
