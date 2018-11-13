package io.anuke.mindustry;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.core.ContentLoader;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Atlas;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Log.LogHandler;
import io.anuke.ucore.util.Log.NoopLogHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ImageContext {
    static ObjectMap<String, TextureRegion> regionCache = new ObjectMap<>();
    static ObjectMap<TextureRegion, BufferedImage> imageCache = new ObjectMap<>();

    static void load() throws IOException{
        Log.setLogger(new NoopLogHandler());
        Vars.content = new ContentLoader();
        Vars.content.load();
        Log.setLogger(new LogHandler());

        Files.walk(Paths.get("../../../assets-raw/sprites_out")).forEach(path -> {
            try{
                if(Files.isDirectory(path)) return;

                String fname = path.getFileName().toString();
                fname = fname.substring(0, fname.length() - 4);

                BufferedImage image = ImageIO.read(path.toFile());
                GenRegion region = new GenRegion(fname){

                    @Override
                    public int getRegionX(){
                        return 0;
                    }

                    @Override
                    public int getRegionY(){
                        return 0;
                    }

                    @Override
                    public int getRegionWidth(){
                        return image.getWidth();
                    }

                    @Override
                    public int getRegionHeight(){
                        return image.getHeight();
                    }
                };

                regionCache.put(fname, region);
                imageCache.put(region, image);

            }catch(IOException e){
                e.printStackTrace();
            }
        });

        Core.atlas = new Atlas(){
            @Override
            public TextureRegion getRegion(String name){
                if(!regionCache.containsKey(name)){
                    GenRegion region = new GenRegion(name);
                    region.invalid = true;
                    return region;
                }
                return regionCache.get(name);
            }

            @Override
            public boolean hasRegion(String s) {
                return regionCache.containsKey(s);
            }
        };

        Core.atlas.setErrorRegion("error");
    }

    static void generate(String name, Runnable run){
        Timers.mark();
        run.run();
        Log.info("&ly[Generator]&lc Time to generate &lm{0}&lc: &lg{1}&lcms", name, Timers.elapsed());
    }

    static BufferedImage buf(TextureRegion region){
        return imageCache.get(region);
    }

    static Image create(int width, int height){
        return new Image(width, height);
    }

    static Image get(String name){
        return get(Core.atlas.getRegion(name));
    }

    static Image get(TextureRegion region){
        GenRegion.validate(region);

        return new Image(imageCache.get(region));
    }

    static void err(String message, Object... args){
        Log.err(message, args);
        System.exit(-1);
    }
}
