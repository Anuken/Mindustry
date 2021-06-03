package mindustry.graphics;

import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.*;

public class MultiPacker implements Disposable{
    private PixmapPacker[] packers = new PixmapPacker[PageType.all.length];

    public MultiPacker(){
        for(int i = 0; i < packers.length; i++){
            packers[i] = new PixmapPacker(Math.min(Vars.maxTextureSize, PageType.all[i].width), Math.min(Vars.maxTextureSize, PageType.all[i].height), 2, true);
        }
    }

    @Nullable
    public PixmapRegion get(String name){
        for(var packer : packers){
            var region = packer.getRegion(name);
            if(region != null){
                return region;
            }
        }
        return null;
    }

    public PixmapPacker getPacker(PageType type){
        return packers[type.ordinal()];
    }

    public boolean has(String name){
        for(var page : PageType.all){
            if(packers[page.ordinal()].getRect(name) != null){
                return true;
            }
        }
        return false;
    }

    public boolean has(PageType type, String name){
        return packers[type.ordinal()].getRect(name) != null;
    }

    public void add(PageType type, String name, PixmapRegion region){
        packers[type.ordinal()].pack(name, region);
    }

    public void add(PageType type, String name, PixmapRegion region, int[] splits, int[] pads){
        packers[type.ordinal()].pack(name, region, splits, pads);
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
    //zone page (sprites4.png) - zone preview
    //rubble page - scorch textures for unit deaths & wrecks
    //ui page (sprites5.png) - content icons, white icons, fonts and UI elements
    public enum PageType{
        main(4096),
        environment,
        editor(4096, 2048),
        rubble,
        ui(4096);

        public static final PageType[] all = values();

        public int width = 2048, height = 2048;

        PageType(int defaultSize){
            this.width = this.height = defaultSize;
        }

        PageType(int width, int height){
            this.width = width;
            this.height = height;
        }

        PageType(){

        }
    }
}
