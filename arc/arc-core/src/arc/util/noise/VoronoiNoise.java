package arc.util.noise;

import arc.math.Rand;

import java.util.Random;

public class VoronoiNoise{

    /// Noise module that outputs Voronoi cells.
    ///
    /// In mathematics, a <i>Voronoi cell</i> is a region containing all the
    /// points that are closer to a specific <i>seed point</i> than to any
    /// other seed point.  These cells mesh with one another, producing
    /// polygon-like formations.
    ///
    /// By default, this noise module randomly places a seed point within
    /// each unit cube.  By modifying the <i>frequency</i> of the seed points,
    /// an application can change the distance between seed points.  The
    /// higher the frequency, the closer together this noise module places
    /// the seed points, which reduces the size of the cells.  To specify the
    /// frequency of the cells, call the setFrequency() method.
    ///
    /// This noise module assigns each Voronoi cell with a random constant
    /// value from a coherent-noise function.  The <i>displacement value</i>
    /// controls the range of random values to assign to each cell.  The
    /// range of random values is +/- the displacement value.  Call the
    /// setDisplacement() method to specify the displacement value.
    ///
    /// To modify the random positions of the seed points, call the SetSeed()
    /// method.
    ///
    /// This noise module can optionally add the distance from the nearest
    /// seed to the output value.  To enable this feature, call the
    /// enableDistance() method.  This causes the points in the Voronoi cells
    /// to increase in value the further away that point is from the nearest
    /// seed point.

    //for speed, we can approximate the sqrt term in the distance funtions
    private static final double SQRT_2 = 1.4142135623730950488;
    private static final double SQRT_3 = 1.7320508075688772935;

    //You can either use the feature point height (for biomes or solid pillars), or the distance to the feature point
    private boolean useDistance = false;

    private long seed;
    private boolean useManhattan;
    private Rand rnd = new Rand();

    public VoronoiNoise(long seed, boolean useManhattan){
        this.seed = seed;
        this.useManhattan = useManhattan;
    }

    /**
     * To avoid having to store the feature points, we use a hash function
     * of the coordinates and the seed instead. Those big scary numbers are
     * arbitrary primes.
     */
    public static double valueNoise2D(int x, int z, long seed){
        long n = (1619 * x + 6971 * z + 1013 * seed) & 0x7fffffff;
        n = (n >> 13) ^ n;
        return 1.0 - ((double)((n * (n * n * 60493 + 19990303) + 1376312589) & 0x7fffffff) / 1073741824.0);
    }

    public static double valueNoise3D(int x, int y, int z, long seed){
        long n = (1619 * x + 31337 * y + 6971 * z + 1013 * seed) & 0x7fffffff;
        n = (n >> 13) ^ n;
        return 1.0 - ((double)((n * (n * n * 60493 + 19990303) + 1376312589) & 0x7fffffff) / 1073741824.0);
    }

    private double getDistance(double xDist, double zDist){
        return useManhattan ? xDist + zDist : Math.sqrt(xDist * xDist + zDist * zDist) / SQRT_2;
    }

    private double getDistance(double xDist, double yDist, double zDist){
        return useManhattan ? xDist + yDist + zDist : Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) / SQRT_3;
    }

    public boolean isUseDistance(){
        return useDistance;
    }

    public void setUseDistance(boolean useDistance){
        this.useDistance = useDistance;
    }

    public long getSeed(){
        return seed;
    }

    public void setSeed(long seed){
        this.seed = seed;
    }

    public double noise(double x, double z, double frequency){
        x *= frequency;
        z *= frequency;
        rnd.setSeed(seed);
        long result = rnd.nextLong();

        int xInt = (x > .0 ? (int)x : (int)x - 1);
        int zInt = (z > .0 ? (int)z : (int)z - 1);

        double minDist = 32000000.0;

        double xCandidate = 0;
        double zCandidate = 0;

        for(int zCur = zInt - 2; zCur <= zInt + 2; zCur++){
            for(int xCur = xInt - 2; xCur <= xInt + 2; xCur++){

                double xPos = xCur + valueNoise2D(xCur, zCur, seed);
                double zPos = zCur + valueNoise2D(xCur, zCur, result);
                double xDist = xPos - x;
                double zDist = zPos - z;
                double dist = xDist * xDist + zDist * zDist;

                if(dist < minDist){
                    minDist = dist;
                    xCandidate = xPos;
                    zCandidate = zPos;
                }
            }
        }

        if(useDistance){
            double xDist = xCandidate - x;
            double zDist = zCandidate - z;
            return getDistance(xDist, zDist);
        }else return (VoronoiNoise.valueNoise2D((int)(Math.floor(xCandidate)), (int)(Math.floor(zCandidate)), seed));
    }

    public double noise(double x, double y, double z, double frequency){
        // Inside each unit cube, there is a seed point at a random position.  Go
        // through each of the nearby cubes until we find a cube with a seed point
        // that is closest to the specified position.
        x *= frequency;
        y *= frequency;
        z *= frequency;

        int xInt = (x > .0 ? (int)x : (int)x - 1);
        int yInt = (y > .0 ? (int)y : (int)y - 1);
        int zInt = (z > .0 ? (int)z : (int)z - 1);

        double minDist = 32000000.0;

        double xCandidate = 0;
        double yCandidate = 0;
        double zCandidate = 0;

        Random rand = new Random(seed);

        for(int zCur = zInt - 2; zCur <= zInt + 2; zCur++){
            for(int yCur = yInt - 2; yCur <= yInt + 2; yCur++){
                for(int xCur = xInt - 2; xCur <= xInt + 2; xCur++){
                    // Calculate the position and distance to the seed point inside of
                    // this unit cube.

                    double xPos = xCur + valueNoise3D(xCur, yCur, zCur, seed);
                    double yPos = yCur + valueNoise3D(xCur, yCur, zCur, rand.nextLong());
                    double zPos = zCur + valueNoise3D(xCur, yCur, zCur, rand.nextLong());
                    double xDist = xPos - x;
                    double yDist = yPos - y;
                    double zDist = zPos - z;
                    double dist = xDist * xDist + yDist * yDist + zDist * zDist;

                    if(dist < minDist){
                        // This seed point is closer to any others found so far, so record
                        // this seed point.
                        minDist = dist;
                        xCandidate = xPos;
                        yCandidate = yPos;
                        zCandidate = zPos;
                    }
                }
            }
        }

        if(useDistance){
            double xDist = xCandidate - x;
            double yDist = yCandidate - y;
            double zDist = zCandidate - z;

            return getDistance(xDist, yDist, zDist);
        }else return valueNoise3D(
        (int)(Math.floor(xCandidate)),
        (int)(Math.floor(yCandidate)),
        (int)(Math.floor(zCandidate)), seed);
    }
}
