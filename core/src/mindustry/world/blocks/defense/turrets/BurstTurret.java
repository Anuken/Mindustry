package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import arc.util.*;
import mindustry.entities.bullet.*;

import static mindustry.Vars.tilesize;

public class BurstTurret extends ItemTurret{
    public float burstSpacing = 5;

    public BurstTurret(String name){
        super(name);
    }

    public class BurstTurretEntity extends ItemTurretEntity{

        @Override
        protected void shoot(BulletType ammo){
            heat = 1f;

            for(int i = 0; i < shots; i++){
                Time.run(burstSpacing * i, () -> {
                    if(!(tile.entity instanceof TurretEntity) || !hasAmmo()) return;

                    recoil = recoilAmount;

                    tr.trns(rotation, size * tilesize / 2, Mathf.range(xRand));
                    bullet(ammo, rotation + Mathf.range(inaccuracy));
                    effects();
                    useAmmo();
                });
            }
        }
    }
}
