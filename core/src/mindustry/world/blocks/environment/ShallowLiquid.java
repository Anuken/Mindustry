package mindustry.world.blocks.environment;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.world.*;

/**
 * Blends water together with a standard floor. No new mechanics.
 * */
public class ShallowLiquid extends Floor{
    public @Nullable Floor liquidBase, floorBase;
    public float liquidOpacity = 0.35f;

    public ShallowLiquid(String name){
        super(name);
    }

    public void set(Block liquid, Block floor){
        this.liquidBase = liquid.asFloor();
        this.floorBase = floor.asFloor();

        isLiquid = true;
        variants = floorBase.variants;
        status = liquidBase.status;
        liquidDrop = liquidBase.liquidDrop;
        cacheLayer = liquidBase.cacheLayer;
        shallow = true;
    }

    @Override
    public void createIcons(MultiPacker packer){
        //TODO might not be necessary at all, but I am not sure yet
        //super.createIcons(packer);

        if(liquidBase != null && floorBase != null){
            var overlay = Core.atlas.getPixmap(liquidBase.region);
            int index = 0;
            for(TextureRegion region : floorBase.variantRegions()){
                var res = Core.atlas.getPixmap(region).crop();
                for(int x = 0; x < res.width; x++){
                    for(int y = 0; y < res.height; y++){
                        res.setRaw(x, y, Pixmap.blend((overlay.getRaw(x, y) & 0xffffff00) | (int)(liquidOpacity * 255), res.getRaw(x, y)));
                    }
                }

                String baseName = this.name + "" + (++index);
                packer.add(PageType.environment, baseName, res);
                packer.add(PageType.editor, "editor-" + baseName, res);

                res.dispose();
            }
        }
    }
}
