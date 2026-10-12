package arc.math.geom;

/**
 * A Segment is a line in 3-space having a staring and an ending position.
 * @author mzechner
 */
public class Segment{
    /** the starting position **/
    public final Vec3 a = new Vec3();
    /** the ending position **/
    public final Vec3 b = new Vec3();

    /**
     * Constructs a new Segment from the two points given.
     * @param a the first point
     * @param b the second point
     */
    public Segment(Vec3 a, Vec3 b){
        this.a.set(a);
        this.b.set(b);
    }

    /**
     * Constructs a new Segment from the two points given.
     * @param aX the x-coordinate of the first point
     * @param aY the y-coordinate of the first point
     * @param aZ the z-coordinate of the first point
     * @param bX the x-coordinate of the second point
     * @param bY the y-coordinate of the second point
     * @param bZ the z-coordinate of the second point
     */
    public Segment(float aX, float aY, float aZ, float bX, float bY, float bZ){
        this.a.set(aX, aY, aZ);
        this.b.set(bX, bY, bZ);
    }

    public float len(){
        return a.dst(b);
    }

    public float len2(){
        return a.dst2(b);
    }

    @Override
    public boolean equals(Object o){
        if(o == this) return true;
        if(o == null || o.getClass() != this.getClass()) return false;
        Segment s = (Segment)o;
        return this.a.equals(s.a) && this.b.equals(s.b);
    }

    @Override
    public int hashCode(){
        final int prime = 71;
        int result = 1;
        result = prime * result + this.a.hashCode();
        result = prime * result + this.b.hashCode();
        return result;
    }
}
