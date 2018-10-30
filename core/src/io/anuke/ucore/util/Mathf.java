package io.anuke.ucore.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Consumer;

import java.util.Random;

public class Mathf{
	public static final int[] signs = {-1, 1};
	public static final boolean[] booleans = {false, true};
	private static final RandomXS128 seedr = new RandomXS128();
	private static final RandomXS128 rand = new RandomXS128();
	
	/**X/Y flipped, returns degrees*/
	public static float atan2(float x, float y){
		float out = (float)Math.atan2(y, x) * MathUtils.radDeg;
		if(out < 0) out += 360f;
		return out;
	}
	
	/**Mod function that works properly for negative integers..*/
	public static int mod(int i, int m){
		if(i >= 0){
			return i % m;
		}else{
			return i % m + m;
		}
	}

	public static int pow2(int i){
		return (int)Math.pow(2, i);
	}

	public static int ceil(float f){
		return (int)Math.ceil(f);
	}

	public static float sqrt(float x){
		return (float)Math.sqrt(x);
	}

	public static float sqr(float x){
		return x*x;
	}

	public static float dst(float x, float y){
		return Vector2.dst(x, y, 0, 0);
	}
	
	public static float sin(float in, float scl, float mag){
		return MathUtils.sin(in/scl)*mag;
	}

	public static float cos(float in, float scl, float mag){
		return MathUtils.cos(in/scl)*mag;
	}
	
	public static float absin(float in, float scl, float mag){
		return (sin(in, scl*2f, mag) + mag) / 2f;
	}

	/**Converts a value range from 0-1 to a value range 0-1-0.*/
	public static float abscurve(float f){
		return 1f-Math.abs(f-0.5f)*2f;
	}

	public static float curve(float f, float offset){
		if(f < offset){
			return 0f;
		}else{
			return (f-offset)/(1f-offset);
		}
	}

	public static float curve(float f, float offset, float max){
		if(f < offset) {
			return 0f;
		}else if(f-offset > max){
			return 1f;
		}else{
			return (f-offset)/max;
		}
	}
	
	public static boolean zero(float f){
		return MathUtils.isEqual(f, 0);
	}
	
	public static boolean zero(float f, float tolerance){
		return MathUtils.isEqual(f, 0, tolerance);
	}
	
	public static boolean in(float value, float target, float range){
		return Math.abs(value-target) < range;
	}
	
	public static float pow(float a, float b){
		return (float)Math.pow(a, b);
	}
	
	public static <T> T choose(T... items){
		return items[random(0, items.length-1)];
	}

	public static <T> T select(int index, T... items){
		return items[index];
	}

	public static <T> T select(T[] items){
		return items[random(0, items.length-1)];
	}
	
	public static boolean near(float a, float b, float range){
		return Math.abs(a-b) < range;
	}
	
	public static boolean near2d(float x, float y, float x2, float y2, float range){
		return near(x, x2, range) && near(y, y2, range);
	}
	
	public static boolean angNear(float a, float b, float range){
		return Math.abs(a-b) < range || Math.abs((360f-a)-b) < range;
	}

	/**Mutates the first vector. Goes forward. Used for value greater than 1.*/
	public static Vector2 lerp2 (Vector2 v, Vector2 target, float alpha) {
		v.x = (v.x) + ((target.x - v.x) * alpha);
		v.y = (v.y) + ((target.y - v.y) * alpha);
		return v;
	}
	
	public static float slerp(float angle, float target, float alpha){
		return MathUtils.lerpAngleDeg(angle, target, alpha);
	}

	public static float slerpDelta(float from, float to, float alpha){
		return MathUtils.lerpAngleDeg(from, to, Math.min(alpha * Timers.delta(), 1f));
	}
	
	public static float lerp(float from, float to, float alpha){
		return MathUtils.lerp(from, to, alpha);
	}
	
	public static float lerpDelta(float from, float to, float alpha){
		return MathUtils.lerp(from, to, Math.min(alpha * Timers.delta(), 1f));
	}
	
	public static boolean randBool(){
		return MathUtils.randomBoolean();
	}
	
	/**Inclusive.*/
	public static int random(int a, int b){
		return MathUtils.random(a, b);
	}
	
	/**0-range, inclusive*/
	public static int random(int range){
		return MathUtils.random(range);
	}
	
	public static float random(float a, float b){
		return MathUtils.random(a, b);
	}
	
	public static float random(float range){
		return MathUtils.random(range);
	}
	
	public static Random random(){
		return MathUtils.random;
	}
	
	public static int randomSeed(long seed, int min, int max){
		seedr.setSeed(seed);
		if(MathUtils.isPowerOfTwo(max)){
			seedr.nextInt();
		}
		return seedr.nextInt(max-min+1)+min;
	}
	
	public static float randomSeed(long seed){
		seedr.setSeed(seed*99999);
		return seedr.nextFloat();
	}

	public static float randomSeedRange(long seed, float range){
		seedr.setSeed(seed*99999);
		return range*(seedr.nextFloat() - 0.5f)*2f;
	}
	
	public static float range(float range){
		return random(-range, range);
	}
	
	public static int range(int range){
		return random(-range, range);
	}
	
	public static float range(float min, float max){
		if(chance(0.5)){
			return random(min, max);
		}else{
			return -random(min, max);
		}
	}
	
	public static boolean chance(double d){
		return rand.nextFloat() < d;
	}
	
	public static int roundi(float a, int b){
		return (int)(a/b)*b;
	}
	
	public static float round(float a, float b){
		return (int)(a/b)*b;
	}
	
	public static float round2(float a, float b){
		return Math.round(a/b)*b;
	}
	
	public static int scl(float a, float b){
		return (int)(a/b);
	}
	
	public static int scl2(float a, float b){
		return Math.round(a/b);
	}
	
	public static int sclb(float a, float b, boolean round){
		return round ? Math.round(a/b) : (int)(a/b);
	}
	
	public static boolean inRect(float x, float y, float bx, float by, float tx, float ty){
		return x > bx && y > by && x < tx && y < ty;
	}
	
	
	public static boolean intersect(float x1, float y1, float s1, float x2, float y2, float s2){
		return MathUtils.isEqual(x1, x2, s1+s2) && MathUtils.isEqual(y1, y2, s1+s2);
	}
	
	public static int sign(float f){
		return(f < 0 ? -1 : 1);
	}
	
	/**Returns 1 if true, -1 if false.*/
	public static int sign(boolean b){
		return b ? 1 : -1;
	}

	public static float clamp(float i, float min, float max){
		if(i < min)
			i = min;
		if(i > max)
			i = max;
		return i;
	}
	
	/**Clamps to [0,1]*/
	public static float clamp(double i){
		return clamp((float)i,0,1);
	}
	
	public static boolean between(float i, float min, float max){
		return i > min && i < max;
	}

	public static int clamp(int i, int min, int max){
		if(i < min)
			i = min;
		if(i > max)
			i = max;
		return i;
	}

	public static <K, V> ObjectMap<K, V> map(Object... values){
		ObjectMap<K, V> map = new ObjectMap<>();

		for(int i = 0; i < values.length/2; i ++){
			map.put((K)values[i*2], (V)values[i*2+1]);
		}

		return map;
	}

	public static <T> void each(Consumer<T> cons, T... objects){
		for(T t : objects){
			cons.accept(t);
		}
	}
	
	public static <T> boolean inBounds(int x, int y, T[][] array){
		return x >= 0 && y >= 0 && x < array.length && y < array[0].length;
	}
	
	public static boolean inBounds(int x, int y, int[][] array){
		return x >= 0 && y >= 0 && x < array.length && y < array[0].length;
	}
	
	public static boolean inBounds(int x, int y, float[][] array){
		return x >= 0 && y >= 0 && x < array.length && y < array[0].length;
	}
	
	public static boolean inBounds(int x, int y, boolean[][] array){
		return x >= 0 && y >= 0 && x < array.length && y < array[0].length;
	}

	public static <T> boolean inBounds(int x, int y, int z, T[][][] array){
		return x >= 0 && y >= 0 && z >= 0 && x < array.length && y < array[0].length && z < array[0][0].length;
	}

	public static <T> boolean inBounds(int x, int y, int z, int[][][] array){
		return x >= 0 && y >= 0 && z >= 0 && x < array.length && y < array[0].length && z < array[0][0].length;
	}

	public static boolean inBounds(int x, int y, int z, int size, int padding){
		return x >= padding && y >= padding && z >= padding && x < size - padding && y < size - padding
				&& z < size - padding;
	}

	public static <T> boolean inBounds(int x, int y, int width, int height){
		return x >= 0 && y >= 0 && x < width && y < height;
	}
}
