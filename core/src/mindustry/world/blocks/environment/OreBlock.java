package mindustry.world.blocks.environment;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
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
        this.localizedName = ore.localizedName;
        this.itemDrop = ore;
        this.mapColor.set(ore.color);
    }

    @Override
    @OverrideCallSuper
    public void createIcons(MultiPacker packer){
        for(int i = 0; i < variants; i++){
            PixmapRegion shadow = Core.atlas.getPixmap(itemDrop.name + (i + 1));
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

            packer.add(PageType.environment, name + (i + 1), image);
            packer.add(PageType.editor, "editor-" + name + (i + 1), image);

            if(i == 0){
                packer.add(PageType.editor, "editor-block-" + name + "-full", image);
                packer.add(PageType.main, "block-" + name + "-full", image);
            }
        }
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
