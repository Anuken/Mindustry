package io.anuke.mindustry;

import io.anuke.arc.*;
import io.anuke.arc.backends.sdl.*;
import io.anuke.arc.files.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.util.*;

public class Upscaler{
    static Res[] resolutions = {
        new Res(Vars.iconsizesmall, "-small"),
        new Res(Vars.iconsizemed, "-med"),
        new Res(Vars.iconsize, ""),
    };

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
        FileHandle file = Core.files.local("");

        Log.info("Upscaling icons...");
        Time.mark();
        FileHandle[] list = file.list();

        for(Res res : resolutions){
            SquareMarcher marcher = new SquareMarcher(res.size);

            for(FileHandle img : list){
                if(img.extension().equals("png")){
                    marcher.render(new Pixmap(img), img.sibling(img.nameWithoutExtension() + res.suffix + ".png"));
                }
            }
        }

        Log.info("Done upscaling icons in &lm{0}&lgs.", Time.elapsed()/1000f);
        Core.app.exit();
    }

    static class Res{
        final int size;
        final String suffix;

        public Res(int size, String suffix){
            this.size = size;
            this.suffix = suffix;
        }
    }
}
