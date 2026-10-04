package arc.scene.style;

/** A drawable that draws two drawables stacked on top of each other (first, then second). Size-setting methods are proxied to both drawables. */
public class StackDrawable implements TransformDrawable{
    public Drawable first, second;

    public StackDrawable(){
    }

    public StackDrawable(Drawable first, Drawable second){
        this.first = first;
        this.second = second;
    }

    @Override
    public void draw(float x, float y, float width, float height){
        first.draw(x, y, width, height);
        second.draw(x, y, width, height);
    }

    @Override
    public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation){
        drawTransformed(first, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        drawTransformed(second, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
    }

    private void drawTransformed(Drawable drawable, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation){
        if(drawable instanceof TransformDrawable){
            drawable.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        }else{
            drawable.draw(x, y, width, height);
        }
    }

    @Override
    public float getLeftWidth(){
        return first.getLeftWidth();
    }

    @Override
    public void setLeftWidth(float leftWidth){
        first.setLeftWidth(leftWidth);
        second.setLeftWidth(leftWidth);
    }

    @Override
    public float getRightWidth(){
        return first.getRightWidth();
    }

    @Override
    public void setRightWidth(float rightWidth){
        first.setRightWidth(rightWidth);
        second.setRightWidth(rightWidth);
    }

    @Override
    public float getTopHeight(){
        return first.getTopHeight();
    }

    @Override
    public void setTopHeight(float topHeight){
        first.setTopHeight(topHeight);
        second.setTopHeight(topHeight);
    }

    @Override
    public float getBottomHeight(){
        return first.getBottomHeight();
    }

    @Override
    public void setBottomHeight(float bottomHeight){
        first.setBottomHeight(bottomHeight);
        second.setBottomHeight(bottomHeight);
    }

    @Override
    public float getMinWidth(){
        return Math.max(first.getMinWidth(), second.getMinWidth());
    }

    @Override
    public void setMinWidth(float minWidth){
        first.setMinWidth(minWidth);
        second.setMinWidth(minWidth);
    }

    @Override
    public float getMinHeight(){
        return Math.max(first.getMinHeight(), second.getMinHeight());
    }

    @Override
    public void setMinHeight(float minHeight){
        first.setMinHeight(minHeight);
        second.setMinHeight(minHeight);
    }
}