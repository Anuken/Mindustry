package io.anuke.mindustry.world.blocks.types.defense.turrets;

import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;

public class BurstTurret extends ItemTurret {
    protected float burstSpacing = 5;

    public BurstTurret(String name) {
        super(name);
    }

    @Override
    protected void shoot(Tile tile, AmmoType ammo){
        TurretEntity entity = tile.entity();

        entity.heat = 1f;

        for (int i = 0; i < shots; i++) {
            Timers.run(burstSpacing * i, () -> {
                if(!(tile.entity instanceof TurretEntity) ||
                        !hasAmmo(tile)) return;

                entity.recoil = recoil;

                tr.trns(entity.rotation, size * tilesize / 2);
                useAmmo(tile);
                bullet(tile, ammo.bullet, entity.rotation + Mathf.range(inaccuracy));
                effects(tile);
            });
        }
    }
}
