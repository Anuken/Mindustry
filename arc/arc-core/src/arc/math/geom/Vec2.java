package arc.math.geom;

import arc.math.*;
import arc.util.*;

/**
 * Encapsulates a 2D vector. Allows chaining methods by returning a reference to itself
 * @author badlogicgames@gmail.com
 */
public class Vec2 implements Vector<Vec2>, Position{
    public static final Vec2 X = new Vec2(1, 0), Y = new Vec2(0, 1), ZERO = new Vec2(0, 0);

    /** the x-component of this vector **/
    public float x;
    /** the y-component of this vector **/
    public float y;

    /** Constructs a new vector at (0,0) */
    public Vec2(){
    }

    /**
     * Constructs a vector with the given components
     * @param x The x-component
     * @param y The y-component
     */
    public Vec2(float x, float y){
        this.x = x;
        this.y = y;
    }

    /**
     * Constructs a vector from the given vector
     * @param v The vector
     */
    public Vec2(Vec2 v){
        set(v);
    }

    public Vec2 trns(float angle, float amount){
        return set(amount, 0).rotate(angle);
    }

    public Vec2 trns(float angle, float x, float y){
        return set(x, y).rotate(angle);
    }

    public Vec2 trnsExact(float angle, float amount){
        return set(amount, 0).rotateRadExact(angle * Mathf.degreesToRadians);
    }

    /**Snaps this vector's coordinates to integers.*/
    public Vec2 snap(){
        return set((int)x, (int)y);
    }

    @Override
    public Vec2 div(Vec2 other){
        x /= other.x;
        y /= other.y;
        return this;
    }

    @Override
    public Vec2 cpy(){
        return new Vec2(this);
    }

    @Override
    public float len(){
        return (float)Math.sqrt(x * x + y * y);
    }

    @Override
    public float len2(){
        return x * x + y * y;
    }

    @Override
    public Vec2 set(Vec2 v){
        x = v.x;
        y = v.y;
        return this;
    }

    public Vec2 set(Position v){
        x = v.getX();
        y = v.getY();
        return this;
    }

    /**
     * Sets the components of this vector
     * @param x The x-component
     * @param y The y-component
     * @return This vector for chaining
     */
    public Vec2 set(float x, float y){
        this.x = x;
        this.y = y;
        return this;
    }

    public Vec2 set(Vec3 other){
        this.x = other.x;
        this.y = other.y;
        return this;
    }

    public Vec2 sub(Position v){
        return sub(v.getX(), v.getY());
    }

    @Override
    public Vec2 sub(Vec2 v){
        x -= v.x;
        y -= v.y;
        return this;
    }

    /**
     * Substracts the other vector from this vector.
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @return This vector for chaining
     */
    public Vec2 sub(float x, float y){
        this.x -= x;
        this.y -= y;
        return this;
    }

    public Vec2 sub(Vec3 v){
        return sub(v.x, v.y);
    }

    @Override
    public Vec2 nor(){
        float len = len();
        if(len != 0){
            x /= len;
            y /= len;
        }
        return this;
    }

    @Override
    public Vec2 add(Vec2 v){
        x += v.x;
        y += v.y;
        return this;
    }

    public Vec2 add(Position pos){
        return add(pos.getX(), pos.getY());
    }

    public Vec2 add(Vec2 vec, float scl){
        this.x += vec.x * scl;
        this.y += vec.y * scl;
        return this;
    }

    /**
     * Adds the given components to this vector
     * @param x The x-component
     * @param y The y-component
     * @return This vector for chaining
     */
    public Vec2 add(float x, float y){
        this.x += x;
        this.y += y;
        return this;
    }

    @Override
    public float dot(Vec2 v){
        return x * v.x + y * v.y;
    }

    public float dot(float ox, float oy){
        return x * ox + y * oy;
    }

    @Override
    public Vec2 scl(float scalar){
        x *= scalar;
        y *= scalar;
        return this;
    }

    public Vec2 inv(){
        return scl(-1f);
    }

    /**
     * Multiplies this vector by a scalar
     * @return This vector for chaining
     */
    public Vec2 scl(float x, float y){
        this.x *= x;
        this.y *= y;
        return this;
    }

    @Override
    public Vec2 scl(Vec2 v){
        this.x *= v.x;
        this.y *= v.y;
        return this;
    }

    @Override
    public Vec2 mulAdd(Vec2 vec, float scalar){
        this.x += vec.x * scalar;
        this.y += vec.y * scalar;
        return this;
    }

    @Override
    public Vec2 mulAdd(Vec2 vec, Vec2 mulVec){
        this.x += vec.x * mulVec.x;
        this.y += vec.y * mulVec.y;
        return this;
    }

    @Override
    public float dst(Vec2 v){
        final float x_d = v.x - x;
        final float y_d = v.y - y;
        return (float)Math.sqrt(x_d * x_d + y_d * y_d);
    }

    /**
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @return the distance between this and the other vector
     */
    public float dst(float x, float y){
        final float x_d = x - this.x;
        final float y_d = y - this.y;
        return (float)Math.sqrt(x_d * x_d + y_d * y_d);
    }

    @Override
    public float dst2(Vec2 v){
        final float x_d = v.x - x;
        final float y_d = v.y - y;
        return x_d * x_d + y_d * y_d;
    }

    /**
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @return the squared distance between this and the other vector
     */
    public float dst2(float x, float y){
        final float xd = x - this.x;
        final float yd = y - this.y;
        return xd * xd + yd * yd;
    }

    public Vec2 clampLength(float min, float max){
        float len2 = len2();
        if(len2 >= max * max){
            return limit(max);
        }else if(len2 <= min * min){
            return setLength(min);
        }
        return this;
    }

    @Override
    public Vec2 limit(float limit){
        return limit2(limit * limit);
    }

    @Override
    public Vec2 limit2(float limit2){
        float len2 = len2();
        if(len2 > limit2){
            return scl((float)Math.sqrt(limit2 / len2));
        }
        return this;
    }

    @Override
    public Vec2 clamp(float min, float max){
        final float len2 = len2();
        if(len2 == 0f) return this;
        float max2 = max * max;
        if(len2 > max2) return scl((float)Math.sqrt(max2 / len2));
        float min2 = min * min;
        if(len2 < min2) return scl((float)Math.sqrt(min2 / len2));
        return this;
    }

    @Override
    public Vec2 setLength(float len){
        return setLength2(len * len);
    }

    @Override
    public Vec2 setLength2(float len2){
        float oldLen2 = len2();
        return (oldLen2 == 0 || oldLen2 == len2) ? this : scl((float)Math.sqrt(len2 / oldLen2));
    }

    /**
     * Converts this {@code Vec2} to a string in the format {@code (x,y)}.
     * @return a string representation of this object.
     */
    @Override
    public String toString(){
        return "(" + x + "," + y + ")";
    }

    public Vec2 clamp(float minx, float miny, float maxx, float maxy){
        x = Mathf.clamp(x, minx, maxx);
        y = Mathf.clamp(y, miny, maxy);
        return this;
    }

    public Vec2 tryFromString(String v){
        try{
            int s = v.indexOf(',', 1);
            if(s != -1 && v.charAt(0) == '(' && v.charAt(v.length() - 1) == ')'){
                float x = Float.parseFloat(v.substring(1, s));
                float y = Float.parseFloat(v.substring(s + 1, v.length() - 1));
                return this.set(x, y);
            }
        }catch(Throwable t){

        }
        return setZero();
    }

    /**
     * Sets this {@code Vec2} to the value represented by the specified string according to the format of {@link #toString()}.
     * @param v the string.
     * @return this vector for chaining
     */
    public Vec2 fromString(String v){
        int s = v.indexOf(',', 1);
        if(s != -1 && v.charAt(0) == '(' && v.charAt(v.length() - 1) == ')'){
            try{
                float x = Float.parseFloat(v.substring(1, s));
                float y = Float.parseFloat(v.substring(s + 1, v.length() - 1));
                return this.set(x, y);
            }catch(NumberFormatException ex){
                // Throw a ArcRuntimeException
            }
        }
        throw new ArcRuntimeException("Malformed Vec2: " + v);
    }

    /**
     * Left-multiplies this vector by the given matrix
     * @param mat the matrix
     * @return this vector
     */
    public Vec2 mul(Mat mat){
        float x = this.x * mat.val[0] + this.y * mat.val[3] + mat.val[6];
        float y = this.x * mat.val[1] + this.y * mat.val[4] + mat.val[7];
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Calculates the 2D cross product between this and the given vector.
     * @param v the other vector
     * @return the cross product
     */
    public float crs(Vec2 v){
        return this.x * v.y - this.y * v.x;
    }

    /**
     * Calculates the 2D cross product between this and the given vector.
     * @param x the x-coordinate of the other vector
     * @param y the y-coordinate of the other vector
     * @return the cross product
     */
    public float crs(float x, float y){
        return this.x * y - this.y * x;
    }

    /**
     * @return the angle in degrees of this vector (point) relative to the x-axis. Angles are towards the positive y-axis
     * (typically counter-clockwise) and between 0 and 360.
     */
    public float angle(){
        float angle = Mathf.atan2(x, y) * Mathf.radiansToDegrees;
        if(angle < 0) angle += 360;
        return angle;
    }

    /**
     * @return the angle in degrees of this vector (point) relative to the given vector. Angles are towards the positive y-axis
     * (typically counter-clockwise.) between -180 and +180
     */
    public float angle(Vec2 reference){
        return (float)Math.atan2(crs(reference), dot(reference)) * Mathf.radiansToDegrees;
    }

    /**Sets this vector to a random direction with the specified length.*/
    public Vec2 rnd(float length){
        setToRandomDirection().scl(length);
        return this;
    }

    /**
     * @return the angle in radians of this vector (point) relative to the x-axis. Angles are towards the positive y-axis.
     * (typically counter-clockwise)
     */
    public float angleRad(){
        return (float)Math.atan2(y, x);
    }

    /**
     * @return the angle in radians of this vector (point) relative to the given vector. Angles are towards the positive y-axis.
     * (typically counter-clockwise.)
     */
    public float angleRad(Vec2 reference){
        return (float)Math.atan2(crs(reference), dot(reference));
    }

    /**
     * Sets the angle of the vector in degrees relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
     * @param degrees The angle in degrees to set.
     */
    public Vec2 setAngle(float degrees){
        return setAngleRad(degrees * Mathf.degreesToRadians);
    }

    /**
     * Sets the angle of the vector in radians relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
     * @param radians The angle in radians to set.
     */
    public Vec2 setAngleRad(float radians){
        this.set(len(), 0f);
        this.rotateRad(radians);

        return this;
    }

    public Vec2 rotateTo(float angle, float speed){
        return setAngle(Angles.moveToward(angle(), angle, speed));
    }

    /**
     * Rotates the Vec2 by the given angle, counter-clockwise assuming the y-axis points up.
     * @param degrees the angle in degrees
     */
    public Vec2 rotate(float degrees){
        return rotateRad(degrees * Mathf.degreesToRadians);
    }

    /**
     * Rotates the Vec2 by the given angle around reference vector, counter-clockwise assuming the y-axis points up.
     * @param degrees the angle in degrees
     * @param reference center Vec2
     */
    public Vec2 rotateAround(Vec2 reference, float degrees){
        return this.sub(reference).rotate(degrees).add(reference);
    }

    /**
     * Rotates the Vec2 by the given angle, counter-clockwise assuming the y-axis points up.
     * @param radians the angle in radians
     */
    public Vec2 rotateRad(float radians){
        float cos = Mathf.cos(radians);
        float sin = Mathf.sin(radians);

        float newX = this.x * cos - this.y * sin;
        float newY = this.x * sin + this.y * cos;

        this.x = newX;
        this.y = newY;

        return this;
    }

    public Vec2 rotateRadExact(float radians){
        float cos = (float)Math.cos(radians);
        float sin = (float)Math.sin(radians);

        float newX = this.x * cos - this.y * sin;
        float newY = this.x * sin + this.y * cos;

        this.x = newX;
        this.y = newY;

        return this;
    }

    /**
     * Rotates the Vec2 by the given angle around reference vector, counter-clockwise assuming the y-axis points up.
     * @param radians the angle in radians
     * @param reference center Vec2
     */
    public Vec2 rotateAroundRad(Vec2 reference, float radians){
        return this.sub(reference).rotateRad(radians).add(reference);
    }

    /** Rotates the Vec2 by 90 degrees in the specified direction, where >= 0 is counter-clockwise and < 0 is clockwise. */
    public Vec2 rotate90(int dir){
        float x = this.x;
        if(dir >= 0){
            this.x = -y;
            y = x;
        }else{
            this.x = y;
            y = -x;
        }
        return this;
    }

    public Vec2 approachDelta(Vec2 target, float alpha){
        return approach(target, Time.delta * alpha);
    }

    public Vec2 approach(Vec2 target, float alpha){
        float dx = x - target.x, dy = y - target.y;
        float alpha2 = alpha*alpha;
        float len2 = Mathf.len2(dx, dy);

        if(len2 > alpha2){
            float scl = Mathf.sqrt(alpha2 / len2);
            dx *= scl;
            dy *= scl;

            return sub(dx, dy);
        }else{
            return set(target);
        }
    }

    public Vec2 lerpPast(Vec2 target, float alpha){
        x = (x) + ((target.x - x) * alpha);
        y = (y) + ((target.y - y) * alpha);
        return this;
    }

    public Vec2 lerpDelta(float tx, float ty, float alpha){
        alpha = Mathf.clamp(alpha * Time.delta);
        final float invAlpha = 1.0f - alpha;
        this.x = (x * invAlpha) + (tx * alpha);
        this.y = (y * invAlpha) + (ty * alpha);
        return this;
    }

    public Vec2 lerpDelta(Position target, float alpha){
        alpha = Mathf.clamp(alpha * Time.delta);
        final float invAlpha = 1.0f - alpha;
        this.x = (x * invAlpha) + (target.getX() * alpha);
        this.y = (y * invAlpha) + (target.getY() * alpha);
        return this;
    }

    public Vec2 lerp(Position target, float alpha){
        final float invAlpha = 1.0f - alpha;
        this.x = (x * invAlpha) + (target.getX() * alpha);
        this.y = (y * invAlpha) + (target.getY() * alpha);
        return this;
    }

    public Vec2 lerp(float tx, float ty, float alpha){
        final float invAlpha = 1.0f - alpha;
        this.x = (x * invAlpha) + (tx * alpha);
        this.y = (y * invAlpha) + (ty * alpha);
        return this;
    }

    @Override
    public Vec2 lerp(Vec2 target, float alpha){
        final float invAlpha = 1.0f - alpha;
        this.x = (x * invAlpha) + (target.x * alpha);
        this.y = (y * invAlpha) + (target.y * alpha);
        return this;
    }

    @Override
    public Vec2 interpolate(Vec2 target, float alpha, Interp interpolation){
        return lerp(target, interpolation.apply(alpha));
    }

    @Override
    public Vec2 setToRandomDirection(){
        float theta = Mathf.random(0f, Mathf.PI2);
        return this.set(Mathf.cos(theta), Mathf.sin(theta));
    }

    public Vec2 setToRandomDirection(Rand rand){
        float theta = rand.random(0f, Mathf.PI2);
        return this.set(Mathf.cos(theta), Mathf.sin(theta));
    }

    @Override
    public int hashCode(){
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(x);
        result = prime * result + Float.floatToIntBits(y);
        return result;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        Vec2 other = (Vec2)obj;
        if(Float.floatToIntBits(x) != Float.floatToIntBits(other.x)) return false;
        return Float.floatToIntBits(y) == Float.floatToIntBits(other.y);
    }

    @Override
    public boolean epsilonEquals(Vec2 other, float epsilon){
        if(other == null) return false;
        if(Math.abs(other.x - x) > epsilon) return false;
        return !(Math.abs(other.y - y) > epsilon);
    }

    /**
     * Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
     * @return whether the vectors are the same.
     */
    public boolean epsilonEquals(float x, float y, float epsilon){
        if(Math.abs(x - this.x) > epsilon) return false;
        return !(Math.abs(y - this.y) > epsilon);
    }

    /**
     * Compares this vector with the other vector using Mathf.FLOAT_ROUNDING_ERROR for fuzzy equality testing
     * @param other other vector to compare
     * @return true if vector are equal, otherwise false
     */
    public boolean epsilonEquals(final Vec2 other){
        return epsilonEquals(other, Mathf.FLOAT_ROUNDING_ERROR);
    }

    /**
     * Compares this vector with the other vector using Mathf.FLOAT_ROUNDING_ERROR for fuzzy equality testing
     * @param x x component of the other vector to compare
     * @param y y component of the other vector to compare
     * @return true if vector are equal, otherwise false
     */
    public boolean epsilonEquals(float x, float y){
        return epsilonEquals(x, y, Mathf.FLOAT_ROUNDING_ERROR);
    }

    public boolean isNaN(){
        return Float.isNaN(x) || Float.isNaN(y);
    }

    public boolean isInfinite(){
        return Float.isInfinite(x) || Float.isInfinite(y);
    }

    @Override
    public boolean isUnit(){
        return isUnit(0.000000001f);
    }

    @Override
    public boolean isUnit(final float margin){
        return Math.abs(len2() - 1f) < margin;
    }

    @Override
    public boolean isZero(){
        return x == 0 && y == 0;
    }

    @Override
    public boolean isZero(final float margin){
        return len2() < margin;
    }

    @Override
    public boolean isOnLine(Vec2 other){
        return Mathf.zero(x * other.y - y * other.x);
    }

    @Override
    public boolean isOnLine(Vec2 other, float epsilon){
        return Mathf.zero(x * other.y - y * other.x, epsilon);
    }

    @Override
    public boolean isCollinear(Vec2 other, float epsilon){
        return isOnLine(other, epsilon) && dot(other) > 0f;
    }

    @Override
    public boolean isCollinear(Vec2 other){
        return isOnLine(other) && dot(other) > 0f;
    }

    @Override
    public boolean isCollinearOpposite(Vec2 other, float epsilon){
        return isOnLine(other, epsilon) && dot(other) < 0f;
    }

    @Override
    public boolean isCollinearOpposite(Vec2 other){
        return isOnLine(other) && dot(other) < 0f;
    }

    @Override
    public boolean isPerpendicular(Vec2 vector){
        return Mathf.zero(dot(vector));
    }

    @Override
    public boolean isPerpendicular(Vec2 vector, float epsilon){
        return Mathf.zero(dot(vector), epsilon);
    }

    @Override
    public boolean hasSameDirection(Vec2 vector){
        return dot(vector) > 0;
    }

    @Override
    public boolean hasOppositeDirection(Vec2 vector){
        return dot(vector) < 0;
    }

    @Override
    public Vec2 setZero(){
        this.x = 0;
        this.y = 0;
        return this;
    }

    @Override
    public float getX(){
        return x;
    }

    @Override
    public float getY(){
        return y;
    }
}
