package io.anuke.ucore.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import io.anuke.ucore.util.Mathf;

import static java.lang.Math.abs;

public class Hue{
	static private float[] hsv = new float[3];
	
	public static boolean approximate(Color a, Color b, float r){
		return MathUtils.isEqual(a.r, b.r, r) && MathUtils.isEqual(a.g, b.g, r) && MathUtils.isEqual(a.b, b.b, r);
	}

	public static Color shift(Color color, int index, float amount){
		color.toHsv(hsv);
		hsv[index] += amount;
		return color.fromHsv(hsv);
	}

	public static Color multiply(Color color, int index, float amount){
		color.toHsv(hsv);
		hsv[index] *= amount;
		return color.fromHsv(hsv);
	}
	
	public static int rgb(Color color){
		return Color.rgba8888(color);
	}
	
	public static Color random(){
		return new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1f);
	}
	
	public static Color random(Color color){
		return color.set(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1f);
	}
	
	public static Color round(Color color, float amount){
		color.r = (int)(color.r/amount) * amount;
		color.g = (int)(color.g/amount) * amount;
		color.b = (int)(color.b/amount) * amount;
		return color;
	}
	
	/**Returns the closest color in the array calculated by hue.*/
	public static int closest(Color input, Color[] colors){
		
		float bh = RGBtoHSB(input, hsv)[0];
		float bs = hsv[1];
		
		int index = 0;
		float cd = 360;
		for(int i = 0; i < colors.length; i ++){
			RGBtoHSB(colors[i], hsv);
			float cr = abs(bh -hsv[0]) + abs(bs -hsv[1]);
			if(cr < cd){
				index = i;
				cd = cr;
			}
		}
		return index;
	}
	
	public static float sum(Color color){
		return color.r + color.g + color.b;
	}

	public static boolean brighter(Color color, Color other){
		float avg1 = (color.r + color.g + color.b) / 3f;
		float r1 = Math.abs(color.r - avg1) + Math.abs(color.g - avg1) + Math.abs(color.b - avg1);
		float avg2 = (other.r + other.g + other.b) / 3f;
		float r2 = Math.abs(other.r - avg2) + Math.abs(other.g - avg2) + Math.abs(other.b - avg2);
		return r1 >= r2;
	}
	
	public static float diff(Color a, Color b){
		return Math.abs(a.r - b.r) + Math.abs(a.g - b.g) + Math.abs(a.b - b.b);
	}
	
	public static Color addu(Color a, Color b){
		a.r += b.r;
		a.g += b.g;
		a.b += b.b;
		a.a += b.a;
		return a;
	}
	
	public static Color fromHSB(float hue, float saturation, float brightness){
		return Hue.fromHSB(hue, saturation, brightness, new Color(1, 1, 1, 1));
	}
	
	public static Color fromHSB(float hue, float saturation, float brightness, Color color){
		int r = 0, g = 0, b = 0;
		if(saturation == 0){
			r = g = b = (int)(brightness * 255.0f + 0.5f);
		}else{
			float h = (hue - (float)Math.floor(hue)) * 6.0f;
			float f = h - (float)java.lang.Math.floor(h);
			float p = brightness * (1.0f - saturation);
			float q = brightness * (1.0f - saturation * f);
			float t = brightness * (1.0f - (saturation * (1.0f - f)));
			switch((int)h){
			case 0:
				r = (int)(brightness * 255.0f + 0.5f);
				g = (int)(t * 255.0f + 0.5f);
				b = (int)(p * 255.0f + 0.5f);
				break;
			case 1:
				r = (int)(q * 255.0f + 0.5f);
				g = (int)(brightness * 255.0f + 0.5f);
				b = (int)(p * 255.0f + 0.5f);
				break;
			case 2:
				r = (int)(p * 255.0f + 0.5f);
				g = (int)(brightness * 255.0f + 0.5f);
				b = (int)(t * 255.0f + 0.5f);
				break;
			case 3:
				r = (int)(p * 255.0f + 0.5f);
				g = (int)(q * 255.0f + 0.5f);
				b = (int)(brightness * 255.0f + 0.5f);
				break;
			case 4:
				r = (int)(t * 255.0f + 0.5f);
				g = (int)(p * 255.0f + 0.5f);
				b = (int)(brightness * 255.0f + 0.5f);
				break;
			case 5:
				r = (int)(brightness * 255.0f + 0.5f);
				g = (int)(p * 255.0f + 0.5f);
				b = (int)(q * 255.0f + 0.5f);
				break;
			}
		}
		return color.set(r/255f, g/255f, b/255f, color.a);
	}
	
	public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
        float hue, saturation, brightness;
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
        int cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        int cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;

        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0)
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        else
            saturation = 0;
        if (saturation == 0)
            hue = 0;
        else {
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax)
                hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0f + redc - bluec;
            else
                hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0)
                hue = hue + 1.0f;
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }
	
	public static float[] RGBtoHSB(Color color, float[] hsbvals){
		return RGBtoHSB((int)(color.r*255),(int)(color.g*255),(int)(color.b*255),hsbvals);
	}
	
	public static float[] RGBtoHSB(Color color){
		return RGBtoHSB((int)(color.r*255),(int)(color.g*255),(int)(color.b*255), hsv);
	}

	
	public static Color mix(int r1, int g1, int b1, int r2, int g2, int b2, float s){
		float i = 1f - s;
		return rgb(r1*i + r2*s, g1*i + g2*s, b1*i + b2*s);
	}
	
	public static Color mix(Color a, Color b, float s){
		float i = 1f - s;
		return new Color(a.r*i + b.r*s, a.g*i + b.g*s, a.b*i + b.b*s, a.a*i + b.a*s);
	}
	
	public static Color mix(Color[] colors, Color out, float s){
		int l = colors.length;
		Color a = colors[(int)(s*(l-1))];
		Color b = colors[Mathf.clamp((int)(s*(l-1)+1), 0, l-1)];
		
		float n = s*(l-1)-(int)(s*(l-1));
		float i = 1f-n;
		return out.set(a.r*i + b.r*n, a.g*i + b.g*n, a.b*i + b.b*n, 1f);
	}
	
	public static Color mix(Color a, Color b, float s, Color to){
		float i = 1f - s;
		return to.set(a.r*i + b.r*s, a.g*i + b.g*s, a.b*i + b.b*s, a.a*i + b.a*s);
	}
	
	public static Color blend2d(Color a, Color b, Color c, Color d, float x, float y){
		return new Color(( b.r*y + a.r*(1-y) ) * (x)  +  ( d.r*y + c.r*(1-y) ) * (1-x),
				( b.g*y + a.g*(1-y) ) * (x)  +  ( d.g*y + c.g*(1-y) ) * (1-x),
				( b.b*y + a.b*(1-y) ) * (x)  +  ( d.b*y + c.b*(1-y) ) * (1-x), 1f).clamp();
	}
	
	public static Color rgb(int rgb, float lightness){
		return new Color(rgb).mul(lightness, lightness, lightness, 1f);
	}
	
	public static Color rgb(int r, int g, int b){
		return new Color(r/255f, g/255f, b/255f, 1f);
	}
	
	public static Color rgb(float r, float g, float b){
		return new Color(r, g, b, 1f);
	}
	
	public static Color rgb(double r, double g, double b){
		return new Color((float)r, (float)g, (float)b, 1f);
	}
	
	public static Color rgb(int r, int g, int b, float brightness){
		return new Color(r/255f + brightness, g/255f + brightness, b/255f + brightness, 1f);
	}
	
	public static Color lightness(float l){
		return new Color(l, l, l, 1f);
	}
}
