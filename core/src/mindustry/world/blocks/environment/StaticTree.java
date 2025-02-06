package mindustry.world.blocks.environment;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class StaticTree extends StaticWall{

    public StaticTree(String name){
        super(name);
        variants = 0;
    }

    @Override
    public void drawBase(Tile tile){
        TextureRegion reg = variants > 0 ? variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))] : region;

        TextureRegion r = Tmp.tr1;
        r.set(reg);
        int crop = (r.width - tilesize*4) / 2;
        float ox = 0;
        float oy = 0;

        for(int i = 0; i < 4; i++){
            if(tile.nearby(i) != null && tile.nearby(i).block() instanceof StaticWall){

                if(i == 0){
                    r.setWidth(r.width - crop);
                    ox -= crop /2f;
                }else if(i == 1){
                    r.setY(r.getY() + crop);
                    oy -= crop /2f;
                }else if(i == 2){
                    r.setX(r.getX() + crop);
                    ox += crop /2f;
                }else{
                    r.setHeight(r.height - crop);
                    oy += crop /2f;
                }
            }
        }
        Draw.rect(r, tile.drawx() + ox * Draw.scl, tile.drawy() + oy * Draw.scl);
    }
}
