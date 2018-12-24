package io.anuke.mindustry.graphics;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.math.Vector2;
import io.anuke.arc.util.FloatArray;
import io.anuke.arc.util.Time;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.Fill;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Mathf;

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
        if(Mathf.dst(curx, cury, lastX, lastY) >= maxJump){
            points.clear();
        }

        points.add(curx, cury);

        while(points.size > (int)(length * 2 / Math.min(Time.delta(), 1f))){
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
