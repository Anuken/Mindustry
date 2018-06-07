/*
 * Copyright (C) 2015 Michael Zucchi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.anuke.mindustry.net.delta;

import java.util.Random;

import static java.lang.Integer.rotateLeft;

/**
 * Cyclic polynomial rolling hash.
 * <p>
 * This implements a rolling hash of a fixed length.
 * <p>
 * Input bytes are hashed using a random table. The randomness
 * affects the quality of the hash.
 */
public class CyclicHash {

	private static final int[] random;

	private final int b;
	private int hash;
	private final int first;

	static {
		// keyboard bashed the results unvalidated.
		Random r = new Random(97435);
		random = new int[256];
		for(int i = 0; i < random.length; i ++){
			random[i] = r.nextInt();
		}
	}

	/**
	 * Creates a cyclic hash.
	 *
	 * @param b
	 */
	public CyclicHash(int b) {
		this.b = b;
		this.first = ((b - 1) * 9) & 31;
	}

	/**
	 * Initialises the hash.
	 * <p>
	 * This will hash a block of data at the given location.
	 *
	 * @param data
	 * @param off
	 * @return
	 */
	public int init(byte[] data, int off) {
		hash = 0;
		for (int i = 0; i < b; i++)
			hash = rotateLeft(hash, 9) ^ random[data[i + off] & 0xff];
		return hash;
	}

	/**
	 * Updates the hash incrementally.
	 * <p>
	 * Advance the hash by one location.
	 *
	 * @param leave the byte leaving. Must match the oldest byte included in the hash value.
	 * @param enter the byte entering.
	 * @return
	 */
	public int update(byte leave, byte enter) {
		int leaving = rotateLeft(random[leave & 0xff], first);
		int entering = random[enter & 0xff];

		hash = rotateLeft(hash ^ leaving, 9) ^ entering;
		return hash;
	}
}
