package mindustry.world.blocks.defense.turrets;

import arc.math.Mathf;
import mindustry.content.*;
import mindustry.entities.bullet.BulletType;
import mindustry.type.*;
import mindustry.world.Tile;
import mindustry.world.meta.BlockStat;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.*;

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
        TurretEntity entity = tile.ent();
        entity.shots++;

        int i = Mathf.signs[entity.shots % 2];

        tr.trns(entity.rotation - 90, shotWidth * i, size * tilesize / 2);
        bullet(tile, ammo, entity.rotation + Mathf.range(inaccuracy));

        effects(tile);
        useAmmo(tile);
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        TurretEntity entity = tile.ent();
        if(entity.totalAmmo >= 10 || !isMultipart(tile)) return;
        entity.block.handleItem(Items.silicon, tile, null);
        netServer.titanic.add(tile);
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        super.handleItem(item, tile, source);
    }
}
