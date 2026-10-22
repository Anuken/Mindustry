package arc.graphics.g2d;

import arc.graphics.*;

/** Defines a region of a pixmap, like a TextureRegion. */
public class PixmapRegion{
    public Pixmap pixmap;
    public int x, y, width, height;

    public PixmapRegion(Pixmap pixmap, int x, int y, int width, int height){
        set(pixmap, x, y, width, height);
    }

    public PixmapRegion(Pixmap pixmap){
        set(pixmap);
    }

    /** @return the RGBA value at a region position. */
    public int get(int x, int y){
        return pixmap.get(this.x + x, this.y + y);
    }

    /** @return the RGBA value at a region position without bounds checks. */
    public int getRaw(int x, int y){
        return pixmap.getRaw(this.x + x, this.y + y);
    }

    /** @return the alpha value at a region position, 0 - 255. */
    public int getA(int x, int y){
        return pixmap.getA(this.x + x, this.y + y);
    }

    public int get(int x, int y, Color color){
        int c = get(x, y);
        color.set(c);
        return c;
    }

    public PixmapRegion set(Pixmap pixmap){
        return set(pixmap, 0, 0, pixmap.width, pixmap.height);
    }

    public PixmapRegion set(Pixmap pixmap, int x, int y, int width, int height){
        this.pixmap = pixmap;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    /** Allocates a new pixmap based on this region data. */
    public Pixmap crop(){
        return Pixmaps.crop(pixmap, x, y, width, height);
    }

    /** Allocates a new pixmap with specific offsets. */
    public Pixmap crop(int x, int y, int width, int height){
        return Pixmaps.crop(pixmap, this.x + x, this.y + y, width, height);
    }
}
