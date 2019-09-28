package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.tilesize;

/**
 * Artillery turrets have special shooting calculations done to hit targets.
 */
public class ArtilleryTurret extends ItemTurret{
    protected float velocityInaccuracy = 0f;

    public ArtilleryTurret(String name){
        super(name);
        targetAir = false;
    }

    @Override
    protected void shoot(Tile tile, BulletType ammo){
        TurretEntity entity = tile.entity();

        entity.recoil = recoil;
        entity.heat = 1f;

        BulletType type = peekAmmo(tile);

        tr.trns(entity.rotation, size * tilesize / 2);

        Vector2 predict = Predict.intercept(tile, entity.target, type.speed);

        float dst = entity.dst(predict.x, predict.y);
        float maxTraveled = type.lifetime * type.speed;

        for(int i = 0; i < shots; i++){
            Bullet.create(ammo, tile.entity, tile.getTeam(), tile.drawx() + tr.x, tile.drawy() + tr.y,
            entity.rotation + Mathf.range(inaccuracy + type.inaccuracy), 1f + Mathf.range(velocityInaccuracy), (dst / maxTraveled));
        }

        effects(tile);
        useAmmo(tile);
    }
}
