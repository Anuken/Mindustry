package mindustry.graphics;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;

//TODO: this needs to pack to a texture array
public class MultiPacker implements Disposable{
    private PixmapPacker packer;
    private ObjectSet<String> outlined = new ObjectSet<>();

    public MultiPacker(int size){
        if(size > 0){
            packer = new PixmapPacker(size, size, 2, true);
        }
    }

    public PixmapRegion get(TextureRegion region){
        return get(((AtlasRegion)region).name);
    }

    public PixmapRegion get(String region){
        PixmapRegion out = getOrNull(region);
        //this should not happen in normal situations
        if(out == null) return getOrNull("error");
        return out;
    }

    public @Nullable PixmapRegion getOrNull(String name){
        return packer.getRegion(name);
    }

    public void printStats(){
        if(Log.level != LogLevel.debug) return;

        Log.debug("[Atlas] " + (packer.getPages().size > 1 ? "&fb&lr" : "&lg") + "@ page@&r", packer.getPages().size, packer.getPages().size > 1 ? "s" : "");
        int i = 0;
        for(var page : packer.getPages()){
            float totalArea = 0;
            for(var region : page.getRects().values()){
                totalArea += region.area();
            }

            Log.debug("[Atlas] - [@] @x@ (&lk@% used&fr)", i, page.getPixmap().width, page.getPixmap().height, (int)(totalArea / (page.getPixmap().width * page.getPixmap().height) * 100f));

            i ++;
        }
    }

    /** @return whether this image was not already outlined. */
    public boolean registerOutlined(String named){
        return outlined.add(named);
    }

    public boolean isOutlined(String name){
        return outlined.contains(name);
    }

    public boolean has(String name){
        return packer.getRect(name) != null;
    }

    public void add(String name, Pixmap pix){
        add(name, new PixmapRegion(pix));
    }

    public void add(String name, PixmapRegion region){
        add(name, region, null, null);
    }

    public void add(String name, PixmapRegion region, int[] splits, int[] pads){
        packer.pack(name, region, splits, pads);
    }

    public TextureAtlas create(TextureFilter filter){
        return packer.generateTextureAtlas(filter, filter, false, true, 1);
    }

    @Override
    public void dispose(){
        if(packer != null){
            packer.forceDispose();
        }
    }
}
