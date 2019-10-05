package io.anuke.mindustry.world.blocks;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.tilesize;

/**An overlay ore for a specific item type.*/
public class OreBlock extends OverlayFloor{

    public OreBlock(Item ore){
        super("ore-" + ore.name);
        this.localizedName = ore.localizedName();
        this.itemDrop = ore;
        this.variants = 3;
        this.color.set(ore.color);
    }

    /** For mod use only!*/
    public OreBlock(String name){
        super(name);
    }

    public void setup(Item ore){
        this.localizedName = ore.localizedName();
        this.itemDrop = ore;
        this.variants = 3;
        this.color.set(ore.color);
    }

    @Override
    @OverrideCallSuper
    public void createIcons(PixmapPacker out, PixmapPacker editor){
        for(int i = 0; i < 3; i++){
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

            out.pack(name + (i + 1), image);
            editor.pack("editor-" + name + (i + 1), image);

            if(i == 0){
                editor.pack("editor-block-" + name + "-full", image);
                out.pack("block-" + name + "-full", image);
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
        return itemDrop.localizedName();
    }
}
