package mindustry.tools;

import arc.*;
import arc.backend.sdl.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.ui.*;

public class Upscaler{
    public static void main(String[] args){
        new SdlApplication(new ApplicationListener(){
            @Override
            public void init(){
                scale();
            }
        }, new SdlConfig(){{
            initialVisible = false;
        }});
    }

    static void scale(){
        Core.batch = new SpriteBatch();
        Core.atlas = new TextureAtlas();
        Core.atlas.addRegion("white", Pixmaps.blankTextureRegion());
        Fi file = Core.files.local("");

        Log.info("Upscaling icons...");
        Time.mark();
        Fi[] list = file.list();

        for(IconSize size : IconSize.values()){
            String suffix = size == IconSize.def ? "" : "-" + size.name();
            SquareMarcher marcher = new SquareMarcher(size.size);

            for(Fi img : list){
                if(img.extension().equals("png")){
                    marcher.render(new Pixmap(img), img.sibling(img.nameWithoutExtension() + suffix + ".png"));
                }
            }
        }

        Log.info("Done upscaling icons in &lm{0}&lgs.", Time.elapsed()/1000f);
        Core.app.exit();
    }
}
