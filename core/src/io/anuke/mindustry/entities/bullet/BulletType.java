package io.anuke.mindustry.entities.bullet;

import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.entities.impl.BaseBulletType;
import io.anuke.ucore.util.Translator;

public abstract class BulletType extends Content implements BaseBulletType<Bullet>{
    public float lifetime;
    public float speed;
    public float damage;
    public float hitsize = 4;
    public float drawSize = 20f;
    public float drag = 0f;
    public boolean pierce;
    public Effect hiteffect, despawneffect;

    public float splashDamage = 0f;
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
    /**Whether this bullet type collides with air units.*/
    public boolean collidesAir = true;
    /**Whether this bullet types collides with anything at all.*/
    public boolean collides = true;
    /**Whether velocity is inherited from the shooter.*/
    public boolean keepVelocity = true;

    protected Translator vector = new Translator();

    public BulletType(float speed, float damage){
        this.speed = speed;
        this.damage = damage;
        lifetime = 40f;
        hiteffect = BulletFx.hitBulletSmall;
        despawneffect = BulletFx.hitBulletSmall;
    }

    public boolean collides(Bullet bullet, Tile tile){
        return true;
    }

    public void hitTile(Bullet b, Tile tile){
        hit(b);
    }

    @Override
    public float drawSize(){
        return 40;
    }

    @Override
    public float lifetime(){
        return lifetime;
    }

    @Override
    public float speed(){
        return speed;
    }

    @Override
    public float damage(){
        return damage;
    }

    @Override
    public float hitSize(){
        return hitsize;
    }

    @Override
    public float drag(){
        return drag;
    }

    @Override
    public boolean pierce(){
        return pierce;
    }

    @Override
    public Effect hitEffect(){
        return hiteffect;
    }

    @Override
    public Effect despawnEffect(){
        return despawneffect;
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
    public ContentType getContentType(){
        return ContentType.bullet;
    }
}
