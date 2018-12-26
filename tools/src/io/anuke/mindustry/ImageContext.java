package io.anuke.mindustry;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.graphics.g2d.TextureAtlas;
import io.anuke.arc.graphics.g2d.TextureAtlas.AtlasRegion;
import io.anuke.arc.graphics.g2d.TextureAtlas.TextureAtlasData;
import io.anuke.arc.graphics.g2d.TextureAtlas.TextureAtlasData.Region;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Log.LogHandler;
import io.anuke.arc.util.Log.NoopLogHandler;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.core.ContentLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageContext {
    private BufferedImage image;

    public void load() throws IOException{
        Log.setLogger(new NoopLogHandler());
        Vars.content = new ContentLoader();
        Vars.content.load();
        Log.setLogger(new LogHandler());

        String spritesFolder = new File("../../../assets/sprites").getAbsolutePath();
        TextureAtlasData data = new TextureAtlasData(new FileHandle(spritesFolder + "/sprites.atlas"),
                new FileHandle(spritesFolder), false);

        ObjectMap<String, AtlasRegion> regionCache = new ObjectMap<>();

        for(Region region : data.getRegions()){
            int x = region.left, y = region.top, width = region.width, height = region.height;

            regionCache.put(region.name, new GenRegion(){
                {
                    name = region.name;
                    context = ImageContext.this;
                }

                @Override
                public int getX(){
                    return x;
                }

                @Override
                public int getY(){
                    return y;
                }

                @Override
                public int getWidth(){
                    return width;
                }

                @Override
                public int getHeight(){
                    return height;
                }
            });
        }

        Core.atlas = new TextureAtlas(){
            @Override
            public AtlasRegion find(String name){
                if(!regionCache.containsKey(name)){
                    GenRegion region = new GenRegion();
                    region.name = name;
                    region.context = ImageContext.this;
                    region.invalid = true;
                    return region;
                }
                return regionCache.get(name);
            }

            @Override
            public boolean has(String s) {
                return regionCache.containsKey(s);
            }
        };

        image = ImageIO.read(new File(spritesFolder + "/sprites.png"));
    }

    public void generate(String name, Runnable run){
        Time.mark();
        run.run();
        Log.info("&ly[Generator]&lc Time to generate &lm{0}&lc: &lg{1}&lcms", name, Time.elapsed());
    }

    public Image create(int width, int height){
        return new Image(image, width, height);
    }

    public Image get(String name){
        return get(Core.atlas.find(name));
    }

    public Image get(TextureRegion region){
        GenRegion.validate(region);

        return new Image(image, region);
    }

    public void err(String message, Object... args){
        Log.err(message, args);
        System.exit(-1);
    }
}
