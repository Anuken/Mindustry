package mindustry.world.blocks.defense.turrets;

import arc.util.ArcAnnotate.*;
import mindustry.entities.bullet.BulletType;
import mindustry.world.Tile;
import mindustry.world.meta.BlockStat;
import mindustry.world.meta.StatUnit;

public class PowerTurret extends CooledTurret{
    public @NonNull BulletType shootType;
    public float powerUse = 1f;

    public PowerTurret(String name){
        super(name);
        hasPower = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.damage, shootType.damage, StatUnit.none);
    }

    @Override
    public void init(){
        consumes.powerCond(powerUse, entity -> ((TurretEntity)entity).target != null);
        super.init();
    }

    @Override
    public BulletType useAmmo(Tile tile){
        //nothing used directly
        return shootType;
    }

    @Override
    public boolean hasAmmo(Tile tile){
        //you can always rotate, but never shoot if there's no power
        return true;
    }

    @Override
    public BulletType peekAmmo(Tile tile){
        return shootType;
    }

    @Override
    protected float baseReloadSpeed(Tile tile){
        return tile.isEnemyCheat() ? 1f : tile.entity.power.status;
    }
}
