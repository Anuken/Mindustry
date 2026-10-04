package arc.assets.loaders;

import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.struct.*;

public abstract class CustomLoader extends AsynchronousAssetLoader{
    public Runnable loaded = () -> {};

    public CustomLoader(){
        super(Core.files::internal);
    }

    @Override
    public Object loadSync(AssetManager manager, String fileName, Fi file, AssetLoaderParameters parameter){
        loaded.run();
        return this;
    }

    @Override
    public Seq<AssetDescriptor> getDependencies(String fileName, Fi file, AssetLoaderParameters parameter){
        return null;
    }
}
