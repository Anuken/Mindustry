package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import mindustry.entities.bullet.*;

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

    public class ArtilleryTurretEntity extends ItemTurretEntity{
        @Override
        protected void shoot(BulletType ammo){
            recoil = recoilAmount;
            heat = 1f;

            BulletType type = peekAmmo();

            tr.trns(rotation, size * tilesize / 2);

            float dst = dst(targetPos.x, targetPos.y);
            float maxTraveled = type.lifetime * type.speed;

            for(int i = 0; i < shots; i++){
                ammo.create(tile.entity, team, x + tr.x, y + tr.y,
                rotation + Mathf.range(inaccuracy + type.inaccuracy), 1f + Mathf.range(velocityInaccuracy), (dst / maxTraveled));
            }

            effects();
            useAmmo();
        }
    }
}
