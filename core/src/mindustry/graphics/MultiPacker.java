package mindustry.graphics;

import arc.graphics.*;
import arc.graphics.Pixmap.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.util.*;

public class MultiPacker implements Disposable{
    private PixmapPacker[] packers = new PixmapPacker[PageType.all.length];

    public MultiPacker(){
        for(int i = 0; i < packers.length; i++){
            int pageSize = 2048;
            packers[i] = new PixmapPacker(pageSize, pageSize, Format.rgba8888, 2, true);
        }
    }

    public boolean has(PageType type, String name){
        return packers[type.ordinal()].getRect(name) != null;
    }

    public void add(PageType type, String name, PixmapRegion region){
        packers[type.ordinal()].pack(name, region);
    }

    public void add(PageType type, String name, Pixmap pix){
        packers[type.ordinal()].pack(name, pix);
    }

    public TextureAtlas flush(TextureFilter filter, TextureAtlas atlas){
        for(PixmapPacker p : packers){
            p.updateTextureAtlas(atlas, filter, filter, false, false);
        }
        return atlas;
    }

    @Override
    public void dispose(){
        for(PixmapPacker packer : packers){
            packer.dispose();
        }
    }


    //There are several pages for sprites.
    //main page (sprites.png) - all sprites for units, weapons, placeable blocks, effects, bullets, etc
    //environment page (sprites2.png) - all sprites for things in the environmental cache layer
    //editor page (sprites3.png) - all sprites needed for rendering in the editor, including block icons and a few minor sprites
    //zone page (sprites4.png) - zone previews
    //ui page (sprites5.png) - content icons, white icons and UI elements
    public enum PageType{
        main,
        environment,
        editor,
        ui;

        public static final PageType[] all = values();
    }
}
