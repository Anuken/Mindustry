package arc.util.serialization;

import arc.util.serialization.Jval.*;

import java.io.*;
import java.util.*;

/** Used internally by Jval. Don't use directly. */
class JvalReader{
    /** current == ',': peeks ahead (skipping comments) for ':'/'}'/']', or a bare trailing comma before EOL, to confirm a real separator; cap that to prevent ridiculously long lookahead */
    private static final int commaLookaheadLimit = 256;

    private final char[] buffer;
    private int index, bufferLength;
    private int line;
    private int lineOffset;
    private int current;
    private StringBuilder captureBuffer;
    private int captureStart;
    private int rawStart;
    private boolean escaped;
    /** True while reading an array element, where ',' always acts as a terminator/separator. */
    private boolean inArray;

    JvalReader(String string){
        buffer = string.toCharArray();
        bufferLength = buffer.length;
        reset();
    }

    JvalReader(Reader reader) throws IOException{
        char[] data = new char[8 * 1024];
        int size = 0, n;
        while((n = reader.read(data, size, data.length - size)) != -1){
            size += n;
            if(size == data.length) data = Arrays.copyOf(data, data.length * 2);
        }
        buffer = size == data.length ? data : Arrays.copyOf(data, size);
        bufferLength = buffer.length;
        reset();
    }

    static boolean isWhiteSpace(int ch){
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
    }

    void reset(){
        index = lineOffset = current = 0;
        line = 1;
        captureBuffer = null;
        escaped = false;
        inArray = false;
    }

    Jval parse(){
        //braces for the root object are optional

        read();
        skipWhiteSpace();

        switch(current){
            case '[':
            case '{':
                return checkTrailing(readValue());
            default:
                try{
                    // assume we have a root object without braces
                    return checkTrailing(readObject(true));
                }catch(Exception exception){
                    // test if we are dealing with a single JSON value instead (true/false/null/num/"")
                    reset();
                    read();
                    skipWhiteSpace();
                    try{
                        return checkTrailing(readValue());
                    }catch(Exception ignored){
                    }
                    throw exception; // throw original error
                }
        }
    }

    Jval checkTrailing(Jval v) throws JsonParseException{
        skipWhiteSpace();
        if(!isEndOfText()) throw error("Extra characters in input: " + current);
        return v;
    }

    private Jval readValue(){
        switch(current){
            case '\'':
            case '"':
                return readString();
            case '[':
                return readArray();
            case '{':
                return readObject(false);
            default:
                return readTfnns();
        }
    }

    private Jval readTfnns(){
        int start = index - 1;
        int first = current;
        if(JvalWriter.isPunctuatorChar(first))
            throw error("Found a punctuator character '" + (char)first + "' when expecting a quoteless string (check your syntax)");

        while(true){
            read();
            boolean isComment = current == '#' || (current == '/' && (peek() == '/' || peek() == '*'));
            // in objects, only treat ',' as a separator if it looks like it precedes another 'key:'
            boolean commaStop = current == ',' && (inArray || commaEndsValue());
            boolean isEol = current < 0 || current == '\r' || current == '\n' || commaStop || current == ']' || current == '}' || isComment;
            if(isEol){
                int stop = current < 0 ? index : index - 1; // position of the stopping char, not yet part of the value

                switch(first){
                    case 'f':
                    case 'n':
                    case 't':{
                        int s = start, e = stop;
                        while(s < e && isTrimChar(buffer[s])) s++;
                        while(e > s && isTrimChar(buffer[e - 1])) e--;
                        int len = e - s;
                        if(len == 5 && regionMatches(s, "false")) return Jval.falseValue;
                        if(len == 4 && regionMatches(s, "null")) return Jval.nullValue;
                        if(len == 4 && regionMatches(s, "true")) return Jval.trueValue;
                        break;
                    }
                    default:
                        if(first == '-' || first >= '0' && first <= '9'){
                            Jval n = tryParseNumber(buffer, start, stop, false);
                            if(n != null) return n;
                        }
                }
                if(isEol){
                    int end = stop;
                    //remove trailing comma
                    if(end > start && buffer[end - 1] == ',') end--;
                    //trim like String.trim() (<= 0x20), matching original .trim() behavior
                    int s = start, e = end;
                    while(s < e && isTrimChar(buffer[s])) s++;
                    while(e > s && isTrimChar(buffer[e - 1])) e--;
                    return new JsonString(new String(buffer, s, e - s));
                }
            }
        }
    }

    private boolean commaEndsValue(){
        int i = 0;
        boolean sawContent = false;
        while(i < commaLookaheadLimit){
            int ch = peek(i);
            if(ch < 0 || ch == '\n') return !sawContent;
            if(ch == ':' || ch == '}' || ch == ']') return true;
            if(isWhiteSpace(ch)){ i++; continue; }
            if(ch == '#'){
                i++;
                while(i < commaLookaheadLimit && peek(i) >= 0 && peek(i) != '\n') i++;
                continue;
            }
            if(ch == '/' && peek(i + 1) == '/'){
                i += 2;
                while(i < commaLookaheadLimit && peek(i) >= 0 && peek(i) != '\n') i++;
                continue;
            }
            if(ch == '/' && peek(i + 1) == '*'){
                i += 2;
                while(i < commaLookaheadLimit && peek(i) >= 0 && !(peek(i) == '*' && peek(i + 1) == '/')) i++;
                if(peek(i) >= 0) i += 2;
                continue;
            }
            sawContent = true;
            i++;
        }
        return false; // no resolution within the bound -> treat as literal, not a separator
    }

    static Jval tryParseNumber(char[] buf, int from, int to, boolean stopAtNext){
        int idx = from, len = to;
        if(idx < len && buf[idx] == '-') idx++;

        if(idx >= len) return null;
        char first = buf[idx++];
        if(!isDigit(first)) return null;

        if(first == '0' && idx < len && isDigit(buf[idx]))
            return null; // leading zero is not allowed

        while(idx < len && isDigit(buf[idx])) idx++;

        // frac
        if(idx < len && buf[idx] == '.'){
            idx++;
            if(idx >= len || !isDigit(buf[idx++])) return null;
            while(idx < len && isDigit(buf[idx])) idx++;
        }

        // exp
        if(idx < len && Character.toLowerCase(buf[idx]) == 'e'){
            idx++;
            if(idx < len && (buf[idx] == '+' || buf[idx] == '-')) idx++;
            if(idx >= len || !isDigit(buf[idx++])) return null;
            while(idx < len && isDigit(buf[idx])) idx++;
        }

        int last = idx;
        while(idx < len && isWhiteSpace(buf[idx])) idx++;

        boolean foundStop = false;
        if(idx < len && stopAtNext){
            char ch = buf[idx];
            if(ch == ',' || ch == '}' || ch == ']' || ch == '#' || ch == '/' && (len > idx + 1 && (buf[idx + 1] == '/' || buf[idx + 1] == '*')))
                foundStop = true;
        }

        if(idx < len && !foundStop) return null;

        boolean isDecimal = false;
        for(int i = from; i < last; i++){
            char c = buf[i];
            if(c == '.' || c == 'e' || c == 'E'){
                isDecimal = true;
                break;
            }
        }

        String str = new String(buf, from, last - from);

        if(!isDecimal){
            try{
                return new JsonLong(Long.parseLong(str));
            }catch(NumberFormatException ignored){
            }
        }

        return new JsonDouble(Double.parseDouble(str));
    }

    private boolean regionMatches(int off, String kw){
        int n = kw.length();
        for(int i = 0; i < n; i++) if(buffer[off + i] != kw.charAt(i)) return false;
        return true;
    }

    /** Matches String.trim()'s definition (chars <= 0x20), used where the original relied on .trim(). */
    private static boolean isTrimChar(char c){
        return c <= ' ';
    }

    private Jval readArray(){
        boolean previousInArray = inArray;
        inArray = true;
        read();
        JsonArray array = new JsonArray();
        skipWhiteSpace();
        if(readIf(']')){
            inArray = previousInArray;
            return array;
        }
        while(true){
            skipWhiteSpace();
            array.add(readValue());
            skipWhiteSpace();
            if(readIf(',')) skipWhiteSpace(); // , is optional
            if(readIf(']')) break;
            else if(isEndOfText()) throw error("End of input while parsing an array (did you forget a closing ']'?)");
        }
        inArray = previousInArray;
        return array;
    }

    private Jval readObject(boolean objectWithoutBraces){
        boolean previousInArray = inArray;
        inArray = false; // object values use commaEndsValue() instead, regardless of outer context
        if(!objectWithoutBraces) read();
        JsonMap object = new JsonMap();
        skipWhiteSpace();
        while(true){
            if(objectWithoutBraces){
                if(isEndOfText()) break;
            }else{
                if(isEndOfText()) throw error("End of input while parsing an object (did you forget a closing '}'?)");
                if(readIf('}')) break;
            }
            String name = readName();
            skipWhiteSpace();
            if(!readIf(':')){
                throw expected("':'");
            }
            skipWhiteSpace();
            object.putAdd(name, readValue());
            skipWhiteSpace();
            if(readIf(',')) skipWhiteSpace(); // , is optional
        }
        inArray = previousInArray;
        return object;
    }

    private String readName(){
        if(current == '"' || current == '\'') return readStringInternal(false);

        StringBuilder name = new StringBuilder();
        int space = -1, start = index;
        while(true){
            if(current == ':'){
                if(name.length() == 0) throw error("Found ':' but no key name (for an empty key name use quotes)");
                else if(space >= 0 && space != name.length()){
                    index = start + space;
                    throw error("Found whitespace in your key name (use quotes to include)");
                }
                return name.toString();
            }else if(isWhiteSpace(current)){
                if(space < 0) space = name.length();
            }else if(current < ' '){
                throw error("Name is not closed");
            }else if(JvalWriter.isPunctuatorChar(current)){
                throw error("Found '" + (char)current + "' where a key name was expected (check your syntax or use quotes if the key name includes {}[],: or whitespace)");
            }else name.append((char)current);
            read();
        }
    }

    private String readMlString(){

        // Parse a multiline string value.
        StringBuilder sb = new StringBuilder();
        int triple = 0;

        // we are at '''
        int indent = index - lineOffset - 4;

        // skip white/to (newline)
        while(true){
            if(isWhiteSpace(current) && current != '\n') read();
            else break;
        }
        if(current == '\n'){
            read();
            skipIndent(indent);
        }

        // When parsing for string values, we must look for " and \ characters.
        while(true){
            if(current < 0) throw error("Bad multiline string");
            else if(current == '\''){
                triple++;
                read();
                if(triple == 3){
                    if(sb.charAt(sb.length() - 1) == '\n') sb.deleteCharAt(sb.length() - 1);

                    return sb.toString();
                }else continue;
            }else{
                while(triple > 0){
                    sb.append('\'');
                    triple--;
                }
            }
            if(current == '\n'){
                sb.append('\n');
                read();
                skipIndent(indent);
            }else{
                if(current != '\r') sb.append((char)current);
                read();
            }
        }
    }

    private void skipIndent(int indent){
        while(indent-- > 0){
            if(isWhiteSpace(current) && current != '\n') read();
            else break;
        }
    }

    private Jval readString(){
        return new JsonString(readStringInternal(true));
    }

    private String readStringInternal(boolean allowML){
        // callees make sure that (current=='"' || current=='\'')
        int exitCh = current;
        read();
        startCapture();
        while(current >= 0 && current != exitCh){
            if(current == '\\') readEscape();
            else read();
        }
        String string = endCapture();
        read();

        if(allowML && exitCh == '\'' && current == '\'' && string.length() == 0){
            // ''' indicates a multiline string
            read();
            return readMlString();
        }else return string;
    }

    private void startCapture(){
        captureStart = index - 1; // current already holds buffer[index-1]
        rawStart = captureStart;
        escaped = false;
    }

    private void readEscape(){
        // flush the raw (unescaped) run seen so far, up to this backslash
        int backslashPos = index - 1;
        if(captureBuffer == null) captureBuffer = new StringBuilder(32);
        captureBuffer.append(buffer, rawStart, backslashPos - rawStart);
        escaped = true;

        read(); // consume '\', current -> escape designator
        switch(current){
            case '"':
            case '\'':
            case '#':
            case '/':
            case '\\':
                captureBuffer.append((char)current);
                break;
            case 'b':
                captureBuffer.append('\b');
                break;
            case 'f':
                captureBuffer.append('\f');
                break;
            case 'n':
                captureBuffer.append('\n');
                break;
            case 'r':
                captureBuffer.append('\r');
                break;
            case 't':
                captureBuffer.append('\t');
                break;
            case 'u':
                char[] hexChars = new char[4];
                for(int i = 0; i < 4; i++){
                    read();
                    if(!isHexDigit()){
                        throw expected("hexadecimal digit");
                    }
                    hexChars[i] = (char)current;
                }
                captureBuffer.append((char)Integer.parseInt(new String(hexChars), 16));
                break;
            default:
                throw expected("valid escape sequence");
        }
        read(); // advance past the escape sequence
        rawStart = index - 1; // next raw run starts here
    }

    private String endCapture(){
        int end = index - 1; // current is exitCh (or -1 on unterminated input, pre-existing edge case)
        String result;
        if(escaped){
            captureBuffer.append(buffer, rawStart, end - rawStart);
            result = captureBuffer.toString();
            captureBuffer.setLength(0); // reuse the builder for the next escaped string, if any
        }else{
            result = new String(buffer, captureStart, end - captureStart); // single copy, common case
        }
        return result;
    }

    private static boolean isDigit(char ch){
        return ch >= '0' && ch <= '9';
    }

    static Jval tryParseNumber(StringBuilder value, boolean stopAtNext){
        int idx = 0, len = value.length();
        if(idx < len && value.charAt(idx) == '-') idx++;

        if(idx >= len) return null;
        char first = value.charAt(idx++);
        if(!isDigit(first)) return null;

        if(first == '0' && idx < len && isDigit(value.charAt(idx)))
            return null; // leading zero is not allowed

        while(idx < len && isDigit(value.charAt(idx))) idx++;

        // frac
        if(idx < len && value.charAt(idx) == '.'){
            idx++;
            if(idx >= len || !isDigit(value.charAt(idx++))) return null;
            while(idx < len && isDigit(value.charAt(idx))) idx++;
        }

        // exp
        if(idx < len && Character.toLowerCase(value.charAt(idx)) == 'e'){
            idx++;
            if(idx < len && (value.charAt(idx) == '+' || value.charAt(idx) == '-')) idx++;

            if(idx >= len || !isDigit(value.charAt(idx++))) return null;
            while(idx < len && isDigit(value.charAt(idx))) idx++;
        }

        int last = idx;
        while(idx < len && isWhiteSpace(value.charAt(idx))) idx++;

        boolean foundStop = false;
        if(idx < len && stopAtNext){
            char ch = value.charAt(idx);
            if(ch == ',' || ch == '}' || ch == ']' || ch == '#' || ch == '/' && (len > idx + 1 && (value.charAt(idx + 1) == '/' || value.charAt(idx + 1) == '*')))
                foundStop = true;
        }

        if(idx < len && !foundStop) return null;

        boolean isDecimal = false;
        for(int i = 0; i < last; i++){
            char c = value.charAt(i);
            if(c == '.' || c == 'e' || c == 'E'){
                isDecimal = true;
                break;
            }
        }

        String str = value.substring(0, last);

        if(!isDecimal){
            try{
                return new JsonLong(Long.parseLong(str));
            }catch(NumberFormatException ignored){
            }
        }

        return new JsonDouble(Double.parseDouble(str));
    }

    static Jval tryParseNumber(String value){
        return tryParseNumber(new StringBuilder(value), true);
    }

    private boolean readIf(char ch){
        if(current != ch){
            return false;
        }
        read();
        return true;
    }

    private void skipWhiteSpace(){
        while(!isEndOfText()){
            while(isWhiteSpace()) read();
            if(current == '#' || current == '/' && peek() == '/'){
                do{
                    read();
                }while(current >= 0 && current != '\n');
            }else if(current == '/' && peek() == '*'){
                read();
                do{
                    read();
                }while(current >= 0 && !(current == '*' && peek() == '/'));
                read();
                read();
            }else break;
        }
    }

    private void read(){
        if(current == '\n'){
            line++;
            lineOffset = index;
        }
        current = index < bufferLength ? buffer[index++] : -1;
    }

    private int peek(int idx){
        int p = index + idx;
        return p < bufferLength ? buffer[p] : -1;
    }

    private int peek(){
        return peek(0);
    }

    private JsonParseException expected(String expected){
        if(isEndOfText()){
            return error("Unexpected end of input");
        }
        return error("Expected " + expected);
    }

    private JsonParseException error(String message){
        int column = index - lineOffset;
        int offset = isEndOfText() ? index : index - 1;
        return new JsonParseException(message, offset, line, column - 1);
    }

    private boolean isWhiteSpace(){
        return isWhiteSpace((char)current);
    }

    private boolean isHexDigit(){
        return current >= '0' && current <= '9'
        || current >= 'a' && current <= 'f'
        || current >= 'A' && current <= 'F';
    }

    private boolean isEndOfText(){
        return current == -1;
    }
}