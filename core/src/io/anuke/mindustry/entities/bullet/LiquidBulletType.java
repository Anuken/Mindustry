package io.anuke.mindustry.entities.bullet;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Fill;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Point2;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class LiquidBulletType extends BulletType{
    Liquid liquid;

    public LiquidBulletType(Liquid liquid){
        super(3.5f, 0);
        this.liquid = liquid;

        lifetime = 74f;
        status = liquid.effect;
        statusDuration = 90f;
        despawnEffect = Fx.none;
        hitEffect = Fx.hitLiquid;
        smokeEffect = Fx.none;
        shootEffect = Fx.none;
        drag = 0.009f;
        knockback = 0.55f;
    }

    @Override
    public float range(){
        return speed * lifetime / 2f;
    }

    @Override
    public void update(Bullet b){
        super.update(b);

        if(liquid.canExtinguish()){
            Tile tile = world.tileWorld(b.x, b.y);
            if(tile != null && Fire.has(tile.x, tile.y)){
                Fire.extinguish(tile, 100f);
                b.remove();
                hit(b);
            }
        }
    }

    @Override
    public void draw(Bullet b){
        Draw.color(liquid.color, Color.WHITE, b.fout() / 100f + Mathf.randomSeedRange(b.id, 0.1f));

        Fill.circle(b.x, b.y, 0.5f + b.fout() * 2.5f);
    }

    @Override
    public void hit(Bullet b, float hitx, float hity){
        Effects.effect(hitEffect, liquid.color, hitx, hity);
        Puddle.deposit(world.tileWorld(hitx, hity), liquid, 5f);

        if(liquid.temperature <= 0.5f && liquid.flammability < 0.3f){
            float intensity = 400f;
            Fire.extinguish(world.tileWorld(hitx, hity), intensity);
            for(Point2 p : Geometry.d4){
                Fire.extinguish(world.tileWorld(hitx + p.x * tilesize, hity + p.y * tilesize), intensity);
            }
        }
    }
}
