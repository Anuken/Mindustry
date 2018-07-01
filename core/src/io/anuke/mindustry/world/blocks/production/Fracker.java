package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;

public class Fracker extends SolidPump {
    protected Liquid inputLiquid;
    protected float inputLiquidUse;
    protected float inputCapacity = 20f;
    protected Item inputItem = Items.sand;
    protected float itemUseTime = 100f;

    protected TextureRegion liquidRegion;
    protected TextureRegion rotatorRegion;
    protected TextureRegion topRegion;

    public Fracker(String name) {
        super(name);
        hasItems = true;
    }

    @Override
    public void load() {
        super.load();

        liquidRegion = Draw.region(name + "-liquid");
        rotatorRegion = Draw.region(name + "-rotator");
        topRegion = Draw.region(name + "-top");
    }

    @Override
    public void setStats() {
        super.setStats();

        stats.add(BlockStat.inputItem, inputItem);
        stats.add(BlockStat.inputLiquid, inputLiquid);
        stats.add(BlockStat.liquidUse, 60f *inputLiquidUse, StatUnit.liquidSecond);
    }

    @Override
    public void draw(Tile tile) {
        FrackerEntity entity = tile.entity();

        Draw.rect(name, tile.drawx(), tile.drawy());

        Draw.color(tile.entity.liquids.liquid.color);
        Draw.alpha(tile.entity.liquids.amount/liquidCapacity);
        Draw.rect(liquidRegion, tile.drawx(), tile.drawy());
        Draw.color();

        Draw.rect(rotatorRegion, tile.drawx(), tile.drawy(), entity.pumpTime);
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
    }

    @Override
    public TextureRegion[] getIcon() {
        return new TextureRegion[]{Draw.region(name), Draw.region(name + "-rotator"), Draw.region(name + "-top")};
    }

    @Override
    public void update(Tile tile) {
        FrackerEntity entity = tile.entity();

        while(entity.accumulator > itemUseTime && entity.items.hasItem(inputItem, 1)){
            entity.items.removeItem(inputItem, 1);
            entity.accumulator -= itemUseTime;
        }

        if(entity.input >= inputLiquidUse * Timers.delta() && entity.accumulator < itemUseTime){
            super.update(tile);
            entity.input -= inputLiquidUse * Timers.delta();
            entity.accumulator += Timers.delta();
        }else{
            tryDumpLiquid(tile);
        }
    }

    @Override
    public float handleAuxLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        if(liquid != inputLiquid){
            return 0f;
        }else{
            FrackerEntity entity = tile.entity();
            float accepted = Math.min(inputCapacity - entity.input, amount);
            entity.input += accepted;
            return accepted;
        }
    }

    @Override
    public TileEntity getEntity() {
        return new FrackerEntity();
    }

    public static class FrackerEntity extends SolidPumpEntity{
        public float input;
        public float accumulator;
    }
}
