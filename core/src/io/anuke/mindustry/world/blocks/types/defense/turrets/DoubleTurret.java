package io.anuke.mindustry.world.blocks.types.defense.turrets;

import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
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
        entity.shots ++;

        int i = Mathf.signs[entity.shots % 2];

        tr.trns(entity.rotation - 90, shotWidth * i, size * tilesize / 2);
        bullet(tile, ammo.bullet, entity.rotation + Mathf.range(inaccuracy));

        useAmmo(tile);
        effects(tile);
    }
}
