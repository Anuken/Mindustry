package io.anuke.mindustry.entities.bullet;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class LiquidBulletType extends BulletType{
    Liquid liquid;

    public LiquidBulletType(Liquid liquid){
        super(2.5f, 0);
        this.liquid = liquid;

        lifetime = 70f;
        despawneffect = Fx.none;
        hiteffect = BulletFx.hitLiquid;
        drag = 0.01f;
        knockback = 0.5f;
    }

    @Override
    public void update(Bullet b) {
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
        Effects.effect(hiteffect, liquid.color, hitx, hity);
        Puddle.deposit(world.tileWorld(hitx, hity), liquid, 5f);

        if(liquid.temperature <= 0.5f && liquid.flammability < 0.3f){
            float intensity = 400f;
            Fire.extinguish(world.tileWorld(hitx, hity), intensity);
            for(GridPoint2 p : Geometry.d4){
                Fire.extinguish(world.tileWorld(hitx + p.x * tilesize, hity + p.y * tilesize), intensity);
            }
        }
    }
}
