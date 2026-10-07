package arc.graphics.font;

import arc.assets.*;
import arc.assets.loaders.*;
import arc.files.*;
import arc.graphics.font.FreeTypeFontGenerator.*;
import arc.struct.*;

/**
 * Creates {@link Font} instances from FreeType font files. Requires a {@link FreeTypeFontLoaderParameter} to be
 * passed to {@link AssetManager#load(String, Class, AssetLoaderParameters)} which specifies the name of the TTF
 * file as well the parameters used to generate the BitmapFont (size, characters, etc.)
 */
public class FreetypeFontLoader extends AsynchronousAssetLoader<Font, FreetypeFontLoader.FreeTypeFontLoaderParameter>{
    public FreetypeFontLoader(FileHandleResolver resolver){
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, FreeTypeFontLoaderParameter parameter){
        if(parameter == null)
            throw new RuntimeException("FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!");
    }

    @Override
    public Font loadSync(AssetManager manager, String fileName, Fi file, FreeTypeFontLoaderParameter parameter){
        if(parameter == null)
            throw new RuntimeException("FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!");
        FreeTypeFontGenerator generator = manager.get(parameter.fontFileName + ".gen", FreeTypeFontGenerator.class);
        return generator.generateFont(parameter.fontParameters);
    }

    @Override
    public Seq<AssetDescriptor> getDependencies(String fileName, Fi file, FreeTypeFontLoaderParameter parameter){
        Seq<AssetDescriptor> deps = new Seq<>();
        deps.add(new AssetDescriptor<>(parameter.fontFileName + ".gen", FreeTypeFontGenerator.class));
        return deps;
    }

    public static class FreeTypeFontLoaderParameter extends AssetLoaderParameters<Font>{
        /** the name of the TTF file to be used to load the font **/
        public String fontFileName;
        /** the parameters used to generate the font, e.g. size, characters, etc. **/
        public FreeTypeFontParameter fontParameters = new FreeTypeFontParameter();

        public FreeTypeFontLoaderParameter(){
        }

        public FreeTypeFontLoaderParameter(String fontFileName, FreeTypeFontParameter fontParameters){
            this.fontFileName = fontFileName;
            this.fontParameters = fontParameters;
        }
    }
}
