package io.anuke.mindustry.world.blocks.types.defense;

import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;

public abstract class LiquidTurret extends Turret{
    public Liquid ammoLiquid = Liquids.water;
    public float liquidCapacity = 60f;
    public float liquidPerShot = 1f;

    public LiquidTurret(String name) {
        super(name);
        hasLiquids = true;
    }

    @Override
    public boolean hasAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        return entity.liquid.amount > liquidPerShot;
    }

    @Override
    public void consumeAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        entity.liquid.amount -= liquidPerShot;
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return ammoLiquid == liquid && super.acceptLiquid(tile, source, liquid, amount);
    }


}
