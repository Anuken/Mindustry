package arc.graphics.g3d;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;

/** A SpriteBatch that projects sprites onto a plane in 3D space. */
public class PlaneBatch3D extends Batch{
    protected final Vec3 up = new Vec3(), right = new Vec3(), origin = new Vec3(), vec = new Vec3();
    protected final VertexBatch3D batch;
    protected final float[] vertex = new float[7]; //format: xyzcuvd
    protected float scaling = 1f;

    public PlaneBatch3D(){
        this(5000);
    }

    public PlaneBatch3D(int vertices){
        batch = new VertexBatch3D(vertices, false, true, true);
    }

    /** Sets scaling of sprite units. */
    public void setScaling(float scaling){
        this.scaling = scaling;
    }

    /** Sets the plane that this batch projects sprites onto. */
    public void setPlane(Vec3 origin, Vec3 up, Vec3 right){
        this.origin.set(origin);
        this.up.set(up).nor();
        this.right.set(right).nor();
    }

    public void proj(Mat3D mat){
        batch.proj(mat);
    }

    @Override
    protected void flush(){
        if(lastTexture == null || idx == 0){
            return;
        }

        //disable depth mask when flushing to prevent Z fighting
        Gl.depthMask(false);

        lastTexture.bind();
        batch.flush(Gl.triangles);
        idx = 0;

        Gl.depthMask(true);
    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){

        Texture texture = region.texture;
        if(lastTexture == null || texture.getHandle()  != lastTexture.getHandle()){
            switchTexture(texture);
        }

        float depth = texture.getDepth();

        checkFlush();

        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;

        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;

        // rotate
        final float cos = Mathf.cosDeg(rotation);
        final float sin = Mathf.sinDeg(rotation);

        x1 = cos * fx - sin * fy;
        y1 = sin * fx + cos * fy;

        x2 = cos * fx - sin * fy2;
        y2 = sin * fx + cos * fy2;

        x3 = cos * fx2 - sin * fy2;
        y3 = sin * fx2 + cos * fy2;

        x4 = x1 + (x3 - x2);
        y4 = y3 - (y2 - y1);

        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;

        final float u = region.u;
        final float v = region.v2;
        final float u2 = region.u2;
        final float v2 = region.v;
        final float color = this.colorPacked;

        vertex(x2, y2, color, u, v2, depth);
        vertex(x1, y1, color, u, v, depth);
        vertex(x3, y3, color, u2, v2, depth);

        vertex(x4, y4, color, u2, v, depth);
        vertex(x3, y3, color, u2, v2, depth);
        vertex(x1, y1, color, u, v, depth);

        idx ++;
    }

    @Override
    protected void draw(Texture texture, float[] v, int offset, int count){
        if(lastTexture == null || texture.getHandle() != lastTexture.getHandle()){
            switchTexture(texture);
        }

        for(int i = offset; i < count; i += SpriteBatch.spriteSize){
            checkFlush();

            vertex(v, i);
            vertex(v, i + SpriteBatch.vertexSize * 2);
            vertex(v, i + SpriteBatch.vertexSize);

            vertex(v, i + SpriteBatch.vertexSize * 2);
            vertex(v, i);
            vertex(v, i + SpriteBatch.vertexSize * 3);

            idx ++;
        }
    }

    private void checkFlush(){
        if(idx >= batch.getMaxVertices() / 6 / 6){
            flush();
        }
    }

    private void vertex(float[] v, int offset){
        vertex(v[offset], v[offset + 1], v[offset + 5], v[offset + 2], v[offset + 3], v[offset + 4]);
    }

    private void vertex(float x1, float y1, float c1, float u1, float v1, float depth){
        vec.set(origin).add(right, x1 * scaling).add(up, y1 * scaling);
        vertex[0] = vec.x;
        vertex[1] = vec.y;
        vertex[2] = vec.z;
        vertex[3] = c1;
        vertex[4] = u1;
        vertex[5] = v1;
        vertex[6] = depth;
        batch.vertex(vertex);
    }
}
