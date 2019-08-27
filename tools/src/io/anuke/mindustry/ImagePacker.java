package io.anuke.mindustry;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.g2d.TextureAtlas.AtlasRegion;
import io.anuke.arc.util.*;
import io.anuke.arc.util.Log.LogHandler;
import io.anuke.arc.util.Log.NoopLogHandler;
import io.anuke.mindustry.core.ContentLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;

public class ImagePacker{
    static ObjectMap<String, TextureRegion> regionCache = new ObjectMap<>();
    static ObjectMap<TextureRegion, BufferedImage> imageCache = new ObjectMap<>();

    public static void main(String[] args) throws IOException{
        Vars.headless = true;

        Log.setLogger(new NoopLogHandler());
        Vars.content = new ContentLoader();
        Vars.content.createContent();
        Log.setLogger(new LogHandler());

        Files.walk(Paths.get("../../../assets-raw/sprites_out")).forEach(path -> {
            try{
                if(Files.isDirectory(path)) return;

                String fname = path.getFileName().toString();
                fname = fname.substring(0, fname.length() - 4);

                BufferedImage image = ImageIO.read(path.toFile());
                GenRegion region = new GenRegion(fname, path){

                    @Override
                    public int getX(){
                        return 0;
                    }

                    @Override
                    public int getY(){
                        return 0;
                    }

                    @Override
                    public int getWidth(){
                        return image.getWidth();
                    }

                    @Override
                    public int getHeight(){
                        return image.getHeight();
                    }
                };

                regionCache.put(fname, region);
                imageCache.put(region, image);

            }catch(IOException e){
                e.printStackTrace();
            }
        });

        Core.atlas = new TextureAtlas(){
            @Override
            public AtlasRegion find(String name){
                if(!regionCache.containsKey(name)){
                    GenRegion region = new GenRegion(name, null);
                    region.invalid = true;
                    return region;
                }
                return (AtlasRegion)regionCache.get(name);
            }

            @Override
            public AtlasRegion find(String name, TextureRegion def){
                if(!regionCache.containsKey(name)){
                    return (AtlasRegion)def;
                }
                return (AtlasRegion)regionCache.get(name);
            }

            @Override
            public boolean has(String s){
                return regionCache.containsKey(s);
            }
        };

        Draw.scl = 1f / Core.atlas.find("scale_marker").getWidth();

        Time.mark();
        Generators.generate();
        Log.info("&ly[Generator]&lc Total time to generate: &lg{0}&lcms", Time.elapsed());
        Log.info("&ly[Generator]&lc Total images created: &lg{0}", Image.total());
        Image.dispose();
    }

    static void generate(String name, Runnable run){
        Time.mark();
        run.run();
        Log.info("&ly[Generator]&lc Time to generate &lm{0}&lc: &lg{1}&lcms", name, Time.elapsed());
    }

    static BufferedImage buf(TextureRegion region){
        return imageCache.get(region);
    }

    static Image create(int width, int height){
        return new Image(width, height);
    }

    static Image get(String name){
        return get(Core.atlas.find(name));
    }

    static boolean has(String name){
        return Core.atlas.has(name);
    }

    static Image get(TextureRegion region){
        GenRegion.validate(region);

        return new Image(imageCache.get(region));
    }

    static void err(String message, Object... args){
        throw new IllegalArgumentException(Strings.format(message, args));
    }

    static class GenRegion extends AtlasRegion{
        String name;
        boolean invalid;
        Path path;

        GenRegion(String name, Path path){
            this.name = name;
            this.path = path;
        }

        static void validate(TextureRegion region){
            if(((GenRegion)region).invalid){
                ImagePacker.err("Region does not exist: {0}", ((GenRegion)region).name);
            }
        }
    }
}
