package arc.util.serialization;

import arc.util.*;

public class Base64Coder{
    public static final CharMap regularMap = new CharMap('+', '/'), urlsafeMap = new CharMap('-', '_');
    // The line separator string of the operating system.
    private static final String systemLineSeparator = "\n";

    // Dummy constructor.
    private Base64Coder(){
    }

    /**
     * Encodes a string into Base64 format. No blanks or line breaks are inserted.
     *
     * @param s A String to be encoded.
     * @return A String containing the Base64 encoded data.
     */
    public static String encodeString(String s){
        return encodeString(s, false);
    }

    public static String encodeString(String s, boolean useUrlsafeEncoding){
        return new String(encode(s.getBytes(Strings.utf8), useUrlsafeEncoding ? urlsafeMap.encodingMap : regularMap.encodingMap));
    }

    /**
     * Encodes a byte array into Base64 format and breaks the output into lines of 76 characters. This method is compatible with
     * <code>sun.misc.BASE64Encoder.encodeBuffer(byte[])</code>.
     *
     * @param in An array containing the data bytes to be encoded.
     * @return A String containing the Base64 encoded data, broken into lines.
     */
    public static String encodeLines(byte[] in){
        return encodeLines(in, 0, in.length, 76, systemLineSeparator, regularMap.encodingMap);
    }

    public static String encodeLines(byte[] in, int iOff, int iLen, int lineLen, String lineSeparator, CharMap charMap){
        return encodeLines(in, iOff, iLen, lineLen, lineSeparator, charMap.encodingMap);
    }

    /**
     * Encodes a byte array into Base64 format and breaks the output into lines.
     *
     * @param in An array containing the data bytes to be encoded.
     * @param iOff Offset of the first byte in <code>in</code> to be processed.
     * @param iLen Number of bytes to be processed in <code>in</code>, starting at <code>iOff</code>.
     * @param lineLen Line length for the output data. Should be a multiple of 4.
     * @param lineSeparator The line separator to be used to separate the output lines.
     * @param charMap char map to use
     * @return A String containing the Base64 encoded data, broken into lines.
     */
    public static String encodeLines(byte[] in, int iOff, int iLen, int lineLen, String lineSeparator, char[] charMap){
        int blockLen = (lineLen * 3) / 4;
        if(blockLen <= 0){
            throw new IllegalArgumentException();
        }
        int lines = (iLen + blockLen - 1) / blockLen;
        int bufLen = ((iLen + 2) / 3) * 4 + lines * lineSeparator.length();
        StringBuilder buf = new StringBuilder(bufLen);
        int ip = 0;
        while(ip < iLen){
            int l = Math.min(iLen - ip, blockLen);
            buf.append(encode(in, iOff + ip, l, charMap));
            buf.append(lineSeparator);
            ip += l;
        }
        return buf.toString();
    }

    /**
     * Encodes a byte array into Base64 format. No blanks or line breaks are inserted in the output.
     *
     * @param in An array containing the data bytes to be encoded.
     * @return A character array containing the Base64 encoded data.
     */
    public static char[] encode(byte[] in){
        return encode(in, regularMap.encodingMap);
    }

    public static char[] encode(byte[] in, boolean urlSafe){
        return encode(in, urlSafe ? urlsafeMap.encodingMap : regularMap.encodingMap);
    }

    public static char[] encode(byte[] in, CharMap charMap){
        return encode(in, 0, in.length, charMap);
    }

    public static char[] encode(byte[] in, char[] charMap){
        return encode(in, 0, in.length, charMap);
    }

    /**
     * Encodes a byte array into Base64 format. No blanks or line breaks are inserted in the output.
     *
     * @param in An array containing the data bytes to be encoded.
     * @param iLen Number of bytes to process in <code>in</code>.
     * @return A character array containing the Base64 encoded data.
     */
    public static char[] encode(byte[] in, int iLen){
        return encode(in, 0, iLen, regularMap.encodingMap);
    }

    public static char[] encode(byte[] in, int iOff, int iLen, CharMap charMap){
        return encode(in, iOff, iLen, charMap.encodingMap);
    }

    /**
     * Encodes a byte array into Base64 format. No blanks or line breaks are inserted in the output.
     *
     * @param in An array containing the data bytes to be encoded.
     * @param iOff Offset of the first byte in <code>in</code> to be processed.
     * @param iLen Number of bytes to process in <code>in</code>, starting at <code>iOff</code>.
     * @param charMap char map to use
     * @return A character array containing the Base64 encoded data.
     */
    public static char[] encode(byte[] in, int iOff, int iLen, char[] charMap){
        int oDataLen = (iLen * 4 + 2) / 3; // output length without padding
        int oLen = ((iLen + 2) / 3) * 4; // output length including padding
        char[] out = new char[oLen];
        int ip = iOff;
        int iEnd = iOff + iLen;
        int op = 0;
        while(ip < iEnd){
            int i0 = in[ip++] & 0xff;
            int i1 = ip < iEnd ? in[ip++] & 0xff : 0;
            int i2 = ip < iEnd ? in[ip++] & 0xff : 0;
            int o0 = i0 >>> 2;
            int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            int o3 = i2 & 0x3F;
            out[op++] = charMap[o0];
            out[op++] = charMap[o1];
            out[op] = op < oDataLen ? charMap[o2] : '=';
            op++;
            out[op] = op < oDataLen ? charMap[o3] : '=';
            op++;
        }
        return out;
    }

    /**
     * Decodes a string from Base64 format. No blanks or line breaks are allowed within the Base64 encoded input data.
     *
     * @param s A Base64 String to be decoded.
     * @return A String containing the decoded data.
     * @throws IllegalArgumentException If the input is not valid Base64 encoded data.
     */
    public static String decodeString(String s){
        return decodeString(s, false);
    }

    public static String decodeString(String s, boolean useUrlSafeEncoding){
        return new String(decode(s.toCharArray(), useUrlSafeEncoding ? urlsafeMap.decodingMap : regularMap.decodingMap));
    }

    public static byte[] decodeLines(String s){
        return decodeLines(s, regularMap.decodingMap);
    }

    public static byte[] decodeLines(String s, CharMap inverseCharMap){
        return decodeLines(s, inverseCharMap.decodingMap);
    }

    /**
     * Decodes a byte array from Base64 format and ignores line separators, tabs and blanks. CR, LF, Tab and Space characters are
     * ignored in the input data. This method is compatible with <code>sun.misc.BASE64Decoder.decodeBuffer(String)</code>.
     *
     * @param s A Base64 String to be decoded.
     * @param inverseCharMap
     * @return An array containing the decoded data bytes.
     * @throws IllegalArgumentException If the input is not valid Base64 encoded data.
     */
    public static byte[] decodeLines(String s, byte[] inverseCharMap){
        char[] buf = new char[s.length()];
        int p = 0;
        for(int ip = 0; ip < s.length(); ip++){
            char c = s.charAt(ip);
            if(c != ' ' && c != '\r' && c != '\n' && c != '\t'){
                buf[p++] = c;
            }
        }
        return decode(buf, 0, p, inverseCharMap);
    }

    /**
     * Decodes a byte array from Base64 format. No blanks or line breaks are allowed within the Base64 encoded input data.
     *
     * @param s A Base64 String to be decoded.
     * @return An array containing the decoded data bytes.
     * @throws IllegalArgumentException If the input is not valid Base64 encoded data.
     */
    public static byte[] decode(String s){
        return decode(s.toCharArray());
    }

    /**
     * Decodes a byte array from Base64 format. No blanks or line breaks are allowed within the Base64 encoded input data.
     *
     * @param s A Base64 String to be decoded.
     * @param inverseCharMap
     * @return An array containing the decoded data bytes.
     * @throws IllegalArgumentException If the input is not valid Base64 encoded data.
     */
    public static byte[] decode(String s, CharMap inverseCharMap){
        return decode(s.toCharArray(), inverseCharMap);
    }

    public static byte[] decode(char[] in, byte[] inverseCharMap){
        return decode(in, 0, in.length, inverseCharMap);
    }

    public static byte[] decode(char[] in, CharMap inverseCharMap){
        return decode(in, 0, in.length, inverseCharMap);
    }

    /**
     * Decodes a byte array from Base64 format. No blanks or line breaks are allowed within the Base64 encoded input data.
     *
     * @param in A character array containing the Base64 encoded data.
     * @return An array containing the decoded data bytes.
     * @throws IllegalArgumentException If the input is not valid Base64 encoded data.
     */
    public static byte[] decode(char[] in){
        return decode(in, 0, in.length, regularMap.decodingMap);
    }

    public static byte[] decode(char[] in, int iOff, int iLen, CharMap inverseCharMap){
        return decode(in, iOff, iLen, inverseCharMap.decodingMap);
    }

    /**
     * Decodes a byte array from Base64 format. No blanks or line breaks are allowed within the Base64 encoded input data.
     *
     * @param in A character array containing the Base64 encoded data.
     * @param iOff Offset of the first character in <code>in</code> to be processed.
     * @param iLen Number of characters to process in <code>in</code>, starting at <code>iOff</code>.
     * @param inverseCharMap charMap to use
     * @return An array containing the decoded data bytes.
     * @throws IllegalArgumentException If the input is not valid Base64 encoded data.
     */
    public static byte[] decode(char[] in, int iOff, int iLen, byte[] inverseCharMap){
        if(iLen % 4 != 0){
            throw new IllegalArgumentException("Length of Base64 encoded input string is not a multiple of 4.");
        }
        while(iLen > 0 && in[iOff + iLen - 1] == '='){
            iLen--;
        }
        int oLen = (iLen * 3) / 4;
        byte[] out = new byte[oLen];
        int ip = iOff;
        int iEnd = iOff + iLen;
        int op = 0;
        while(ip < iEnd){
            int i0 = in[ip++];
            int i1 = in[ip++];
            int i2 = ip < iEnd ? in[ip++] : 'A';
            int i3 = ip < iEnd ? in[ip++] : 'A';
            if(i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127){
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            }
            int b0 = inverseCharMap[i0];
            int b1 = inverseCharMap[i1];
            int b2 = inverseCharMap[i2];
            int b3 = inverseCharMap[i3];
            if(b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0){
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            }
            int o0 = (b0 << 2) | (b1 >>> 4);
            int o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
            int o2 = ((b2 & 3) << 6) | b3;
            out[op++] = (byte)o0;
            if(op < oLen){
                out[op++] = (byte)o1;
            }
            if(op < oLen){
                out[op++] = (byte)o2;
            }
        }
        return out;
    }

    public static class CharMap{
        protected final char[] encodingMap = new char[64];
        protected final byte[] decodingMap = new byte[128];

        public CharMap(char char63, char char64){
            int i = 0;
            for(char c = 'A'; c <= 'Z'; c++){
                encodingMap[i++] = c;
            }
            for(char c = 'a'; c <= 'z'; c++){
                encodingMap[i++] = c;
            }
            for(char c = '0'; c <= '9'; c++){
                encodingMap[i++] = c;
            }
            encodingMap[i++] = char63;
            encodingMap[i++] = char64;
            for(i = 0; i < decodingMap.length; i++){
                decodingMap[i] = -1;
            }
            for(i = 0; i < 64; i++){
                decodingMap[encodingMap[i]] = (byte)i;
            }
        }

        public byte[] getDecodingMap(){
            return decodingMap;
        }

        public char[] getEncodingMap(){
            return encodingMap;
        }
    }
}
