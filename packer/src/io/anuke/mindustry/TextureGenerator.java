package io.anuke.mindustry;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Region;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.core.ContentLoader;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.util.Atlas;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Log.NoopLogHandler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**Used for generating extra textures before packing.*/
public class TextureGenerator {
    static BufferedImage image;
    static Graphics2D graphics;

    public static void main(String[] args) throws Exception{
        Log.setLogger(new NoopLogHandler());

        ContentLoader.load();

        String spritesFolder = new File("../../../assets/sprites").getAbsolutePath();

        TextureAtlasData data = new TextureAtlasData(new FileHandle(spritesFolder + "/sprites.atlas"),
                new FileHandle(spritesFolder), false);

        ObjectMap<String, TextureRegion> regionCache = new ObjectMap<>();

        for(Region region : data.getRegions()){
            int x = region.left, y = region.top, width = region.width, height = region.height;

            regionCache.put(region.name, new TextureRegion(){

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
                return regionCache.get(name);
            }

            @Override
            public boolean hasRegion(String s) {
                return regionCache.containsKey(s);
            }
        };

        Core.atlas.setErrorRegion("error");

        image = ImageIO.read(new File(spritesFolder + "/sprites.png"));
        graphics = image.createGraphics();

        generateBlocks();
    }

    /**Generates full block icons for use in the editor.*/
    static void generateBlocks() throws IOException {

        for(Block block : Block.all()){
            TextureRegion[] regions = block.getBlockIcon();

            if(regions.length == 0){
                continue;
            }

            if(regions[0] == null){
                System.err.println("Error in block \"" + block.name + "\": null region!");
                System.exit(-1);
            }

            BufferedImage target = new BufferedImage(regions[0].getRegionWidth(), regions[0].getRegionHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D tg = target.createGraphics();

            for(TextureRegion region : regions){

                tg.drawImage(image,
                        0, 0,
                        region.getRegionWidth(),
                        region.getRegionHeight(),
                        region.getRegionX(),
                        region.getRegionY(),
                        region.getRegionX() + region.getRegionWidth(),
                        region.getRegionY() + region.getRegionHeight(),
                        null);
            }

            tg.dispose();

            ImageIO.write(target, "png", new File("block-icon-" + block.name + ".png"));
        }
    }
}
