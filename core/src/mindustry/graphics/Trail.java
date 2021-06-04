package mindustry.graphics;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;

public class Trail{
    public int length;

    private final FloatSeq points;
    private float lastX = -1, lastY = -1, lastAngle = -1, counter = 0f;

    public Trail(int length){
        this.length = length;
        points = new FloatSeq(length*3);
    }

    public Trail copy(){
        Trail out = new Trail(length);
        out.points.addAll(points);
        out.lastX = lastX;
        out.lastY = lastY;
        out.lastAngle = lastAngle;
        return out;
    }

    public void clear(){
        points.clear();
    }

    public int size(){
        return points.size/3;
    }

    public void drawCap(Color color, float width){
        if(points.size > 0){
            Draw.color(color);
            float[] items = points.items;
            int i = points.size - 3;
            float x1 = items[i], y1 = items[i + 1], w1 = items[i + 2], w = w1 * width / (points.size/3) * i/3f * 2f;
            if(w1 <= 0.001f) return;
            Draw.rect("hcircle", x1, y1, w, w, -Mathf.radDeg * lastAngle + 180f);
            Draw.reset();
        }
    }

    public void draw(Color color, float width){
        Draw.color(color);
        float[] items = points.items;
        float lx = lastX, ly = lastY, lastAngle = this.lastAngle;

        for(int i = 0; i < points.size - 3; i+= 3){
            float x1 = items[i], y1 = items[i + 1], w1 = items[i + 2],
            x2 = items[i + 3], y2 = items[i + 4], w2 = items[i + 5];
            float size = width / (points.size/3);
            float z1 = lastAngle;
            float z2 = -Angles.angleRad(x2, y2, lx, ly);
            if(w1 <= 0.001f || w2 <= 0.001f) continue;

            float cx = Mathf.sin(z1) * i/3f * size * w1, cy = Mathf.cos(z1) * i/3f * size * w1,
                nx = Mathf.sin(z2) * (i/3f + 1) * size * w2, ny = Mathf.cos(z2) * (i/3f + 1) * size * w2;
            Fill.quad(x1 - cx, y1 - cy, x1 + cx, y1 + cy, x2 + nx, y2 + ny, x2 - nx, y2 - ny);

            lastAngle = z2;
            lx = x2;
            ly = y2;
        }

        Draw.reset();
    }

    /** Removes the last point from the trail at intervals. */
    public void shorten(){
        if((counter += Time.delta) >= 0.99f){
            if(points.size >= 3){
                points.removeRange(0, 2);
            }
        }
    }

    /** Adds a new point to the trail at intervals. */
    public void update(float x, float y){
        update(x, y, 1f);
    }

    /** Adds a new point to the trail at intervals. */
    public void update(float x, float y, float width){
        if((counter += Time.delta) >= 0.99f){
            if(points.size > length*3){
                points.removeRange(0, 2);
            }

            lastAngle = -Angles.angleRad(x, y, lastX, lastY);

            points.add(x, y, width);

            lastX = x;
            lastY = y;

            counter = 0f;
        }
    }
}
