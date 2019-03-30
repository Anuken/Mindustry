package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.modules.LiquidModule;

public class LiquidMixer extends GenericCrafter{
    protected TextureRegion liquidRegion, bottomRegion, topRegion;

    public LiquidMixer(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();

        liquidRegion = Core.atlas.find(name + "-liquid");
        topRegion = Core.atlas.find(name + "-top");
        bottomRegion = Core.atlas.find(name + "-bottom");
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-bottom"), Core.atlas.find(name + "-top")};
    }

    @Override
    public void draw(Tile tile){
        LiquidModule mod = tile.entity.liquids;

        int rotation = rotate ? tile.getRotation() * 90 : 0;

        Draw.rect(bottomRegion, tile.drawx(), tile.drawy(), rotation);

        if(mod.total() > 0.001f){
            Draw.color(outputLiquid.liquid.color);
            Draw.alpha(mod.get(outputLiquid.liquid) / liquidCapacity);
            Draw.rect(liquidRegion, tile.drawx(), tile.drawy(), rotation);
            Draw.color();
        }

        Draw.rect(topRegion, tile.drawx(), tile.drawy(), rotation);
    }
}
