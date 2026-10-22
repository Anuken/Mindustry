package arc.math;

import arc.*;
import arc.func.*;
import arc.math.geom.*;

public class Angles{
    private static final Rand rand = new Rand();
    private static final Vec2 rv = new Vec2();

    public static float forwardDistance(float angle1, float angle2){
        return Math.abs(angle1 - angle2);
    }

    public static float backwardDistance(float angle1, float angle2){
        return 360 - Math.abs(angle1 - angle2);
    }

    public static boolean within(float a, float b, float margin){
        return angleDist(a, b) <= margin;
    }

    public static float angleDist(float a, float b){
        a = Mathf.mod(a, 360f);
        b = Mathf.mod(b, 360f);
        return Math.min((a - b) < 0 ? a - b + 360 : a - b, (b - a) < 0 ? b - a + 360 : b - a);
    }

    public static boolean near(float a, float b, float range){
        return angleDist(a, b) < range;
    }

    public static float clampRange(float angle, float dest, float range){
        float dst = angleDist(angle, dest);
        return dst <= range ? angle : moveToward(angle, dest, dst - range);
    }

    public static float moveToward(float angle, float to, float speed){
        if(Math.abs(angleDist(angle, to)) < speed) return to;
        angle = Mathf.mod(angle, 360f);
        to = Mathf.mod(to, 360f);

        if(angle > to == backwardDistance(angle, to) > forwardDistance(angle, to)){
            angle -= speed;
        }else{
            angle += speed;
        }

        return angle;
    }

    public static float angle(float x, float y){
        return angle(0, 0, x, y);
    }

    public static float angle(float x, float y, float x2, float y2){
        float ang = Mathf.atan2(x2 - x, y2 - y) * Mathf.radDeg;
        if(ang < 0) ang += 360f;
        return ang;
    }

    public static float angleRad(float x, float y, float x2, float y2){
        return Mathf.atan2(x2 - x, y2 - y);
    }

    public static float trnsx(float angle, float len){
        return len * Mathf.cosDeg(angle);
    }

    public static float trnsy(float angle, float len){
        return len * Mathf.sinDeg(angle);
    }

    public static float trnsx(float angle, float x, float y){
        return rv.set(x, y).rotate(angle).x;
    }

    public static float trnsy(float angle, float x, float y){
        return rv.set(x, y).rotate(angle).y;
    }

    public static float mouseAngle(float cx, float cy){
        Vec2 avector = Core.camera.project(cx, cy);
        return angle(avector.x, avector.y, Core.input.mouseX(), Core.input.mouseY());
    }

    public static void circleVectors(int points, float length, Floatc2 pos){
        for(int i = 0; i < points; i++){
            float f = i * 360f / points;
            pos.get(trnsx(f, length), trnsy(f, length));
        }
    }

    public static void circleVectors(int points, float length, float offset, Floatc2 pos){
        for(int i = 0; i < points; i++){
            float f = i * 360f / points + offset;
            pos.get(trnsx(f, length), trnsy(f, length));
        }
    }

    public static void randVectors(long seed, int amount, float length, Floatc2 cons){
        rand.setSeed(seed);
        for(int i = 0; i < amount; i++){
            rv.trns(rand.random(360f), length);
            cons.get(rv.x, rv.y);
        }
    }

    public static void randLenVectors(long seed, int amount, float length, Floatc2 cons){
        rand.setSeed(seed);
        for(int i = 0; i < amount; i++){
            rv.trns(rand.random(360f), rand.random(length));
            cons.get(rv.x, rv.y);
        }
    }

    public static void randLenVectors(long seed, int amount, float minLength, float length, Floatc2 cons){
        rand.setSeed(seed);
        for(int i = 0; i < amount; i++){
            rv.trns(rand.random(360f), minLength + rand.random(length));
            cons.get(rv.x, rv.y);
        }
    }

    public static void randLenVectors(long seed, int amount, float length, float angle, float range, Floatc2 cons){
        rand.setSeed(seed);
        for(int i = 0; i < amount; i++){
            rv.trns(angle + rand.range(range), rand.random(length));
            cons.get(rv.x, rv.y);
        }
    }

    public static void randLenVectors(long seed, int amount, float length, float angle, float range, float spread, Floatc2 cons){
        rand.setSeed(seed);
        for(int i = 0; i < amount; i++){
            rv.trns(angle + rand.range(range), rand.random(length));
            cons.get(rv.x + rand.range(spread), rv.y + rand.range(spread));
        }
    }

    public static void randLenVectors(long seed, float fin, int amount, float length, ParticleConsumer cons){
        rand.setSeed(seed);
        for(int i = 0; i < amount; i++){
            float l = rand.nextFloat();
            rv.trns(rand.random(360f), length * l * fin);
            cons.accept(rv.x, rv.y, fin * l, (1f - fin) * l);
        }
    }

    public static void randLenVectors(long seed, float fin, int amount, float length, float angle, float range, ParticleConsumer cons){
        rand.setSeed(seed);
        for(int i = 0; i < amount; i++){
            rv.trns(angle + rand.range(range), rand.random(length * fin));
            cons.accept(rv.x, rv.y, fin * (rand.nextFloat()), 0f);
        }
    }

    public interface ParticleConsumer{
        void accept(float x, float y, float fin, float fout);
    }

}

