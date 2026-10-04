package mindustry.world.blocks.environment;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/**An overlay ore for a specific item type.*/
public class OreBlock extends OverlayFloor{

    public OreBlock(String name, Item ore){
        super(name);
        this.localizedName = ore.localizedName;
        this.itemDrop = ore;
        this.variants = 3;
        this.mapColor.set(ore.color);
        this.useColor = true;
    }

    public OreBlock(Item ore){
        this("ore-" + ore.name, ore);
    }

    /** For mod use only!*/
    public OreBlock(String name){
        super(name);
        this.useColor = true;
        variants = 3;
    }

    public void setup(Item ore){
        this.localizedName = ore.localizedName + (wallOre ? " " + Core.bundle.get("wallore") : "");
        this.itemDrop = ore;
        this.mapColor.set(ore.color);
    }

    @Override
    @OverrideCallSuper
    public void packSprites(PackContext packer){
        Pixmap full = null;

        for(int i = 0; i < variants; i++){
            //use name (e.g. "ore-copper1"), fallback to "copper1" as per the old naming system
            PixmapRegion shadow = packer.has(name + (i + 1)) ?
            packer.get(name + (i + 1)) :
            packer.get(itemDrop.name + (i + 1));

            Pixmap image = shadow.crop();

            int offset = image.width / tilesize - 1;
            int shadowColor = Color.rgba8888(0, 0, 0, 0.3f);

            for(int x = 0; x < image.width; x++){
                for(int y = offset; y < image.height; y++){
                    if(shadow.getA(x, y) == 0 && shadow.getA(x, y - offset) != 0){
                        image.setRaw(x, y, shadowColor);
                    }
                }
            }

            packer.add(name + (i + 1), image);

            //the last variant is the best looking one on purpose, so it represents the ore in icons
            if(i == variants - 1){
                packer.add("block-" + name + "-full", image);
                full = image;
            }else{
                image.dispose();
            }
        }

        if(isVanilla() && full != null){
            saveScaled(packer, full, "block-" + name + "-ui", Math.min(full.width, maxUiIcon));
        }

        if(full != null) full.dispose();
    }

    @Override
    public void init(){
        super.init();

        if(itemDrop != null){
            setup(itemDrop);
        }else{
            throw new IllegalArgumentException(name + " must have an item drop!");
        }
    }

    @Override
    public String getDisplayName(Tile tile){
        return itemDrop.localizedName;
    }
}