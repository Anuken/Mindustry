package mindustry.mod;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.mod.data.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Manages data patch images. */
public class DataImagePacker{
    private static int defaultAtlasDepth = -1;

    public static final String regionPrefix = "dp-";
    /** Prefix for images pushed dynamically by the server via {@link #addTexture(String, byte[])}, as opposed to map/mod data patches. */
    public static final String serverRegionPrefix = "net-";

    private @Nullable Seq<AtlasRegion> addedRegions;
    /** Textures added at runtime via addTexture(), keyed by their unprefixed name. Tracked separately from patchAtlas so they can be added/removed individually. */
    private ObjectMap<String, Texture> serverImages = new ObjectMap<>();

    /** Packs a new set of images. If images are already packed, disposes of the old ones. */
    public void pack(Seq<ImageAsset> images){
        if(addedRegions != null){
            unload();
        }

        if(images.isEmpty()) return;

        if(defaultAtlasDepth <= 0){
            defaultAtlasDepth = Core.atlas.getTexture().getDepth();
        }

        Time.mark();

        AtomicInteger totalSumArea = new AtomicInteger(), maxSize = new AtomicInteger();
        var sizeTasks = new Seq<Future<?>>();

        for(var image : images){
            sizeTasks.add(Vars.mainExecutor.submit(() -> {
                try{
                    try(DataInputStream in = new DataInputStream(image.getCacheFile().read())){
                        long header = in.readLong();
                        if(header != 0x89504e470d0a1a0aL) return; //not a PNG
                        in.readInt(); //length
                        int type = in.readInt(); //chunk type
                        if(type != 0x49484452) return; //no HDR
                        int width = in.readInt();
                        int height = in.readInt();
                        if(width <= 0 || height <= 0) return; //negative size

                        totalSumArea.addAndGet(width * height);

                        synchronized(maxSize){
                            maxSize.set(Math.max(maxSize.get(), Math.max(width, height)));
                        }
                    }
                }catch(Exception ignored){}
            }));
        }

        Threads.awaitAll(sizeTasks);

        int targetPower = Mathf.nextPowerOfTwo((int)(Mathf.sqrt(totalSumArea.get()) * 1.4f));
        int targetSize = Mathf.clamp(Math.max(targetPower, maxSize.get()), 128, 4096);

        PixmapPacker packer = new PixmapPacker(targetSize, targetSize, 2, true);

        Seq<ImageAsset> toPack = new Seq<>(), pending = new Seq<>();
        ObjectSet<String> generatedNames = new ObjectSet<>();

        //this makes sure that generated images are prioritized
        for(var image : images){
            if(image.isGenerated()){
                generatedNames.add(image.name);
                toPack.add(image);
            }else{
                pending.add(image);
            }
        }

        for(var image : pending){
            if(!generatedNames.contains(image.name)){
                toPack.add(image);
            }
        }

        var tasks = new Seq<Future<?>>();
        for(var image : toPack){
            tasks.add(Vars.mainExecutor.submit(() -> {
                Fi cacheFile = image.getCacheFile();
                //logged elsewhere
                if(cacheFile == null) return;

                try{
                    Pixmap pixmap = new Pixmap(cacheFile);
                    //don't add the double dp prefix, only add it if it's not already present
                    String name = (image.isGenerated() && image.name.contains("-dp-") ? "" : regionPrefix) + image.name;

                    packer.pack(name, pixmap);

                    pixmap.dispose();
                }catch(Throwable e){
                    Log.err("Invalid patch image: " + image.path, e);
                }
            }));
        }

        Threads.awaitAll(tasks);

        Draw.flush();
        var pagesToAdd = packer.getPages().select(p -> p.addedRects.size > 0);
        addedRegions = new Seq<>();
        Core.atlas.getTexture().resizeDepth(defaultAtlasDepth + pagesToAdd.size);

        for(int i = 0; i < pagesToAdd.size; i ++){
            var page = pagesToAdd.get(i);
            ArraySliceTexture tex = new ArraySliceTexture(Core.atlas.getTexture(), i + defaultAtlasDepth);
            tex.draw(page.image);

            for(String name : page.addedRects){
                var rect = page.rects.get(name);
                var region = new AtlasRegion(tex, (int)rect.x, (int)rect.y, (int)rect.width, (int)rect.height);

                if(rect.splits != null){
                    region.splits = rect.splits;
                    region.pads = rect.pads;
                }

                region.name = name;
                region.offsetX = rect.offsetX;
                region.offsetY = (int)(rect.originalHeight - rect.height - rect.offsetY);
                region.originalWidth = rect.originalWidth;
                region.originalHeight = rect.originalHeight;

                Core.atlas.getRegionMap().put(name, region);
                addedRegions.add(region);
            }
        }

        printStats(packer);
        packer.forceDispose();

        //getRegions is intentionally not modified, it's a hassle to manage, O(n) to unapply, and not used anywhere important. there's no point.

        Log.debug("[Patch Atlas] Time to pack: @ms", Time.elapsed());
    }

    public void unload(){
        if(addedRegions != null){
            for(var region : addedRegions){
                Core.atlas.getRegionMap().remove(region.name);
                Core.atlas.getDrawables().remove(region.name);
            }
            addedRegions = null;
        }

        serverImages.each((name, texture) -> {
            Core.atlas.getRegionMap().remove(serverRegionPrefix + name);
            Core.atlas.getTextures().remove(texture);
            texture.dispose();
            Core.atlas.getDrawables().remove(serverRegionPrefix + name);

        });
        serverImages.clear();
    }

    /** Decodes PNG bytes and registers them into the atlas under "netRegionPrefix + name", replacing any existing image with the same name. Safe to call from any thread. */
    public void addTexture(String name, byte[] pngData){
        Vars.mainExecutor.submit(() -> {
            Pixmap pixmap;
            try{
                pixmap = new Pixmap(pngData);
            }catch(Throwable e){
                Log.err("Invalid server-provided image data for '" + name + "'", e);
                return;
            }

            if(pixmap.width > DataPatcher.maxImageSize || pixmap.height > DataPatcher.maxImageSize){
                Log.err("Server-provided image '" + name + "' exceeds max size of " + DataPatcher.maxImageSize);
                pixmap.dispose();
                return;
            }

            Core.app.post(() -> {
                removeTexture(name);

                Texture texture = new Texture(pixmap);
                pixmap.dispose();

                var region = new TextureAtlas.AtlasRegion(texture, 0, 0, texture.width, texture.height);
                region.name = serverRegionPrefix + name;

                serverImages.put(name, texture);
                Core.atlas.getTextures().add(texture);
                Core.atlas.getRegionMap().put(serverRegionPrefix + name, region);

                //fired with the full atlas region name
                Events.fire(new TextureStreamEvent(serverRegionPrefix + name));
            });
        });
    }

    /** Removes a texture previously added with {@link #addTexture} */
    public void removeTexture(String name){
        Texture texture = serverImages.remove(name);
        if(texture != null){
            Core.atlas.getRegionMap().remove(serverRegionPrefix + name);
            Core.atlas.getTextures().remove(texture);
            texture.dispose();
            Core.atlas.getDrawables().remove(serverRegionPrefix + name);
        }
    }

    public void printStats(PixmapPacker packer){
        if(Log.level != LogLevel.debug) return;

        int total = packer.getPages().sum(p -> p.rects.size);
        Log.debug("[Patch Atlas] " +  (packer.getPages().size > 1 ? "&fb&lr" : "&lg") + "@ page@&lc (" + total + " sprites)", packer.getPages().size, packer.getPages().size > 1 ? "s" : "");
        int i = 0;
        for(var page : packer.getPages()){
            float totalArea = 0;
            for(var region : page.getRects().values()){
                totalArea += region.area();
            }

            Log.debug("[Patch Atlas] - [@] @x@ (&lk@% used&fr)", i, page.getPixmap().width, page.getPixmap().height, (int)(totalArea / (page.getPixmap().width * page.getPixmap().height) * 100f));

            i ++;
        }
    }
}
