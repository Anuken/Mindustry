package io.anuke.mindustry;

import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.backends.lwjgl3.Lwjgl3Application;
import io.anuke.arc.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.graphics.Pixmaps;
import io.anuke.arc.graphics.g2d.SpriteBatch;
import io.anuke.arc.graphics.g2d.TextureAtlas;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;

public class Upscaler{
    static Res[] resolutions = {
        new Res(Vars.iconsize, ""),
        new Res(Vars.iconsizesmall, "-small")
    };

    public static void main(String[] args){
        new Lwjgl3Application(new ApplicationListener(){
            @Override
            public void init(){
                scale();
            }
        }, new Lwjgl3ApplicationConfiguration(){{
            setInitialVisible(false);
        }});
    }

    static void scale(){
        Core.batch = new SpriteBatch();
        Core.atlas = new TextureAtlas();
        Core.atlas.addRegion("white", Pixmaps.blankTextureRegion());
        FileHandle file = Core.files.local("");

        Log.info("Upscaling icons...");
        Time.mark();

        for(Res res : resolutions){
            SquareMarcher marcher = new SquareMarcher(res.size);

            for(FileHandle img : file.list()){
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
