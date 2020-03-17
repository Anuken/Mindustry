package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import mindustry.entities.bullet.*;
import mindustry.world.meta.*;

import static mindustry.Vars.tilesize;

public class DoubleTurret extends ItemTurret{
    public float shotWidth = 2f;

    public DoubleTurret(String name){
        super(name);
        shots = 2;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.reload);
        stats.add(BlockStat.reload, 60f / reloadTime, StatUnit.none);
    }

    public class DoubleTurretEntity extends ItemTurretEntity{
        @Override
        protected void shoot(BulletType ammo){
            shotCount++;
            heat = 1f;

            int i = Mathf.signs[shotCount % 2];

            tr.trns(rotation - 90, shotWidth * i, size * tilesize / 2);
            bullet(ammo, rotation + Mathf.range(inaccuracy));

            effects();
            useAmmo();
        }
    }
}
