package arc.math.geom;

import arc.math.*;

/**
 * A simple quaternion class.
 * @author badlogicgames@gmail.com
 * @author vesuvio
 * @author xoppa
 * @see <a href="http://en.wikipedia.org/wiki/Quaternion">http://en.wikipedia.org/wiki/Quaternion</a>
 */
public class Quat{
    private static Quat tmp1 = new Quat(0, 0, 0, 0);
    private static Quat tmp2 = new Quat(0, 0, 0, 0);

    public float x;
    public float y;
    public float z;
    public float w;

    /**
     * Constructor, sets the four components of the quaternion.
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @param w The w-component
     */
    public Quat(float x, float y, float z, float w){
        this.set(x, y, z, w);
    }

    public Quat(){
        idt();
    }

    /**
     * Constructor, sets the quaternion components from the given quaternion.
     * @param quat The quaternion to copy.
     */
    public Quat(Quat quat){
        this.set(quat);
    }

    /**
     * Constructor, sets the quaternion from the given axis vector and the angle around that axis in degrees.
     * @param axis The axis
     * @param angle The angle in degrees.
     */
    public Quat(Vec3 axis, float angle){
        this.set(axis, angle);
    }

    /** @return the euclidean length of the specified quaternion */
    public static float len(final float x, final float y, final float z, final float w){
        return (float)Math.sqrt(x * x + y * y + z * z + w * w);
    }

    public static float len2(final float x, final float y, final float z, final float w){
        return x * x + y * y + z * z + w * w;
    }

    /**
     * Get the dot product between the two quaternions (commutative).
     * @param x1 the x component of the first quaternion
     * @param y1 the y component of the first quaternion
     * @param z1 the z component of the first quaternion
     * @param w1 the w component of the first quaternion
     * @param x2 the x component of the second quaternion
     * @param y2 the y component of the second quaternion
     * @param z2 the z component of the second quaternion
     * @param w2 the w component of the second quaternion
     * @return the dot product between the first and second quaternion.
     */
    public static float dot(final float x1, final float y1, final float z1, final float w1, final float x2, final float y2,
                            final float z2, final float w2){
        return x1 * x2 + y1 * y2 + z1 * z2 + w1 * w2;
    }

    /**
     * Sets the components of the quaternion
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @param w The w-component
     * @return This quaternion for chaining
     */
    public Quat set(float x, float y, float z, float w){
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /**
     * Sets the quaternion components from the given quaternion.
     * @param quat The quaternion.
     * @return This quaternion for chaining.
     */
    public Quat set(Quat quat){
        return this.set(quat.x, quat.y, quat.z, quat.w);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     * @param axis The axis
     * @param angle The angle in degrees
     * @return This quaternion for chaining.
     */
    public Quat set(Vec3 axis, float angle){
        return setFromAxis(axis.x, axis.y, axis.z, angle);
    }

    /** @return a copy of this quaternion */
    public Quat cpy(){
        return new Quat(this);
    }

    /** @return the euclidean length of this quaternion */
    public float len(){
        return (float)Math.sqrt(x * x + y * y + z * z + w * w);
    }

    @Override
    public String toString(){
        return "[" + x + "|" + y + "|" + z + "|" + w + "]";
    }

    /**
     * Sets the quaternion to the given euler angles in degrees.
     * @param yaw the rotation around the y axis in degrees
     * @param pitch the rotation around the x axis in degrees
     * @param roll the rotation around the z axis degrees
     * @return this quaternion
     */
    public Quat setEulerAngles(float yaw, float pitch, float roll){
        return setEulerAnglesRad(yaw * Mathf.degreesToRadians, pitch * Mathf.degreesToRadians, roll
        * Mathf.degreesToRadians);
    }

    /**
     * Sets the quaternion to the given euler angles in radians.
     * @param yaw the rotation around the y axis in radians
     * @param pitch the rotation around the x axis in radians
     * @param roll the rotation around the z axis in radians
     * @return this quaternion
     */
    public Quat setEulerAnglesRad(float yaw, float pitch, float roll){
        final float hr = roll * 0.5f;
        final float shr = (float)Math.sin(hr);
        final float chr = (float)Math.cos(hr);
        final float hp = pitch * 0.5f;
        final float shp = (float)Math.sin(hp);
        final float chp = (float)Math.cos(hp);
        final float hy = yaw * 0.5f;
        final float shy = (float)Math.sin(hy);
        final float chy = (float)Math.cos(hy);
        final float chy_shp = chy * shp;
        final float shy_chp = shy * chp;
        final float chy_chp = chy * chp;
        final float shy_shp = shy * shp;

        x = (chy_shp * chr) + (shy_chp * shr); // cos(yaw/2) * sin(pitch/2) * cos(roll/2) + sin(yaw/2) * cos(pitch/2) * sin(roll/2)
        y = (shy_chp * chr) - (chy_shp * shr); // sin(yaw/2) * cos(pitch/2) * cos(roll/2) - cos(yaw/2) * sin(pitch/2) * sin(roll/2)
        z = (chy_chp * shr) - (shy_shp * chr); // cos(yaw/2) * cos(pitch/2) * sin(roll/2) - sin(yaw/2) * sin(pitch/2) * cos(roll/2)
        w = (chy_chp * chr) + (shy_shp * shr); // cos(yaw/2) * cos(pitch/2) * cos(roll/2) + sin(yaw/2) * sin(pitch/2) * sin(roll/2)
        return this;
    }

    /**
     * Get the pole of the gimbal lock, if any.
     * @return positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock
     */
    public int getGimbalPole(){
        final float t = y * x + z * w;
        return t > 0.499f ? 1 : (t < -0.499f ? -1 : 0);
    }

    /**
     * Get the roll euler angle in radians, which is the rotation around the z axis. Requires that this quaternion is normalized.
     * @return the rotation around the z axis in radians (between -PI and +PI)
     */
    public float getRollRad(){
        final int pole = getGimbalPole();
        return pole == 0 ? Mathf.atan2(1f - 2f * (x * x + z * z), 2f * (w * z + y * x)) : (float)pole * 2f
        * Mathf.atan2(w, y);
    }

    /**
     * Get the roll euler angle in degrees, which is the rotation around the z axis. Requires that this quaternion is normalized.
     * @return the rotation around the z axis in degrees (between -180 and +180)
     */
    public float getRoll(){
        return getRollRad() * Mathf.radiansToDegrees;
    }

    /**
     * Get the pitch euler angle in radians, which is the rotation around the x axis. Requires that this quaternion is normalized.
     * @return the rotation around the x axis in radians (between -(PI/2) and +(PI/2))
     */
    public float getPitchRad(){
        final int pole = getGimbalPole();
        return pole == 0 ? (float)Math.asin(Mathf.clamp(2f * (w * x - z * y), -1f, 1f)) : (float)pole * Mathf.PI * 0.5f;
    }

    /**
     * Get the pitch euler angle in degrees, which is the rotation around the x axis. Requires that this quaternion is normalized.
     * @return the rotation around the x axis in degrees (between -90 and +90)
     */
    public float getPitch(){
        return getPitchRad() * Mathf.radiansToDegrees;
    }

    /**
     * Get the yaw euler angle in radians, which is the rotation around the y axis. Requires that this quaternion is normalized.
     * @return the rotation around the y axis in radians (between -PI and +PI)
     */
    public float getYawRad(){
        return getGimbalPole() == 0 ? Mathf.atan2(1f - 2f * (y * y + x * x), 2f * (y * w + x * z)) : 0f;
    }

    /**
     * Get the yaw euler angle in degrees, which is the rotation around the y axis. Requires that this quaternion is normalized.
     * @return the rotation around the y axis in degrees (between -180 and +180)
     */
    public float getYaw(){
        return getYawRad() * Mathf.radiansToDegrees;
    }

    /** @return the length of this quaternion without square root */
    public float len2(){
        return x * x + y * y + z * z + w * w;
    }

    /**
     * Normalizes this quaternion to unit length
     * @return the quaternion for chaining
     */
    public Quat nor(){
        float len = len2();
        if(len != 0.f && !Mathf.equal(len, 1f)){
            len = (float)Math.sqrt(len);
            w /= len;
            x /= len;
            y /= len;
            z /= len;
        }
        return this;
    }

    // TODO : this would better fit into the Vec3 class

    /**
     * Conjugate the quaternion.
     * @return This quaternion for chaining
     */
    public Quat conjugate(){
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    /**
     * Transforms the given vector using this quaternion
     * @param v Vector to transform
     */
    public Vec3 transform(Vec3 v){
        tmp2.set(this);
        tmp2.conjugate();
        tmp2.mulLeft(tmp1.set(v.x, v.y, v.z, 0)).mulLeft(this);

        v.x = tmp2.x;
        v.y = tmp2.y;
        v.z = tmp2.z;
        return v;
    }

    /**
     * Multiplies this quaternion with another one in the form of this = this * other
     * @param other Quaternion to multiply with
     * @return This quaternion for chaining
     */
    public Quat mul(final Quat other){
        final float newX = this.w * other.x + this.x * other.w + this.y * other.z - this.z * other.y;
        final float newY = this.w * other.y + this.y * other.w + this.z * other.x - this.x * other.z;
        final float newZ = this.w * other.z + this.z * other.w + this.x * other.y - this.y * other.x;
        final float newW = this.w * other.w - this.x * other.x - this.y * other.y - this.z * other.z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /**
     * Multiplies this quaternion with another one in the form of this = this * other
     * @param x the x component of the other quaternion to multiply with
     * @param y the y component of the other quaternion to multiply with
     * @param z the z component of the other quaternion to multiply with
     * @param w the w component of the other quaternion to multiply with
     * @return This quaternion for chaining
     */
    public Quat mul(final float x, final float y, final float z, final float w){
        final float newX = this.w * x + this.x * w + this.y * z - this.z * y;
        final float newY = this.w * y + this.y * w + this.z * x - this.x * z;
        final float newZ = this.w * z + this.z * w + this.x * y - this.y * x;
        final float newW = this.w * w - this.x * x - this.y * y - this.z * z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /**
     * Multiplies this quaternion with another one in the form of this = other * this
     * @param other Quaternion to multiply with
     * @return This quaternion for chaining
     */
    public Quat mulLeft(Quat other){
        final float newX = other.w * this.x + other.x * this.w + other.y * this.z - other.z * this.y;
        final float newY = other.w * this.y + other.y * this.w + other.z * this.x - other.x * this.z;
        final float newZ = other.w * this.z + other.z * this.w + other.x * this.y - other.y * this.x;
        final float newW = other.w * this.w - other.x * this.x - other.y * this.y - other.z * this.z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /**
     * Multiplies this quaternion with another one in the form of this = other * this
     * @param x the x component of the other quaternion to multiply with
     * @param y the y component of the other quaternion to multiply with
     * @param z the z component of the other quaternion to multiply with
     * @param w the w component of the other quaternion to multiply with
     * @return This quaternion for chaining
     */
    public Quat mulLeft(final float x, final float y, final float z, final float w){
        final float newX = w * this.x + x * this.w + y * this.z - z * this.y;
        final float newY = w * this.y + y * this.w + z * this.x - x * this.z;
        final float newZ = w * this.z + z * this.w + x * this.y - y * this.x;
        final float newW = w * this.w - x * this.x - y * this.y - z * this.z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /** Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion */
    public Quat add(Quat quat){
        this.x += quat.x;
        this.y += quat.y;
        this.z += quat.z;
        this.w += quat.w;
        return this;
    }

    /** Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion */
    public Quat add(float qx, float qy, float qz, float qw){
        this.x += qx;
        this.y += qy;
        this.z += qz;
        this.w += qw;
        return this;
    }

    public void toMatrix (final float[] matrix) {
        final float xx = x * x;
        final float xy = x * y;
        final float xz = x * z;
        final float xw = x * w;
        final float yy = y * y;
        final float yz = y * z;
        final float yw = y * w;
        final float zz = z * z;
        final float zw = z * w;
        // Set matrix from quaternion
        matrix[Mat3D.M00] = 1 - 2 * (yy + zz);
        matrix[Mat3D.M01] = 2 * (xy - zw);
        matrix[Mat3D.M02] = 2 * (xz + yw);
        matrix[Mat3D.M03] = 0;
        matrix[Mat3D.M10] = 2 * (xy + zw);
        matrix[Mat3D.M11] = 1 - 2 * (xx + zz);
        matrix[Mat3D.M12] = 2 * (yz - xw);
        matrix[Mat3D.M13] = 0;
        matrix[Mat3D.M20] = 2 * (xz - yw);
        matrix[Mat3D.M21] = 2 * (yz + xw);
        matrix[Mat3D.M22] = 1 - 2 * (xx + yy);
        matrix[Mat3D.M23] = 0;
        matrix[Mat3D.M30] = 0;
        matrix[Mat3D.M31] = 0;
        matrix[Mat3D.M32] = 0;
        matrix[Mat3D.M33] = 1;
    }

    /**
     * Sets the quaternion to an identity Quaternion
     * @return this quaternion for chaining
     */
    public Quat idt(){
        return this.set(0, 0, 0, 1);
    }

    /** @return If this quaternion is an identity Quaternion */
    public boolean isIdentity(){
        return Mathf.zero(x) && Mathf.zero(y) && Mathf.zero(z) && Mathf.equal(w, 1f);
    }

    // todo : the setFromAxis(v3,float) method should replace the set(v3,float) method

    /** @return If this quaternion is an identity Quaternion */
    public boolean isIdentity(final float tolerance){
        return Mathf.zero(x, tolerance) && Mathf.zero(y, tolerance) && Mathf.zero(z, tolerance)
        && Mathf.equal(w, 1f, tolerance);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     * @param axis The axis
     * @param degrees The angle in degrees
     * @return This quaternion for chaining.
     */
    public Quat setFromAxis(final Vec3 axis, final float degrees){
        return setFromAxis(axis.x, axis.y, axis.z, degrees);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     * @param axis The axis
     * @param radians The angle in radians
     * @return This quaternion for chaining.
     */
    public Quat setFromAxisRad(final Vec3 axis, final float radians){
        return setFromAxisRad(axis.x, axis.y, axis.z, radians);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     * @param x X direction of the axis
     * @param y Y direction of the axis
     * @param z Z direction of the axis
     * @param degrees The angle in degrees
     * @return This quaternion for chaining.
     */
    public Quat setFromAxis(final float x, final float y, final float z, final float degrees){
        return setFromAxisRad(x, y, z, degrees * Mathf.degreesToRadians);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     * @param x X direction of the axis
     * @param y Y direction of the axis
     * @param z Z direction of the axis
     * @param radians The angle in radians
     * @return This quaternion for chaining.
     */
    public Quat setFromAxisRad(final float x, final float y, final float z, final float radians){
        float d = Vec3.len(x, y, z);
        if(d == 0f) return idt();
        d = 1f / d;
        float l_ang = radians < 0 ? Mathf.PI2 - (-radians % Mathf.PI2) : radians % Mathf.PI2;
        float l_sin = (float)Math.sin(l_ang / 2);
        float l_cos = (float)Math.cos(l_ang / 2);
        return this.set(d * x * l_sin, d * y * l_sin, d * z * l_sin, l_cos).nor();
    }

    /** Sets the Quaternion from the given matrix, optionally removing any scaling. */
    public Quat setFromMatrix (boolean normalizeAxes, Mat3D matrix) {
        return setFromAxes(normalizeAxes, matrix.val[Mat3D.M00], matrix.val[Mat3D.M01], matrix.val[Mat3D.M02],
        matrix.val[Mat3D.M10], matrix.val[Mat3D.M11], matrix.val[Mat3D.M12], matrix.val[Mat3D.M20],
        matrix.val[Mat3D.M21], matrix.val[Mat3D.M22]);
    }

    /** Sets the Quaternion from the given rotation matrix, which must not contain scaling. */
    public Quat setFromMatrix (Mat3D matrix) {
        return setFromMatrix(false, matrix);
    }

    /** Sets the Quaternion from the given matrix, optionally removing any scaling. */
    public Quat setFromMatrix (boolean normalizeAxes, Mat matrix) {
        return setFromAxes(normalizeAxes, matrix.val[Mat.M00], matrix.val[Mat.M01], matrix.val[Mat.M02],
        matrix.val[Mat.M10], matrix.val[Mat.M11], matrix.val[Mat.M12], matrix.val[Mat.M20],
        matrix.val[Mat.M21], matrix.val[Mat.M22]);
    }

    /** Sets the Quaternion from the given rotation matrix, which must not contain scaling. */
    public Quat setFromMatrix (Mat matrix) {
        return setFromMatrix(false, matrix);
    }

    /**
     * <p>
     * Sets the Quaternion from the given x-, y- and z-axis which have to be orthonormal.
     * </p>
     *
     * <p>
     * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
     * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
     * </p>
     * @param xx x-axis x-coordinate
     * @param xy x-axis y-coordinate
     * @param xz x-axis z-coordinate
     * @param yx y-axis x-coordinate
     * @param yy y-axis y-coordinate
     * @param yz y-axis z-coordinate
     * @param zx z-axis x-coordinate
     * @param zy z-axis y-coordinate
     * @param zz z-axis z-coordinate
     */
    public Quat setFromAxes(float xx, float xy, float xz, float yx, float yy, float yz, float zx, float zy, float zz){
        return setFromAxes(false, xx, xy, xz, yx, yy, yz, zx, zy, zz);
    }

    /**
     * <p>
     * Sets the Quaternion from the given x-, y- and z-axis.
     * </p>
     *
     * <p>
     * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
     * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
     * </p>
     * @param normalizeAxes whether to normalize the axes (necessary when they contain scaling)
     * @param xx x-axis x-coordinate
     * @param xy x-axis y-coordinate
     * @param xz x-axis z-coordinate
     * @param yx y-axis x-coordinate
     * @param yy y-axis y-coordinate
     * @param yz y-axis z-coordinate
     * @param zx z-axis x-coordinate
     * @param zy z-axis y-coordinate
     * @param zz z-axis z-coordinate
     */
    public Quat setFromAxes(boolean normalizeAxes, float xx, float xy, float xz, float yx, float yy, float yz, float zx,
                            float zy, float zz){
        if(normalizeAxes){
            final float lx = 1f / Vec3.len(xx, xy, xz);
            final float ly = 1f / Vec3.len(yx, yy, yz);
            final float lz = 1f / Vec3.len(zx, zy, zz);
            xx *= lx;
            xy *= lx;
            xz *= lx;
            yx *= ly;
            yy *= ly;
            yz *= ly;
            zx *= lz;
            zy *= lz;
            zz *= lz;
        }
        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        final float t = xx + yy + zz;

        // we protect the division by s by ensuring that s>=1
        if(t >= 0){ // |w| >= .5
            float s = (float)Math.sqrt(t + 1); // |s|>=1 ...
            w = 0.5f * s;
            s = 0.5f / s; // so this division isn't bad
            x = (zy - yz) * s;
            y = (xz - zx) * s;
            z = (yx - xy) * s;
        }else if((xx > yy) && (xx > zz)){
            float s = (float)Math.sqrt(1.0 + xx - yy - zz); // |s|>=1
            x = s * 0.5f; // |x| >= .5
            s = 0.5f / s;
            y = (yx + xy) * s;
            z = (xz + zx) * s;
            w = (zy - yz) * s;
        }else if(yy > zz){
            float s = (float)Math.sqrt(1.0 + yy - xx - zz); // |s|>=1
            y = s * 0.5f; // |y| >= .5
            s = 0.5f / s;
            x = (yx + xy) * s;
            z = (zy + yz) * s;
            w = (xz - zx) * s;
        }else{
            float s = (float)Math.sqrt(1.0 + zz - xx - yy); // |s|>=1
            z = s * 0.5f; // |z| >= .5
            s = 0.5f / s;
            x = (xz + zx) * s;
            y = (zy + yz) * s;
            w = (yx - xy) * s;
        }

        return this;
    }

    /**
     * Set this quaternion to the rotation between two vectors.
     * @param v1 The base vector, which should be normalized.
     * @param v2 The target vector, which should be normalized.
     * @return This quaternion for chaining
     */
    public Quat setFromCross(final Vec3 v1, final Vec3 v2){
        final float dot = Mathf.clamp(v1.dot(v2), -1f, 1f);
        final float angle = (float)Math.acos(dot);
        return setFromAxisRad(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x, angle);
    }

    /**
     * Set this quaternion to the rotation between two vectors.
     * @param x1 The base vectors x value, which should be normalized.
     * @param y1 The base vectors y value, which should be normalized.
     * @param z1 The base vectors z value, which should be normalized.
     * @param x2 The target vector x value, which should be normalized.
     * @param y2 The target vector y value, which should be normalized.
     * @param z2 The target vector z value, which should be normalized.
     * @return This quaternion for chaining
     */
    public Quat setFromCross(final float x1, final float y1, final float z1, final float x2, final float y2, final float z2){
        final float dot = Mathf.clamp(Vec3.dot(x1, y1, z1, x2, y2, z2), -1f, 1f);
        final float angle = (float)Math.acos(dot);
        return setFromAxisRad(y1 * z2 - z1 * y2, z1 * x2 - x1 * z2, x1 * y2 - y1 * x2, angle);
    }

    /**
     * Spherical Linear interpolation between this quaternion and the other quaternion, based on the alpha value in the range
     * [0,1]. Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/
     * @param end the end quaternion
     * @param alpha alpha in the range [0,1]
     * @return this quaternion for chaining
     */
    public Quat slerp(Quat end, float alpha){
        final float d = this.x * end.x + this.y * end.y + this.z * end.z + this.w * end.w;
        float absDot = d < 0.f ? -d : d;

        // Set the first and second scale for the interpolation
        float scale0 = 1f - alpha;
        float scale1 = alpha;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if((1 - absDot) > 0.1){// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            final float angle = (float)Math.acos(absDot);
            final float invSinTheta = 1f / (float)Math.sin(angle);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = ((float)Math.sin((1f - alpha) * angle) * invSinTheta);
            scale1 = ((float)Math.sin((alpha * angle)) * invSinTheta);
        }

        if(d < 0.f) scale1 = -scale1;

        // Calculate the x, y, z and w values for the quaternion by using a
        // special form of Linear interpolation for quaternions.
        x = (scale0 * x) + (scale1 * end.x);
        y = (scale0 * y) + (scale1 * end.y);
        z = (scale0 * z) + (scale1 * end.z);
        w = (scale0 * w) + (scale1 * end.w);

        // Return the interpolated quaternion
        return this;
    }

    /**
     * Spherical linearly interpolates multiple quaternions and stores the result in this Quaternion. Will not destroy the data
     * previously inside the elements of q. result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n) where w_i=1/n.
     * @param q List of quaternions
     * @return This quaternion for chaining
     */
    public Quat slerp(Quat[] q){

        // Calculate exponents and multiply everything from left to right
        final float w = 1.0f / q.length;
        set(q[0]).exp(w);
        for(int i = 1; i < q.length; i++)
            mul(tmp1.set(q[i]).exp(w));
        nor();
        return this;
    }

    /**
     * Spherical linearly interpolates multiple quaternions by the given weights and stores the result in this Quaternion. Will not
     * destroy the data previously inside the elements of q or w. result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n) where the sum of w_i
     * is 1. Lists must be equal in length.
     * @param q List of quaternions
     * @param w List of weights
     * @return This quaternion for chaining
     */
    public Quat slerp(Quat[] q, float[] w){

        // Calculate exponents and multiply everything from left to right
        set(q[0]).exp(w[0]);
        for(int i = 1; i < q.length; i++)
            mul(tmp1.set(q[i]).exp(w[i]));
        nor();
        return this;
    }

    /**
     * Calculates (this quaternion)^alpha where alpha is a real number and stores the result in this quaternion. See
     * http://en.wikipedia.org/wiki/Quaternion#Exponential.2C_logarithm.2C_and_power
     * @param alpha Exponent
     * @return This quaternion for chaining
     */
    public Quat exp(float alpha){

        // Calculate |q|^alpha
        float norm = len();
        float normExp = (float)Math.pow(norm, alpha);

        // Calculate theta
        float theta = (float)Math.acos(w / norm);

        // Calculate coefficient of basis elements
        float coeff = 0;
        if(Math.abs(theta) < 0.001) // If theta is small enough, use the limit of sin(alpha*theta) / sin(theta) instead of actual
// value
            coeff = normExp * alpha / norm;
        else
            coeff = (float)(normExp * Math.sin(alpha * theta) / (norm * Math.sin(theta)));

        // Write results
        w = (float)(normExp * Math.cos(alpha * theta));
        x *= coeff;
        y *= coeff;
        z *= coeff;

        // Fix any possible discrepancies
        nor();

        return this;
    }

    @Override
    public int hashCode(){
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToRawIntBits(w);
        result = prime * result + Float.floatToRawIntBits(x);
        result = prime * result + Float.floatToRawIntBits(y);
        result = prime * result + Float.floatToRawIntBits(z);
        return result;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj){
            return true;
        }
        if(obj == null){
            return false;
        }
        if(!(obj instanceof Quat)){
            return false;
        }
        Quat other = (Quat)obj;
        return (Float.floatToRawIntBits(w) == Float.floatToRawIntBits(other.w))
        && (Float.floatToRawIntBits(x) == Float.floatToRawIntBits(other.x))
        && (Float.floatToRawIntBits(y) == Float.floatToRawIntBits(other.y))
        && (Float.floatToRawIntBits(z) == Float.floatToRawIntBits(other.z));
    }

    /**
     * Get the dot product between this and the other quaternion (commutative).
     * @param other the other quaternion.
     * @return the dot product of this and the other quaternion.
     */
    public float dot(final Quat other){
        return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
    }

    /**
     * Get the dot product between this and the other quaternion (commutative).
     * @param x the x component of the other quaternion
     * @param y the y component of the other quaternion
     * @param z the z component of the other quaternion
     * @param w the w component of the other quaternion
     * @return the dot product of this and the other quaternion.
     */
    public float dot(final float x, final float y, final float z, final float w){
        return this.x * x + this.y * y + this.z * z + this.w * w;
    }

    /**
     * Multiplies the components of this quaternion with the given scalar.
     * @param scalar the scalar.
     * @return this quaternion for chaining.
     */
    public Quat mul(float scalar){
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        this.w *= scalar;
        return this;
    }

    /**
     * Get the axis angle representation of the rotation in degrees. The supplied vector will receive the axis (x, y and z values)
     * of the rotation and the value returned is the angle in degrees around that axis. Note that this method will alter the
     * supplied vector, the existing value of the vector is ignored. </p> This will normalize this quaternion if needed. The
     * received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the axis may be
     * zero.
     * @param axis vector which will receive the axis
     * @return the angle in degrees
     * @see <a href="http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation">wikipedia</a>
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle">calculation</a>
     */
    public float getAxisAngle(Vec3 axis){
        return getAxisAngleRad(axis) * Mathf.radiansToDegrees;
    }

    /**
     * Get the axis-angle representation of the rotation in radians. The supplied vector will receive the axis (x, y and z values)
     * of the rotation and the value returned is the angle in radians around that axis. Note that this method will alter the
     * supplied vector, the existing value of the vector is ignored. </p> This will normalize this quaternion if needed. The
     * received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the axis may be
     * zero.
     * @param axis vector which will receive the axis
     * @return the angle in radians
     * @see <a href="http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation">wikipedia</a>
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle">calculation</a>
     */
    public float getAxisAngleRad(Vec3 axis){
        if(this.w > 1)
            this.nor(); // if w>1 acos and sqrt will produce errors, this cant happen if quaternion is normalised
        float angle = (float)(2.0 * Math.acos(this.w));
        double s = Math.sqrt(1 - this.w * this.w); // assuming quaternion normalised then w is less than 1, so term always positive.
        if(s < Mathf.FLOAT_ROUNDING_ERROR){ // test to avoid divide by zero, s is always positive due to sqrt
            // if s close to zero then direction of axis not important
            axis.x = this.x; // if it is important that axis is normalised then replace with x=1; y=z=0;
            axis.y = this.y;
            axis.z = this.z;
        }else{
            axis.x = (float)(this.x / s); // normalise axis
            axis.y = (float)(this.y / s);
            axis.z = (float)(this.z / s);
        }

        return angle;
    }

    /**
     * Get the angle in radians of the rotation this quaternion represents. Does not normalize the quaternion. Use
     * {@link #getAxisAngleRad(Vec3)} to get both the axis and the angle of this rotation. Use
     * {@link #getAngleAroundRad(Vec3)} to get the angle around a specific axis.
     * @return the angle in radians of the rotation
     */
    public float getAngleRad(){
        return (float)(2.0 * Math.acos((this.w > 1) ? (this.w / len()) : this.w));
    }

    /**
     * Get the angle in degrees of the rotation this quaternion represents. Use {@link #getAxisAngle(Vec3)} to get both the axis
     * and the angle of this rotation. Use {@link #getAngleAround(Vec3)} to get the angle around a specific axis.
     * @return the angle in degrees of the rotation
     */
    public float getAngle(){
        return getAngleRad() * Mathf.radiansToDegrees;
    }

    /**
     * Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
     * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
     * axis perpendicular to the specified axis. </p> The swing and twist rotation can be used to reconstruct the original
     * quaternion: this = swing * twist
     * @param axisX the X component of the normalized axis for which to get the swing and twist rotation
     * @param axisY the Y component of the normalized axis for which to get the swing and twist rotation
     * @param axisZ the Z component of the normalized axis for which to get the swing and twist rotation
     * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis
     * @param twist will receive the twist rotation: the rotation around the specified axis
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a>
     */
    public void getSwingTwist(final float axisX, final float axisY, final float axisZ, final Quat swing,
                              final Quat twist){
        final float d = Vec3.dot(this.x, this.y, this.z, axisX, axisY, axisZ);
        twist.set(axisX * d, axisY * d, axisZ * d, this.w).nor();
        if(d < 0) twist.mul(-1f);
        swing.set(twist).conjugate().mulLeft(this);
    }

    /**
     * Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
     * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
     * axis perpendicular to the specified axis. </p> The swing and twist rotation can be used to reconstruct the original
     * quaternion: this = swing * twist
     * @param axis the normalized axis for which to get the swing and twist rotation
     * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis
     * @param twist will receive the twist rotation: the rotation around the specified axis
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a>
     */
    public void getSwingTwist(final Vec3 axis, final Quat swing, final Quat twist){
        getSwingTwist(axis.x, axis.y, axis.z, swing, twist);
    }

    /**
     * Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
     * @param axisX the x component of the normalized axis for which to get the angle
     * @param axisY the y component of the normalized axis for which to get the angle
     * @param axisZ the z component of the normalized axis for which to get the angle
     * @return the angle in radians of the rotation around the specified axis
     */
    public float getAngleAroundRad(final float axisX, final float axisY, final float axisZ){
        final float d = Vec3.dot(this.x, this.y, this.z, axisX, axisY, axisZ);
        final float l2 = Quat.len2(axisX * d, axisY * d, axisZ * d, this.w);
        return Mathf.zero(l2) ? 0f : (float)(2.0 * Math.acos(Mathf.clamp(
        (float)((d < 0 ? -this.w : this.w) / Math.sqrt(l2)), -1f, 1f)));
    }

    /**
     * Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
     * @param axis the normalized axis for which to get the angle
     * @return the angle in radians of the rotation around the specified axis
     */
    public float getAngleAroundRad(final Vec3 axis){
        return getAngleAroundRad(axis.x, axis.y, axis.z);
    }

    /**
     * Get the angle in degrees of the rotation around the specified axis. The axis must be normalized.
     * @param axisX the x component of the normalized axis for which to get the angle
     * @param axisY the y component of the normalized axis for which to get the angle
     * @param axisZ the z component of the normalized axis for which to get the angle
     * @return the angle in degrees of the rotation around the specified axis
     */
    public float getAngleAround(final float axisX, final float axisY, final float axisZ){
        return getAngleAroundRad(axisX, axisY, axisZ) * Mathf.radiansToDegrees;
    }

    /**
     * Get the angle in degrees of the rotation around the specified axis. The axis must be normalized.
     * @param axis the normalized axis for which to get the angle
     * @return the angle in degrees of the rotation around the specified axis
     */
    public float getAngleAround(final Vec3 axis){
        return getAngleAround(axis.x, axis.y, axis.z);
    }
}
