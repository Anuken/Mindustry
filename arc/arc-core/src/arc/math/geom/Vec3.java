package arc.math.geom;

import arc.math.*;
import arc.util.*;

/**
 * Encapsulates a 3D vector. Allows chaining operations by returning a reference to itself in all modification methods.
 * @author badlogicgames@gmail.com
 */
public class Vec3 implements Vector<Vec3>{
    public final static Vec3 X = new Vec3(1, 0, 0);
    public final static Vec3 Y = new Vec3(0, 1, 0);
    public final static Vec3 Z = new Vec3(0, 0, 1);
    public final static Vec3 Zero = new Vec3(0, 0, 0);
    private static final Mat tmpMat = new Mat();
    /** the x-component of this vector **/
    public float x;
    /** the y-component of this vector **/
    public float y;
    /** the z-component of this vector **/
    public float z;

    /** Constructs a vector at (0,0,0) */
    public Vec3(){
    }

    /**
     * Creates a vector with the given components
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     */
    public Vec3(float x, float y, float z){
        this.set(x, y, z);
    }

    public Vec3(double x, double y, double z){
        this((float)x, (float)y, (float)z);
    }

    /**
     * Creates a vector from the given vector
     * @param vector The vector
     */
    public Vec3(Vec3 vector){
        this.set(vector);
    }

    /**
     * Creates a vector from the given array. The array must have at least 3 elements.
     * @param values The array
     */
    public Vec3(float[] values){
        this.set(values[0], values[1], values[2]);
    }

    /**
     * Creates a vector from the given vector and z-component
     * @param vector The vector
     * @param z The z-component
     */
    public Vec3(Vec2 vector, float z){
        this.set(vector.x, vector.y, z);
    }

    /** @return The euclidean length */
    public static float len(float x, float y, float z){
        return (float)Math.sqrt(x * x + y * y + z * z);
    }

    /** @return The squared euclidean length */
    public static float len2(final float x, final float y, final float z){
        return x * x + y * y + z * z;
    }

    /** @return The euclidean distance between the two specified vectors */
    public static float dst(final float x1, final float y1, final float z1, final float x2, final float y2, final float z2){
        final float a = x2 - x1;
        final float b = y2 - y1;
        final float c = z2 - z1;
        return (float)Math.sqrt(a * a + b * b + c * c);
    }

    /** @return the squared distance between the given points */
    public static float dst2(final float x1, final float y1, final float z1, final float x2, final float y2, final float z2){
        final float a = x2 - x1;
        final float b = y2 - y1;
        final float c = z2 - z1;
        return a * a + b * b + c * c;
    }

    /** @return The dot product between the two vectors */
    public static float dot(float x1, float y1, float z1, float x2, float y2, float z2){
        return x1 * x2 + y1 * y2 + z1 * z2;
    }

    /**
     * Sets the vector to the given components
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @return this vector for chaining
     */
    public Vec3 set(float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Override
    public Vec3 div(Vec3 other){
        x /= other.x;
        y /= other.y;
        z /= other.z;

        return this;
    }

    @Override
    public Vec3 set(final Vec3 vector){
        return this.set(vector.x, vector.y, vector.z);
    }

    /**
     * Sets the components from the array. The array must have at least 3 elements
     * @param values The array
     * @return this vector for chaining
     */
    public Vec3 set(final float[] values){
        return this.set(values[0], values[1], values[2]);
    }

    public Vec3 set(final float[] values, int offset){
        return this.set(values[offset], values[offset + 1], values[offset + 2]);
    }

    /**
     * Sets the components of the given vector and z-component
     * @param vector The vector
     * @param z The z-component
     * @return This vector for chaining
     */
    public Vec3 set(final Vec2 vector, float z){
        return this.set(vector.x, vector.y, z);
    }

    /**
     * Sets the components from the given spherical coordinate
     * @param azimuthalAngle The angle between x-axis in radians [0, 2pi]
     * @param polarAngle The angle between z-axis in radians [0, pi]
     * @return This vector for chaining
     */
    public Vec3 setFromSpherical(float azimuthalAngle, float polarAngle){
        float cosPolar = Mathf.cos(polarAngle);
        float sinPolar = Mathf.sin(polarAngle);

        float cosAzim = Mathf.cos(azimuthalAngle);
        float sinAzim = Mathf.sin(azimuthalAngle);

        return this.set(cosAzim * sinPolar, sinAzim * sinPolar, cosPolar);
    }

    @Override
    public Vec3 setToRandomDirection(){
        return setToRandomDirection(Mathf.rand);
    }

    public Vec3 setToRandomDirection(Rand rand){
        float u = rand.random(1f);
        float v = rand.random(1f);

        float theta = Mathf.PI2 * u; // azimuthal angle
        float phi = (float)Math.acos(2f * v - 1f); // polar angle

        return this.setFromSpherical(theta, phi);
    }

    @Override
    public Vec3 cpy(){
        return new Vec3(this);
    }

    @Override
    public Vec3 add(final Vec3 vector){
        return this.add(vector.x, vector.y, vector.z);
    }

    public Vec3 cpy(Vec3 dest){
        return dest.set(this);
    }

    public Vec3 add(Vec3 vector, float scale){
        return this.add(vector.x * scale, vector.y * scale, vector.z * scale);
    }

    public Vec3 sub(Vec3 vector, float scale){
        return this.sub(vector.x * scale, vector.y * scale, vector.z * scale);
    }

    /**
     * Adds the given vector to this component
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining.
     */
    public Vec3 add(float x, float y, float z){
        return this.set(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Adds the given value to all three components of the vector.
     * @param values The value
     * @return This vector for chaining
     */
    public Vec3 add(float values){
        return this.set(this.x + values, this.y + values, this.z + values);
    }

    @Override
    public Vec3 sub(final Vec3 a_vec){
        return this.sub(a_vec.x, a_vec.y, a_vec.z);
    }

    /**
     * Subtracts the other vector from this vector.
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining
     */
    public Vec3 sub(float x, float y, float z){
        return this.set(this.x - x, this.y - y, this.z - z);
    }

    /**
     * Subtracts the given value from all components of this vector
     * @param value The value
     * @return This vector for chaining
     */
    public Vec3 sub(float value){
        return this.set(this.x - value, this.y - value, this.z - value);
    }

    @Override
    public Vec3 scl(float scalar){
        return this.set(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    @Override
    public Vec3 scl(final Vec3 other){
        return this.set(x * other.x, y * other.y, z * other.z);
    }

    /**
     * Scales this vector by the given values
     * @param vx X value
     * @param vy Y value
     * @param vz Z value
     * @return This vector for chaining
     */
    public Vec3 scl(float vx, float vy, float vz){
        return this.set(this.x * vx, this.y * vy, this.z * vz);
    }

    @Override
    public Vec3 mulAdd(Vec3 vec, float scalar){
        this.x += vec.x * scalar;
        this.y += vec.y * scalar;
        this.z += vec.z * scalar;
        return this;
    }

    @Override
    public Vec3 mulAdd(Vec3 vec, Vec3 mulVec){
        this.x += vec.x * mulVec.x;
        this.y += vec.y * mulVec.y;
        this.z += vec.z * mulVec.z;
        return this;
    }

    @Override
    public float len(){
        return (float)Math.sqrt(x * x + y * y + z * z);
    }

    @Override
    public float len2(){
        return x * x + y * y + z * z;
    }

    /**
     * @param vector The other vector
     * @return Whether this and the other vector are equal
     */
    public boolean idt(final Vec3 vector){
        return x == vector.x && y == vector.y && z == vector.z;
    }

    @Override
    public float dst(final Vec3 vector){
        final float a = vector.x - x;
        final float b = vector.y - y;
        final float c = vector.z - z;
        return (float)Math.sqrt(a * a + b * b + c * c);
    }

    /** @return the distance between this point and the given point */
    public float dst(float x, float y, float z){
        final float a = x - this.x;
        final float b = y - this.y;
        final float c = z - this.z;
        return (float)Math.sqrt(a * a + b * b + c * c);
    }

    @Override
    public float dst2(Vec3 point){
        final float a = point.x - x;
        final float b = point.y - y;
        final float c = point.z - z;
        return a * a + b * b + c * c;
    }

    /**
     * Returns the squared distance between this point and the given point
     * @param x The x-component of the other point
     * @param y The y-component of the other point
     * @param z The z-component of the other point
     * @return The squared distance
     */
    public float dst2(float x, float y, float z){
        final float a = x - this.x;
        final float b = y - this.y;
        final float c = z - this.z;
        return a * a + b * b + c * c;
    }

    public boolean within(Vec3 v, float dst){
        return dst2(v) < dst * dst;
    }

    @Override
    public Vec3 nor(){
        final float len2 = this.len2();
        if(len2 == 0f || len2 == 1f) return this;
        return this.scl(1f / (float)Math.sqrt(len2));
    }

    @Override
    public float dot(final Vec3 vector){
        return x * vector.x + y * vector.y + z * vector.z;
    }

    /** @return the angle to the other vector, in radians. */
    public float angleRad(final Vec3 vector){
        float l = len();
        float l2 = vector.len();
        return (float)Math.acos(dot(x / l, y / l, z / l, vector.x / l2, vector.y / l2, vector.z / l2));
    }

    /** @return the angle to the other vector, in degrees. */
    public float angle(final Vec3 vector){
        return angleRad(vector) * Mathf.radDeg;
    }

    /**
     * Returns the dot product between this and the given vector.
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return The dot product
     */
    public float dot(float x, float y, float z){
        return this.x * x + this.y * y + this.z * z;
    }

    /**
     * Sets this vector to the cross product between it and the other vector.
     * @param vector The other vector
     * @return This vector for chaining
     */
    public Vec3 crs(final Vec3 vector){
        return this.set(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x);
    }

    /**
     * Sets this vector to the cross product between it and the other vector.
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining
     */
    public Vec3 crs(float x, float y, float z){
        return this.set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x);
    }

    /**
     * Left-multiplies the vector by the given 4x3 column major matrix. The matrix should be composed by a 3x3 matrix representing
     * rotation and scale plus a 1x3 matrix representing the translation.
     * @param matrix The matrix
     * @return This vector for chaining
     */
    public Vec3 mul4x3(float[] matrix){
        return set(x * matrix[0] + y * matrix[3] + z * matrix[6] + matrix[9], x * matrix[1] + y * matrix[4] + z * matrix[7]
        + matrix[10], x * matrix[2] + y * matrix[5] + z * matrix[8] + matrix[11]);
    }

    /**
     * Left-multiplies the vector by the given matrix.
     * @param matrix The matrix
     * @return This vector for chaining
     */
    public Vec3 mul(Mat matrix){
        final float[] l_mat = matrix.val;
        return set(x * l_mat[Mat.M00] + y * l_mat[Mat.M01] + z * l_mat[Mat.M02], x * l_mat[Mat.M10] + y
        * l_mat[Mat.M11] + z * l_mat[Mat.M12], x * l_mat[Mat.M20] + y * l_mat[Mat.M21] + z * l_mat[Mat.M22]);
    }

    /**
     * Multiplies the vector by the transpose of the given matrix.
     * @param matrix The matrix
     * @return This vector for chaining
     */
    public Vec3 traMul(Mat matrix){
        final float[] l_mat = matrix.val;
        return set(x * l_mat[Mat.M00] + y * l_mat[Mat.M10] + z * l_mat[Mat.M20], x * l_mat[Mat.M01] + y
        * l_mat[Mat.M11] + z * l_mat[Mat.M21], x * l_mat[Mat.M02] + y * l_mat[Mat.M12] + z * l_mat[Mat.M22]);
    }

    /**
     * Rotates this vector by the given angle in degrees around the given axis.
     * @param axis the axis
     * @param degrees the angle in degrees
     * @return This vector for chaining
     */
    public Vec3 rotate(final Vec3 axis, float degrees){
        tmpMat.setToRotation(axis, degrees);
        return this.mul(tmpMat);
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
        return x == 0 && y == 0 && z == 0;
    }

    @Override
    public boolean isZero(final float margin){
        return len2() < margin;
    }

    @Override
    public boolean isOnLine(Vec3 other, float epsilon){
        return len2(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= epsilon;
    }

    @Override
    public boolean isOnLine(Vec3 other){
        return len2(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= Mathf.FLOAT_ROUNDING_ERROR;
    }

    @Override
    public boolean isCollinear(Vec3 other, float epsilon){
        return isOnLine(other, epsilon) && hasSameDirection(other);
    }

    @Override
    public boolean isCollinear(Vec3 other){
        return isOnLine(other) && hasSameDirection(other);
    }

    @Override
    public boolean isCollinearOpposite(Vec3 other, float epsilon){
        return isOnLine(other, epsilon) && hasOppositeDirection(other);
    }

    @Override
    public boolean isCollinearOpposite(Vec3 other){
        return isOnLine(other) && hasOppositeDirection(other);
    }

    @Override
    public boolean isPerpendicular(Vec3 vector){
        return Mathf.zero(dot(vector));
    }

    @Override
    public boolean isPerpendicular(Vec3 vector, float epsilon){
        return Mathf.zero(dot(vector), epsilon);
    }

    @Override
    public boolean hasSameDirection(Vec3 vector){
        return dot(vector) > 0;
    }

    @Override
    public boolean hasOppositeDirection(Vec3 vector){
        return dot(vector) < 0;
    }

    @Override
    public Vec3 lerp(final Vec3 target, float alpha){
        x += alpha * (target.x - x);
        y += alpha * (target.y - y);
        z += alpha * (target.z - z);
        return this;
    }

    @Override
    public Vec3 interpolate(Vec3 target, float alpha, Interp interpolator){
        return lerp(target, interpolator.apply(0f, 1f, alpha));
    }

    /**
     * Spherically interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is
     * stored in this vector.
     * @param target The target vector
     * @param alpha The interpolation coefficient
     * @return This vector for chaining.
     */
    public Vec3 slerp(final Vec3 target, float alpha){
        final float dot = dot(target);
        // If the inputs are too close for comfort, simply linearly interpolate.
        if(dot > 0.9995 || dot < -0.9995) return lerp(target, alpha);

        // theta0 = angle between input vectors
        final float theta0 = (float)Math.acos(dot);
        // theta = angle between this vector and result
        final float theta = theta0 * alpha;

        final float st = (float)Math.sin(theta);
        final float tx = target.x - x * dot;
        final float ty = target.y - y * dot;
        final float tz = target.z - z * dot;
        final float l2 = tx * tx + ty * ty + tz * tz;
        final float dl = st * ((l2 < 0.0001f) ? 1f : 1f / (float)Math.sqrt(l2));

        return scl((float)Math.cos(theta)).add(tx * dl, ty * dl, tz * dl).nor();
    }

    /**
     * Converts this {@code Vec3} to a string in the format {@code (x,y,z)}.
     * @return a string representation of this object.
     */
    @Override
    public String toString(){
        return "(" + x + "," + y + "," + z + ")";
    }

    /**
     * Sets this {@code Vec3} to the value represented by the specified string according to the format of {@link #toString()}.
     * @param v the string.
     * @return this vector for chaining
     */
    public Vec3 fromString(String v){
        int s0 = v.indexOf(',', 1);
        int s1 = v.indexOf(',', s0 + 1);
        if(s0 != -1 && s1 != -1 && v.charAt(0) == '(' && v.charAt(v.length() - 1) == ')'){
            try{
                float x = Float.parseFloat(v.substring(1, s0));
                float y = Float.parseFloat(v.substring(s0 + 1, s1));
                float z = Float.parseFloat(v.substring(s1 + 1, v.length() - 1));
                return this.set(x, y, z);
            }catch(NumberFormatException ex){
                // Throw a ArcRuntimeException
            }
        }
        throw new ArcRuntimeException("Malformed Vec3: " + v);
    }

    @Override
    public Vec3 limit(float limit){
        return limit2(limit * limit);
    }

    @Override
    public Vec3 limit2(float limit2){
        float len2 = len2();
        if(len2 > limit2){
            scl((float)Math.sqrt(limit2 / len2));
        }
        return this;
    }

    @Override
    public Vec3 setLength(float len){
        return setLength2(len * len);
    }

    @Override
    public Vec3 setLength2(float len2){
        float oldLen2 = len2();
        return (oldLen2 == 0 || oldLen2 == len2) ? this : scl((float)Math.sqrt(len2 / oldLen2));
    }

    @Override
    public Vec3 clamp(float min, float max){
        final float len2 = len2();
        if(len2 == 0f) return this;
        float max2 = max * max;
        if(len2 > max2) return scl((float)Math.sqrt(max2 / len2));
        float min2 = min * min;
        if(len2 < min2) return scl((float)Math.sqrt(min2 / len2));
        return this;
    }

    @Override
    public int hashCode(){
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(x);
        result = prime * result + Float.floatToIntBits(y);
        result = prime * result + Float.floatToIntBits(z);
        return result;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        Vec3 other = (Vec3)obj;
        return Float.floatToIntBits(x) == Float.floatToIntBits(other.x) && Float.floatToIntBits(y) == Float.floatToIntBits(other.y) && Float.floatToIntBits(z) == Float.floatToIntBits(other.z);
    }

    @Override
    public boolean epsilonEquals(final Vec3 other, float epsilon){
        if(other == null) return false;
        if(Math.abs(other.x - x) > epsilon) return false;
        if(Math.abs(other.y - y) > epsilon) return false;
        return !(Math.abs(other.z - z) > epsilon);
    }

    /**
     * Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
     * @return whether the vectors are the same.
     */
    public boolean epsilonEquals(float x, float y, float z, float epsilon){
        if(Math.abs(x - this.x) > epsilon) return false;
        if(Math.abs(y - this.y) > epsilon) return false;
        return !(Math.abs(z - this.z) > epsilon);
    }

    /**
     * Compares this vector with the other vector using Mathf.FLOAT_ROUNDING_ERROR for fuzzy equality testing
     * @param other other vector to compare
     * @return true if vector are equal, otherwise false
     */
    public boolean epsilonEquals(final Vec3 other){
        return epsilonEquals(other, Mathf.FLOAT_ROUNDING_ERROR);
    }

    /**
     * Compares this vector with the other vector using Mathf.FLOAT_ROUNDING_ERROR for fuzzy equality testing
     * @param x x component of the other vector to compare
     * @param y y component of the other vector to compare
     * @param z z component of the other vector to compare
     * @return true if vector are equal, otherwise false
     */
    public boolean epsilonEquals(float x, float y, float z){
        return epsilonEquals(x, y, z, Mathf.FLOAT_ROUNDING_ERROR);
    }

    @Override
    public Vec3 setZero(){
        this.x = 0;
        this.y = 0;
        this.z = 0;
        return this;
    }
}
