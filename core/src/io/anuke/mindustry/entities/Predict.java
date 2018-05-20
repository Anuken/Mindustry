package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Vector2;
import io.anuke.ucore.util.Mathf;

/**Class for predicting shoot angles based on velocities of targets.*/
public class Predict {
    private static Vector2 vec = new Vector2();
    private static Vector2 vresult = new Vector2();

    /**Returns resulting predicted vector.
     * Don't call from multiple threads.*/
    public static Vector2 intercept(float srcx, float srcy, float dstx, float dsty, float dstvx, float dstvy, float v) {
        float tx = dstx - srcx,
                ty = dsty - srcy,
                tvx = dstvx,
                tvy = dstvy;

        // Get quadratic equation components
        float a = tvx*tvx + tvy*tvy - v*v;
        float b = 2 * (tvx * tx + tvy * ty);
        float c = tx*tx + ty*ty;

        // Solve quadratic
        Vector2 ts = quad(a, b, c);

        // Find smallest positive solution
        Vector2 sol = vresult.set(0, 0);
        if (ts != null) {
            float t0 = ts.x, t1 = ts.y;
            float t = Math.min(t0, t1);
            if (t < 0) t = Math.max(t0, t1);
            if (t > 0) {
                sol.set(dstx + dstvx*t, dsty + dstvy*t);
            }
        }

        return sol;
    }

    private static Vector2 quad(float a, float b, float c) {
        Vector2 sol = null;
        if (Math.abs(a) < 1e-6) {
            if (Math.abs(b) < 1e-6) {
                sol = Math.abs(c) < 1e-6 ? vec.set(0,0) : null;
            } else {
                vec.set(-c/b, -c/b);
            }
        } else {
            float disc = b*b - 4*a*c;
            if (disc >= 0) {
                disc = Mathf.sqrt(disc);
                a = 2*a;
                sol = vec.set((-b-disc)/a, (-b+disc)/a);
            }
        }
        return sol;
    }
}
