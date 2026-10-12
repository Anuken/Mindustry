package arc.fx.filters;

import arc.*;
import arc.fx.*;
import arc.math.geom.*;
import arc.util.*;

public class CrtFilter extends FxFilter{
    public final Vec2 viewportSize = new Vec2();
    public SizeSource sizeSource = SizeSource.VIEWPORT;

    public CrtFilter(){
        this(LineStyle.HORIZONTAL_HARD, 1.3f, 0.5f);
    }

    /** Brightness is a value between [0..2] (default is 1.0). */
    public CrtFilter(LineStyle lineStyle, float brightnessMin, float brightnessMax){
        super(compileShader(
        Core.files.internal("vfxshaders/screenspace.vert"),
        Core.files.internal("vfxshaders/crt.frag"),
        "#define SL_BRIGHTNESS_MIN " + brightnessMin + "\n" +
        "#define SL_BRIGHTNESS_MAX " + brightnessMax + "\n" +
        "#define LINE_TYPE " + lineStyle.ordinal()));
        rebind();
    }

    @Override
    public void resize(int width, int height){
        this.viewportSize.set(width, height);
        rebind();
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        shader.setUniformf("u_resolution", sizeSource == SizeSource.SCREEN ? Tmp.v1.set(Core.graphics.getWidth(), Core.graphics.getHeight()) : viewportSize);
    }

    /** Shader resolution parameter source. */
    public enum SizeSource{
        /** Resolution will be defined by the application internal viewport. */
        VIEWPORT,
        /** Resolution will be defined by the application window size. */
        SCREEN,
    }

    public enum LineStyle{
        CROSSLINE_HARD,
        VERTICAL_HARD,
        HORIZONTAL_HARD,
        VERTICAL_SMOOTH,
        HORIZONTAL_SMOOTH,
    }
}
