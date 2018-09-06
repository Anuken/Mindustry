package io.anuke.mindustry;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Region;
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

        ObjectMap<String, TextureRegion> regionCache = new ObjectMap<>();

        for(Region region : data.getRegions()){
            int x = region.left, y = region.top, width = region.width, height = region.height;

            regionCache.put(region.name, new GenRegion(){
                {
                    name = region.name;
                    context = ImageContext.this;
                }

                @Override
                public int getRegionX(){
                    return x;
                }

                @Override
                public int getRegionY(){
                    return y;
                }

                @Override
                public int getRegionWidth(){
                    return width;
                }

                @Override
                public int getRegionHeight(){
                    return height;
                }
            });
        }

        Core.atlas = new Atlas(){
            @Override
            public TextureRegion getRegion(String name){
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
            public boolean hasRegion(String s) {
                return regionCache.containsKey(s);
            }
        };

        Core.atlas.setErrorRegion("error");

        image = ImageIO.read(new File(spritesFolder + "/sprites.png"));
    }

    public void generate(String name, Runnable run){
        Timers.mark();
        run.run();
        Log.info("&ly[Generator]&lc Time to generate &lm{0}&lc: &lg{1}&lcms", name, Timers.elapsed());
    }

    public Image create(int width, int height){
        return new Image(image, width, height);
    }

    public Image get(String name){
        return get(Core.atlas.getRegion(name));
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
