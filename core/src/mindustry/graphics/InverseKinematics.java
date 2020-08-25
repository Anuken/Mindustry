package mindustry.graphics;

import arc.math.*;
import arc.math.geom.*;

public class InverseKinematics{
    private static final Vec2[] mat1 = {new Vec2(), new Vec2()}, mat2 = {new Vec2(), new Vec2()};
    private static final Vec2 temp = new Vec2(), temp2 = new Vec2(), at1 = new Vec2();

    public static boolean solve(float lengthA, float lengthB, Vec2 end, boolean side, Vec2 result){
        at1.set(end).rotate(side ? 1 : -1).setLength(lengthA + lengthB).add(end.x / 2f, end.y / 2f);
        return solve(lengthA, lengthB, end, at1, result);
    }

    public static boolean solve(float lengthA, float lengthB, Vec2 end, Vec2 attractor, Vec2 result){
        Vec2 axis = mat2[0].set(end).nor();
        mat2[1].set(attractor).sub(temp2.set(axis).scl(attractor.dot(axis))).nor();
        mat1[0].set(mat2[0].x, mat2[1].x);
        mat1[1].set(mat2[0].y, mat2[1].y);
        result.set(mat2[0].dot(end), mat2[1].dot(end));
        float len = result.len();
        float dist = Math.max(0, Math.min(lengthA, (len + (lengthA * lengthA - lengthB * lengthB) / len) / 2));
        Vec2 src = temp.set(dist, Mathf.sqrt(lengthA * lengthA - dist * dist));
        result.set(mat1[0].dot(src), mat1[1].dot(src));

        return dist > 0 && dist < lengthA;
    }

}