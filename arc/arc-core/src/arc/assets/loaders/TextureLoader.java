package arc.assets.loaders;

import arc.assets.*;
import arc.files.*;
import arc.graphics.*;
import arc.util.*;

/**
 * {@link AssetLoader} for {@link Texture} instances. The pixel data is loaded asynchronously. The texture is then created on the
 * rendering thread, synchronously. Passing a {@link TextureParameter} to
 * {@link AssetManager#load(String, Class, AssetLoaderParameters)} allows one to specify parameters as can be passed to the
 * various Texture constructors, e.g. filtering, whether to generate mipmaps and so on.
 * @author mzechner
 */
public class TextureLoader extends AsynchronousAssetLoader<Texture, TextureLoader.TextureParameter>{
    final TextureLoaderInfo info = new TextureLoaderInfo();

    public TextureLoader(FileHandleResolver resolver){
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, TextureParameter parameter){
        info.pixmap = new Pixmap(file);
    }

    @Override
    public Texture loadSync(AssetManager manager, String fileName, Fi file, TextureParameter parameter){

        Texture texture = parameter == null ? null : parameter.texture;
        if(texture == null) texture = new Texture();

        texture.load(info.pixmap, true, parameter != null && parameter.genMipMaps);

        if(parameter != null){
            texture.setFilter(parameter.minFilter, parameter.magFilter);
            texture.setWrap(parameter.wrapU, parameter.wrapV);
        }
        return texture;
    }

    public static class TextureLoaderInfo{
        Pixmap pixmap;
    }

    public static class TextureParameter extends AssetLoaderParameters<Texture>{
        /** whether to generate mipmaps **/
        public boolean genMipMaps = false;
        /** The texture to put the data in, optional. **/
        public @Nullable Texture texture = null;
        public TextureFilter minFilter = TextureFilter.nearest;
        public TextureFilter magFilter = TextureFilter.nearest;
        public TextureWrap wrapU = TextureWrap.clampToEdge;
        public TextureWrap wrapV = TextureWrap.clampToEdge;

        public TextureParameter(){
        }
    }
}
