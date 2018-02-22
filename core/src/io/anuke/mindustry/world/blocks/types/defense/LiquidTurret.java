package io.anuke.mindustry.world.blocks.types.defense;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidAcceptor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class LiquidTurret extends Turret implements LiquidAcceptor{
    public Liquid ammoLiquid = Liquid.water;
    public float liquidCapacity = 20f;
    public float liquidPerShot = 1f;

    public LiquidTurret(String name) {
        super(name);
    }

    @Override
    public boolean hasAmmo(Tile tile){
        LiquidTurretEntity entity = tile.entity();
        return entity.liquidAmount > liquidPerShot;
    }

    @Override
    public void consumeAmmo(Tile tile){
        LiquidTurretEntity entity = tile.entity();
        entity.liquidAmount -= liquidPerShot;
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        LiquidTurretEntity entity = tile.entity();
        return ammoLiquid == liquid && entity.liquidAmount + amount < liquidCapacity && (entity.liquid == liquid || entity.liquidAmount <= 0.01f);
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        LiquidTurretEntity entity = tile.entity();
        entity.liquid = liquid;
        entity.liquidAmount += amount;
    }

    @Override
    public float getLiquid(Tile tile){
        LiquidTurretEntity entity = tile.entity();
        return entity.liquidAmount;
    }

    @Override
    public float getLiquidCapacity(Tile tile){
        return liquidCapacity;
    }

    @Override
    public TileEntity getEntity() {
        return new LiquidTurretEntity();
    }

    static class LiquidTurretEntity extends TurretEntity{
        public Liquid liquid;
        public float liquidAmount;

        @Override
        public void write(DataOutputStream stream) throws IOException {
            super.write(stream);
            stream.writeByte(liquid == null ? -1 : liquid.id);
            stream.writeByte((byte)(liquidAmount));
        }

        @Override
        public void read(DataInputStream stream) throws IOException{
            super.read(stream);
            byte id = stream.readByte();
            liquid = id == -1 ? null : Liquid.getByID(id);
            liquidAmount = stream.readByte();
        }
    }
}
