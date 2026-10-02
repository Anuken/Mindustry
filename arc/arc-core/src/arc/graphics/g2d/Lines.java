package arc.graphics.g2d;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;

public class Lines{
    public static boolean useLegacyLine = false;

    private static float stroke = 1f;
    private static Vec2 vector = new Vec2(), u = new Vec2(), v = new Vec2();
    private static FloatSeq floats = new FloatSeq(20);
    private static FloatSeq floatBuilder = new FloatSeq(20);
    private static boolean building;
    private static float circlePrecision = 0.4f;

    /** Set the vertices used for drawing a line circle. */
    public static void setCirclePrecision(float amount){
        circlePrecision = amount;
    }

    public static int circleVertices(float rad){
        return 11 + (int)(rad * circlePrecision);
    }

    public static void lineAngle(float x, float y, float angle, float length, boolean cap){
        vector.trns(angle, length);

        line(x, y, x + vector.x, y + vector.y, cap);
    }

    public static void lineAngle(float x, float y, float angle, float length){
        vector.trns(angle, length);

        line(x, y, x + vector.x, y + vector.y);
    }

    public static void lineAngle(float x, float y, float angle, float length, float offset){
        vector.trns(angle, 1f);

        line(x + vector.x * offset, y + vector.y * offset, x + vector.x * (length + offset), y + vector.y * (length + offset));
    }

    public static void lineAngleCenter(float x, float y, float angle, float length, boolean cap){
        vector.trns(angle, length);

        line(x - vector.x / 2, y - vector.y / 2, x + vector.x / 2, y + vector.y / 2, cap);
    }

    public static void lineAngleCenter(float x, float y, float angle, float length){
        vector.trns(angle, length);

        line(x - vector.x / 2, y - vector.y / 2, x + vector.x / 2, y + vector.y / 2);
    }

    public static void line(float x, float y, float x2, float y2){
        line(x, y, x2, y2, true);
    }

    public static void line(float x, float y, Color c, float x2, float y2, Color c2){
        line(Core.atlas.white(), x, y, c, x2, y2, c2, true);
    }

    public static void line(float x, float y, float x2, float y2, boolean cap){
        line(Core.atlas.white(), x, y, Core.batch.getPackedColor(), x2, y2, Core.batch.getPackedColor(), cap);
    }

    public static void line(TextureRegion region, float x, float y, float x2, float y2, boolean cap){
        line(region, x, y, Core.batch.getPackedColor(), x2, y2, Core.batch.getPackedColor(), cap);
    }

    public static void line(TextureRegion region, float x, float y, Color c, float x2, float y2, Color c2, boolean cap){
        line(region, x, y, c.toFloatBits(), x2, y2, c2.toFloatBits(), cap);
    }

    public static void line(TextureRegion region, float x, float y, float color1, float x2, float y2, float color2, boolean cap){

        if(useLegacyLine){
            float length = Mathf.dst(x, y, x2, y2) + (!cap ? 0 : stroke);
            float angle = (Mathf.atan2(x2 - x, y2 - y)) * Mathf.radDeg;

            if(cap){
                Draw.rect(region, x - stroke / 2 + length/2f, y, length, stroke, stroke / 2, stroke / 2, angle);
            }else{
                Draw.rect(region, x + length/2f, y, length, stroke, 0, stroke / 2, angle);
            }
        }else{
            float hstroke = stroke/2f;
            float len = Mathf.len(x2 - x, y2 - y);
            float diffx = (x2 - x) / len * hstroke, diffy = (y2 - y) / len * hstroke;

            if(cap){
                Fill.quad(
                region,

                x - diffx - diffy,
                y - diffy + diffx,
                color1,

                x - diffx + diffy,
                y - diffy - diffx,
                color1,

                x2 + diffx + diffy,
                y2 + diffy - diffx,
                color2,

                x2 + diffx - diffy,
                y2 + diffy + diffx,
                color2
                );
            }else{
                Fill.quad(
                region,

                x - diffy,
                y + diffx,

                x + diffy,
                y - diffx,

                x2 + diffy,
                y2 - diffx,

                x2 - diffy,
                y2 + diffx

                );
            }
        }
    }

    public static void linePoint(Position p){
        linePoint(p.getX(), p.getY());
    }

    public static void linePoint(float x, float y){
        if(!building) throw new IllegalStateException("Not building");
        floatBuilder.add(x, y);
    }

    public static void beginLine(){
        if(building) throw new IllegalStateException("Already building");
        floatBuilder.clear();
        building = true;
    }

    public static void endLine(){
        endLine(false);
    }

    public static void endLine(boolean wrap){
        endLine(wrap, false);
    }

    public static void endLine(boolean wrap, boolean flatEnd){
        if(!building) throw new IllegalStateException("Not building");
        polyline(floatBuilder.items, floatBuilder.size, wrap, flatEnd);
        building = false;
    }

    public static void polyline(FloatSeq points, boolean wrap){
        polyline(points.items, points.size, wrap);
    }

    private static final Vec2 AB = new Vec2(), BC = new Vec2(),
    A = new Vec2(), B = new Vec2(), C = new Vec2(), E = new Vec2(), D = new Vec2(), vec1 = new Vec2(),
    D0 = new Vec2(), E0 = new Vec2(), q1 = new Vec2(), q2 = new Vec2(), q3 = new Vec2(), q4 = new Vec2(),
    D2 = new Vec2(), E2 = new Vec2(), D02 = new Vec2(), E02 = new Vec2();
    private static final float miterLimit = 4f;

    public static void polyline(float[] points, int length, boolean wrap){
        polyline(points, length, wrap, false);
    }

    //implementation taken from https://github.com/earlygrey/shapedrawer/blob/master/drawer/src/space/earlygrey/shapedrawer/ShapeDrawer.java
    public static void polyline(float[] points, int length, boolean wrap, boolean flatEnd){
        if(length < 4) return;

        float halfWidth = 0.5f * stroke;

        for(int i = 2; i < length - 2; i += 2){
            A.set(points[i - 2], points[i - 1]);
            B.set(points[i], points[i + 1]);
            C.set(points[i + 2], points[i + 3]);

            preparePointyJoin(A, B, C, D, E, D2, E2, halfWidth, flatEnd);

            //D2/E2 are the points to be reused as the start of the NEXT quad (may differ from D/E if beveled)
            float x3 = D2.x, y3 = D2.y;
            float x4 = E2.x, y4 = E2.y;

            q3.set(D);
            q4.set(E);

            if(i == 2){
                if(!wrap){
                    prepareFlatEndpoint(points[2], points[3], points[0], points[1], D, E, halfWidth);
                    q1.set(E);
                    q2.set(D);
                }else{
                    vec1.set(points[length - 2], points[length - 1]);
                    preparePointyJoin(vec1, A, B, D0, E0, D02, E02, halfWidth, flatEnd);

                    q1.set(E02);
                    q2.set(D02);
                }
            }

            pushQuad();
            q1.set(x4, y4);
            q2.set(x3, y3);
        }

        if(!wrap){
            //draw last link on path
            prepareFlatEndpoint(B, C, D, E, halfWidth);
            q3.set(E);
            q4.set(D);
            pushQuad();
        }else{
            //draw last link on path
            A.set(points[0], points[1]);
            preparePointyJoin(B, C, A, D, E, D2, E2, halfWidth, flatEnd);
            q3.set(D);
            q4.set(E);
            pushQuad();

            //draw connection back to first vertex
            q1.set(D2);
            q2.set(E2);
            q3.set(E0);
            q4.set(D0);
            pushQuad();
        }
    }

    private static void pushQuad(){
        Fill.quad(q1.x, q1.y, q2.x, q2.y, q3.x, q3.y, q4.x, q4.y);
    }

    private static void pushBevelFill(Vec2 p1, Vec2 p2, Vec2 apex){
        Fill.quad(p1.x, p1.y, apex.x, apex.y, apex.x, apex.y, p2.x, p2.y);
    }

    private static void prepareFlatEndpoint(Vec2 pathPoint, Vec2 endPoint, Vec2 D, Vec2 E, float halfLineWidth){
        prepareFlatEndpoint(pathPoint.x, pathPoint.y, endPoint.x, endPoint.y, D, E, halfLineWidth);
    }

    private static void prepareFlatEndpoint(float pathPointX, float pathPointY, float endPointX, float endPointY, Vec2 D, Vec2 E, float halfLineWidth){
        v.set(endPointX, endPointY).sub(pathPointX, pathPointY).setLength(halfLineWidth);
        D.set(v.y, -v.x).add(endPointX, endPointY);
        E.set(-v.y, v.x).add(endPointX, endPointY);
    }

    private static void preparePointyJoin(Vec2 A, Vec2 B, Vec2 C, Vec2 D, Vec2 E, Vec2 D2, Vec2 E2, float halfLineWidth, boolean flatEnd){
        AB.set(B).sub(A);
        BC.set(C).sub(B);
        float angle = angleRad(AB, BC);

        if(Mathf.equal(angle, 0) || Mathf.equal(angle, Mathf.PI2)){
            prepareStraightJoin(B, D, E, halfLineWidth);
            D2.set(D);
            E2.set(E);
            return;
        }

        boolean bendsLeft = angle < 0;
        float sin = (float)Math.sin(angle);

        if(!flatEnd && 1f / Math.abs(sin) <= miterLimit){
            float len = halfLineWidth / sin;
            AB.setLength(len);
            BC.setLength(len);

            Vec2 insidePoint = bendsLeft ? D : E;
            Vec2 outsidePoint = bendsLeft ? E : D;
            insidePoint.set(B).sub(AB).add(BC);
            outsidePoint.set(B).add(AB).sub(BC);

            D2.set(D);
            E2.set(E);
        }else{
            AB.setLength(halfLineWidth);
            D.set(-AB.y, AB.x).add(B);
            E.set(AB.y, -AB.x).add(B);

            BC.setLength(halfLineWidth);
            D2.set(-BC.y, BC.x).add(B);
            E2.set(BC.y, -BC.x).add(B);

            Vec2 outsideAB = bendsLeft ? E : D;
            Vec2 outsideBC = bendsLeft ? E2 : D2;
            pushBevelFill(outsideAB, outsideBC, B);
        }
    }

    private static float angleRad(Vec2 v, Vec2 reference){
        return (float)Math.atan2(reference.x * v.y - reference.y * v.x, v.x * reference.x + v.y * reference.y);
    }

    private static void prepareStraightJoin(Vec2 B, Vec2 D, Vec2 E, float halfLineWidth){
        AB.setLength(halfLineWidth);
        D.set(-AB.y, AB.x).add(B);
        E.set(AB.y, -AB.x).add(B);
    }

    public static void dashLine(float x1, float y1, float x2, float y2, int divisions){
        float dx = x2 - x1, dy = y2 - y1;

        for(int i = 0; i < divisions; i += 2){
            line(x1 + ((float)i / divisions) * dx, y1 + ((float)i / divisions) * dy,
            x1 + ((i + 1f) / divisions) * dx, y1 + ((i + 1f) / divisions) * dy);
        }
    }

    public static void circle(float x, float y, float rad){
        poly(x, y, circleVertices(rad), rad);
    }

    public static void ellipse(float x, float y, float rad, float width, float height, float rot){
        ellipse(circleVertices(rad), x, y, width * rad, height * rad, rot);

        beginLine();
        float sides = circleVertices(rad);
        float space = 360f / sides;
        for(int i = 0; i < sides; i++){
            float a = space * i;
            u.trns(rot,
                rad * width * Mathf.cosDeg(a),
                rad * height * Mathf.sinDeg(a)
            );
            linePoint(x + u.x, y + u.y);
        }
        endLine(true);
    }

    public static void ellipse(int sides, float x, float y, float width, float height, float rot){
        beginLine();
        float space = 360f / sides;
        for(int i = 0; i < sides; i++){
            float a = space * i;
            u.trns(rot,
            width * Mathf.cosDeg(a),
            height * Mathf.sinDeg(a)
            );
            linePoint(x + u.x, y + u.y);
        }
        endLine(true);
    }

    public static void dashCircle(float x, float y, float radius){
        float scaleFactor = 0.6f;
        int sides = 10 + (int)(radius * scaleFactor);
        if(sides % 2 == 1) sides++;

        vector.set(0, 0);

        for(int i = 0; i < sides; i += 2){
            vector.set(radius, 0).rotate(360f / sides * i + 90);
            float x1 = vector.x;
            float y1 = vector.y;

            vector.set(radius, 0).rotate(360f / sides * (i + 1) + 90);

            line(x1 + x, y1 + y, vector.x + x, vector.y + y);
        }
    }

    public static void spikes(float x, float y, float radius, float length, int spikes, float rot){
        vector.set(0, 1);
        float step = 360f / spikes;

        for(int i = 0; i < spikes; i++){
            vector.trns(i * step + rot, radius);
            float x1 = vector.x, y1 = vector.y;
            vector.setLength(radius + length);

            line(x + x1, y + y1, x + vector.x, y + vector.y);
        }
    }

    public static void spikes(float x, float y, float rad, float length, int spikes){
        spikes(x, y, rad, length, spikes, 0);
    }

    public static void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4){
        floatBuilder.clear();

        floatBuilder.add(x1, y1, x2, y2);
        floatBuilder.add(x3, y3, x4, y4);

        polyline(floatBuilder, true);
    }

    public static void poly(float x, float y, int sides, float radius, float startAngle, float endAngle){
        float space = (endAngle - startAngle) / sides;
        float hstep = stroke / 2f / Mathf.cosDeg(space/2f);
        float r1 = radius - hstep, r2 = radius + hstep;

        for(int i = 0; i < sides; i++){
            float a = space * i + startAngle, cos = Mathf.cosDeg(a), sin = Mathf.sinDeg(a), cos2 = Mathf.cosDeg(a + space), sin2 = Mathf.sinDeg(a + space);
            Fill.quad(
            x + r1*cos, y + r1*sin,
            x + r1*cos2, y + r1*sin2,
            x + r2*cos2, y + r2*sin2,
            x + r2*cos, y + r2*sin
            );
        }
    }

    public static void poly(float x, float y, int sides, float radius, float angle){
        poly(x, y, sides, radius, angle, angle + 360);
    }

    public static void poly(float x, float y, int sides, float radius){
        poly(x, y, sides, radius, 0);
    }

    public static void poly(Vec2[] vertices, float offsetx, float offsety, float scl){
        for(int i = 0; i < vertices.length; i++){
            Vec2 current = vertices[i];
            Vec2 next = i == vertices.length - 1 ? vertices[0] : vertices[i + 1];
            line(current.x * scl + offsetx, current.y * scl + offsety, next.x * scl + offsetx, next.y * scl + offsety);
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

        float dddfx = tmp2x * pre5;
        float dddfy = tmp2y * pre5;

        beginLine();

        while(segments-- > 0){
            linePoint(fx, fy);

            fx += dfx;
            fy += dfy;
            dfx += ddfx;
            dfy += ddfy;
            ddfx += dddfx;
            ddfy += dddfy;
        }

        linePoint(x2, y2);

        endLine();
    }

    public static void arc(float x, float y, float radius, float fraction){
        arc(x, y, radius, fraction, 0f);
    }

    public static void arc(float x, float y, float radius, float fraction, float rotation){
        arc(x, y, radius, fraction, rotation, 50);
    }

    public static void arc(float x, float y, float radius, float fraction, float rotation, int sides){
        int max = Mathf.ceil(sides * fraction);
        floats.clear();

        for(int i = 0; i <= max; i++){
            vector.trns((float)i / max * fraction * 360f + rotation, radius);
            float x1 = vector.x;
            float y1 = vector.y;

            vector.trns((float)(i + 1) / max * fraction * 360f + rotation, radius);

            floats.add(x1 + x, y1 + y);
        }

        polyline(floats, false);
    }

    public static void square(float x, float y, float rad){
        rect(x - rad, y - rad, rad * 2, rad * 2);
    }

    public static void square(float x, float y, float rad, float rot){
        poly(x, y, 4, rad, rot - 45);
    }

    public static void rect(float x, float y, float width, float height, float xspace, float yspace){
        x -= xspace;
        y -= yspace;
        width += xspace * 2;
        height += yspace * 2;

        Fill.crect(x, y, width, stroke);
        Fill.crect(x, y + height, width, -stroke);

        Fill.crect(x + width, y, -stroke, height);
        Fill.crect(x, y, stroke, height);
    }

    public static void rect(float x, float y, float width, float height){
        rect(x, y, width, height, 0);
    }

    public static void rect(Rect rect){
        rect(rect.x, rect.y, rect.width, rect.height, 0);
    }

    public static void rect(float x, float y, float width, float height, float space){
        rect(x, y, width, height, space, space);
    }

    public static void stroke(float thick){
        stroke = thick;
    }

    public static void stroke(float thick, Color color){
        stroke = thick;
        Draw.color(color);
    }

    public static float getStroke(){
        return stroke;
    }
}
