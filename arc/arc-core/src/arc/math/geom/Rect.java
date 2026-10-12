package arc.math.geom;

import arc.util.*;

/**
 * Encapsulates a 2D rectangle defined by its corner point in the bottom left and its extents in x (width) and y (height).
 * @author badlogicgames@gmail.com
 */
public class Rect implements Shape2D{
    /** Static temporary rectangle. Use with care! Use only when sure other code will not also use this. */
    public static final Rect tmp = new Rect();

    /** Static temporary rectangle. Use with care! Use only when sure other code will not also use this. */
    public static final Rect tmp2 = new Rect();

    public float x, y;
    public float width, height;

    /** Constructs a new rectangle with all values set to zero */
    public Rect(){

    }

    /**
     * Constructs a new rectangle with the given corner point in the bottom left and dimensions.
     * @param x The corner point x-coordinate
     * @param y The corner point y-coordinate
     * @param width The width
     * @param height The height
     */
    public Rect(float x, float y, float width, float height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Constructs a rectangle based on the given rectangle
     * @param rect The rectangle
     */
    public Rect(Rect rect){
        x = rect.x;
        y = rect.y;
        width = rect.width;
        height = rect.height;
    }

    public Rect setCentered(float x, float y, float size){
        return set(x - size/2f, y - size/2f, size, size);
    }

    public Rect setCentered(float x, float y, float width, float height){
        return set(x - width/2f, y - height/2f, width, height);
    }

    /**
     * @param x bottom-left x coordinate
     * @param y bottom-left y coordinate
     * @param width width
     * @param height height
     * @return this rectangle for chaining
     */
    public Rect set(float x, float y, float width, float height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        return this;
    }

    /** @return the x-coordinate of the bottom left corner */
    public float getX(){
        return x;
    }

    /**
     * Sets the x-coordinate of the bottom left corner
     * @param x The x-coordinate
     * @return this rectangle for chaining
     */
    public Rect setX(float x){
        this.x = x;

        return this;
    }

    /** @return the y-coordinate of the bottom left corner */
    public float getY(){
        return y;
    }

    /**
     * Sets the y-coordinate of the bottom left corner
     * @param y The y-coordinate
     * @return this rectangle for chaining
     */
    public Rect setY(float y){
        this.y = y;

        return this;
    }

    /** @return the width */
    public float getWidth(){
        return width;
    }

    /**
     * Sets the width of this rectangle
     * @param width The width
     * @return this rectangle for chaining
     */
    public Rect setWidth(float width){
        this.width = width;

        return this;
    }

    /** @return the height */
    public float getHeight(){
        return height;
    }

    /**
     * Sets the height of this rectangle
     * @param height The height
     * @return this rectangle for chaining
     */
    public Rect setHeight(float height){
        this.height = height;

        return this;
    }

    /**
     * return the Vec2 with coordinates of this rectangle
     * @param position The Vec2
     */
    public Vec2 getPosition(Vec2 position){
        return position.set(x, y);
    }

    /**
     * Sets the x and y-coordinates of the bottom left corner from vector
     * @param position The position vector
     * @return this rectangle for chaining
     */
    public Rect setPosition(Vec2 position){
        this.x = position.x;
        this.y = position.y;

        return this;
    }

    /**
     * Sets the x and y-coordinates of the bottom left corner
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return this rectangle for chaining
     */
    public Rect setPosition(float x, float y){
        this.x = x;
        this.y = y;

        return this;
    }

    public Rect move(float cx, float cy){
        x += cx;
        y += cy;
        return this;
    }

    /**
     * Sets the width and height of this rectangle
     * @param width The width
     * @param height The height
     * @return this rectangle for chaining
     */
    public Rect setSize(float width, float height){
        this.width = width;
        this.height = height;

        return this;
    }

    /**
     * Sets the squared size of this rectangle
     * @param sizeXY The size
     * @return this rectangle for chaining
     */
    public Rect setSize(float sizeXY){
        this.width = sizeXY;
        this.height = sizeXY;

        return this;
    }

    /**
     * @param size The Vec2
     * @return the Vec2 with size of this rectangle
     */
    public Vec2 getSize(Vec2 size){
        return size.set(width, height);
    }

    public static boolean contains(float x, float y, float width, float height, float px, float py){
        return x <= px && x + width >= px && y <= py && y + height >= py;
    }

    public static boolean contains(float x, float y, float width, float height, float rx, float ry, float rwidth, float rheight){
        float xmax = rx + rwidth;

        float ymax = ry + rheight;

        return ((rx > x && rx < x + width) && (xmax > x && xmax < x + width))
        && ((ry > y && ry < y + height) && (ymax > y && ymax < y + height));
    }

    /**
     * @param x point x coordinate
     * @param y point y coordinate
     * @return whether the point is contained in the rectangle
     */
    public boolean contains(float x, float y){
        return this.x <= x && this.x + this.width >= x && this.y <= y && this.y + this.height >= y;
    }

    /**
     * @param point The coordinates vector
     * @return whether the point is contained in the rectangle
     */
    public boolean contains(Vec2 point){
        return contains(point.x, point.y);
    }

    /**
     * @param circle the circle
     * @return whether the circle is contained in the rectangle
     */
    public boolean contains(Circle circle){
        return (circle.x - circle.radius >= x) && (circle.x + circle.radius <= x + width)
        && (circle.y - circle.radius >= y) && (circle.y + circle.radius <= y + height);
    }

    /**
     * @param rect the other {@link Rect}.
     * @return whether the other rectangle is contained in this rectangle.
     */
    public boolean contains(Rect rect){
        float xmin = rect.x;
        float xmax = xmin + rect.width;

        float ymin = rect.y;
        float ymax = ymin + rect.height;

        return ((xmin > x && xmin < x + width) && (xmax > x && xmax < x + width))
        && ((ymin > y && ymin < y + height) && (ymax > y && ymax < y + height));
    }

    /**
     * @param r the other {@link Rect}
     * @return whether this rectangle overlaps the other rectangle.
     */
    public boolean overlaps(Rect r){
        return x < r.x + r.width && x + width > r.x && y < r.y + r.height && y + height > r.y;
    }

    /**
     * @return whether this rectangle overlaps the other rectangle.
     */
    public boolean overlaps(float rx, float ry, float rwidth, float rheight){
        return x < rx + rwidth && x + width > rx && y < ry + rheight && y + height > ry;
    }

    /**
     * Sets the values of the given rectangle to this rectangle.
     * @param rect the other rectangle
     * @return this rectangle for chaining
     */
    public Rect set(Rect rect){
        this.x = rect.x;
        this.y = rect.y;
        this.width = rect.width;
        this.height = rect.height;

        return this;
    }

    public Rect grow(float amount){
        return grow(amount, amount);
    }

    public Rect grow(float amountX, float amountY){
        x -= amountX/2f;
        y -= amountY/2f;
        width += amountX;
        height += amountY;
        return this;
    }

    /**
     * Merges this rectangle with the other rectangle. The rectangle should not have negative width or negative height.
     * @param rect the other rectangle
     * @return this rectangle for chaining
     */
    public Rect merge(Rect rect){
        float minX = Math.min(x, rect.x);
        float maxX = Math.max(x + width, rect.x + rect.width);
        x = minX;
        width = maxX - minX;

        float minY = Math.min(y, rect.y);
        float maxY = Math.max(y + height, rect.y + rect.height);
        y = minY;
        height = maxY - minY;

        return this;
    }

    /** "fixes" negative size dimensions. */
    public Rect normalize(){
        if(width < 0){
            x += width;
            width = -width;
        }

        if(height < 0){
            y += height;
            height = -height;
        }
        return this;
    }

    /**
     * Merges this rectangle with a point. The rectangle should not have negative width or negative height.
     * @param x the x coordinate of the point
     * @param y the y coordinate of the point
     * @return this rectangle for chaining
     */
    public Rect merge(float x, float y){
        float minX = Math.min(this.x, x);
        float maxX = Math.max(this.x + width, x);
        this.x = minX;
        this.width = maxX - minX;

        float minY = Math.min(this.y, y);
        float maxY = Math.max(this.y + height, y);
        this.y = minY;
        this.height = maxY - minY;

        return this;
    }

    /**
     * Merges this rectangle with a point. The rectangle should not have negative width or negative height.
     * @param vec the vector describing the point
     * @return this rectangle for chaining
     */
    public Rect merge(Vec2 vec){
        return merge(vec.x, vec.y);
    }

    /**
     * Merges this rectangle with a list of points. The rectangle should not have negative width or negative height.
     * @param vecs the vectors describing the points
     * @return this rectangle for chaining
     */
    public Rect merge(Vec2[] vecs){
        float minX = x;
        float maxX = x + width;
        float minY = y;
        float maxY = y + height;
        for(int i = 0; i < vecs.length; ++i){
            Vec2 v = vecs[i];
            minX = Math.min(minX, v.x);
            maxX = Math.max(maxX, v.x);
            minY = Math.min(minY, v.y);
            maxY = Math.max(maxY, v.y);
        }
        x = minX;
        width = maxX - minX;
        y = minY;
        height = maxY - minY;
        return this;
    }

    /**
     * Calculates the aspect ratio ( width / height ) of this rectangle
     * @return the aspect ratio of this rectangle. Returns Float.NaN if height is 0 to avoid ArithmeticException
     */
    public float getAspectRatio(){
        return (height == 0) ? Float.NaN : width / height;
    }

    /**
     * Calculates the center of the rectangle. Results are located in the given Vec2
     * @param vector the Vec2 to use
     * @return the given vector with results stored inside
     */
    public Vec2 getCenter(Vec2 vector){
        vector.x = x + width / 2;
        vector.y = y + height / 2;
        return vector;
    }

    /**
     * Moves this rectangle so that its center point is located at a given position
     * @param x the position's x
     * @param y the position's y
     * @return this for chaining
     */
    public Rect setCenter(float x, float y){
        setPosition(x - width / 2, y - height / 2);
        return this;
    }

    /**
     * Moves this rectangle so that its center point is located at a given position
     * @param position the position
     * @return this for chaining
     */
    public Rect setCenter(Vec2 position){
        setPosition(position.x - width / 2, position.y - height / 2);
        return this;
    }

    /**
     * Fits this rectangle around another rectangle while maintaining aspect ratio. This scales and centers the rectangle to the
     * other rectangle (e.g. Having a camera translate and scale to show a given area)
     * @param rect the other rectangle to fit this rectangle around
     * @return this rectangle for chaining
     * @see Scaling
     */
    public Rect fitOutside(Rect rect){
        float ratio = getAspectRatio();

        if(ratio > rect.getAspectRatio()){
            // Wider than tall
            setSize(rect.height * ratio, rect.height);
        }else{
            // Taller than wide
            setSize(rect.width, rect.width / ratio);
        }

        setPosition((rect.x + rect.width / 2) - width / 2, (rect.y + rect.height / 2) - height / 2);
        return this;
    }

    /**
     * Fits this rectangle into another rectangle while maintaining aspect ratio. This scales and centers the rectangle to the
     * other rectangle (e.g. Scaling a texture within a arbitrary cell without squeezing)
     * @param rect the other rectangle to fit this rectangle inside
     * @return this rectangle for chaining
     * @see Scaling
     */
    public Rect fitInside(Rect rect){
        float ratio = getAspectRatio();

        if(ratio < rect.getAspectRatio()){
            // Taller than wide
            setSize(rect.height * ratio, rect.height);
        }else{
            // Wider than tall
            setSize(rect.width, rect.width / ratio);
        }

        setPosition((rect.x + rect.width / 2) - width / 2, (rect.y + rect.height / 2) - height / 2);
        return this;
    }

    /**
     * Converts this {@code Rectangle} to a string in the format {@code [x,y,width,height]}.
     * @return a string representation of this object.
     */
    public String toString(){
        return "[" + x + "," + y + "," + width + "," + height + "]";
    }

    /**
     * Sets this {@code Rectangle} to the value represented by the specified string according to the format of {@link #toString()}
     * .
     * @param v the string.
     * @return this rectangle for chaining
     */
    public Rect fromString(String v){
        int s0 = v.indexOf(',', 1);
        int s1 = v.indexOf(',', s0 + 1);
        int s2 = v.indexOf(',', s1 + 1);
        if(s0 != -1 && s1 != -1 && s2 != -1 && v.charAt(0) == '[' && v.charAt(v.length() - 1) == ']'){
            try{
                float x = Float.parseFloat(v.substring(1, s0));
                float y = Float.parseFloat(v.substring(s0 + 1, s1));
                float width = Float.parseFloat(v.substring(s1 + 1, s2));
                float height = Float.parseFloat(v.substring(s2 + 1, v.length() - 1));
                return this.set(x, y, width, height);
            }catch(NumberFormatException ex){
                // Throw a ArcRuntimeException
            }
        }
        throw new ArcRuntimeException("Malformed Rectangle: " + v);
    }

    public float area(){
        return this.width * this.height;
    }

    public float perimeter(){
        return 2 * (this.width + this.height);
    }

    public int hashCode(){
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToRawIntBits(height);
        result = prime * result + Float.floatToRawIntBits(width);
        result = prime * result + Float.floatToRawIntBits(x);
        result = prime * result + Float.floatToRawIntBits(y);
        return result;
    }

    public boolean equals(Object obj){
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        Rect other = (Rect)obj;
        if(Float.floatToRawIntBits(height) != Float.floatToRawIntBits(other.height)) return false;
        if(Float.floatToRawIntBits(width) != Float.floatToRawIntBits(other.width)) return false;
        if(Float.floatToRawIntBits(x) != Float.floatToRawIntBits(other.x)) return false;
        return Float.floatToRawIntBits(y) == Float.floatToRawIntBits(other.y);
    }

}
