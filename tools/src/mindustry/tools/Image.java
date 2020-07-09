package mindustry.tools;

import arc.func.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.tools.ImagePacker.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

class Image{
    private static Seq<Image> toDispose = new Seq<>();

    private BufferedImage image;
    private Graphics2D graphics;
    private Color color = new Color();

    public final int width, height;

    Image(TextureRegion region){
        this(ImagePacker.buf(region));
    }

    Image(BufferedImage src){
        this.image = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        this.graphics = image.createGraphics();
        this.graphics.drawImage(src, 0, 0, null);
        this.width = image.getWidth();
        this.height = image.getHeight();

        toDispose.add(this);
    }

    Image(int width, int height){
        this(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
    }

    Image copy(){
        Image out =new Image(width, height);
        out.draw(this);
        return out;
    }

    boolean isEmpty(int x, int y){
        if(!Structs.inBounds(x, y, width, height)){
            return true;
        }
        Color color = getColor(x, y);
        return color.a <= 0.001f;
    }

    Color getColor(int x, int y){
        if(!Structs.inBounds(x, y, width, height)) return color.set(0, 0, 0, 0);
        int i = image.getRGB(x, y);
        color.argb8888(i);
        return color;
    }

    void each(Intc2 cons){
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                cons.get(x, y);
            }
        }
    }

    void draw(int x, int y, Color color){
        graphics.setColor(new java.awt.Color(color.r, color.g, color.b, color.a));
        graphics.fillRect(x, y, 1, 1);
    }


    /** Draws a region at the top left corner. */
    void draw(TextureRegion region){
        draw(region, 0, 0, false, false);
    }

    /** Draws a region at the center. */
    void drawCenter(TextureRegion region){
        draw(region, (width - region.getWidth()) / 2, (height - region.getHeight()) / 2, false, false);
    }

    /** Draws a region at the center. */
    void drawCenter(TextureRegion region, boolean flipx, boolean flipy){
        draw(region, (width - region.getWidth()) / 2, (height - region.getHeight()) / 2, flipx, flipy);
    }

    void drawScaled(Image image){
        graphics.drawImage(image.image.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, width, height, null);
    }

    /** Draws an image at the top left corner. */
    void draw(Image image){
        draw(image, 0, 0);
    }

    /** Draws an image at the coordinates specified. */
    void draw(Image image, int x, int y){
        graphics.drawImage(image.image, x, y, null);
    }

    void draw(TextureRegion region, boolean flipx, boolean flipy){
        draw(region, 0, 0, flipx, flipy);
    }

    void draw(TextureRegion region, int x, int y, boolean flipx, boolean flipy){
        GenRegion.validate(region);

        int ofx = 0, ofy = 0;

        graphics.drawImage(ImagePacker.get(region).image,
        x, y,
        x + region.getWidth(),
        y + region.getHeight(),
        (flipx ? region.getWidth() : 0) + ofx,
        (flipy ? region.getHeight() : 0) + ofy,
        (flipx ? 0 : region.getWidth()) + ofx,
        (flipy ? 0 : region.getHeight()) + ofy,
        null);
    }

    /** @param name Name of texture file name to create, without any extensions. */
    void save(String name){
        try{
            ImageIO.write(image, "png", new File(name + ".png"));
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    void save(String name, boolean antialias){
        save(name);
        if(!antialias){
            new File(name + ".png").setLastModified(0);
        }
    }

    static int total(){
        return toDispose.size;
    }

    static void dispose(){
        for(Image image : toDispose){
            image.graphics.dispose();
        }
        toDispose.clear();
    }
}
