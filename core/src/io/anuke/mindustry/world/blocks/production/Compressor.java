package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.production.GenericCrafter.GenericCrafterEntity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

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
            frameRegions[i] = Draw.region(name + "-frame" + i);
        }

        liquidRegion = Draw.region(name + "-liquid");
        topRegion = Draw.region(name + "-top");
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
    public TextureRegion[] getIcon(){
        return new TextureRegion[]{Draw.region(name), Draw.region(name + "-top")};
    }
}
