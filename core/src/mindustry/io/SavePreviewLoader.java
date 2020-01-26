package mindustry.io;

import arc.assets.*;
import arc.assets.loaders.*;
import arc.assets.loaders.resolvers.*;
import arc.files.*;

public class SavePreviewLoader extends TextureLoader{

    public SavePreviewLoader(){
        super(new AbsoluteFileHandleResolver());
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, TextureParameter parameter){
        try{
            super.loadAsync(manager, fileName, file.sibling(file.nameWithoutExtension()), parameter);
        }catch(Exception e){
            e.printStackTrace();
            file.sibling(file.nameWithoutExtension()).delete();
        }
    }
}
