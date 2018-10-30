package io.anuke.ucore.noise;

/*
 * Copyright (C) 2003, 2004 Jason Bevins (original libnoise code)
 * Copyright © 2010 Thomas J. Hodge (java port of libnoise)
 * 
 * This file was part of libnoiseforjava.
 * 
 * libnoiseforjava is a Java port of the C++ library libnoise, which may be found at 
 * http://libnoise.sourceforge.net/.  libnoise was developed by Jason Bevins, who may be 
 * contacted at jlbezigvins@gmzigail.com (for great email, take off every 'zig').
 * Porting to Java was done by Thomas Hodge, who may be contacted at
 * libnoisezagforjava@gzagmail.com (remove every 'zag').
 * 
 * libnoiseforjava is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * libnoiseforjava is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * libnoiseforjava.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

import java.util.Random;

/**
 * This is a Voronoi noise generator, originally from https://github.com/TJHJava/libnoiseforjava
 * It was modified to work in a similar way to the bukkit noise generators, and to support
 * octaves and 2d noise, by mncat77 and jtjj222. 
 * 
 * (taken from bukkit source)
 */
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
	private short distanceMethod;


	public VoronoiNoise(long seed, short distanceMethod) {
		this.seed = seed;
		this.distanceMethod = distanceMethod;
	}

	private double getDistance(double xDist, double zDist) {
		switch(distanceMethod) {
		case 0:
			return Math.sqrt(xDist * xDist + zDist * zDist) / SQRT_2;
		case 1:
			return xDist + zDist;
		default:
			return Double.NaN;
		}
	}

	private double getDistance(double xDist, double yDist, double zDist) {
		switch(distanceMethod) {
		case 0:
			return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) / SQRT_3; //Approximation (for speed) of elucidean (regular) distance
		case 1:
			return xDist + yDist + zDist;
		default:
			return Double.NaN;
		}
	}
	
	public boolean isUseDistance() {
		return useDistance;
	}

	public void setUseDistance(boolean useDistance) {
		this.useDistance = useDistance;
	}

	public short getDistanceMethod() {
		return distanceMethod;
	}

	public long getSeed() {
		return seed;
	}
	
	public void setDistanceMethod(short distanceMethod) {
		this.distanceMethod = distanceMethod;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}
	
	public double noise(double x, double z, double frequency) {
		x *= frequency;
		z *= frequency;

		int xInt = (x > .0? (int)x: (int)x - 1);
		int zInt = (z > .0? (int)z: (int)z - 1);

		double minDist = 32000000.0;

		double xCandidate = 0;
		double zCandidate = 0;

		for(int zCur = zInt - 2; zCur <= zInt + 2; zCur++) {
			for(int xCur = xInt - 2; xCur <= xInt + 2; xCur++) {

				double xPos = xCur + valueNoise2D(xCur, zCur, seed);
				double zPos = zCur + valueNoise2D(xCur, zCur, new Random(seed).nextLong());
				double xDist = xPos - x;
				double zDist = zPos - z;
				double dist = xDist * xDist + zDist * zDist;

				if(dist < minDist) {
					minDist = dist;
					xCandidate = xPos;
					zCandidate = zPos;
				}
			}
		}
		
		if (useDistance) {
			double xDist = xCandidate - x;
			double zDist = zCandidate - z;
			return getDistance(xDist, zDist);
		}
		
		else return ((double)VoronoiNoise.valueNoise2D (
		       (int)(Math.floor (xCandidate)),
		       (int)(Math.floor (zCandidate)), seed));
	}

	public double noise(double x, double y, double z, double frequency) {
		// Inside each unit cube, there is a seed point at a random position.  Go
		// through each of the nearby cubes until we find a cube with a seed point
		// that is closest to the specified position.
		x *= frequency;
		y *= frequency;
		z *= frequency;

		int xInt = (x > .0? (int)x: (int)x - 1);
		int yInt = (y > .0? (int)y: (int)y - 1);
		int zInt = (z > .0? (int)z: (int)z - 1);

		double minDist = 32000000.0;

		double xCandidate = 0;
		double yCandidate = 0;
		double zCandidate = 0;

		Random rand = new Random(seed);

		for(int zCur = zInt - 2; zCur <= zInt + 2; zCur++) {
			for(int yCur = yInt - 2; yCur <= yInt + 2; yCur++) {
				for(int xCur = xInt - 2; xCur <= xInt + 2; xCur++) {
					// Calculate the position and distance to the seed point inside of
					// this unit cube.

					double xPos = xCur + valueNoise3D (xCur, yCur, zCur, seed);
					double yPos = yCur + valueNoise3D (xCur, yCur, zCur, rand.nextLong());
					double zPos = zCur + valueNoise3D (xCur, yCur, zCur, rand.nextLong());
					double xDist = xPos - x;
					double yDist = yPos - y;
					double zDist = zPos - z;
					double dist = xDist * xDist + yDist * yDist + zDist * zDist;

					if(dist < minDist) {
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

		if (useDistance) {
			double xDist = xCandidate - x;
			double yDist = yCandidate - y;
			double zDist = zCandidate - z;

			return getDistance(xDist, yDist, zDist);
		}

		else return ((double)VoronoiNoise.valueNoise3D (
		       (int)(Math.floor (xCandidate)),
		       (int)(Math.floor (yCandidate)),
		       (int)(Math.floor (zCandidate)), seed));
		
	}

	/**
	 * To avoid having to store the feature points, we use a hash function 
	 * of the coordinates and the seed instead. Those big scary numbers are
	 * arbitrary primes.
	 */
	public static double valueNoise2D (int x, int z, long seed) {
		long n = (1619 * x + 6971 * z + 1013 * seed) & 0x7fffffff;
		n = (n >> 13) ^ n;
		return 1.0 - ((double)((n * (n * n * 60493 + 19990303) + 1376312589) & 0x7fffffff) / 1073741824.0);
	}

	public static double valueNoise3D (int x, int y, int z, long seed) {
		long n = (1619 * x + 31337 * y + 6971 * z + 1013 * seed) & 0x7fffffff;
		n = (n >> 13) ^ n;
		return 1.0 - ((double)((n * (n * n * 60493 + 19990303) + 1376312589) & 0x7fffffff) / 1073741824.0);
	}
}
