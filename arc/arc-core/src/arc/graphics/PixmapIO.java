package arc.graphics;

import arc.files.*;
import arc.util.*;
import arc.util.io.*;

import java.io.*;
import java.nio.*;
import java.util.zip.*;

/**
 * Writes Pixmaps to various formats.
 * @author mzechner
 * @author Nathan Sweet
 */
public class PixmapIO{

    /**
     * Writes the pixmap as a PNG with compression. See {@link PngWriter} to configure the compression level, more efficiently flip the
     * pixmap vertically, and to write out multiple PNGs with minimal allocation.
     */
    public static void writePng(Fi file, Pixmap pixmap){
        try{
            PngWriter writer = new PngWriter((int)(pixmap.width * pixmap.height * 1.5f)); // Guess at deflated size.
            try{
                writer.setFlipY(false);
                writer.write(file, pixmap);
            }finally{
                writer.dispose();
            }
        }catch(IOException ex){
            throw new ArcRuntimeException("Error writing PNG: " + file, ex);
        }
    }

    /**
     * Writes the pixmap as a PNG with compression. See {@link PngWriter} to configure the compression level, more efficiently flip the
     * pixmap vertically, and to write out multiple PNGs with minimal allocation.
     */
    public static byte[] writePngBytes(Pixmap pixmap) throws IOException{
        PngWriter writer = new PngWriter((int)(pixmap.width * pixmap.height * 1.5f)); // Guess at deflated size.
        try{
            writer.setFlipY(false);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            writer.write(stream, pixmap);
            return stream.toByteArray();
        }finally{
            writer.dispose();
        }
    }

    /** Reads a PNG file using a pure-Java PNG decoder. */
    public static Pixmap readPNG(Fi file){
        try{
            PngReader reader = new PngReader();
            ByteBuffer result = reader.read(new ByteArrayInputStream(file.readBytes()));
            return new Pixmap(result, reader.width, reader.height);
        }catch(Exception e){
            throw new ArcRuntimeException("Error reading PNG: " + file, e);
        }
    }

    /** Reads a PNG file using a pure-Java PNG decoder. */
    public static Pixmap readPNG(byte[] bytes){
        try{
            PngReader reader = new PngReader();
            ByteBuffer result = reader.read(new ByteArrayInputStream(bytes));
            return new Pixmap(result, reader.width, reader.height);
        }catch(Exception e){
            throw new ArcRuntimeException("Error reading PNG", e);
        }
    }

    /**
     * PNG encoder with compression. An instance can be reused to encode multiple PNGs with minimal allocation.
     * @author Matthias Mann
     * @author Nathan Sweet
     */
    public static class PngWriter implements Disposable{
        private static final byte[] SIGNATURE = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        private static final int IHDR = 0x49484452, IDAT = 0x49444154, IEND = 0x49454E44;
        private static final byte COLOR_ARGB = 6;
        private static final byte COMPRESSION_DEFLATE = 0;
        private static final byte FILTER_NONE = 0;
        private static final byte INTERLACE_NONE = 0;

        private final ChunkBuffer buffer;
        private final Deflater deflater;
        private boolean flipY = true;

        public PngWriter(){
            this(128 * 128);
        }

        public PngWriter(int initialBufferSize){
            buffer = new ChunkBuffer(initialBufferSize);
            deflater = new Deflater();
        }

        /** If true, the resulting PNG is flipped vertically. Default is true. */
        public void setFlipY(boolean flipY){
            this.flipY = flipY;
        }

        /** Sets the deflate compression level. Default is {@link Deflater#DEFAULT_COMPRESSION}. */
        public void setCompression(int level){
            deflater.setLevel(level);
        }

        public void write(Fi file, Pixmap pixmap) throws IOException{
            OutputStream output = file.write(false);
            try{
                write(output, pixmap);
            }finally{
                Streams.close(output);
            }
        }

        /** Writes the pixmap to the stream without closing the stream. */
        public void write(OutputStream output, Pixmap pixmap) throws IOException{
            DeflaterOutputStream deflaterOutput = new DeflaterOutputStream(buffer, deflater);
            DataOutputStream dataOutput = new DataOutputStream(output);
            dataOutput.write(SIGNATURE);

            buffer.writeInt(IHDR);
            buffer.writeInt(pixmap.width);
            buffer.writeInt(pixmap.height);
            buffer.writeByte(8); // 8 bits per component.
            buffer.writeByte(COLOR_ARGB);
            buffer.writeByte(COMPRESSION_DEFLATE);
            buffer.writeByte(FILTER_NONE);
            buffer.writeByte(INTERLACE_NONE);
            buffer.endChunk(dataOutput);

            buffer.writeInt(IDAT);
            deflater.reset();

            int lineLen = pixmap.width * 4;
            //1 extra byte for filter 0
            byte[] curLine = new byte[lineLen + 1];

            ByteBuffer pixels = pixmap.pixels;
            int oldPosition = pixels.position();
            for(int y = 0, h = pixmap.height; y < h; y++){
                int py = flipY ? (h - y - 1) : y;
                pixels.position(py * lineLen);
                pixels.get(curLine, 1, lineLen);

                deflaterOutput.write(curLine, 0, lineLen + 1);
            }
            pixels.position(oldPosition);
            deflaterOutput.finish();
            buffer.endChunk(dataOutput);

            buffer.writeInt(IEND);
            buffer.endChunk(dataOutput);

            output.flush();
        }

        @Override
        public void dispose(){
            deflater.end();
        }

        static class ChunkBuffer extends DataOutputStream{
            final ByteArrayOutputStream buffer;
            final CRC32 crc;

            ChunkBuffer(int initialSize){
                this(new ByteArrayOutputStream(initialSize), new CRC32());
            }

            private ChunkBuffer(ByteArrayOutputStream buffer, CRC32 crc){
                super(new CheckedOutputStream(buffer, crc));
                this.buffer = buffer;
                this.crc = crc;
            }

            public void endChunk(DataOutputStream target) throws IOException{
                flush();
                target.writeInt(buffer.size() - 4);
                buffer.writeTo(target);
                target.writeInt((int)crc.getValue());
                buffer.reset();
                crc.reset();
            }
        }
    }

    /** Class based on https://github.com/Mike-C/lwjPNG, with many modifications */
    public static class PngReader{
        private static final int
        ctypeRgba = 6,
        ctypePalette = 3,
        ctypeRgb = 2;

        /** Size fields are set after reading. */
        public int width, height;

        public byte bitDepth, colorType, compression, filter, interlace;

        private int dataLen, cs;
        private byte[] imgData = null;
        private ByteBuffer buf = null;
        private int[] palette;
        private boolean foundHeader;

        public ByteBuffer read(InputStream in) throws IOException{
            readChunks(new DataInputStream(in));

            if(buf != null) buf.clear();
            buf = ByteBuffer.allocateDirect(cs);
            try{
                getImage(buf);
            }catch(DataFormatException e){
                throw new IOException(e);
            }
            buf.flip();
            return buf;
        }

        private void readChunks(DataInputStream in) throws IOException{
            if(imgData == null && in.available() > 4){
                long header = in.readLong(); //PNG signature
                if(header != 0x89504e470d0a1a0aL){
                    String headerString = Long.toHexString(header);
                    throw new IOException(headerString.startsWith("ffd8ff") ? "This is a JPEG, not a PNG." : "This isn't a PNG. Header: 0x" + headerString);
                }
            }else if(imgData == null){
                width = 0;
                return;
            }
            dataLen = 0;
            int chunkType;
            while(true){
                int chunkLen = in.readInt(); // Read the chunk length.
                if(chunkLen <= 0 || chunkLen > 99998192) break;

                chunkType = in.readInt();
                if(chunkType == 0x49454e44) //IEND
                    break; // last chunk reached..
                if(chunkType == 0x49444154){ //IDAT
                    in.readFully(imgData, dataLen, chunkLen);
                    dataLen += chunkLen;
                }else if(chunkType == 0x49484452){ //IHDR
                    if(foundHeader) throw new IOException("Multiple IHDR chunks are not allowed.");
                    width = in.readInt();
                    height = in.readInt();
                    bitDepth = in.readByte();
                    colorType = in.readByte();
                    compression = in.readByte();
                    filter = in.readByte();
                    interlace = in.readByte();

                    cs = 4 * width * height;
                    imgData = new byte[in.available()]; //initialize image array
                    foundHeader = true;

                    //validation
                    if(bitDepth == 16) throw new IOException("16-bit depth is not supported.");
                    if(colorType == ctypePalette && bitDepth < 4) throw new IOException("Only PNG palettes with 4 or 8-bit depth are supported. Depth given: " + bitDepth);
                    if(colorType != ctypePalette && colorType != ctypeRgb && colorType != ctypeRgba) throw new IOException("Unsupported color type: " + colorType + " (Note that grayscale is not supported)");
                    if(interlace != 0) throw new IOException("PNG interlacing is not supported.");

                }else if(colorType == ctypePalette && chunkType == 0x504c5445){ //PLTE
                    int colors = chunkLen/3;
                    palette = new int[colors];
                    for(int i = 0; i < colors; i++){
                        palette[i] = Color.packRgba(in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedByte(), 255);
                    }
                }else if(colorType == ctypePalette && chunkType == 0x74524e53){ //tRNS
                    for(int i = 0; i < chunkLen; i++){
                        palette[i] = (palette[i] & 0xffffff00) | in.readUnsignedByte();
                    }
                }else{
                    in.skipBytes(chunkLen);
                }
                in.readInt(); // checksum skip
            }
        }

        private void getImage(ByteBuffer bb) throws DataFormatException{
            //bpx bytes per pixel, wT total output width, v scanline width
            int
            bpx = colorType == ctypePalette ? 1 : colorType == ctypeRgb ? 3 : 4,
            wT = width * 4,
            v = (bitDepth == 4 ? width / 2 : width) * bpx + 1; // scanLine width

            Inflater inflater = new Inflater();
            inflater.setInput(imgData, 0, dataLen);

            byte[] prev = new byte[wT + 1], row = new byte[wT + 1]; // every row contains filter byte

            for(int i = 1, s = 0; s < height; i = 1, s++){ // scanLine
                //inflating each line is the bottleneck here, but unfortunately there's nothing I can do about it
                inflater.inflate(row, 0, v);
                byte first = row[0];

                if(first != 0){ //apply filters

                    if(first == 1){
                        for(i += bpx; i < v; i++){
                            row[i] += row[i - bpx];
                        }
                    }else if(first == 2){
                        for(; i < v; i++){
                            row[i] += prev[i];
                        }
                    }else if(first == 3){
                        for(; i < bpx + 1; i++){
                            row[i] += (prev[i] & 0xFF) >>> 1;
                        }
                        for(; i < v; i++){
                            row[i] += ((prev[i] & 0xFF) + (row[i - bpx] & 0xFF)) >>> 1;
                        }
                    }else{
                        for(; i < bpx + 1; i++){
                            row[i] += prev[i];
                        }
                        for(; i < v; i++){
                            row[i] += paeth(row[i - bpx] & 0xFF, prev[i] & 0xFF, prev[i - bpx] & 0xFF);
                        }
                    }
                }

                //format output, normal mode
                if(bpx == 3){
                    //this could probably made faster, but ehhh
                    ByteBuffer wRow = ByteBuffer.wrap(row);
                    for(i = 1; i < v; i += bpx){
                        bb.putInt((wRow.getInt(i) & 0xFFFFFF00) + 0xFF);
                    }
                }else if(bpx == 1){ //palette
                    //when bitDepth is 4, split every byte in two
                    if(bitDepth == 4){
                        for(i = 1; i < v; i += bpx){
                            bb.putInt(palette[Pack.leftByte(row[i])]);
                            bb.putInt(palette[Pack.rightByte(row[i])]);
                        }
                    }else{
                        for(i = 1; i < v; i += bpx){
                            bb.putInt(palette[row[i] & 0xFF]);
                        }
                    }
                }else{
                    bb.put(row, 1, v - 1);
                }
                byte[] swap = prev;
                prev = row;
                row = swap;
            }
            bb.position(bb.capacity());
            imgData = null;
        }

        private static int ab(int a){
            int b = a >> 8;
            return (a ^ b) - b;
        }

        private static int paeth(int a, int b, int c){
            int pa = b - c, pb = a - c, pc = ab(pa + pb);
            pa = ab(pa);
            pb = ab(pb);
            return (pa <= pb && pa <= pc) ? a : (pb <= pc) ? b : c;
        }
    }
}
