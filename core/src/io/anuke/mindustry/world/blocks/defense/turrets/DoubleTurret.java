package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import static io.anuke.mindustry.Vars.tilesize;

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
        stats.add(BlockStat.reload, 60f / reload, StatUnit.none);
    }

    @Override
    protected void shoot(Tile tile, BulletType ammo){
        TurretEntity entity = tile.entity();
        entity.shots++;

        int i = Mathf.signs[entity.shots % 2];

        tr.trns(entity.rotation - 90, shotWidth * i, size * tilesize / 2);
        bullet(tile, ammo, entity.rotation + Mathf.range(inaccuracy));

        effects(tile);
        useAmmo(tile);
    }
}
