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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 'DeltaZ-1' format encoder.
 * <p>
 * Encoder for very simple binary delta format.
 * <p>
 * <h3>Header</h3>
 * <pre>
 *  magic: 'D' 'E' 'Z' '1'
 *  flags: one byte
 *  source size: one integer
 *  target size: one integer
 *  instructions follow directly
 *  ?? no epilogue defined ??
 * </pre>
 * <p>
 * Integers are encoded as a compacted big-endian sequence
 * with 7 bits per byte. Leading zero septets are discarded.
 * The MSB of each byte is a continue bit which indicates
 * another 7 bits are to be read.
 * <p>
 * <h3>Instruction stream</h3>
 * <pre>
 * x0000000                     - reserved
 * 00XXXXXX CAAAAAAA*           - copy + 6 bit length + address
 * 10XXXXXX CXXXXXXX* CAAAAAAA* - copy + extended length + address
 * 010XXXXX DDDDDDDD*           - add + 5 bit length - 1 + sequence
 * 110XXXXX CXXXXXXX* DDDDDDDD* - add + extended length - 1 + sequence
 * 011XXXXX RRRRRRRR            - run + 5 bit count - 1 + byte
 * 111XXXXX CXXXXXXX* RRRRRRRR  - run + extended count - 1 + byte
 * </pre>
 * <p>
 * Opcodes include a length encoded as an integer.
 * <dl>
 * <dt>COPY
 * <dd>The opcode/length followed by an absolute address of the source of
 * the copy. COPY is decodeable directly as an integer. A length of 0 is reserved.
 * <p>
 * The address covers the range of the source buffer concatenated with as much
 * of the target buffer as has currently been decoded. The address+length will
 * not span buffers.
 * <dt>ADD
 * <dd>The opcode/length followed by (length+1) bytes of data
 * to copy to the current output location.
 * <dt>RUN
 * <dd>The opcode/length followed by a byte to be duplicated
 * into the current output stream (length+1) times.
 * </dl>
 *
 */
public class DEZEncoder implements ByteDeltaEncoder {

	private final ByteArrayOutputStream patch = new ByteArrayOutputStream();
	private final byte[] work = new byte[6];

	public static final byte[] MAGIC = {'D', 'E', 'Z', '1'};
	public static final int COPY = 0x00;
	public static final int COPY_EXT = 0x80;
	public static final int ADD = 0x40;
	public static final int ADD_EXT = 0xc0;
	public static final int RUN = 0x60;
	public static final int RUN_EXT = 0xe0;

	public void init(int sourceSize, int targetSize) {
		try {
			patch.reset();
			patch.write(MAGIC);
			// some flags
			patch.write(0);
			encodeInt(sourceSize);
			encodeInt(targetSize);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Encode an opcode + length.
	 *
	 * @param op opcode. extend bit is added automatically.
	 * @param max maximum size of value that can fit in the first byte inclusive. Leave room for opcode bits.
	 * @param len length to encode.
	 */
	private void encodeOp(int op, int max, int len) {
		if (len <= max) {
			patch.write((byte) (len | op));
		} else {
			int i = work.length;
			int cont = 0;

			while (len > max) {
				work[--i] = (byte) ((len & 0x7f) | cont);
				len >>= 7;
				cont = 0x80;
			}
			work[--i] = (byte) (len | 0x80 | op);
			patch.write(work, i, work.length - i);
		}
	}

	/**
	 * Encodes an integer.
	 * <p>
	 * Format is big-endian order encoded as:
	 * <p>
	 * CXXXXXXX
	 * <p>
	 * Where C is the continue bit.
	 *
	 */
	void encodeInt(int addr) {
		int i = work.length;
		int cont = 0;
		while (addr > 0x7f) {
			work[--i] = (byte) ((addr & 0x7f) | cont);
			addr >>= 7;
			cont = 0x80;
		}
		work[--i] = (byte) (addr | cont);
		patch.write(work, i, work.length - i);
	}

	public void copy(int addr, int len) {
		encodeOp(COPY, 0x3f, len);
		encodeInt(addr);
	}

	public void add(byte[] data, int off, int len) {
		encodeOp(ADD, 0x1f, len - 1);
		patch.write(data, off, len);
	}

	public void run(byte b, int len) {
		encodeOp(RUN, 0x1f, len - 1);
		patch.write(b);
	}

	public byte[] toPatch() {
		return patch.toByteArray();
	}
}
