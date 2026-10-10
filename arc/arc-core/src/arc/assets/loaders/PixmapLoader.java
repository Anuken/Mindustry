package arc.assets.loaders;

import arc.assets.AssetDescriptor;
import arc.assets.AssetLoaderParameters;
import arc.assets.AssetManager;
import arc.struct.Seq;
import arc.files.Fi;
import arc.graphics.Pixmap;

/**
 * {@link AssetLoader} for {@link Pixmap} instances. The Pixmap is loaded asynchronously.
 * @author mzechner
 */
public class PixmapLoader extends AsynchronousAssetLoader<Pixmap, PixmapLoader.PixmapParameter>{
    Pixmap pixmap;

    public PixmapLoader(FileHandleResolver resolver){
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, PixmapParameter parameter){
        pixmap = null;
        pixmap = new Pixmap(file);
    }

    @Override
    public Pixmap loadSync(AssetManager manager, String fileName, Fi file, PixmapParameter parameter){
        Pixmap pixmap = this.pixmap;
        this.pixmap = null;
        return pixmap;
    }

    @Override
    public Seq<AssetDescriptor> getDependencies(String fileName, Fi file, PixmapParameter parameter){
        return null;
    }

    public static class PixmapParameter extends AssetLoaderParameters<Pixmap>{
    }
}
