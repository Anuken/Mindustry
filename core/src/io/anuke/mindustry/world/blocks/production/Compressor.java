package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.production.GenericCrafter.GenericCrafterEntity;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class Compressor extends PowerCrafter {

    public Compressor(String name) {
        super(name);
        hasLiquids = true;
    }

    @Override
    public void update(Tile tile) {
        GenericCrafterEntity entity = tile.entity();

        float powerUsed = Math.min(Timers.delta() * powerUse, tile.entity.power.amount);
        float liquidAdded = Math.min(outputLiquidAmount * Timers.delta(), liquidCapacity - entity.liquids.amount);
        int itemsUsed = Mathf.ceil(1 + input.amount * entity.progress);

        if(entity.power.amount > powerUsed && entity.items.hasItem(input.item, itemsUsed) && liquidAdded > 0.001f){
            entity.progress += 1f/craftTime;
            entity.totalProgress += Timers.delta();
            handleLiquid(tile, tile, outputLiquid, liquidAdded);
        }

        if(entity.progress >= 1f){
            entity.items.removeItem(input);
            if(outputItem != null) offloadNear(tile, outputItem);
            entity.progress = 0f;
        }

        if(outputItem != null && entity.timer.get(timerDump, 5)){
            tryDump(tile, outputItem);
        }

        if(outputLiquid != null){
            tryDumpLiquid(tile);
        }
    }

    @Override
    public void draw(Tile tile) {
        GenericCrafterEntity entity = tile.entity();

        Draw.rect(name, tile.drawx(), tile.drawy());
        Draw.rect(name + "-frame" + (int) Mathf.absin(entity.totalProgress, 5f, 2.999f), tile.drawx(), tile.drawy());
        Draw.color(Color.CLEAR, tile.entity.liquids.liquid.color, tile.entity.liquids.amount / liquidCapacity);
        Draw.rect(name + "-liquid", tile.drawx(), tile.drawy());
        Draw.color();
        Draw.rect(name + "-top", tile.drawx(), tile.drawy());
    }

    @Override
    public TextureRegion[] getIcon() {
        return new TextureRegion[]{Draw.region(name), Draw.region(name + "-top")};
    }
}
