package io.anuke.mindustry;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.ucore.util.Structs;

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
    private Color color = new Color();

    public Image(BufferedImage atlas, TextureRegion region){
        this(atlas, region.getRegionWidth(), region.getRegionHeight());

        draw(region);
    }

    public Image(BufferedImage atlas, int width, int height){
        this.atlas = atlas;

        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.graphics = image.createGraphics();

        toDispose.add(this);
    }

    public int width(){
        return image.getWidth();
    }

    public int height(){
        return image.getHeight();
    }

    public boolean isEmpty(int x, int y){
        if(!Structs.inBounds(x, y, width(), height())){
            return true;
        }
        Color color = getColor(x, y);
        return color.a <= 0.001f;
    }

    public Color getColor(int x, int y){
        int i = image.getRGB(x, y);
        Color.argb8888ToColor(color, i);
        return color;
    }

    public void draw(int x, int y, Color color){
        graphics.setColor(new java.awt.Color(color.r, color.g, color.b, color.a));
        graphics.fillRect(x, y, 1, 1);
    }

    /**Draws a region at the top left corner.*/
    public void draw(TextureRegion region){
        draw(region, 0, 0, false, false);
    }

    /**Draws a region at the center.*/
    public void drawCenter(TextureRegion region){
        draw(region, (width() - region.getRegionWidth())/2, (height() - region.getRegionHeight())/2, false, false);
    }

    /**Draws a region at the center.*/
    public void drawCenter(TextureRegion region, boolean flipx, boolean flipy){
        draw(region, (width() - region.getRegionWidth())/2, (height() - region.getRegionHeight())/2, flipx, flipy);
    }

    /**Draws an image at the top left corner.*/
    public void draw(Image image){
        draw(image, 0, 0);
    }

    /**Draws an image at the coordinates specified.*/
    public void draw(Image image, int x, int y){
        graphics.drawImage(image.image, x, y, null);
    }

    public void draw(TextureRegion region, boolean flipx, boolean flipy){
        draw(region, 0, 0, flipx, flipy);
    }

    public void draw(TextureRegion region, int x, int y, boolean flipx, boolean flipy){
        GenRegion.validate(region);

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
