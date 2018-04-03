package io.anuke.mindustry.world.blocks.types.defense.turrets;

import io.anuke.mindustry.graphics.fx.BulletFx;
import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;

public class DoubleTurret extends Turret {
    protected float shotWidth = 2f;

    public DoubleTurret(String name) {
        super(name);
        shots = 2;
    }

    @Override
    protected void shoot(Tile tile, AmmoType ammo){
        TurretEntity entity = tile.entity();

        entity.recoil = recoil;

        for (int i : Mathf.signs) {
            for(int j = 0; j < 3; j ++){
                tr.trns(entity.rotation - 90, shotWidth * i, size * tilesize / 2 + j);
                Effects.effect(BulletFx.smokeParticleSmall, tile.drawx() + tr.x + Mathf.range(1f), tile.drawy() + tr.y + Mathf.range(1f));
            }

            tr.trns(entity.rotation - 90, shotWidth * i, size * tilesize / 2);
            bullet(tile, ammo.bullet, entity.rotation + Mathf.range(inaccuracy));

            Effects.effect(shootEffect, tile.drawx() + tr.x,
                    tile.drawy() + tr.y, entity.rotation);
        }

        if (shootShake > 0) {
            Effects.shake(shootShake, shootShake, tile.entity);
        }
    }
}
