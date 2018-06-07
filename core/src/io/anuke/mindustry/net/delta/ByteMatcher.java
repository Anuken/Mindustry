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

/**
 * Common interface for byte matchers.
 * <p>
 * Byte matchers look for common sub-strings between a source and
 * a target byte array and may optionally detect runs of duplicated
 * bytes.
 */
public interface ByteMatcher {

	public final static int COPY = 0;
	public final static int RUN = 1;
	public static final int EOF = -1;

	/**
	 * Finds the next match or run.
	 * <p>
	 * Note that only matches or byte runs will be indicated. The location
	 * of non-matching data (i.e. append sequences) must be determined from
	 * the difference between the last targetOffset, the last length, and the
	 * current targetOffset.
	 * </p>
	 *
	 * @return the new state.
	 */
	public int nextMatch();

	/**
	 * Retrieves the current target position.
	 * <p>
	 * The position within the target to which the current match refers.
	 *
	 * @return
	 */
	public int getTargetOffset();

	/**
	 * Retrieves the best match location.
	 * <p>
	 * If the current state is COPY then this returns a valid location
	 * of the best match. This should be interpreted
	 * using {@link #getBlockArray} and {@link #getBlockOffset}.
	 *
	 * @return
	 */
	public int getMatchOffset();

	/**
	 * Retrieves the byte to be run-length encoded.
	 * <p>
	 * If the current state is RUN then this returns the corresponding byte to run.
	 *
	 * @return
	 */
	public byte getRunByte();

	/**
	 * Retrieves the current length.
	 * <p>
	 * This is the number of bytes to copy for the COPY state or repeat for the RUN state.
	 *
	 * @return
	 */
	public int getLength();

	/**
	 * Retrieves the array containing the current match.
	 * <p>
	 * Maps the offset to the correct internal array.
	 *
	 * @param offset
	 * @return
	 * @see #getBlockOffset
	 */
	public byte[] getBlockArray(int offset);

	/**
	 * Calculates the offset for the block array.
	 * <p>
	 * Maps the match offset to the array from <code>getBlockArray</code>.
	 *
	 * @param offset
	 * @return
	 * @see #getBlockArray
	 */
	public int getBlockOffset(int offset);

	public byte[] getSource();

	public byte[] getTarget();

}
