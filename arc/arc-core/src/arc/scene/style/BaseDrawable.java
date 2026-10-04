package arc.scene.style;

/**
 * Drawable that stores the size information but doesn't draw anything.
 * @author Nathan Sweet
 */
public class BaseDrawable implements Drawable{
    private String name;
    private float leftWidth, rightWidth, topHeight, bottomHeight, minWidth, minHeight;

    public BaseDrawable(){
    }

    /** Creates a new empty drawable with the same sizing information as the specified drawable. */
    public BaseDrawable(Drawable drawable){
        if(drawable instanceof BaseDrawable) name = ((BaseDrawable)drawable).getName();
        leftWidth = drawable.getLeftWidth();
        rightWidth = drawable.getRightWidth();
        topHeight = drawable.getTopHeight();
        bottomHeight = drawable.getBottomHeight();
        minWidth = drawable.getMinWidth();
        minHeight = drawable.getMinHeight();
    }

    @Override
    public void draw(float x, float y, float width, float height){
    }

    @Override
    public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX,
                     float scaleY, float rotation){
    }

    @Override
    public float getLeftWidth(){
        return leftWidth;
    }

    @Override
    public void setLeftWidth(float leftWidth){
        this.leftWidth = leftWidth;
    }

    @Override
    public float getRightWidth(){
        return rightWidth;
    }

    @Override
    public void setRightWidth(float rightWidth){
        this.rightWidth = rightWidth;
    }

    @Override
    public float getTopHeight(){
        return topHeight;
    }

    @Override
    public void setTopHeight(float topHeight){
        this.topHeight = topHeight;
    }

    @Override
    public float getBottomHeight(){
        return bottomHeight;
    }

    @Override
    public void setBottomHeight(float bottomHeight){
        this.bottomHeight = bottomHeight;
    }

    @Override
    public float getMinWidth(){
        return minWidth;
    }

    @Override
    public void setMinWidth(float minWidth){
        this.minWidth = minWidth;
    }

    @Override
    public float getMinHeight(){
        return minHeight;
    }

    @Override
    public void setMinHeight(float minHeight){
        this.minHeight = minHeight;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String toString(){
        if(name == null) return getClass().toString();
        return name;
    }
}
