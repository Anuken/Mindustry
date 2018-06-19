package io.anuke.mindustry;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Image {
    private static ArrayList<Image> toDispose = new ArrayList<>();

    private BufferedImage atlas;

    private BufferedImage image;
    private Graphics2D graphics;

    public Image(BufferedImage atlas, TextureRegion region){
        this.atlas = atlas;

        this.image = new BufferedImage(region.getRegionWidth(), region.getRegionHeight(), BufferedImage.TYPE_INT_ARGB);
        this.graphics = image.createGraphics();

        draw(region);

        toDispose.add(this);
    }

    /**Draws a region at the top left corner.*/
    public void draw(TextureRegion region){
        draw(region, 0, 0, false, false);
    }

    /**Draws an image at the top left corner.*/
    public void draw(Image image){
        graphics.drawImage(image.image, 0, 0, null);
    }

    public void draw(TextureRegion region, boolean flipx, boolean flipy){
        draw(region, 0, 0, flipx, flipy);
    }

    public void draw(TextureRegion region, int x, int y, boolean flipx, boolean flipy){
        int ofx = 0, ofy = 0;

        if(x < 0){
            ofx = x;
            x = 0;
        }

        if(y < 0){
            ofy = y;
            y = 0;
        }

        graphics.drawImage(atlas,
                x, y,
                x + region.getRegionWidth(),
                y + region.getRegionHeight(),
                (flipx ? region.getRegionX() + region.getRegionWidth() : region.getRegionX()) + ofx,
                (flipy ? region.getRegionY() + region.getRegionHeight() : region.getRegionY()) + ofy,
                (flipx ? region.getRegionX() : region.getRegionX() + region.getRegionWidth()) + ofx,
                (flipy ? region.getRegionY() : region.getRegionY() + region.getRegionHeight()) + ofy,
                null);
    }

    /** @param name Name of texture file name to create, without any extensions.*/
    public void save(String name){
        try {
            ImageIO.write(image, "png", new File(name + ".png"));
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static int total(){
        return toDispose.size();
    }

    public static void dispose(){
        for(Image image : toDispose){
            image.graphics.dispose();
        }
        toDispose.clear();
    }
}
