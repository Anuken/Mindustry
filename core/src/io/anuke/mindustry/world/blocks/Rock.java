package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class Rock extends Block{
    protected TextureRegion[] shadowRegions, regions;
    protected int variants;

    public Rock(String name){
        super(name);
        breakable = true;
        alwaysReplace = true;
    }

    @Override
    public void draw(Tile tile){
        if(variants > 0){
            Draw.rect(regions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, regions.length - 1))], tile.worldx(), tile.worldy());
        }else{
            Draw.rect(region, tile.worldx(), tile.worldy());
        }
    }

    @Override
    public void drawShadow(Tile tile){
        if(shadowRegions != null){
            Draw.rect(shadowRegions[(Mathf.randomSeed(tile.pos(), 0, variants - 1))], tile.worldx(), tile.worldy());
        }else if(shadowRegion != null){
            Draw.rect(shadowRegion, tile.drawx(), tile.drawy());
        }
    }

    @Override
    public void load(){
        super.load();

        if(variants > 0){
            shadowRegions = new TextureRegion[variants];
            regions = new TextureRegion[variants];

            for(int i = 0; i < variants; i++){
                shadowRegions[i] = Draw.region(name + "shadow" + (i + 1));
                regions[i] = Draw.region(name + (i + 1));
            }
        }
    }
}
