package arc.util;

/**
 * Provides bit flag constants for alignment.
 * @author Nathan Sweet
 */
public class Align{
    public static final int center = 1;
    public static final int top = 1 << 1;
    public static final int bottom = 1 << 2;
    public static final int left = 1 << 3;
    public static final int right = 1 << 4;

    public static final int topLeft = top | left;
    public static final int topRight = top | right;
    public static final int bottomLeft = bottom | left;
    public static final int bottomRight = bottom | right;

    public static boolean isLeft(int align){
        return (align & left) != 0;
    }

    public static boolean isRight(int align){
        return (align & right) != 0;
    }

    public static boolean isTop(int align){
        return (align & top) != 0;
    }

    public static boolean isBottom(int align){
        return (align & bottom) != 0;
    }

    public static boolean isCenterVertical(int align){
        return (align & top) == 0 && (align & bottom) == 0;
    }

    public static boolean isCenterHorizontal(int align){
        return (align & left) == 0 && (align & right) == 0;
    }

    public static String toString(int align){
        StringBuilder buffer = new StringBuilder(13);
        if((align & top) != 0)
            buffer.append("top,");
        else if((align & bottom) != 0)
            buffer.append("bottom,");
        else
            buffer.append("center,");
        if((align & left) != 0)
            buffer.append("left");
        else if((align & right) != 0)
            buffer.append("right");
        else
            buffer.append("center");
        return buffer.toString();
    }
}
