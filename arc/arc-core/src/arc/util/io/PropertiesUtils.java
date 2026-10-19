/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package arc.util.io;

import arc.struct.ObjectMap;
import arc.struct.ObjectMap.Entry;
import arc.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;

/**
 * {@code PropertiesUtils} is a helper class that allows you to load and store key/value pairs of an
 * {@code ObjectMap<String,String>} with the same line-oriented syntax supported by {@code java.util.Properties}.
 */
public final class PropertiesUtils{
    private static final int NONE = 0, SLASH = 1, UNICODE = 2, CONTINUE = 3, KEY_DONE = 4, IGNORE = 5;
    private static final String LINE_SEPARATOR = "\n";

    private PropertiesUtils(){
    }

    /**
     * Adds to the specified {@code ObjectMap} the key/value pairs loaded from the {@code Reader} in a simple line-oriented format
     * compatible with <code>java.util.Properties</code>.
     * <p>
     * The input stream remains open after this method returns.
     * @param properties the map to be filled.
     * @param reader the input character stream reader.
     * @throws IOException if an error occurred when reading from the input stream.
     * @throws IllegalArgumentException if a malformed Unicode escape appears in the input.
     */
    public static void load(ObjectMap<String, String> properties, Reader reader){
        if(properties == null) throw new NullPointerException("ObjectMap cannot be null");
        if(reader == null) throw new NullPointerException("Reader cannot be null");
        int mode = NONE, unicode = 0, count = 0;
        char nextChar;
        char[] buf = new char[40];
        int offset = 0, keyLength = -1, intVal;
        boolean firstChar = true;

        try{

            BufferedReader br = new BufferedReader(reader);

            while(true){
                intVal = br.read();
                if(intVal == -1){
                    break;
                }
                nextChar = (char)intVal;

                if(offset == buf.length){
                    char[] newBuf = new char[buf.length * 2];
                    System.arraycopy(buf, 0, newBuf, 0, offset);
                    buf = newBuf;
                }
                if(mode == UNICODE){
                    int digit = Character.digit(nextChar, 16);
                    if(digit >= 0){
                        unicode = (unicode << 4) + digit;
                        if(++count < 4){
                            continue;
                        }
                    }else if(count <= 4){
                        throw new IllegalArgumentException("Invalid Unicode sequence: illegal character");
                    }
                    mode = NONE;
                    buf[offset++] = (char)unicode;
                    if(nextChar != '\n'){
                        continue;
                    }
                }
                if(mode == SLASH){
                    mode = NONE;
                    switch(nextChar){
                        case '\r':
                            mode = CONTINUE; // Look for a following \n
                            continue;
                        case '\n':
                            mode = IGNORE; // Ignore whitespace on the next line
                            continue;
                        case 'b':
                            nextChar = '\b';
                            break;
                        case 'f':
                            nextChar = '\f';
                            break;
                        case 'n':
                            nextChar = '\n';
                            break;
                        case 'r':
                            nextChar = '\r';
                            break;
                        case 't':
                            nextChar = '\t';
                            break;
                        case 'u':
                            mode = UNICODE;
                            unicode = count = 0;
                            continue;
                    }
                }else{
                    switch(nextChar){
                        case '#':
                        case '!':
                            if(firstChar){
                                while(true){
                                    intVal = br.read();
                                    if(intVal == -1){
                                        break;
                                    }
                                    nextChar = (char)intVal;
                                    if(nextChar == '\r' || nextChar == '\n'){
                                        break;
                                    }
                                }
                                continue;
                            }
                            break;
                        case '\n':
                            if(mode == CONTINUE){ // Part of a \r\n sequence
                                mode = IGNORE; // Ignore whitespace on the next line
                                continue;
                            }
                            // fall into the next case
                        case '\r':
                            mode = NONE;
                            firstChar = true;
                            if(offset > 0 || (offset == 0 && keyLength == 0)){
                                if(keyLength == -1){
                                    keyLength = offset;
                                }
                                String temp = new String(buf, 0, offset);
                                properties.put(temp.substring(0, keyLength), temp.substring(keyLength));
                            }
                            keyLength = -1;
                            offset = 0;
                            continue;
                        case '\\':
                            if(mode == KEY_DONE){
                                keyLength = offset;
                            }
                            mode = SLASH;
                            continue;
                        case ':':
                        case '=':
                            if(keyLength == -1){ // if parsing the key
                                mode = NONE;
                                keyLength = offset;
                                continue;
                            }
                            break;
                    }

                    if(Character.isWhitespace(nextChar)){
                        if(mode == CONTINUE){
                            mode = IGNORE;
                        }
                        // if key length == 0 or value length == 0
                        if(offset == 0 || offset == keyLength || mode == IGNORE){
                            continue;
                        }
                        if(keyLength == -1){ // if parsing the key
                            mode = KEY_DONE;
                            continue;
                        }
                    }
                    if(mode == IGNORE || mode == CONTINUE){
                        mode = NONE;
                    }
                }
                firstChar = false;
                if(mode == KEY_DONE){
                    keyLength = offset;
                    mode = NONE;
                }
                buf[offset++] = nextChar;
            }
            if(mode == UNICODE && count <= 4){
                throw new IllegalArgumentException("Invalid Unicode sequence: expected format \\uxxxx");
            }
            if(keyLength == -1 && offset > 0){
                keyLength = offset;
            }
            if(keyLength >= 0){
                String temp = new String(buf, 0, offset);
                String key = temp.substring(0, keyLength);
                String value = temp.substring(keyLength);
                if(mode == SLASH){
                    value += "\u0000";
                }
                properties.put(key, value);
            }
        }catch(IOException e){
            throw new ArcRuntimeException(e);
        }
    }

    /**
     * Writes the key/value pairs of the specified <code>ObjectMap</code> to the output character stream in a simple line-oriented
     * format compatible with <code>java.util.Properties</code>.
     * <p>
     * Every entry in the <code>ObjectMap</code> is written out, one per line. For each entry the key string is written, then an
     * ASCII <code>=</code>, then the associated element string. For the key, all space characters are written with a preceding
     * <code>\</code> character. For the element, leading space characters, but not embedded or trailing space characters, are
     * written with a preceding <code>\</code> character. The key and element characters <code>#</code>, <code>!</code>,
     * <code>=</code>, and <code>:</code> are written with a preceding backslash to ensure that they are properly loaded.
     * <p>
     * After the entries have been written, the output stream is flushed. The output stream remains open after this method returns.
     * @param properties the {@code ObjectMap}.
     * @param writer an output character stream writer.
     * @param comment an optional comment to be written, or null.
     * @throws IOException if writing this property list to the specified output stream throws an <tt>IOException</tt>.
     * @throws NullPointerException if <code>writer</code> is null.
     */
    public static void store(ObjectMap<String, String> properties, Writer writer, String comment, boolean date) throws IOException{
        storeImpl(properties, writer, comment, false, date);
    }

    public static void store(ObjectMap<String, String> properties, Writer writer, String comment) throws IOException{
        store(properties, writer, comment, false);
    }

    private static void storeImpl(ObjectMap<String, String> properties, Writer writer, String comment, boolean date, boolean escapeUnicode) throws IOException{
        if(comment != null){
            writeComment(writer, comment);
        }
        if(date){
            writer.write("#");
            writer.write(new Date().toString());
            writer.write(LINE_SEPARATOR);
        }

        StringBuilder sb = new StringBuilder(200);
        for(Entry<String, String> entry : properties.entries()){
            dumpString(sb, entry.key, true, escapeUnicode);
            sb.append('=');
            dumpString(sb, entry.value, false, escapeUnicode);
            writer.write(LINE_SEPARATOR);
            writer.write(sb.toString());
            sb.setLength(0);
        }
        writer.flush();
    }

    private static void dumpString(StringBuilder outBuffer, String string, boolean escapeSpace, boolean escapeUnicode){
        int len = string.length();
        for(int i = 0; i < len; i++){
            char ch = string.charAt(i);
            // Handle common case first
            if((ch > 61) && (ch < 127)){
                outBuffer.append(ch == '\\' ? "\\\\" : ch);
                continue;
            }
            switch(ch){
                case ' ':
                    if(i == 0 || escapeSpace){
                        outBuffer.append("\\ ");
                    }else{
                        outBuffer.append(ch);
                    }
                    break;
                case '\n':
                    outBuffer.append("\\n");
                    break;
                case '\r':
                    outBuffer.append("\\r");
                    break;
                case '\t':
                    outBuffer.append("\\t");
                    break;
                case '\f':
                    outBuffer.append("\\f");
                    break;
                case '=': // Fall through
                case ':': // Fall through
                case '#': // Fall through
                case '!':
                    outBuffer.append('\\').append(ch);
                    break;
                default:
                    if(((ch < 0x0020) || (ch > 0x007e)) & escapeUnicode){
                        String hex = Integer.toHexString(ch);
                        outBuffer.append("\\u");
                        for(int j = 0; j < 4 - hex.length(); j++){
                            outBuffer.append('0');
                        }
                        outBuffer.append(hex);
                    }else{
                        outBuffer.append(ch);
                    }
                    break;
            }
        }
    }

    private static void writeComment(Writer writer, String comment) throws IOException{
        writer.write("#");
        int len = comment.length();
        int curIndex = 0;
        int lastIndex = 0;
        while(curIndex < len){
            char c = comment.charAt(curIndex);
            if(c > '\u00ff' || c == '\n' || c == '\r'){
                if(lastIndex != curIndex) writer.write(comment.substring(lastIndex, curIndex));
                if(c > '\u00ff'){
                    String hex = Integer.toHexString(c);
                    writer.write("\\u");
                    for(int j = 0; j < 4 - hex.length(); j++){
                        writer.write('0');
                    }
                    writer.write(hex);
                }else{
                    writer.write(LINE_SEPARATOR);
                    if(c == '\r' && curIndex != len - 1 && comment.charAt(curIndex + 1) == '\n'){
                        curIndex++;
                    }
                    if(curIndex == len - 1 || (comment.charAt(curIndex + 1) != '#' && comment.charAt(curIndex + 1) != '!'))
                        writer.write("#");
                }
                lastIndex = curIndex + 1;
            }
            curIndex++;
        }
        if(lastIndex != curIndex) writer.write(comment.substring(lastIndex, curIndex));
        writer.write(LINE_SEPARATOR);
    }
}
