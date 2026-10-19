package arc.scene.style;

/**
 * A drawable knows how to draw itself at a given rectangular size. It provides border sizes and a minimum size so that other code
 * can determine how to size and position content.
 * @author Nathan Sweet
 */
public interface Drawable{
    /** Draws this drawable at the specified bounds. */
    void draw(float x, float y, float width, float height);

    void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation);

    float getLeftWidth();

    void setLeftWidth(float leftWidth);

    float getRightWidth();

    void setRightWidth(float rightWidth);

    float getTopHeight();

    void setTopHeight(float topHeight);

    float getBottomHeight();

    void setBottomHeight(float bottomHeight);

    float getMinWidth();

    void setMinWidth(float minWidth);

    float getMinHeight();

    void setMinHeight(float minHeight);

    default float imageSize(){
        return getMinWidth();
    }
}
