package io.anuke.ucore.graphics;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import static io.anuke.ucore.core.Core.batch;

public class Lines {
    private static TextureRegion blankregion = Pixmaps.blankTextureRegion();
    private static float stroke = 1f;
    private static Vector2 vector = new Vector2();
    private static Vector2[] circle;

    static{
        setCircleVertices(30);
    }

    private static void setCircleVertices(Vector2[] vertices, int amount){
        float step = 360f / amount;
        vector.set(1f, 0);
        for(int i = 0; i < amount; i++){
            vector.setAngle(i * step);
            vertices[i] = vector.cpy();
        }
    }

    /**Set the vertices used for drawing a line circle.*/
    public static void setCircleVertices(int amount){
        circle = new Vector2[amount];
        setCircleVertices(circle, amount);
    }

    public static void lineAngle(float x, float y, float angle, float length){
        vector.set(1, 1).setLength(length).setAngle(angle);

        line(x, y, x + vector.x, y + vector.y);
    }

    public static void lineAngle(float x, float y, float offset, float angle, float length){
        vector.set(1, 1).setLength(length + offset).setAngle(angle);

        line(x, y, x + vector.x, y + vector.y);
    }

    public static void lineAngleCenter(float x, float y, float angle, float length){
        vector.set(1, 1).setLength(length).setAngle(angle);

        line(x - vector.x / 2, y - vector.y / 2, x + vector.x / 2, y + vector.y / 2);
    }

    public static void line(float x, float y, float x2, float y2){
        line(x, y, x2, y2, CapStyle.square, 0f);
    }

    public static void line(float x, float y, float x2, float y2, CapStyle cap){
        line(x, y, x2, y2, cap, 0f);
    }

    public static void line(float x, float y, float x2, float y2, CapStyle cap, float padding){
        line(blankregion, x, y, x2, y2, cap, padding);
    }

    public static void line(TextureRegion blankregion, float x, float y, float x2, float y2, CapStyle cap, float padding){
        float length = Vector2.dst(x, y, x2, y2) + ( cap == CapStyle.none ? padding*2f : stroke / 2 + (cap == CapStyle.round ? 0 : padding*2));
        float angle = ((float) Math.atan2(y2 - y, x2 - x) * MathUtils.radDeg);

        if(cap == CapStyle.square){
            batch.draw(blankregion, x - stroke / 2 - padding, y - stroke / 2, stroke / 2 + padding, stroke / 2, length, stroke, 1f, 1f, angle);
        }else if(cap == CapStyle.none){
            batch.draw(blankregion, x - padding, y - stroke / 2, padding, stroke / 2, length, stroke, 1f, 1f, angle);
        }else if(cap == CapStyle.round){
            //Padding does not apply here.
            batch.draw(blankregion, x, y - stroke / 2, 0, stroke / 2, length - stroke/2, stroke, 1f, 1f, angle);
            Fill.circle(x, y, stroke/2f);
            Fill.circle(x2, y2, stroke/2f);
        }
    }

    public static void dashLine(float x1, float y1, float x2, float y2, int divisions){
        float dx = x2 - x1, dy = y2 - y1;

        for(int i = 0; i < divisions; i ++){
            if(i % 2 == 0){
                line(x1 + ((float)i /divisions) * dx, y1 + ((float)i /divisions) * dy,
                        x1 + ((i+1f) /divisions) * dx, y1 + ((i + 1f) /divisions) * dy);
            }
        }
    }

    public static void circle(float x, float y, float rad){
        poly(circle, x, y, rad);
    }

    public static void dashCircle(float x, float y, float radius){
        float scaleFactor = 0.6f;
        int sides = 10 + (int)(radius*scaleFactor);
        if(sides % 2 == 1) sides ++;

        vector.set(0, 0);

        for(int i = 0; i < sides; i++){
            if(i % 2 == 0) continue;
            vector.set(radius, 0).setAngle(360f / sides * i + 90);
            float x1 = vector.x;
            float y1 = vector.y;

            vector.set(radius, 0).setAngle(360f / sides * (i + 1) + 90);

            line(x1 + x, y1 + y, vector.x + x, vector.y + y);
        }
    }

    public static void spikes(float x, float y, float radius, float length, int spikes, float rot){
        vector.set(0, 1);
        float step = 360f / spikes;

        for(int i = 0; i < spikes; i++){
            vector.setAngle(i * step + rot);
            vector.setLength(radius);
            float x1 = vector.x, y1 = vector.y;
            vector.setLength(radius + length);

            line(x + x1, y + y1, x + vector.x, y + vector.y);
        }
    }

    public static void spikes(float x, float y, float rad, float length, int spikes){
        spikes(x, y, rad, length, spikes, 0);
    }

    public static void poly(float x, float y, int sides, float radius, float angle){
        vector.set(0, 0);

        for(int i = 0; i < sides; i++){
            vector.set(radius, 0).setAngle(360f / sides * i + angle + 90);
            float x1 = vector.x;
            float y1 = vector.y;

            vector.set(radius, 0).setAngle(360f / sides * (i + 1) + angle + 90);

            line(x1 + x, y1 + y, vector.x + x, vector.y + y);
        }
    }

    public static void polySeg(int sides, int from, int to, float x, float y, float radius, float angle){
        vector.set(0, 0);

        for(int i = from; i < to; i++){
            vector.set(radius, 0).setAngle(360f / sides * i + angle + 90);
            float x1 = vector.x;
            float y1 = vector.y;

            vector.set(radius, 0).setAngle(360f / sides * (i + 1) + angle + 90);

            line(x1 + x, y1 + y, vector.x + x, vector.y + y);
        }
    }

    public static void curve(float x1, float y1, float cx1, float cy1, float cx2, float cy2, float x2, float y2, int segments){

        // Algorithm shamelessly stolen from shaperenderer class
        float subdiv_step = 1f / segments;
        float subdiv_step2 = subdiv_step * subdiv_step;
        float subdiv_step3 = subdiv_step * subdiv_step * subdiv_step;

        float pre1 = 3 * subdiv_step;
        float pre2 = 3 * subdiv_step2;
        float pre4 = 6 * subdiv_step2;
        float pre5 = 6 * subdiv_step3;

        float tmp1x = x1 - cx1 * 2 + cx2;
        float tmp1y = y1 - cy1 * 2 + cy2;

        float tmp2x = (cx1 - cx2) * 3 - x1 + x2;
        float tmp2y = (cy1 - cy2) * 3 - y1 + y2;

        float fx = x1;
        float fy = y1;

        float dfx = (cx1 - x1) * pre1 + tmp1x * pre2 + tmp2x * subdiv_step3;
        float dfy = (cy1 - y1) * pre1 + tmp1y * pre2 + tmp2y * subdiv_step3;

        float ddfx = tmp1x * pre4 + tmp2x * pre5;
        float ddfy = tmp1y * pre4 + tmp2y * pre5;

        //needs more d
        float dddfx = tmp2x * pre5;
        float dddfy = tmp2y * pre5;

        while(segments-- > 0){
            float fxold = fx, fyold = fy;
            fx += dfx;
            fy += dfy;
            dfx += ddfx;
            dfy += ddfy;
            ddfx += dddfx;
            ddfy += dddfy;
            line(fxold, fyold, fx, fy);
        }

        line(fx, fy, x2, y2);
    }

    public static void swirl(float x, float y, float radius, float finion){
        swirl(x, y, radius, finion, 0f);
    }

    public static void swirl(float x, float y, float radius, float finion, float angle){
        int sides = 50;
        int max = (int) (sides * (finion + 0.001f));
        vector.set(0, 0);

        for(int i = 0; i < max; i++){
            vector.set(radius, 0).setAngle(360f / sides * i + angle);
            float x1 = vector.x;
            float y1 = vector.y;

            vector.set(radius, 0).setAngle(360f / sides * (i + 1) + angle);

            line(x1 + x, y1 + y, vector.x + x, vector.y + y);
        }
    }

    public static void poly(float x, float y, int sides, float radius){
        poly(x, y, sides, radius, 0);
    }

    public static void poly(Vector2[] vertices, float offsetx, float offsety, float scl){
        for(int i = 0; i < vertices.length; i++){
            Vector2 current = vertices[i];
            Vector2 next = i == vertices.length - 1 ? vertices[0] : vertices[i + 1];
            line(current.x * scl + offsetx, current.y * scl + offsety, next.x * scl + offsetx, next.y * scl + offsety);
        }
    }

    public static void poly(float[] vertices, float offsetx, float offsety, float scl){
        for(int i = 0; i < vertices.length / 2; i++){
            float x = vertices[i * 2];
            float y = vertices[i * 2 + 1];

            float x2 = 0, y2 = 0;
            if(i == vertices.length / 2 - 1){
                x2 = vertices[0];
                y2 = vertices[1];
            }else{
                x2 = vertices[i * 2 + 2];
                y2 = vertices[i * 2 + 3];
            }

            line(x * scl + offsetx, y * scl + offsety, x2 * scl + offsetx, y2 * scl + offsety);
        }
    }

    public static void square(float x, float y, float rad){
        rect(x - rad, y - rad, rad * 2, rad * 2);
    }

    public static void rect(float x, float y, float width, float height, int xspace, int yspace){
        x -= xspace;
        y -= yspace;
        width += xspace * 2;
        height += yspace * 2;

        batch.draw(blankregion, x, y, width, stroke);
        batch.draw(blankregion, x, y + height, width, -stroke);

        batch.draw(blankregion, x + width, y, -stroke, height);
        batch.draw(blankregion, x, y, stroke, height);
    }

    public static void rect(float x, float y, float width, float height){
        rect(x, y, width, height, 0);
    }

    public static void rect(Rectangle rect){
        rect(rect.x, rect.y, rect.width, rect.height, 0);
    }

    public static void rect(float x, float y, float width, float height, int space){
        rect(x, y, width, height, space, space);
    }

    public static void crect(float x, float y, float width, float height){
        rect(x - width/2, y - height/2, width, height, 0);
    }

    public static void stroke(float thick){
        stroke = thick;
    }

    public static float getStroke(){
        return stroke;
    }
}
