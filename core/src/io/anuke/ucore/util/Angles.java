package io.anuke.ucore.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.PositionConsumer;
import io.anuke.ucore.function.PositionFractConsumer;

public class Angles{
	private static final RandomXS128 random = new RandomXS128();
	private static final Vector3 v3 = new Vector3();
	private static final Vector2 rv = new Vector2();
	
	static public float forwardDistance(float angle1, float angle2){
		return angle1 > angle2 ? angle1-angle2 : angle2-angle1;
	}

	static public float backwardDistance(float angle1, float angle2){
		return 360 - forwardDistance(angle1, angle2);
	}

	static public float angleDist(float a, float b){
		a = a % 360f;
		b = b % 360f;
		return Math.min(forwardDistance(a, b), backwardDistance(a, b));
	}

	static public float moveToward(float angle, float to, float speed){
		if(Math.abs(angleDist(angle, to)) < speed)return to;

		if((angle > to && backwardDistance(angle, to) > forwardDistance(angle, to)) || 
				(angle < to && backwardDistance(angle, to) < forwardDistance(angle, to)) ){
			angle -= speed;
		}else{
			angle += speed;
		}
		
		return angle;
	}
	
	static public float angle(float x, float y, float x2, float y2){
		return Mathf.atan2(x2 - x, y2 -y);
	}
	
	static public float predictAngle(float x, float y, float x2, float y2, float velocityx, float velocityy, float speed){
		float time = Vector2.dst(x, y, x2, y2) / speed;
		return angle(x, y, x2 + velocityx*time, y2 + velocityy*time);
	}

	static public float trnsx(float angle, float len){
		return len * MathUtils.cos(MathUtils.degreesToRadians * angle);
	}

	static public float trnsy(float angle, float len){
		return len * MathUtils.sin(MathUtils.degreesToRadians * angle);
	}

	static public float trnsx(float angle, float x, float y){
		return rv.set(x, y).rotate(angle).x;
	}

	static public float trnsy(float angle, float x, float y){
		return rv.set(x, y).rotate(angle).y;
	}

	static public float mouseAngle(OrthographicCamera camera, float cx, float cy){
		Vector3 avector = camera.project(v3.set(cx, cy, 0));
		return Mathf.atan2(Gdx.input.getX() - avector.x, Gdx.graphics.getHeight() - Gdx.input.getY() - avector.y);
	}
	
	static public float mouseAngle(float cx, float cy){
		Vector3 avector = Core.camera.project(v3.set(cx, cy, 0));
		return Mathf.atan2(Gdx.input.getX() - avector.x, Gdx.graphics.getHeight() - Gdx.input.getY() - avector.y);
	}

	public static void circle(int points, Consumer<Float> cons){
		for(int i = 0; i < points; i ++){
			cons.accept(i*360f/points);
		}
	}

	public static void circleVectors(int points, float length, PositionConsumer pos){
		for(int i = 0; i < points; i ++){
			float f = i*360f/points;
			pos.accept(trnsx(f, length), trnsy(f, length));
		}
	}

	public static void circleVectors(int points, float length, float offset, PositionConsumer pos){
		for(int i = 0; i < points; i ++){
			float f = i*360f/points + offset;
			pos.accept(trnsx(f, length), trnsy(f, length));
		}
	}

	public static void shotgun(int points, float spacing, float offset, Consumer<Float> cons){
		for(int i = 0; i < points; i ++){
			cons.accept(i*spacing-(points-1)*spacing/2f+offset);
		}
	}

	public static void randVectors(long seed, int amount, float length, PositionConsumer cons){
		random.setSeed(seed);
		for(int i = 0; i < amount; i ++){
			float vang = random.nextFloat()*360f;
			rv.set(length, 0).rotate(vang);
			cons.accept(rv.x, rv.y);
		}
	}

	public static void randLenVectors(long seed, int amount, float length, PositionConsumer cons){
		random.setSeed(seed);
		for(int i = 0; i < amount; i ++){
			float scl = length * random.nextFloat();
			float vang = random.nextFloat()*360f;
			rv.set(scl, 0).rotate(vang);
			cons.accept(rv.x, rv.y);
		}
	}

	public static void randLenVectors(long seed, int amount, float length, float angle, float range, PositionConsumer cons){
		random.setSeed(seed);
		for(int i = 0; i < amount; i ++){
			float scl = length * random.nextFloat();
			float vang = angle + random.nextFloat() * range*2 - range;
			rv.set(scl, 0).rotate(vang);
			cons.accept(rv.x, rv.y);
		}
	}

	public static void randLenVectors(long seed, float fin, int amount, float length,
									  float angle, float range, PositionFractConsumer cons){
		random.setSeed(seed);
		for(int i = 0; i < amount; i ++){
			float scl = length * random.nextFloat() * fin;
			float vang = angle + random.nextFloat() * range*2 - range;
			rv.set(scl, 0).rotate(vang);
			cons.accept(rv.x, rv.y, fin * (random.nextFloat()));
		}
	}
}

