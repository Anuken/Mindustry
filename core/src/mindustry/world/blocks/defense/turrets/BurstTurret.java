package mindustry.world.blocks.defense.turrets;

import arc.math.Mathf;
import arc.util.Time;
import mindustry.entities.bullet.BulletType;
import mindustry.world.Tile;

import static mindustry.Vars.tilesize;

public class BurstTurret extends ItemTurret{
    public float burstSpacing = 5;

    public BurstTurret(String name){
        super(name);
    }

    @Override
    protected void shoot(Tile tile, BulletType ammo){
        TurretEntity entity = tile.ent();

        entity.heat = 1f;

        for(int i = 0; i < shots; i++){
            Time.run(burstSpacing * i, () -> {
                if(!(tile.entity instanceof TurretEntity) ||
                !hasAmmo(tile)) return;

                entity.recoil = recoil;

                tr.trns(entity.rotation, size * tilesize / 2, Mathf.range(xRand));
                bullet(tile, ammo, entity.rotation + Mathf.range(inaccuracy));
                effects(tile);
                useAmmo(tile);
            });
        }
    }
}
