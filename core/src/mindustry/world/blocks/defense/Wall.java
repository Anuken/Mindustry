package mindustry.world.blocks.defense;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class Wall extends Block{
    public int variants = 0;

    public Wall(String name){
        super(name);
        solid = true;
        destructible = true;
        group = BlockGroup.walls;
        buildCostMultiplier = 5f;
    }

    @Override
    public void load(){
        super.load();

        if(variants != 0){
            variantRegions = new TextureRegion[variants];

            for(int i = 0; i < variants; i++){
                variantRegions[i] = Core.atlas.find(name + (i + 1));
            }
            region = variantRegions[0];
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(Core.atlas.has(name) ? name : name + "1")};
    }

    @Override
    public boolean canReplace(Block other){
        return super.canReplace(other) && health > other.health;
    }

    public class WallEntity extends TileEntity{

        @Override
        public void draw(){
            if(variants == 0){
                Draw.rect(region, x, y);
            }else{
                Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], x, y);
            }
        }
    }
}
