package mindustry.world.blocks.defense.turrets;

import arc.math.Mathf;
import arc.math.geom.Vec2;
import mindustry.entities.Predict;
import mindustry.entities.type.Bullet;
import mindustry.entities.bullet.BulletType;
import mindustry.world.Tile;

import static mindustry.Vars.tilesize;

/**
 * Artillery turrets have special shooting calculations done to hit targets.
 */
public class ArtilleryTurret extends ItemTurret{
    public float velocityInaccuracy = 0f;

    public ArtilleryTurret(String name){
        super(name);
        targetAir = false;
    }

    @Override
    protected void shoot(Tile tile, BulletType ammo){
        TurretEntity entity = tile.ent();

        entity.recoil = recoil;
        entity.heat = 1f;

        BulletType type = peekAmmo(tile);

        tr.trns(entity.rotation, size * tilesize / 2);

        Vec2 predict = Predict.intercept(tile, entity.target, type.speed);

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
