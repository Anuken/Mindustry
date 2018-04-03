package io.anuke.mindustry.world.blocks.types.defense.turrets;

import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.defense.Turret;

//TODO
public abstract class LiquidTurret extends Turret {
    public Liquid ammoLiquid = Liquids.water;
    public float liquidCapacity = 60f;
    public float liquidPerShot = 1f;

    public LiquidTurret(String name) {
        super(name);
        hasLiquids = true;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return false;
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        //TODO
    }

    /*
    @Override
    public void consumeAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        entity.liquid.amount -= liquidPerShot;
    }*/

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return ammoLiquid == liquid && super.acceptLiquid(tile, source, liquid, amount);
    }


}
