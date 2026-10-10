package arc.graphics;

import arc.math.*;
import arc.math.geom.*;

/**
 * A color class, holding the r, g, b and alpha component as floats in the range [0,1]. All methods perform clamping on the
 * internal values after execution.
 * @author mzechner
 */
public class Color{
    public static final Color white = new Color(1, 1, 1, 1);
    public static final Color lightGray = new Color(0xbfbfbfff);
    public static final Color gray = new Color(0x7f7f7fff);
    public static final Color darkGray = new Color(0x3f3f3fff);
    public static final Color black = new Color(0, 0, 0, 1);
    public static final Color clear = new Color(0, 0, 0, 0);

    /** Convenience for frequently used <code>WHITE.toFloatBits()</code> */
    public static final float whiteFloatBits = white.toFloatBits();
    public static final float clearFloatBits = clear.toFloatBits();
    public static final float blackFloatBits = black.toFloatBits();

    public static final int whiteRgba = white.rgba();
    public static final int clearRgba = clear.rgba();
    public static final int blackRgba = black.rgba();

    public static final Color blue = new Color(0, 0, 1, 1);
    public static final Color navy = new Color(0, 0, 0.5f, 1);
    public static final Color royal = new Color(0x4169e1ff);
    public static final Color slate = new Color(0x708090ff);
    public static final Color sky = new Color(0x87ceebff);
    public static final Color cyan = new Color(0, 1, 1, 1);
    public static final Color teal = new Color(0, 0.5f, 0.5f, 1);

    public static final Color green = new Color(0x00ff00ff);
    public static final Color acid = new Color(0x7fff00ff);
    public static final Color lime = new Color(0x32cd32ff);
    public static final Color forest = new Color(0x228b22ff);
    public static final Color olive = new Color(0x6b8e23ff);

    public static final Color yellow = new Color(0xffff00ff);
    public static final Color gold = new Color(0xffd700ff);
    public static final Color goldenrod = new Color(0xdaa520ff);
    public static final Color orange = new Color(0xffa500ff);

    public static final Color brown = new Color(0x8b4513ff);
    public static final Color tan = new Color(0xd2b48cff);
    public static final Color brick = new Color(0xb22222ff);

    public static final Color red = new Color(0xff0000ff);
    public static final Color scarlet = new Color(0xff341cff);
    public static final Color crimson = new Color(0xdc143cff);
    public static final Color coral = new Color(0xff7f50ff);
    public static final Color salmon = new Color(0xfa8072ff);
    public static final Color pink = new Color(0xff69b4ff);
    public static final Color magenta = new Color(1, 0, 1, 1);

    public static final Color purple = new Color(0xa020f0ff);
    public static final Color violet = new Color(0xee82eeff);
    public static final Color maroon = new Color(0xb03060ff);

    private static final float[] tmpHSV = new float[3];

    /** the red, green, blue and alpha components **/
    public float r, g, b, a;

    /** Constructs a new Color with all components set to 0. */
    public Color(){
    }

    /** @see #rgba8888(int) */
    public Color(int rgba8888){
        rgba8888(rgba8888);
    }

    /**
     * Constructor, sets the components of the color
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @param a the alpha component
     */
    public Color(float r, float g, float b, float a){
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        clamp();
    }

    /**
     * Constructor, sets the components of the color
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     */
    public Color(float r, float g, float b){
        this(r, g, b, 1f);
    }

    /**
     * Constructs a new color using the given color
     * @param color the color
     */
    public Color(Color color){
        set(color);
    }

    /**
     * Returns a new color from a hex string with the format RRGGBBAA.
     * @see #toString()
     */
    public static Color valueOf(String hex){
        return valueOf(new Color(), hex);
    }

    /**
     * Returns a new color from a hex string with the format RRGGBBAA.
     * @see #toString()
     */
    public static Color valueOf(Color color, String hex){
        int offset = hex.charAt(0) == '#' ? 1 : 0;

        int r = parseHex(hex, offset, offset + 2);
        int g = parseHex(hex, offset + 2, offset + 4);
        int b = parseHex(hex, offset + 4, offset + 6);
        int a = hex.length() - offset != 8 ? 255 : parseHex(hex, offset + 6, offset + 8);
        return color.set(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    private static int parseHex(String string, int from, int to){
        int total = 0;
        for(int i = from; i < to; i++){
            char c = string.charAt(i);
            total += Character.digit(c, 16) * (i == from ? 16 : 1);
        }
        return total;
    }

    /**
     * Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Note that no range
     * checking is performed for higher performance.
     * @param r the red component, 0 - 255
     * @param g the green component, 0 - 255
     * @param b the blue component, 0 - 255
     * @param a the alpha component, 0 - 255
     * @return the packed color as a float
     * @see Color#intToFloatColor(int)
     */
    public static float toFloatBits(int r, int g, int b, int a){
        int color = (a << 24) | (b << 16) | (g << 8) | r;
        return intToFloatColor(color);
    }

    /**
     * Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float.
     * @return the packed color as a 32-bit float
     * @see Color#intToFloatColor(int)
     */
    public static float toFloatBits(float r, float g, float b, float a){
        int color = ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
        return intToFloatColor(color);
    }

    public static double toDoubleBits(float r, float g, float b, float a){
        return Double.longBitsToDouble(Color.rgba8888(r, g, b, a) & 0x00000000_ffffffffL);
    }

    public static double toDoubleBits(int r, int g, int b, int a){
        return toDoubleBits(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    public Color fromDouble(double value){
        return rgba8888((int)(Double.doubleToRawLongBits(value)));
    }

    /**
     * Packs the color components into a 32-bit integer with the format ABGR. Note that no range checking is performed for higher
     * performance.
     * @param r the red component, 0 - 255
     * @param g the green component, 0 - 255
     * @param b the blue component, 0 - 255
     * @param a the alpha component, 0 - 255
     * @return the packed color as a 32-bit int
     */
    public static int abgr(int r, int g, int b, int a){
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    public static int alpha(float alpha){
        return (int)(alpha * 255.0f);
    }

    public static int luminanceAlpha(float luminance, float alpha){
        return ((int)(luminance * 255.0f) << 8) | (int)(alpha * 255);
    }

    public static int rgb565(float r, float g, float b){
        return ((int)(r * 31) << 11) | ((int)(g * 63) << 5) | (int)(b * 31);
    }

    public static int rgba4444(float r, float g, float b, float a){
        return ((int)(r * 15) << 12) | ((int)(g * 15) << 8) | ((int)(b * 15) << 4) | (int)(a * 15);
    }

    public static int rgb888(float r, float g, float b){
        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    public static int rgba8888(float r, float g, float b, float a){
        return ((int)(r * 255) << 24) | ((int)(g * 255) << 16) | ((int)(b * 255) << 8) | (int)(a * 255);
    }

    public static int argb8888(float a, float r, float g, float b){
        return ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    /** @return 4 0-255 RGBA components packed into an int. */
    public static int packRgba(int r, int g, int b, int a){
        return (r << 24) | (g << 16) | (b << 8) | (a);
    }

    public int rgb565(){
        return ((int)(r * 31) << 11) | ((int)(g * 63) << 5) | (int)(b * 31);
    }

    public int rgba4444(){
        return ((int)(r * 15) << 12) | ((int)(g * 15) << 8) | ((int)(b * 15) << 4) | (int)(a * 15);
    }

    public int rgb888(){
        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    public int rgba8888(){
        return ((int)(r * 255) << 24) | ((int)(g * 255) << 16) | ((int)(b * 255) << 8) | (int)(a * 255);
    }

    public int argb8888(){
        return ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    /**
     * Sets the Color components using the specified integer value in the format RGB565. This is inverse to the rgb565(r, g, b)
     * method.
     * @param value An integer color value in RGB565 format.
     */
    public Color rgb565(int value){
        r = ((value & 0x0000F800) >>> 11) / 31f;
        g = ((value & 0x000007E0) >>> 5) / 63f;
        b = ((value & 0x0000001F)) / 31f;
        return this;
    }

    /**
     * Sets the Color components using the specified integer value in the format RGBA4444. This is inverse to the rgba4444(r, g,
     * b, a) method.
     * @param value An integer color value in RGBA4444 format.
     */
    public Color rgba4444(int value){
        r = ((value & 0x0000f000) >>> 12) / 15f;
        g = ((value & 0x00000f00) >>> 8) / 15f;
        b = ((value & 0x000000f0) >>> 4) / 15f;
        a = ((value & 0x0000000f)) / 15f;
        return this;
    }

    /**
     * Sets the Color components using the specified integer value in the format RGB888. This is inverse to the rgb888(r, g, b)
     * method.
     * @param value An integer color value in RGB888 format.
     */
    public Color rgb888(int value){
        r = ((value & 0x00ff0000) >>> 16) / 255f;
        g = ((value & 0x0000ff00) >>> 8) / 255f;
        b = ((value & 0x000000ff)) / 255f;
        return this;
    }

    /**
     * Sets the Color components using the specified integer value in the format RGBA8888. This is inverse to the rgba8888(r, g,
     * b, a) method.
     * @param value An integer color value in RGBA8888 format.
     */
    public Color rgba8888(int value){
        r = ((value & 0xff000000) >>> 24) / 255f;
        g = ((value & 0x00ff0000) >>> 16) / 255f;
        b = ((value & 0x0000ff00) >>> 8) / 255f;
        a = ((value & 0x000000ff)) / 255f;
        return this;
    }

    /**
     * Sets the Color components using the specified integer value in the format ARGB8888. This is the inverse to the argb8888(a,
     * r, g, b) method
     * @param value An integer color value in ARGB8888 format.
     */
    public Color argb8888(int value){
        a = ((value & 0xff000000) >>> 24) / 255f;
        r = ((value & 0x00ff0000) >>> 16) / 255f;
        g = ((value & 0x0000ff00) >>> 8) / 255f;
        b = ((value & 0x000000ff)) / 255f;
        return this;
    }

    /**
     * Sets the Color components using the specified float value in the format ABGB8888.
     */
    public Color abgr8888(float value){
        int c = floatToIntColor(value);
        a = ((c & 0xff000000) >>> 24) / 255f;
        b = ((c & 0x00ff0000) >>> 16) / 255f;
        g = ((c & 0x0000ff00) >>> 8) / 255f;
        r = ((c & 0x000000ff)) / 255f;
        return this;
    }

    /** Creates a grayscale color. */
    public static Color grays(float value){
        return new Color(value, value, value);
    }

    /** Creates a color from 0-255 scaled RGB values. */
    public static Color rgb(int r, int g, int b){
        return new Color(r / 255f, g / 255f, b / 255f);
    }

    /**
     * Converts the color from a float ABGR encoding to an int ABGR encoding. The alpha is expanded from 0-254 in the float
     * encoding (see {@link #intToFloatColor(int)}) to 0-255, which means converting from int to float and back to int can be
     * lossy.
     */
    public static int floatToIntColor(float value){
        int intBits = Float.floatToRawIntBits(value);
        intBits |= (int)((intBits >>> 24) * (255f / 254f)) << 24;
        return intBits;
    }

    /**
     * Encodes the ABGR int color as a float. The alpha is compressed to 0-254 to avoid using bits in the NaN range (see
     * {@link Float#intBitsToFloat(int)} javadocs). Rendering which uses colors encoded as floats should expand the 0-254 back to
     * 0-255.
     */
    public static float intToFloatColor(int value){
        return Float.intBitsToFloat(value & 0xfeffffff);
    }

    public Color rand(){
        return set(Mathf.random(), Mathf.random(), Mathf.random(), 1f);
    }

    public Color randHue(){
        fromHsv(Mathf.random(360f), 1f, 1f);
        a = 1f;
        return this;
    }

    /** Returns the difference of all the HSV components combined. */
    public float diff(Color other){
        return Math.abs(hue() - other.hue()) / 360f + Math.abs(value() - other.value()) + Math.abs(saturation() - other.saturation());
    }

    /** Shorthand for {@link #rgba8888()}.*/
    public int rgba(){
        return rgba8888();
    }

    /**
     * Sets this color to the given color.
     * @param color the Color
     */
    public Color set(Color color){
        this.r = color.r;
        this.g = color.g;
        this.b = color.b;
        this.a = color.a;
        return this;
    }

    public Color set(Vec3 vec){
        return set(vec.x, vec.y, vec.z);
    }

    /**
     * Multiplies the this color and the given color
     * @param color the color
     * @return this color.
     */
    public Color mul(Color color){
        this.r *= color.r;
        this.g *= color.g;
        this.b *= color.b;
        this.a *= color.a;
        return clamp();
    }

    /**
     * Multiplies RGB components of this Color with the given value.
     * @param value the value
     * @return this color
     */
    public Color mul(float value){
        this.r *= value;
        this.g *= value;
        this.b *= value;
        return clamp();
    }

    /**
     * Multiplies RGBA components of this Color with the given value.
     * @param value the value
     * @return this color
     */
    public Color mula(float value){
        this.r *= value;
        this.g *= value;
        this.b *= value;
        this.a *= value;
        return clamp();
    }

    /**
     * Adds the given color to this color.
     * @param color the color
     * @return this color
     */
    public Color add(Color color){
        this.r += color.r;
        this.g += color.g;
        this.b += color.b;
        return clamp();
    }

    /**
     * Subtracts the given color from this color
     * @param color the color
     * @return this color
     */
    public Color sub(Color color){
        this.r -= color.r;
        this.g -= color.g;
        this.b -= color.b;
        return clamp();
    }

    /**
     * Clamps this Color's components to a valid range [0 - 1]
     * @return this Color for chaining
     */
    public Color clamp(){
        if(r < 0)
            r = 0;
        else if(r > 1) r = 1;

        if(g < 0)
            g = 0;
        else if(g > 1) g = 1;

        if(b < 0)
            b = 0;
        else if(b > 1) b = 1;

        if(a < 0)
            a = 0;
        else if(a > 1) a = 1;
        return this;
    }

    /**
     * Sets this Color's component values.
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     * @return this Color for chaining
     */
    public Color set(float r, float g, float b, float a){
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return clamp();
    }

    /**
     * Sets this Color's component values.
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @return this Color for chaining
     */
    public Color set(float r, float g, float b){
        this.r = r;
        this.g = g;
        this.b = b;
        return clamp();
    }

    /**
     * Sets this color's component values through an integer representation.
     * @return this Color for chaining
     */
    public Color set(int rgba){
        return rgba8888(rgba);
    }

    /** Returns the sum of the RGB values of this color.*/
    public float sum(){
        return r + g + b;
    }

    /**
     * Adds the given color component values to this Color's values.
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     * @return this Color for chaining
     */
    public Color add(float r, float g, float b, float a){
        this.r += r;
        this.g += g;
        this.b += b;
        this.a += a;
        return clamp();
    }

    /**
     * Adds the given color component values to this Color's values.
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @return this Color for chaining
     */
    public Color add(float r, float g, float b){
        this.r += r;
        this.g += g;
        this.b += b;
        return clamp();
    }

    /**
     * Subtracts the given values from this Color's component values.
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     * @return this Color for chaining
     */
    public Color sub(float r, float g, float b, float a){
        this.r -= r;
        this.g -= g;
        this.b -= b;
        this.a -= a;
        return clamp();
    }

    /**
     * Subtracts the given values from this Color's component values.
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @return this Color for chaining
     */
    public Color sub(float r, float g, float b){
        this.r -= r;
        this.g -= g;
        this.b -= b;
        return clamp();
    }

    /** Inverts this color's RGB.*/
    public Color inv(){
        r = 1f - r;
        g = 1f - g;
        b = 1f - b;
        return this;
    }

    public Color r(float r){
        this.r = r;
        return this;
    }

    public Color g(float g){
        this.g = g;
        return this;
    }

    public Color b(float b){
        this.b = b;
        return this;
    }

    public Color a(float a){
        this.a = a;
        return this;
    }

    public Color mulA(float a){
        this.a *= a;
        return this;
    }

    /**
     * Multiplies this Color's color components by the given ones.
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     * @return this Color for chaining
     */
    public Color mul(float r, float g, float b, float a){
        this.r *= r;
        this.g *= g;
        this.b *= b;
        this.a *= a;
        return clamp();
    }

    /**
     * Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in
     * this color.
     * @param target The target color
     * @param t The interpolation coefficient
     * @return This color for chaining.
     */
    public Color lerp(final Color target, final float t){
        this.r += t * (target.r - this.r);
        this.g += t * (target.g - this.g);
        this.b += t * (target.b - this.b);
        this.a += t * (target.a - this.a);
        return clamp();
    }

    /**
     * Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in
     * this color.
     * @param r The red component of the target color
     * @param g The green component of the target color
     * @param b The blue component of the target color
     * @param a The alpha component of the target color
     * @param t The interpolation coefficient
     * @return This color for chaining.
     */
    public Color lerp(final float r, final float g, final float b, final float a, final float t){
        this.r += t * (r - this.r);
        this.g += t * (g - this.g);
        this.b += t * (b - this.b);
        this.a += t * (a - this.a);
        return clamp();
    }

    /** Multiplies the RGB values by the alpha. */
    public Color premultiplyAlpha(){
        r *= a;
        g *= a;
        b *= a;
        return this;
    }

    /** @return Euclidean distance from the other color, based on RGB components. */
    public float dst(Color other){
        return Vec3.dst(r, g, b, other.r, other.g, other.b);
    }

    public Color write(Color to){
        return to.set(this);
    }

    public float hue(){
        toHsv(tmpHSV);
        return tmpHSV[0];
    }

    public float saturation(){
        toHsv(tmpHSV);
        return tmpHSV[1];
    }

    public float value(){
        toHsv(tmpHSV);
        return tmpHSV[2];
    }

    public Color hue(float amount){
        toHsv(tmpHSV);
        tmpHSV[0] = amount;
        fromHsv(tmpHSV);
        return this;
    }

    public Color saturation(float amount){
        toHsv(tmpHSV);
        tmpHSV[1] = amount;
        fromHsv(tmpHSV);
        return this;
    }

    public Color value(float amount){
        toHsv(tmpHSV);
        tmpHSV[2] = amount;
        fromHsv(tmpHSV);
        return this;
    }

    public Color shiftHue(float amount){
        toHsv(tmpHSV);
        tmpHSV[0] += amount;
        fromHsv(tmpHSV);
        return this;
    }

    public Color shiftSaturation(float amount){
        toHsv(tmpHSV);
        tmpHSV[1] += amount;
        fromHsv(tmpHSV);
        return this;
    }

    public Color shiftValue(float amount){
        toHsv(tmpHSV);
        tmpHSV[2] += amount;
        fromHsv(tmpHSV);
        return this;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Color color = (Color)o;
        return abgr() == color.abgr();
    }

    @Override
    public int hashCode(){
        int result = (r != +0.0f ? Float.floatToIntBits(r) : 0);
        result = 31 * result + (g != +0.0f ? Float.floatToIntBits(g) : 0);
        result = 31 * result + (b != +0.0f ? Float.floatToIntBits(b) : 0);
        result = 31 * result + (a != +0.0f ? Float.floatToIntBits(a) : 0);
        return result;
    }

    /**
     * Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Alpha is compressed
     * from 0-255 to 0-254 to avoid using float bits in the NaN range (see {@link Color#intToFloatColor(int)}).
     * @return the packed color as a 32-bit float
     */
    public float toFloatBits(){
        int color = ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
        return intToFloatColor(color);
    }

    public double toDoubleBits(){
        return toDoubleBits(r, g, b, a);
    }

    /**
     * Packs the color components into a 32-bit integer with the format ABGR.
     * @return the packed color as a 32-bit int.
     */
    public int abgr(){
        return ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
    }

    /** Returns the color encoded as hex string with the format RRGGBBAA. */
    public String toString(){
        StringBuilder value = new StringBuilder();
        toString(value);
        return value.toString();
    }

    public void toString(StringBuilder builder){
        builder.append(Integer.toHexString(((int)(255 * r) << 24) | ((int)(255 * g) << 16) | ((int)(255 * b) << 8) | ((int)(255 * a))));
        while(builder.length() < 8)
            builder.insert(0, "0");
    }

    /**
     * Sets the RGB Color components using the specified Hue-Saturation-Value. Note that HSV components are voluntary not clamped
     * to preserve high range color and can range beyond typical values.
     * @param h The Hue in degree from 0 to 360
     * @param s The Saturation from 0 to 1
     * @param v The Value (brightness) from 0 to 1
     * @return The modified Color for chaining.
     */
    public Color fromHsv(float h, float s, float v){
        float x = (h / 60f + 6) % 6;
        int i = (int)x;
        float f = x - i;
        float p = v * (1 - s);
        float q = v * (1 - s * f);
        float t = v * (1 - s * (1 - f));
        switch(i){
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            default:
                r = v;
                g = p;
                b = q;
        }

        return clamp();
    }

    /**
     * Sets RGB components using the specified Hue-Saturation-Value. This is a convenient method for
     * {@link #fromHsv(float, float, float)}. This is the inverse of {@link #toHsv(float[])}.
     * @param hsv The Hue, Saturation and Value components in that order.
     * @return The modified Color for chaining.
     */
    public Color fromHsv(float[] hsv){
        return fromHsv(hsv[0], hsv[1], hsv[2]);
    }

    /**
     * Extract Hue-Saturation-Value. This is the inverse of {@link #fromHsv(float[])}.
     * @param hsv The HSV array to be modified.
     * @return HSV components for chaining.
     */
    public float[] toHsv(float[] hsv){
        float max = Math.max(Math.max(r, g), b);
        float min = Math.min(Math.min(r, g), b);
        float range = max - min;
        if(range == 0){
            hsv[0] = 0;
        }else if(max == r){
            hsv[0] = (60 * (g - b) / range + 360) % 360;
        }else if(max == g){
            hsv[0] = 60 * (b - r) / range + 120;
        }else{
            hsv[0] = 60 * (r - g) / range + 240;
        }

        if(max > 0){
            hsv[1] = 1 - min / max;
        }else{
            hsv[1] = 0;
        }

        hsv[2] = max;

        return hsv;
    }

    /**
     * Converts HSV to RGB
     *
     * @param h     hue 0-360
     * @param s     saturation 0-100
     * @param v     value 0-100
     * @param alpha 0-1
     */
    public static Color HSVtoRGB(float h, float s, float v, float alpha) {
        Color c = HSVtoRGB(h, s, v);
        c.a = alpha;
        return c;
    }

    /**
     * Converts HSV color system to RGB
     *
     * @param h hue 0-360
     * @param s saturation 0-100
     * @param v value 0-100
     */
    public static Color HSVtoRGB(float h, float s, float v) {
        Color c = new Color(1, 1, 1, 1);
        HSVtoRGB(h, s, v, c);
        return c;
    }

    /**
     * Converts HSV color system to RGB
     *
     * @param h           hue 0-360
     * @param s           saturation 0-100
     * @param v           value 0-100
     * @param targetColor color that result will be stored in
     * @return targetColor
     */
    public static Color HSVtoRGB(float h, float s, float v, Color targetColor) {
        if(h == 360) h = 359;
        float r, g, b;
        int i;
        float f, p, q, t;
        h = (float) Math.max(0.0, Math.min(360.0, h));
        s = (float) Math.max(0.0, Math.min(100.0, s));
        v = (float) Math.max(0.0, Math.min(100.0, v));
        s /= 100;
        v /= 100;
        h /= 60;
        i = Mathf.floor(h);
        f = h - i;
        p = v * (1 - s);
        q = v * (1 - s * f);
        t = v * (1 - s * (1 - f));
        switch(i) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            default:
                r = v;
                g = p;
                b = q;
        }

        targetColor.set(r, g, b, targetColor.a);
        return targetColor;
    }

    /**
     * Converts {@link Color} to HSV color system
     *
     * @return 3 element int array with hue (0-360), saturation (0-100) and value (0-100)
     */
    public static int[] RGBtoHSV(Color c) {
        return RGBtoHSV(c.r, c.g, c.b);
    }

    /**
     * Converts RGB to HSV color system
     *
     * @param r red 0-1
     * @param g green 0-1
     * @param b blue 0-1
     * @return 3 element int array with hue (0-360), saturation (0-100) and value (0-100)
     */
    public static int[] RGBtoHSV(float r, float g, float b) {
        float h, s, v;
        float min, max, delta;

        min = Math.min(Math.min(r, g), b);
        max = Math.max(Math.max(r, g), b);
        v = max;

        delta = max - min;

        if(max != 0)
            s = delta / max;
        else {
            s = 0;
            h = 0;
            return new int[]{Mathf.round(h), Mathf.round(s), Mathf.round(v)};
        }

        if(delta == 0)
            h = 0;
        else {

            if(r == max)
                h = (g - b) / delta;
            else if(g == max)
                h = 2 + (b - r) / delta;
            else
                h = 4 + (r - g) / delta;
        }

        h *= 60;
        if(h < 0)
            h += 360;

        s *= 100;
        v *= 100;

        return new int[]{Mathf.round(h), Mathf.round(s), Mathf.round(v)};
    }

    /** @return a copy of this color */
    public Color cpy(){
        return new Color(this);
    }

    public Color lerp(Color[] colors, float s){
        int l = colors.length;
        Color a = colors[Mathf.clamp((int)(s * (l - 1)), 0, colors.length - 1)];
        Color b = colors[Mathf.clamp((int)(s * (l - 1) + 1), 0, l - 1)];

        float n = s * (l - 1) - (int)(s * (l - 1));
        float i = 1f - n;
        return set(a.r * i + b.r * n, a.g * i + b.g * n, a.b * i + b.b * n, 1f);
    }

    private static int clampf(float value){
        return Math.min(Math.max((int)value, 0), 255);
    }

    /** @return R value of a RGBA packed color. */
    public static int ri(int rgba){
        return (rgba & 0xff000000) >>> 24;
    }

    /** @return G value of a RGBA packed color. */
    public static int gi(int rgba){
        return (rgba & 0x00ff0000) >>> 16;
    }

    /** @return B value of a RGBA packed color. */
    public static int bi(int rgba){
        return (rgba & 0x0000ff00) >>> 8;
    }

    /** @return A value of a RGBA packed color. */
    public static int ai(int rgba){
        return (rgba & 0x000000ff);
    }

    /** Multiplies 2 RGBA colors together. */
    public static int muli(int ca, int cb){
        int
        r = ((ca & 0xff000000) >>> 24),
        g = ((ca & 0x00ff0000) >>> 16),
        b = ((ca & 0x0000ff00) >>> 8),
        a = ((ca & 0x000000ff)),
        r2 = ((cb & 0xff000000) >>> 24),
        g2 = ((cb & 0x00ff0000) >>> 16),
        b2 = ((cb & 0x0000ff00) >>> 8),
        a2 = ((cb & 0x000000ff));
        return (clampf(r * r2 / 255f) << 24) | (clampf(g * g2 / 255f) << 16) | (clampf(b * b2 / 255f) << 8) | (clampf(a * a2 / 255f));
    }

    /** Multiplies a RGBA color by a float. Alpha channels are not multiplied. */
    public static int muli(int rgba, float value){
        int
        r = ((rgba & 0xff000000) >>> 24),
        g = ((rgba & 0x00ff0000) >>> 16),
        b = ((rgba & 0x0000ff00) >>> 8),
        a = ((rgba & 0x000000ff));
        return (clampf(r * value) << 24) | (clampf(g * value) << 16) | (clampf(b * value) << 8) | (a);
    }
}
