package io.anuke.mindustry.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

/**
 * Class that renders a colored trail.
 */
public class Trail{
    private final static float maxJump = 15f;
    private final int length;
    private final FloatArray points = new FloatArray();
    private float lastX, lastY;

    public Trail(int length){
        this.length = length;
    }

    public void update(float curx, float cury){
        if(Vector2.dst(curx, cury, lastX, lastY) >= maxJump){
            points.clear();
        }

        points.add(curx, cury);

        while(points.size > (int)(length * 2 / Math.min(Timers.delta(), 1f))){
            float[] items = points.items;
            System.arraycopy(items, 2, items, 0, points.size - 2);
            points.size -= 2;
        }

        lastX = curx;
        lastY = cury;
    }

    public void clear(){
        points.clear();
    }

    public void draw(Color color, float stroke){
        Draw.color(color);

        for(int i = 0; i < points.size - 2; i += 2){
            float x = points.get(i);
            float y = points.get(i + 1);
            float x2 = points.get(i + 2);
            float y2 = points.get(i + 3);
            float s = Mathf.clamp((float) (i) / points.size);

            Lines.stroke(s * stroke);
            Lines.line(x, y, x2, y2);
        }

        if(points.size >= 2){
            Fill.circle(points.get(points.size - 2), points.get(points.size - 1), stroke / 2f);
        }

        Draw.reset();
    }
}
