package io.anuke.mindustry;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.util.Structs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.ArrayList;

public class Image {
    private static ArrayList<Image> toDispose = new ArrayList<>();

    private BufferedImage image;
    private Graphics2D graphics;
    private Color color = new Color();

    public Image(TextureRegion region){
        this(ImageContext.buf(region));
    }

    public Image(BufferedImage src){
        this.image = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        this.graphics = image.createGraphics();
        this.graphics.drawImage(src, 0, 0, null);

        toDispose.add(this);
    }

    public Image(int width, int height){
        this(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
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
        draw(region, (width() - region.getWidth())/2, (height() - region.getHeight())/2, false, false);
    }

    /**Draws a region at the center.*/
    public void drawCenter(TextureRegion region, boolean flipx, boolean flipy){
        draw(region, (width() - region.getWidth())/2, (height() - region.getHeight())/2, flipx, flipy);
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

        graphics.drawImage(ImageContext.get(region).image,
                x, y,
                x + region.getWidth(),
                y + region.getHeight(),
                (flipx ?  region.getWidth() : 0) + ofx,
                (flipy ? region.getHeight() : 0) + ofy,
                (flipx ? 0 : region.getWidth()) + ofx,
                (flipy ? 0 : region.getHeight()) + ofy,
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
