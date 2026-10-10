package arc.graphics;

import arc.*;
import arc.graphics.gl.*;

/** Framebuffer attachment formats. */
public enum Format{
    //color - standard
    colorRgba8(Gl.colorAttachment0, Gl.rgba8, Gl.rgba, Gl.unsignedByte),
    colorRgba4(Gl.colorAttachment0, Gl.rgba4, Gl.rgba, Gl.unsignedShort4444),
    colorRgb565(Gl.colorAttachment0, Gl.rgb565, Gl.rgb, Gl.unsignedShort565),
    colorRgb5A1(Gl.colorAttachment0, Gl.rgb5A1, Gl.rgba, Gl.unsignedShort5551),
    colorRgb10A2(Gl.colorAttachment0, Gl.rgb10A2, Gl.rgba, Gl.unsignedInt2101010Rev),
    colorSrgb8Alpha8(Gl.colorAttachment0, Gl.srgb8Alpha8, Gl.rgba, Gl.unsignedByte),

    //color - HDR
    colorRgba16f(Gl.colorAttachment0, Gl.rgba16f, Gl.rgba, Gl.halfFloat),
    colorRgba32f(Gl.colorAttachment0, Gl.rgba32f, Gl.rgba, Gl.floatV),
    colorR11fG11fB10f(Gl.colorAttachment0, Gl.r11fG11fB10f, Gl.rgb, Gl.unsignedInt10f11f11fRev),

    //color - single channel
    colorR8(Gl.colorAttachment0, Gl.r8, Gl.red, Gl.unsignedByte),
    colorR16f(Gl.colorAttachment0, Gl.r16f, Gl.red, Gl.halfFloat),
    colorR32f(Gl.colorAttachment0, Gl.r32f, Gl.red, Gl.floatV),

    //color - two channel
    colorRg8(Gl.colorAttachment0, Gl.rg8, Gl.rg, Gl.unsignedByte),
    colorRg16f(Gl.colorAttachment0, Gl.rg16f, Gl.rg, Gl.halfFloat),
    colorRg32f(Gl.colorAttachment0, Gl.rg32f, Gl.rg, Gl.floatV),

    //color - integer RGBA
    colorRgba8i(Gl.colorAttachment0, Gl.rgba8i, Gl.rgbaInteger, Gl.byteV),
    colorRgba8ui(Gl.colorAttachment0, Gl.rgba8ui, Gl.rgbaInteger, Gl.unsignedByte),
    colorRgba16i(Gl.colorAttachment0, Gl.rgba16i, Gl.rgbaInteger, Gl.shortV),
    colorRgba16ui(Gl.colorAttachment0, Gl.rgba16ui, Gl.rgbaInteger, Gl.unsignedShort),
    colorRgba32i(Gl.colorAttachment0, Gl.rgba32i, Gl.rgbaInteger, Gl.intV),
    colorRgba32ui(Gl.colorAttachment0, Gl.rgba32ui, Gl.rgbaInteger, Gl.unsignedInt),
    colorRgb10A2ui(Gl.colorAttachment0, Gl.rgb10A2ui, Gl.rgbaInteger, Gl.unsignedInt2101010Rev),

    //color - integer RG
    colorRg8i(Gl.colorAttachment0, Gl.rg8i, Gl.rgInteger, Gl.byteV),
    colorRg8ui(Gl.colorAttachment0, Gl.rg8ui, Gl.rgInteger, Gl.unsignedByte),
    colorRg16i(Gl.colorAttachment0, Gl.rg16i, Gl.rgInteger, Gl.shortV),
    colorRg16ui(Gl.colorAttachment0, Gl.rg16ui, Gl.rgInteger, Gl.unsignedShort),
    colorRg32i(Gl.colorAttachment0, Gl.rg32i, Gl.rgInteger, Gl.intV),
    colorRg32ui(Gl.colorAttachment0, Gl.rg32ui, Gl.rgInteger, Gl.unsignedInt),

    //color - integer single channel
    colorR8i(Gl.colorAttachment0, Gl.r8i, Gl.redInteger, Gl.byteV),
    colorR8ui(Gl.colorAttachment0, Gl.r8ui, Gl.redInteger, Gl.unsignedByte),
    colorR16i(Gl.colorAttachment0, Gl.r16i, Gl.redInteger, Gl.shortV),
    colorR16ui(Gl.colorAttachment0, Gl.r16ui, Gl.redInteger, Gl.unsignedShort),
    colorR32i(Gl.colorAttachment0, Gl.r32i, Gl.redInteger, Gl.intV),
    colorR32ui(Gl.colorAttachment0, Gl.r32ui, Gl.redInteger, Gl.unsignedInt),

    //depth
    depth16(Gl.depthAttachment, Gl.depthComponent16, Gl.depthComponent, Gl.unsignedShort),
    depth24(Gl.depthAttachment, Gl.depthComponent24, Gl.depthComponent, Gl.unsignedInt),
    depth32f(Gl.depthAttachment, Gl.depthComponent32f, Gl.depthComponent, Gl.floatV),

    //depth + stencil
    depthStencil24(Gl.depthStencilAttachment, Gl.depth24Stencil8, Gl.depthStencil, Gl.unsignedInt248),
    depthStencil32f(Gl.depthStencilAttachment, Gl.depth32fStencil8, Gl.depthStencil, Gl.float32UnsignedInt248Rev),

    //stencil
    stencil8(Gl.stencilAttachment, Gl.stencilIndex8, Gl.stencilIndex, Gl.unsignedByte),
    ;

    /** Default values for common framebuffer configurations. */
    public static final Format[]
    defaultColor = {colorRgba8},
    defaultColorDepth = {colorRgba8, depth24},
    defaultColorStencil = {colorRgba8, depthStencil24}
    ;

    public final int attachmentPoint, glType, baseFormat, baseType;

    Format(int attachmentPoint, int glType, int baseFormat, int baseType){
        this.attachmentPoint = attachmentPoint;
        this.glType = glType;
        this.baseFormat = baseFormat;
        this.baseType = baseType;
    }

    public boolean isLinearFilterable(){
        return isColor() && !isIntegerFormat() && (baseType != Gl.floatV || Core.graphics.getGLVersion().type != GLVersion.GlType.GLES || supportsLinearFloatExtension());
    }

    public boolean isIntegerFormat(){
        return baseFormat == Gl.rgbaInteger || baseFormat == Gl.rgInteger || baseFormat == Gl.redInteger;
    }

    public boolean isColor(){
        return attachmentPoint == Gl.colorAttachment0;
    }

    public boolean isDepth(){
        return attachmentPoint == Gl.depthAttachment || attachmentPoint == Gl.depthStencilAttachment;
    }

    public boolean isStencil(){
        return attachmentPoint == Gl.stencilAttachment || attachmentPoint == Gl.depthStencilAttachment;
    }

    private static boolean queriedFloatExt, supportsLinearFloat;

    private static boolean supportsLinearFloatExtension(){
        if(!queriedFloatExt){
            supportsLinearFloat = Core.graphics.supportsExtension("OES_texture_float_linear");
            queriedFloatExt = true;
        }
        return supportsLinearFloat;
    }
}
