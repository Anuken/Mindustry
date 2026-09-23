package mindustry.tools;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.graphics.*;

import java.io.*;

/** Writes to sprites_out instead of an atlas. */
public class FilePackContext extends PackContext{
    //own copies of everything added this run, since callers often dispose the source pixmap right after add()
    private ObjectMap<String, PixmapRegion> written = new ObjectMap<>();
    //versions of written sprites that were since replaced; callers may still hold on to them, so they're only disposed at the end
    private Seq<Pixmap> replaced = new Seq<>();

    @Override
    public @Nullable PixmapRegion getOrNull(String name){
        var cached = written.get(name);
        if(cached != null) return cached;
        if(!ImagePacker.has(name)) return null;
        return new PixmapRegion(ImagePacker.get(name));
    }

    @Override
    public boolean has(String name){
        return written.containsKey(name) || ImagePacker.has(name);
    }

    @Override
    public void add(String name, PixmapRegion region, int[] splits, int[] pads, boolean noCrop){
        //splits/pads are unused; nothing in the vanilla path emits 9-patches here
        Pixmap image = region.crop();
        Fi target = new Fi((noCrop ? "../blocks/environment/" : "") + name + ".png");
        Core.executor.execute(() -> target.writePng(image));

        removeOriginal(name, target);

        var previous = written.put(name, new PixmapRegion(image));
        if(previous != null) replaced.add(previous.pixmap);
        written.put(name, new PixmapRegion(image));
    }

    /** Registers a pixmap that was written to disk outside of add() (e.g. an autotile fallback icon), so has()/get() can see it this run. */
    public void seed(String name, Pixmap pixmap){
        var previous = written.put(name, new PixmapRegion(pixmap));
        if(previous != null && previous.pixmap != pixmap) replaced.add(previous.pixmap);
    }

    private void removeOriginal(String name, Fi target){
        var index = ImagePacker.cache.get(name);
        if(index == null) return;

        try{
            //the indexed file can be the very one that was just written (e.g. a stale -ui icon from a previous run); it was overwritten, keep it
            if(index.file.file().getCanonicalFile().equals(target.file().getCanonicalFile())) return;

            //the atlas loads pixmaps lazily on first find(), which would fail once the file is gone
            Core.atlas.find(name);
            index.file.delete();
        }catch(IOException e){
            Log.err("Failed to remove original sprite '@'", name, e);
        }
    }

    public void dispose(){
        written.each((name, region) -> region.pixmap.dispose());
        written.clear();
        replaced.each(Pixmap::dispose);
        replaced.clear();
    }
}