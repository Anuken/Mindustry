package arc.graphics.g2d;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;

/** Base batch class. Provides a mesh, texture, shader, and other state. */
public abstract class Batch implements Disposable{
    protected float z;
    protected int idx = 0;
    protected Texture lastTexture = null;

    protected boolean apply;

    protected final Mat transformMatrix = new Mat();
    protected final Mat projectionMatrix = new Mat();
    protected final Mat combinedMatrix = new Mat();

    protected Blending blending = Blending.normal;

    protected @Nullable Shader customShader;

    protected float colorPacked = Color.whiteFloatBits;
    protected float mixColorPacked = Color.clearFloatBits;

    protected void z(float z){
        this.z = z;
    }

    /** Enables or disables Z-sorting. Flushes the batch. Only does something on supported batches. */
    protected void setSort(boolean sort){

    }

    protected void setPackedColor(float packedColor){
        this.colorPacked = packedColor;
    }

    protected float getPackedColor(){
        return colorPacked;
    }

    protected void setPackedMixColor(float packedColor){
        this.mixColorPacked = packedColor;
    }

    protected float getPackedMixColor(){
        return mixColorPacked;
    }

    protected abstract void draw(Texture texture, float[] spriteVertices, int offset, int count);

    protected abstract void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation);

    protected void draw(Runnable request){
        request.run();
    }

    protected abstract void flush();

    /** Discards any pending sprites. */
    protected void discard(){
        idx = 0;
    }

    protected void setBlending(Blending blending){
        if(this.blending != blending){
            flush();
        }
        this.blending = blending;
    }

    protected Blending getBlending(){
        return blending;
    }

    @Override
    public void dispose(){
    }

    public boolean hasPending(){
        return idx > 0;
    }

    protected Mat getProjection(){
        return projectionMatrix;
    }

    protected Mat getTransform(){
        return transformMatrix;
    }

    protected void setProjection(Mat projection){
        flush();
        projectionMatrix.set(projection);
    }

    protected void setTransform(Mat transform){
        flush();
        transformMatrix.set(transform);
    }

    protected void setupMatrices(){
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        getShader().setUniformMatrix4("u_projTrans", combinedMatrix);
    }

    protected void switchTexture(Texture texture){
        flush();
        lastTexture = texture;
    }

    protected void setShader(Shader shader){
        setShader(shader, true);
    }

    protected void setShader(Shader shader, boolean apply){
        flush();
        customShader = shader;
        this.apply = apply;
    }

    protected Shader getShader(){
        return customShader;
    }
}