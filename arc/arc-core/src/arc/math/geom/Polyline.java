package arc.math.geom;

import arc.math.Mathf;

public class Polyline implements Shape2D{
    private float[] localVertices;
    private float[] worldVertices;
    private float x, y;
    private float originX, originY;
    private float rotation;
    private float scaleX = 1, scaleY = 1;
    private float length;
    private float scaledLength;
    private boolean calculateScaledLength = true;
    private boolean calculateLength = true;
    private boolean dirty = true;

    public Polyline(){
        this.localVertices = new float[0];
    }

    public Polyline(float[] vertices){
        if(vertices.length < 4) throw new IllegalArgumentException("polylines must contain at least 2 points.");
        this.localVertices = vertices;
    }

    /** Returns vertices without scaling or rotation and without being offset by the polyline position. */
    public float[] getVertices(){
        return localVertices;
    }

    public void setVertices(float[] vertices){
        if(vertices.length < 4) throw new IllegalArgumentException("polylines must contain at least 2 points.");
        this.localVertices = vertices;
        dirty = true;
    }

    /** Returns vertices scaled, rotated, and offset by the polygon position. */
    public float[] getTransformedVertices(){
        if(!dirty) return worldVertices;
        dirty = false;

        final float[] localVertices = this.localVertices;
        if(worldVertices == null || worldVertices.length < localVertices.length)
            worldVertices = new float[localVertices.length];

        final float[] worldVertices = this.worldVertices;
        final float positionX = x;
        final float positionY = y;
        final float originX = this.originX;
        final float originY = this.originY;
        final float scaleX = this.scaleX;
        final float scaleY = this.scaleY;
        final boolean scale = scaleX != 1 || scaleY != 1;
        final float rotation = this.rotation;
        final float cos = Mathf.cosDeg(rotation);
        final float sin = Mathf.sinDeg(rotation);

        for(int i = 0, n = localVertices.length; i < n; i += 2){
            float x = localVertices[i] - originX;
            float y = localVertices[i + 1] - originY;

            // scale if needed
            if(scale){
                x *= scaleX;
                y *= scaleY;
            }

            // rotate if needed
            if(rotation != 0){
                float oldX = x;
                x = cos * x - sin * y;
                y = sin * oldX + cos * y;
            }

            worldVertices[i] = positionX + x + originX;
            worldVertices[i + 1] = positionY + y + originY;
        }
        return worldVertices;
    }

    /** Returns the euclidean length of the polyline without scaling */
    public float getLength(){
        if(!calculateLength) return length;
        calculateLength = false;

        length = 0;
        for(int i = 0, n = localVertices.length - 2; i < n; i += 2){
            float x = localVertices[i + 2] - localVertices[i];
            float y = localVertices[i + 1] - localVertices[i + 3];
            length += (float)Math.sqrt(x * x + y * y);
        }

        return length;
    }

    /** Returns the euclidean length of the polyline */
    public float getScaledLength(){
        if(!calculateScaledLength) return scaledLength;
        calculateScaledLength = false;

        scaledLength = 0;
        for(int i = 0, n = localVertices.length - 2; i < n; i += 2){
            float x = localVertices[i + 2] * scaleX - localVertices[i] * scaleX;
            float y = localVertices[i + 1] * scaleY - localVertices[i + 3] * scaleY;
            scaledLength += (float)Math.sqrt(x * x + y * y);
        }

        return scaledLength;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public float getOriginX(){
        return originX;
    }

    public float getOriginY(){
        return originY;
    }

    public float getRotation(){
        return rotation;
    }

    public void setRotation(float degrees){
        this.rotation = degrees;
        dirty = true;
    }

    public float getScaleX(){
        return scaleX;
    }

    public float getScaleY(){
        return scaleY;
    }

    public void setOrigin(float originX, float originY){
        this.originX = originX;
        this.originY = originY;
        dirty = true;
    }

    public void setPosition(float x, float y){
        this.x = x;
        this.y = y;
        dirty = true;
    }

    public void rotate(float degrees){
        rotation += degrees;
        dirty = true;
    }

    public void setScale(float scaleX, float scaleY){
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        dirty = true;
        calculateScaledLength = true;
    }

    public void scale(float amount){
        this.scaleX += amount;
        this.scaleY += amount;
        dirty = true;
        calculateScaledLength = true;
    }

    public void calculateLength(){
        calculateLength = true;
    }

    public void calculateScaledLength(){
        calculateScaledLength = true;
    }

    public void dirty(){
        dirty = true;
    }

    public void translate(float x, float y){
        this.x += x;
        this.y += y;
        dirty = true;
    }

    @Override
    public boolean contains(Vec2 point){
        return false;
    }

    @Override
    public boolean contains(float x, float y){
        return false;
    }
}
