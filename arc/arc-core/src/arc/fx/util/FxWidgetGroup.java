package arc.fx.util;

import arc.fx.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.util.viewport.*;

public class FxWidgetGroup extends WidgetGroup{
    private final FxProcessor fxProcessor;
    private boolean initialized = false;
    private boolean resizePending = false;
    private boolean matchWidgetSize = false;

    public FxWidgetGroup(){
        fxProcessor = new FxProcessor();
        super.setTransform(false);
    }

    public FxProcessor getFxProcessor(){
        return fxProcessor;
    }

    public boolean isMatchWidgetSize(){
        return matchWidgetSize;
    }

    /**
     * @param matchWidgetSize if true, the internal {@link FxProcessor} will be resized
     * to match {@link FxWidgetGroup}'s size (stage units and not screen pixels).
     */
    public void setMatchWidgetSize(boolean matchWidgetSize){
        if(this.matchWidgetSize == matchWidgetSize) return;

        this.matchWidgetSize = matchWidgetSize;
        resizePending = true;
    }

    @Override
    protected void setScene(Scene stage){
        super.setScene(stage);

        if(stage != null){
            initialize();
        }else{
            reset();
        }
    }

    @Override
    protected void sizeChanged(){
        super.sizeChanged();
        resizePending = true;
    }

    @Override
    public void draw(){
        Draw.flush();

        performPendingResize();

        fxProcessor.clear();
        fxProcessor.begin();

        validate();
        drawChildren();

        Draw.flush();

        fxProcessor.end();
        fxProcessor.applyEffects();

        // If something was captured, render result to the screen.
        if(fxProcessor.hasResult()){
            Color color = this.color;
            Draw.color(color.r, color.g, color.b, color.a * parentAlpha);
            Draw.rect(Draw.wrap(fxProcessor.getResultBuffer().texture), x + width / 2f, y + height / 2f, width, height);
        }
    }

    @Override
    protected void drawChildren(){
        boolean capturing = fxProcessor.isCapturing();

        if(capturing){
            // Imitate "transform" child drawing for when capturing into VfxManager.
            super.setTransform(true);
        }
        if(!capturing){
            // Clip children to VfxWidget area when not capturing into FBO.
            clipBegin();
        }

        super.drawChildren();
        Draw.flush();

        if(capturing){
            super.setTransform(false);
        }

        if(!capturing){
            clipEnd();
        }
    }

    @Override
    public void setCullingArea(Rect cullingArea){
        throw new UnsupportedOperationException("VfxWidgetGroup doesn't support culling area.");
    }

    @Override
    public void setTransform(boolean transform){
        throw new UnsupportedOperationException("VfxWidgetGroup doesn't support transform.");
    }

    private void initialize(){
        if(initialized) return;

        performPendingResize();

        resizePending = false;
        initialized = true;
    }

    private void reset(){
        if(!initialized) return;

        fxProcessor.dispose();

        resizePending = false;
        initialized = false;
    }

    private void performPendingResize(){
        if(!resizePending) return;

        final int width;
        final int height;

        // Size may be zero if the widget wasn't laid out yet.
        if((int)getWidth() == 0 || (int)getHeight() == 0){
            // If the size of the widget is not defined,
            // just resize to a small buffer to keep the memory footprint low.
            width = 16;
            height = 16;

        }else if(matchWidgetSize){
            // Set buffer to match the size of the widget.
            width = Mathf.floor(getWidth());
            height = Mathf.floor(getHeight());

        }else{
            // Set buffer to match the screen pixel density.
            Viewport viewport = getScene().getViewport();
            float ppu = viewport.getScreenWidth() / viewport.getWorldWidth();
            width = Mathf.floor(getWidth() * ppu);
            height = Mathf.floor(getHeight() * ppu);
        }

        fxProcessor.resize(width, height);

        resizePending = false;
    }
}
