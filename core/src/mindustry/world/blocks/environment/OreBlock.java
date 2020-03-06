package mindustry.world.blocks.environment;

import arc.*;
import mindustry.annotations.Annotations.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.tilesize;

/**An overlay ore for a specific item type.*/
public class OreBlock extends OverlayFloor{

    public OreBlock(Item ore){
        super("ore-" + ore.name);
        this.localizedName = ore.localizedName;
        this.itemDrop = ore;
        this.variants = 3;
        this.mapColor.set(ore.color);
    }

    /** For mod use only!*/
    public OreBlock(String name){
        super(name);
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
            Pixmap image = new Pixmap(32, 32);
            PixmapRegion shadow = Core.atlas.getPixmap(itemDrop.name + (i + 1));

            int offset = image.getWidth() / tilesize - 1;
            Color color = new Color();

            for(int x = 0; x < image.getWidth(); x++){
                for(int y = offset; y < image.getHeight(); y++){
                    shadow.getPixel(x, y - offset, color);

                    if(color.a > 0.001f){
                        color.set(0, 0, 0, 0.3f);
                        image.draw(x, y, color);
                    }
                }
            }

            image.draw(shadow);

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
