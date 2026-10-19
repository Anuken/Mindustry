package arc.scene.style;

import arc.graphics.g2d.NinePatch;
import arc.scene.ui.layout.*;

public class ScaledNinePatchDrawable extends NinePatchDrawable{
    private float scale = Scl.scl(1f);

    public ScaledNinePatchDrawable(NinePatch patch){
        this(patch, 1f);
    }

    public ScaledNinePatchDrawable(NinePatch patch, float multiplier){
        this.scale = Scl.scl(multiplier);

        setPatch(patch);
    }

    public ScaledNinePatchDrawable(NinePatchDrawable drawable){
        super(drawable);
    }

    @Override
    public void draw(float x, float y, float width, float height){
        getPatch().draw(x, y, 0, 0, width / scale, height / scale, scale, scale, 0);
    }

    @Override
    public void setPatch(NinePatch patch){
        super.setPatch(patch);

        setMinWidth(patch.getTotalWidth() * scale);
        setMinHeight(patch.getTotalHeight() * scale);
    }

    @Override
    public float getLeftWidth(){
        return patch.getPadLeft() * scale;
    }

    @Override
    public float getRightWidth(){
        return patch.getPadRight() * scale;
    }

    @Override
    public float getTopHeight(){
        return patch.getPadTop() * scale;
    }

    @Override
    public float getBottomHeight(){
        return patch.getPadBottom() * scale;
    }

}
