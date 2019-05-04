package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.world.Tile;

public abstract class PowerTurret extends CooledTurret{
    protected BulletType shootType;
    protected float powerUse = 1f;

    public PowerTurret(String name){
        super(name);
        hasPower = true;
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
        //only shoot if there's power
        return tile.entity.cons.valid();
    }

    @Override
    public BulletType peekAmmo(Tile tile){
        return shootType;
    }

    @Override
    protected float baseReloadSpeed(Tile tile){
        return tile.entity.power.satisfaction;
    }
}
