package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.production.GenericCrafter.GenericCrafterEntity;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;

public class Compressor extends PowerCrafter{
    protected TextureRegion liquidRegion, topRegion;
    protected TextureRegion[] frameRegions;

    public Compressor(String name){
        super(name);
        hasLiquids = true;
    }

    @Override
    public void load(){
        super.load();

        frameRegions = new TextureRegion[3];
        for(int i = 0; i < 3; i++){
            frameRegions[i] = Core.atlas.find(name + "-frame" + i);
        }

        liquidRegion = Core.atlas.find(name + "-liquid");
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void draw(Tile tile){
        GenericCrafterEntity entity = tile.entity();

        Draw.rect(region, tile.drawx(), tile.drawy());
        Draw.rect(frameRegions[(int) Mathf.absin(entity.totalProgress, 5f, 2.999f)], tile.drawx(), tile.drawy());
        Draw.color(Color.CLEAR, tile.entity.liquids.current().color, tile.entity.liquids.total() / liquidCapacity);
        Draw.rect(liquidRegion, tile.drawx(), tile.drawy());
        Draw.color();
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-top")};
    }
}
