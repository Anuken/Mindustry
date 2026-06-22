package mindustry.mod;

import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.graphics.*;
import mindustry.mod.data.*;

public class DataManager{
    private DataPatcher patcher = new DataPatcher();
    private DataImagePacker packer = new DataImagePacker();
    private DataAudioLoader soundLoader = new DataAudioLoader();
    private DataBundleLoader bundleLoader = new DataBundleLoader();

    private ObjectMap<DataAssetType, Seq<DataAsset>> assets = new ObjectMap<>();
    private Seq<DataAsset> orderedAssets = new Seq<>();
    private Seq<DataAsset> orderedExternalAssets = new Seq<>();

    public void reloadContent(boolean reloadArrays){

        patcher.unapply(reloadArrays);
        patcher.apply(getPatches(), getContent(), reloadArrays);

        rebuildOrderedAssets();
    }

    public void regenerateContentSprites(boolean forcePack){
        var allContent = patcher.getAddedContent();

        //TODO: what to do when headless (server?)
        if(allContent.isEmpty()){
            if(forcePack) reloadImages();
            return;
        }

        Seq<UnlockableContent> contentToPack = allContent.select(c -> c instanceof UnlockableContent && c.minfo.asset != null).as();
        ObjectSet<String> existingContentHashes = new ObjectSet<>();
        ObjectSet<String> imagesWithHashes = new ObjectSet<>();
        ObjectMap<UnlockableContent, String> hashes = new ObjectMap<>();

        //store hashes based on JSON contents
        for(var content : contentToPack){
            String hash = content.minfo.asset.hashData();
            hashes.put(content, hash);
            existingContentHashes.add(hash);
        }

        var images = getImages();
        images.removeAll(i -> {
            if(i.isGenerated()){
                Fi file = new Fi(i.path);
                String imageToContentHash = file.parent().name();
                imagesWithHashes.add(imageToContentHash);
                //don't remove images that correspond to content that exists with that hash
                return !existingContentHashes.contains(imageToContentHash);
            }
            return false;
        });

        //remove content that doesn't need to be packed
        contentToPack.removeAll(u -> imagesWithHashes.contains(hashes.get(u)));

        ObjectMap<String, ImageAsset> imageMap = new ObjectMap<>();
        for(var image : images){
            imageMap.put(DataImagePacker.regionPrefix + image.name, image);
        }
        int[] packed = {0};
        ObjectMap<String, PixmapRegion> imagePixmaps = new ObjectMap<>();
        PixmapRegion error = new PixmapRegion(Pixmaps.blankPixmap());
        UnlockableContent[] currentContent = {null};
        String[] currentHash = {null};

        MultiPacker saver = new MultiPacker(false){
            @Override
            public void add(PageType type, String name, PixmapRegion region, int[] splits, int[] pads){
                try{
                    if(region.pixmap.width > 2000) throw new IllegalArgumentException("Max image size exceeded");

                    //strip duplicate prefix
                    if(name.startsWith(DataImagePacker.regionPrefix)) name = name.substring(DataImagePacker.regionPrefix.length());
                    String path = "generated/" + currentHash[0] + "/" + name + ".png";
                    ImageAsset newImage = new ImageAsset();
                    newImage.setPath(path);
                    //it would be nice to do this async, but the pixmap typically gets disposed right after add() exist
                    byte[] bytes = PixmapIO.writePngBytes(region.pixmap);
                    newImage.updateData(bytes);

                    images.add(newImage);
                    packed[0] ++;
                }catch(Exception e){
                    Log.err("Failed to pack image " + name, e);
                }
            }

            @Override
            public boolean has(String name){
                return imageMap.containsKey(name);
            }

            @Override
            public boolean has(PageType type, String name){
                return has(name);
            }

            @Override
            public @Nullable PixmapRegion get(TextureRegion region){
                return get(((AtlasRegion)region).name);
            }

            @Override
            public PixmapRegion get(String name){
                var pix = imagePixmaps.get(name);
                if(pix != null) return pix;

                var image = imageMap.get(name);
                if(image == null) return error;

                try{
                    var result = new PixmapRegion(new Pixmap(image.getCacheFileNoNull()));
                    imagePixmaps.put(name, result);
                    return result;
                }catch(Exception e){
                    Log.err("Failed loading image: " + image.path, e);
                    imagePixmaps.put(name, error);
                    return error;
                }
            }
        };

        for(var content : contentToPack){
            currentContent[0] = content;
            currentHash[0] = hashes.get(content);

            try{
               content.createIcons(saver);
            }catch(Throwable e){
                Log.err(e);
            }
        }

        imagePixmaps.each((key, val) -> val.pixmap.dispose());
        error.pixmap.dispose();

        if(packed[0] > 0 || forcePack){
            reloadImages();

            if(!Vars.headless){
                for(var cont : contentToPack){
                    try{
                        cont.loadIcon();
                        cont.load();
                    }catch(Exception e){
                        Log.err("Failed to load icons for " + cont, e);
                    }
                }
            }
        }
    }

    public void reloadPatches(Seq<PatchAsset> patches){
        if(patches != getPatches()) getPatches().set(patches);

        patcher.unapply();
        patcher.apply(patches, getContent());

        rebuildOrderedAssets();
    }

    public void reloadImages(){
        packer.unload();
        packer.pack(getImages());

        rebuildOrderedAssets();
    }

    public void reloadImages(Seq<ImageAsset> images){
        if(images != getImages()) getImages().set(images);

        reloadImages();
    }

    public void reloadAudio(){
        soundLoader.unload();
        soundLoader.load(getSounds(), getMusic());

        rebuildOrderedAssets();
    }

    public void load(Seq<DataAsset> newAssets){
        unload(); //if already loaded

        if(newAssets.isEmpty()) return;

        for(var asset : newAssets){
            assets.get(asset.getType(), Seq::new).add(asset);
        }

        soundLoader.load(getSounds(), getMusic());

        if(!Vars.headless){
            bundleLoader.load(getBundles());
            packer.pack(getImages());
        }

        patcher.apply(getPatches(), getContent());

        rebuildOrderedAssets();
    }

    public void unload(){
        patcher.unapply();
        if(!Vars.headless){
            bundleLoader.unload();
            packer.unload();
        }
        soundLoader.unload();

        assets.clear();
        orderedAssets.clear();
        orderedExternalAssets.clear();
    }

    private void rebuildOrderedAssets(){
        orderedAssets.clear();
        orderedExternalAssets.clear();
        for(DataAssetType type : DataAssetType.all){
            var seq = assets.get(type);
            if(seq != null){
                orderedAssets.addAll(seq);
                if(!type.embedded){
                    orderedExternalAssets.addAll(seq);
                }
            }
        }
    }

    /** @return broken assets with no cache file */
    public Seq<DataAsset> getMissingAssets(){
        return orderedExternalAssets.select(d -> !d.isCached());
    }

    public Seq<DataAsset> getAllAssets(){
        return orderedAssets;
    }

    public boolean hasAssetPath(DataAssetType type, String path){
        return getAssets(type).contains(a -> a.path.equals(path));
    }

    public boolean hasAssetName(DataAssetType type, String name){
        return getAssets(type).contains(a -> a.name.equals(name));
    }

    public boolean hasAssetNameOrPath(DataAssetType type, String name, String path){
        return getAssets(type).contains(a -> a.name.equals(name) || a.path.equals(path));
    }

    /** @return whether any assets like audio/images (external to saves) are loaded, requiring separate network transmission. */
    public boolean hasExternalAssets(){
        return orderedExternalAssets.size > 0;
    }

    /** @return all assets that can be external to a save (for network sync) */
    public Seq<DataAsset> getAllExternalAssets(){
        return orderedExternalAssets;
    }

    public boolean isPatched(Object content){
        return patcher.isPatched(content);
    }

    public <T extends DataAsset> Seq<T> getAssets(DataAssetType type){
        return assets.get(type, Seq::new).as();
    }

    public void clearGeneratedImages(){
        getImages().removeAll(ImageAsset::isGenerated);
    }

    public Seq<ImageAsset> getImages(){
        return getAssets(DataAssetType.image);
    }

    public Seq<PatchAsset> getPatches(){
        return getAssets(DataAssetType.patch);
    }

    public Seq<MusicAsset> getMusic(){
        return getAssets(DataAssetType.music);
    }

    public Seq<SoundAsset> getSounds(){
        return getAssets(DataAssetType.sound);
    }

    public Seq<BundleAsset> getBundles(){
        return getAssets(DataAssetType.bundle);
    }

    public Seq<ContentAsset> getContent(){
        return getAssets(DataAssetType.content);
    }
}
