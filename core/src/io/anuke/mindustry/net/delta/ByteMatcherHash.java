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

import java.util.Arrays;

/**
 * Finds common strings of bytes between a source and target buffer.
 * <p>
 * This is basically an implementation of Bentley &amp; McIllroy's paper
 * ``Data Compression Using Long Common Strings'' applied instead to producing
 * deltas and using a cyclic hash as the fingerprint function.
 * <p>
 * Two other
 * modifications are that back-tracking is not implemented but instead
 * overlapping blocks can be used by setting the step size.
 * And a further refinement is the detection of runs of the same byte which
 * might otherwise pollute the hash tree for certain data.
 */
public class ByteMatcherHash implements ByteMatcher {

	private final int b;
	private final int shortest;
	private final byte[] source;
	private final int sstep;
	private final byte[] target;
	// Incremental hashes
	private final CyclicHash targetHash;
	private final CyclicHash sourceHash;
	// Runtime state
	private int ti;
	private int thash;
	private int skipTo;
	private int targetAvailable;
	// Public state
	private int bestLength;
	private int bestOffset;
	private int targetOffset;
	private byte runByte;

	/**
	 * Inline hash+array table.
	 * <p>
	 * All values which hash the same are appended to the same list.
	 * <p>
	 * Index is the current length/next insertion point for that hash chain.
	 * Values contains the chained hash table values.
	 */
	final private int hashMask;
	final private int[][] hashValues;

	/**
	 * Creates and initialises a new byte matcher.
	 * <p>
	 * This is a single-use object.
	 * <p>
	 * A step size of 1 produces the best output but requires the most memory and run time.
	 * <p>
	 * @param b Sets block size, which is the number of bytes hashed per key (&amp;=3).
	 * @param shortest shortest string considered for a copy. Typically 4 bytes but dependent on the encoder used and
	 * the value of b.
	 * @param source Source array.
	 * @param sstep Sets the step size which is the interval of sampling of the source.
	 * @param target Target array.
	 */
	public ByteMatcherHash(int b, int shortest, byte[] source, int sstep, byte[] target) {
		int size;

		b = Math.max(b, 3);

		// This may need tuning.
		int logN = 31 - Integer.numberOfLeadingZeros((source.length + target.length) / sstep);
		size = 1 << Math.max(14, logN - 5);

		hashMask = size - 1;
		hashValues = new int[size][];

		targetHash = new CyclicHash(b);
		sourceHash = new CyclicHash(b);

		this.b = b;
		this.shortest = shortest;
		this.source = source;
		this.sstep = sstep;
		this.target = target;

		addAll(source, source.length, 0, 0);
		if (target.length >= b)
			this.thash = targetHash.init(target, 0);
	}

	/**
	 * Checks for run of 3 bytes.
	 * <p>
	 * Boundaries are not checked.
	 *
	 * @param s
	 * @param pos
	 * @return
	 */
	private boolean isRun(byte[] s, int pos) {
		byte v = s[pos];
		return v == s[pos + 1] && v == s[pos + 2];
	}

	private int addAll(byte[] s, int limit, int pos, int off) {
		if (sstep == 1) {
			if (pos == 0 && limit >= b) {
				add(sourceHash.init(s, 0), off);
				pos = 1;
			}

			while (pos <= limit - b) {
				int hash = sourceHash.update(s[pos - 1], s[pos - 1 + b]);

				if (!isRun(s, pos))
					add(hash, pos + off);
				pos += 1;
			}
		} else {
			while (pos <= limit - b) {
				if (!isRun(s, pos))
					add(sourceHash.init(s, pos), pos + off);
				pos += sstep;
			}
		}
		return pos;
	}

	private void add(int hash, int value) {
		int j = hash & hashMask;
		int[] vs = hashValues[j];

		if (vs == null) {
			hashValues[j] = vs = new int[4];
			vs[0] = 2;
			vs[1] = value;
		} else {
			int i = vs[0];

			if (i >= vs.length)
				hashValues[j] = vs = Arrays.copyOf(vs, vs.length * 2);
			vs[i++] = value;
			vs[0] = i;
		}
	}

	/**
	 * Finds the length of similarity between the two sub-arrays.
	 * <p>
	 *
	 * @param soff source offset starting location, locations above source.length refer to the target buffer.
	 * @param toff target offset starting location
	 * @return how many bytes are sequentially identical.
	 */
	private int matchLength(int soff, int toff) {
		if (soff < source.length) {
			int limit = Math.min(source.length - soff, target.length - toff);

			for (int i = 0; i < limit; i++)
				if (source[soff + i] != target[toff + i])
					return i;
			return limit;
		} else {
			soff -= source.length;
			int limit = Math.min(target.length - soff, target.length - toff);

			for (int i = 0; i < limit; i++)
				if (target[soff + i] != target[toff + i])
					return i;
			return limit;
		}
	}

	@Override
	public byte[] getSource() {
		return source;
	}

	@Override
	public byte[] getTarget() {
		return target;
	}

	@Override
	public int getMatchOffset() {
		return bestOffset;
	}

	@Override
	public int getTargetOffset() {
		return targetOffset;
	}

	@Override
	public int getLength() {
		return bestLength;
	}

	@Override
	public byte getRunByte() {
		return runByte;
	}

	@Override
	public int nextMatch() {
		bestLength = 0;
		bestOffset = 0;

		/**
		 * Reset thash on seek.
		 */
		if (skipTo != ti) {
			if (skipTo <= target.length - b)
				thash = targetHash.init(target, skipTo);
			ti = skipTo;
		}

		while (bestLength < shortest && ti <= target.length - b) {
			/**
			 * short circuit test for byte-runs.
			 */
			if (isRun(target, ti)) {
				byte b0 = target[ti];
				int j = ti + 3;
				while (j < target.length && target[j] == b0)
					j++;
				targetOffset = ti;
				bestLength = j - ti;
				runByte = b0;
				skipTo = j;
				return RUN;
			}

			/**
			 * Include any of the target buffer which has been decoded to this point.
			 */
			targetAvailable = addAll(target, ti + b - 1, targetAvailable, source.length);

			/**
			 * Checks the current string for the longest match.
			 */
			int j = thash & hashMask;
			int[] soffs = hashValues[j];

			if (soffs != null) {
				int len = soffs[0];

				for (int i = 1; i < len; i++) {
					int soff = soffs[i];
					int length = matchLength(soff, ti);

					if (length > bestLength) {
						bestLength = length;
						bestOffset = soff;
					}
				}
			}

			/**
			 * Advance. thash is always the next block to examine.
			 */
			targetOffset = ti;
			ti += 1;
			if (ti <= target.length - b)
				thash = targetHash.update(target[ti - 1], target[ti - 1 + b]);
		}

		if (bestLength >= shortest) {
			skipTo = targetOffset + bestLength;
			return COPY;
		} else
			return EOF;
	}

	@Override
	public byte[] getBlockArray(int offset) {
		return (offset < source.length) ? source : target;
	}

	@Override
	public int getBlockOffset(int offset) {
		return (offset < source.length) ? offset : offset - source.length;
	}

}
