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
 * The interface for encoding a delta.
 * <p>
 * A delta encoder will implement a specific file/transfer format.
 */
public interface ByteDeltaEncoder {

	/**
	 * Initialises creating a new patch.
	 *
	 * @param sourceSize
	 * @param targetSize
	 */
	public void init(int sourceSize, int targetSize);

	/**
	 * Appends a copy command.
	 *
	 * @param addr
	 * @param len
	 */
	public void copy(int addr, int len);

	/**
	 * Appends an append command.
	 *
	 * @param data
	 * @param off
	 * @param len
	 */
	public void add(byte[] data, int off, int len);

	/**
	 * Appends a byte-run.
	 *
	 * @param b
	 * @param len
	 */
	public void run(byte b, int len);

	/**
	 * Retrieves the patch.
	 *
	 * @return
	 */
	public byte[] toPatch();

	/**
	 * Creates a delta from a matcher and writes it to an encoder.
	 *
	 * @param matcher
	 * @param enc
	 * @return
	 */
	public static byte[] toDiff(ByteMatcher matcher, ByteDeltaEncoder enc) {
		byte[] source = matcher.getSource();
		byte[] target = matcher.getTarget();

		enc.init(source.length, target.length);

		int targetEnd = 0;
		int state;

		while ((state = matcher.nextMatch()) != ByteMatcher.EOF) {
			int toff = matcher.getTargetOffset();
			int slength = matcher.getLength();

			if (targetEnd != toff)
				enc.add(target, targetEnd, toff - targetEnd);

			if (state == ByteMatcher.RUN)
				enc.run(matcher.getRunByte(), slength);
			else
				enc.copy(matcher.getMatchOffset(), slength);

			targetEnd = toff + slength;
		}
		if (targetEnd != target.length)
			enc.add(target, targetEnd, target.length - targetEnd);

		return enc.toPatch();
	}
}
