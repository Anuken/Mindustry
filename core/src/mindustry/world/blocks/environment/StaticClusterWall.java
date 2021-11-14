package mindustry.world.blocks.environment;

import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.annotations.Annotations.*;
import mindustry.world.*;

public class StaticClusterWall extends StaticWall{
    public @Load(value = "@-cluster#", length = 1) TextureRegion[] clusters;

    public StaticClusterWall(String name){
        super(name);
        variants = 1;
    }

    @Override
    public void drawBase(Tile tile){
        super.drawBase(tile);

        if(Mathf.randomSeed(tile.pos(), 10) < 2){
            Draw.rect(clusters[0], tile.worldx(), tile.worldy(), Mathf.randomSeedRange(tile.pos() + 1, 180f));
        }
    }

}
