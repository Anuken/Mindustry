package mindustry.graphics;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.struct.*;
import arc.util.*;

/** Base for anything that content's packSprites() can add generated sprites to. */
public abstract class PackContext{
    private ObjectSet<String> outlined = new ObjectSet<>();

    public PixmapRegion get(TextureRegion region){
        return get(((AtlasRegion)region).name);
    }

    public PixmapRegion get(String region){
        PixmapRegion out = getOrNull(region);
        //this should not happen in normal situations
        if(out == null) return getOrNull("error");
        return out;
    }

    public abstract @Nullable PixmapRegion getOrNull(String name);

    /** @return whether this image was not already outlined. */
    public boolean registerOutlined(String named){
        return outlined.add(named);
    }

    public boolean isOutlined(String name){
        return outlined.contains(name);
    }

    public abstract boolean has(String name);

    public void add(String name, Pixmap pix){
        add(name, new PixmapRegion(pix));
    }

    public void add(String name, PixmapRegion region){
        add(name, region, null, null, false);
    }

    public void add(String name, Pixmap pix, boolean noCrop){
        add(name, new PixmapRegion(pix), null, null, noCrop);
    }

    /** @param noCrop is true, regions will be saved to a folder where x/y whitespace trimming is not applied. Only relevant in vanilla (mods have no such trimming) */
    public abstract void add(String name, PixmapRegion region, int[] splits, int[] pads, boolean noCrop);
}