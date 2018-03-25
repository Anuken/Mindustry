package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.modules.InventoryModule;
import io.anuke.ucore.core.Timers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class UnitFactory extends Block {
    protected UnitType type;
    protected ItemStack[] requirements;
    protected float produceTime = 1000f;
    protected float powerUse = 0.1f;

    public UnitFactory(String name) {
        super(name);
        solid = true;
        update = true;
        hasPower = true;
    }

    @Override
    public void update(Tile tile) {
        UnitFactoryEntity entity = tile.entity();

        float used = Math.min(powerUse * Timers.delta(), powerCapacity);

        if(hasRequirements(entity.inventory, entity.buildTime/produceTime) &&
                entity.power.amount >= used){

            entity.buildTime += Timers.delta();
            entity.power.amount -= used;

            if(entity.buildTime >= produceTime){
                BaseUnit unit = new BaseUnit(type, tile.getTeam());
                unit.set(tile.drawx(), tile.drawy());
                entity.buildTime = 0f;

                for(ItemStack stack : requirements){
                    entity.inventory.removeItem(stack.item, stack.amount);
                }
            }
        }
    }

    @Override
    public TileEntity getEntity() {
        return new UnitFactoryEntity();
    }

    protected boolean hasRequirements(InventoryModule inv, float fraction){
        for(ItemStack stack : requirements){
            if(!inv.hasItem(stack.item, (int)(fraction * stack.amount))){
                return false;
            }
        }
        return true;
    }

    public static class UnitFactoryEntity extends TileEntity{
        public float buildTime;

        @Override
        public void write(DataOutputStream stream) throws IOException {
            stream.writeFloat(buildTime);
        }

        @Override
        public void read(DataInputStream stream) throws IOException {
            buildTime = stream.readFloat();
        }
    }
}
