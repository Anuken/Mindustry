package io.anuke.mindustry.world.blocks;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;

public class Rock extends Block{
    protected TextureRegion[] regions;
    protected int variants;

    public Rock(String name){
        super(name);
        breakable = true;
        alwaysReplace = true;
    }

    @Override
    public void draw(Tile tile){
        Draw.colorl(1f - tile.getRotation() / 4f);
        if(variants > 0){
            Draw.rect(regions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, regions.length - 1))], tile.worldx(), tile.worldy());
        }else{
            Draw.rect(region, tile.worldx(), tile.worldy());
        }
        Draw.color();
    }

    @Override
    public TextureRegion[] variantRegions(){
        return regions;
    }

    @Override
    public TextureRegion[] generateIcons(){
        return variants == 0 ? super.generateIcons() : new TextureRegion[]{Core.atlas.find(name + "1")};
    }

    @Override
    public void load(){
        super.load();

        if(variants > 0){
            regions = new TextureRegion[variants];

            for(int i = 0; i < variants; i++){
                regions[i] = Core.atlas.find(name + (i + 1));
            }
        }
    }
}
