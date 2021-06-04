package mindustry.world.blocks.environment;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.content.*;
import mindustry.world.*;

public class Prop extends Block{
    public int variants;

    public Prop(String name){
        super(name);
        breakable = true;
        alwaysReplace = true;
        instantDeconstruct = true;
        
        deconstructThreshold = 0.35f;
        breakEffect = Fx.breakProp;
    }

    @Override
    public void drawBase(Tile tile){
        Draw.rect(variants > 0 ? variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))] : region, tile.worldx(), tile.worldy());
    }

    @Override
    public TextureRegion[] icons(){
        return variants == 0 ? super.icons() : new TextureRegion[]{Core.atlas.find(name + "1")};
    }

    @Override
    public void load(){
        super.load();

        if(variants > 0){
            variantRegions = new TextureRegion[variants];

            for(int i = 0; i < variants; i++){
                variantRegions[i] = Core.atlas.find(name + (i + 1));
            }
        }
    }
}
