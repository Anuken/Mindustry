package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.modules.InventoryModule;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class UnitFactory extends Block {
    protected UnitType type;
    protected ItemStack[] requirements;
    protected float produceTime = 1000f;
    protected float powerUse = 0.1f;
    protected String unitRegion;

    public UnitFactory(String name) {
        super(name);
        solid = true;
        update = true;
        hasPower = true;
    }

    @Override
    public void setBars() {
        super.setBars();

        bars.add(new BlockBar(BarType.production, true, tile -> tile.<UnitFactoryEntity>entity().buildTime / produceTime));
        bars.remove(BarType.inventory);
    }

    @Override
    public void draw(Tile tile) {
        UnitFactoryEntity entity = tile.entity();
        TextureRegion region = Draw.region(unitRegion == null ? type.name : unitRegion);

        Draw.rect(name(), tile.drawx(), tile.drawy());

        Shaders.build.region = region;
        Shaders.build.progress = entity.buildTime/produceTime;
        Shaders.build.color = Colors.get("accent");
        Shaders.build.time = -entity.time / 10f;

        Graphics.shader(Shaders.build, false);
        Shaders.build.apply();
        Draw.rect(region, tile.drawx(), tile.drawy());
        Graphics.shader();

        Draw.color("accent");

        Lines.lineAngleCenter(
                tile.drawx() + Mathf.sin(entity.time, 6f, Vars.tilesize/2f*size - 2f),
                tile.drawy(),
                90,
                size * Vars.tilesize - 4f);

        Draw.reset();

        Draw.rect(name + "-top", tile.drawx(), tile.drawy());
    }

    @Override
    public void update(Tile tile) {
        UnitFactoryEntity entity = tile.entity();

        float used = Math.min(powerUse * Timers.delta(), powerCapacity);

        if(hasRequirements(entity.inventory, entity.buildTime/produceTime) &&
                entity.power.amount >= used){

            entity.buildTime += Timers.delta();
            entity.time += Timers.delta();
            entity.power.amount -= used;
        }

        if(entity.buildTime >= produceTime){
            BaseUnit unit = new BaseUnit(type, tile.getTeam());
            unit.set(tile.drawx(), tile.drawy()).add();
            unit.velocity.y = 4f;
            entity.buildTime = 0f;

            for(ItemStack stack : requirements){
                entity.inventory.removeItem(stack.item, stack.amount);
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        for(ItemStack stack : requirements){
            if(item == stack.item && tile.entity.inventory.getItem(item) <= stack.amount*2){
                return true;
            }
        }
        return false;
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
        public float time;

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
