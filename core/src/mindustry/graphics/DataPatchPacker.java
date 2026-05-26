package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.PixmapPacker.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import mindustry.*;
import mindustry.mod.DataPatcher.*;

import java.util.concurrent.*;

/** Manages data patch images. */
public class DataPatchPacker{
    public static final String regionPrefix = "dp-";

    private @Nullable TextureAtlas patchAtlas;

    /** Packs a new set of images. If images are already packed, disposes of the old ones. */
    public void pack(Seq<PatchImage> images){
        if(patchAtlas != null){
            unapply();
        }
        if(images.isEmpty()) return;

        Time.mark();

        float totalSumArea = 0f;
        int maxSize = 0;
        for(var image : images){
            totalSumArea += image.width * image.height;
            maxSize += Math.max(image.width, image.height);
        }

        int targetPower = Mathf.nextPowerOfTwo((int)(Mathf.sqrt(totalSumArea) * 1.35f));
        int targetSize = Mathf.clamp(Math.max(targetPower, maxSize), 128, Math.min(4096, Vars.maxTextureSize));

        PixmapPacker packer = new PixmapPacker(targetSize, targetSize, 2, true);

        //env regions are packed onto a special reserved region
        boolean anyEnv = images.contains(p -> p.path.contains("blocks/environment/"));
        TextureRegion envReserveRegion = Core.atlas.find("data-patch-reserved-env");
        PixmapPacker envPacker = anyEnv ? new PixmapPacker(envReserveRegion.width, envReserveRegion.height, 2, true) : null;

        var tasks = new Seq<Future<?>>();
        for(var image : images){
            tasks.add(Vars.mainExecutor.submit(() -> {
                try{
                    Pixmap pixmap = new Pixmap(image.data);
                    String name = regionPrefix + image.name;

                    if(anyEnv && image.path.contains("blocks/environment/")){
                        envPacker.pack(name, pixmap);
                    }else{
                        packer.pack(name, pixmap);
                    }
                }catch(Throwable e){
                    Log.err("Invalid patch image: " + image.path, e);
                }
            }));
        }

        Threads.awaitAll(tasks);

        TextureFilter filter = Core.settings.getBool("linear", !Vars.mobile) ? TextureFilter.linear : TextureFilter.nearest;
        patchAtlas = packer.generateTextureAtlas(filter, filter, false);

        if(envPacker != null && envPacker.getPages().size > 0){
            if(envPacker.getPages().size > 1){
                Log.warn("[Patch Atlas] Unable to fit all environment images into a " + envPacker.getPageWidth() + "x" + envPacker.getPageHeight() + " page. Reduce the size or number of images.");
            }
            //directly update existing atlas page's reserved region
            var page = envPacker.getPages().first();
            envReserveRegion.texture.draw(page.getPixmap(), envReserveRegion.getX(), envReserveRegion.getY());

            //manually add rects to the patch atlas with texture and offsets based on env atlas
            for(String name : page.addedRects){
                PixmapPackerRect rect = page.rects.get(name);
                TextureAtlas.AtlasRegion region = new TextureAtlas.AtlasRegion(envReserveRegion.texture, (int)rect.x + envReserveRegion.getX(), (int)rect.y + envReserveRegion.getY(), (int)rect.width, (int)rect.height);

                if(rect.splits != null){
                    region.splits = rect.splits;
                    region.pads = rect.pads;
                }

                region.name = name;
                region.offsetX = rect.offsetX;
                region.offsetY = (int)(rect.originalHeight - rect.height - rect.offsetY);
                region.originalWidth = rect.originalWidth;
                region.originalHeight = rect.originalHeight;

                patchAtlas.getRegions().add(region);
                patchAtlas.getRegionMap().put(name, region);
            }

        }

        printStats(packer, envPacker);

        packer.dispose();
        //textures are never updated, so force disposal
        if(envPacker != null) envPacker.forceDispose();

        Core.atlas.getTextures().addAll(patchAtlas.getTextures());
        Core.atlas.getRegionMap().putAll(patchAtlas.getRegionMap());
        //getRegions is intentionally not modified, it's a hassle to manage, O(n) to unapply, and not used anywhere important. there's no point.
        //the drawable map isn't used, and thus not modified either

        Log.debug("[Patch Atlas] Time to pack: @ms", Time.elapsed());
    }

    public void unapply(){
        if(patchAtlas != null){
            for(var texture : patchAtlas.getTextures()){
                patchAtlas.getTextures().remove(texture);
            }
            for(var region : patchAtlas.getRegions()){
                patchAtlas.getRegionMap().remove(region.name);
            }

            patchAtlas.dispose();
            patchAtlas = null;
        }
    }

    public void printStats(PixmapPacker mainPacker, PixmapPacker envPacker){
        if(Log.level != LogLevel.debug) return;
        for(PixmapPacker packer : new PixmapPacker[]{mainPacker, envPacker}){
            if(packer == null) continue;

            int total = packer.getPages().sum(p -> p.rects.size);
            Log.debug("[Patch Atlas] " + (packer == mainPacker ? "Main: " : "Env: ") + (packer.getPages().size > 1 ? "&fb&lr" : "&lg") + "@ page@&lc (" + total + " sprites)", packer.getPages().size, packer.getPages().size > 1 ? "s" : "");
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

    static class PackResult{
        String name;
        Pixmap pixmap;

        public PackResult(String name, Pixmap pixmap){
            this.name = name;
            this.pixmap = pixmap;
        }
    }
}
