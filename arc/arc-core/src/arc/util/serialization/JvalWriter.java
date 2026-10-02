package arc.util.serialization;

import arc.struct.*;
import arc.util.serialization.Jval.*;

import java.io.*;

class JvalWriter{

    static void writeJson(Jval value, boolean format, boolean includeQuotes, Writer tw, int level) throws IOException{
        boolean following = false;
        switch(value.getType()){
            case object:
                JsonMap obj = value.asObject();
                tw.write('{');
                for(ObjectMap.Entry<String, Jval> pair : obj){
                    if(following) tw.write(",");
                    if(format) nl(tw, level + 1);
                    if(includeQuotes){
                        tw.write('\"');
                        tw.write(escapeString(pair.key));
                        tw.write("\":");
                    }else{
                        tw.write(escapeName(pair.key));
                        tw.write(':');
                    }

                    Jval v = pair.value;
                    Jtype vType = v.getType();
                    if(format && vType != Jtype.array && vType != Jtype.object) tw.write(" ");
                    writeJson(v, format, includeQuotes, tw, level + 1);
                    following = true;
                }
                if(following && format) nl(tw, level);
                tw.write('}');
                break;
            case array:
                JsonArray arr = value.asArray();
                int n = arr.size;
                tw.write('[');
                for(int i = 0; i < n; i++){
                    if(following) tw.write(",");
                    Jval v = arr.get(i);
                    Jtype vType = v.getType();
                    if(vType != Jtype.array && format) nl(tw, level + 1);
                    writeJson(v, format, includeQuotes, tw, level + 1);
                    following = true;
                }
                if(following && format) nl(tw, level);
                tw.write(']');
                break;
            case bool:
                tw.write(value.isTrue() ? "true" : "false");
                break;
            case string:
                if(includeQuotes){
                    tw.write('"');
                    tw.write(escapeString(value.asString()));
                    tw.write('"');
                }else{
                    tw.write(escapeName(value.asString()));
                }

                break;
            default:
                tw.write(value.toString());
                break;
        }
    }

    static void writeHjson(Jval value, Writer tw, int level, String separator, boolean noIndent) throws IOException{
        if(value == null){
            tw.write(separator);
            tw.write("null");
            return;
        }

        switch(value.getType()){
            case object:
                JsonMap obj = value.asObject();
                if(!noIndent){
                    tw.write(" ");
                }
                if(level >= 0) tw.write('{');
                int index = 0;

                for(ObjectMap.Entry<String, Jval> pair : obj){
                    if(!(index++ == 0 && level < 0)) nl(tw, level + 1);
                    tw.write(escapeName(pair.key));
                    tw.write(":");
                    writeHjson(pair.value, tw, level + 1, " ", false);
                }

                if(obj.size > 0) nl(tw, level);
                if(level >= 0) tw.write('}');
                break;
            case array:
                JsonArray arr = value.asArray();
                int n = arr.size;
                if(!noIndent){
                    tw.write(" ");
                }
                tw.write('[');
                for(int i = 0; i < n; i++){
                    nl(tw, level + 1);
                    writeHjson(arr.get(i), tw, level + 1, "", true);
                }
                if(n > 0) nl(tw, level);
                tw.write(']');
                break;
            case bool:
                tw.write(separator);
                tw.write(value.isTrue() ? "true" : "false");
                break;
            case string:
                writeString(value.asString(), tw, level, separator);
                break;
            default:
                tw.write(separator);
                tw.write(value.toString());
                break;
        }
    }

    static String escapeName(String name){
        if(name.length() == 0 || needsEscapeName(name))
            return "\"" + escapeString(name) + "\"";
        else
            return name;
    }

    static boolean needsEscapeName(String name){
        int len = name.length();
        for(int i = 0; i < len; i++){
            char c = name.charAt(i);
            switch(c){
                case ',': case '{': case '[': case '}': case ']': case ':': case '"': case '\'':
                case ' ': case '\t': case '\n': case '\u000B': case '\f': case '\r': case '#': case '/':
                    return true;
            }
        }
        return false;
    }

    static void writeString(String value, Writer tw, int level, String separator) throws IOException{
        int len = value.length();
        if(len == 0){
            tw.write(separator);
            tw.write("\"\"");
            return;
        }

        char left = value.charAt(0), right = value.charAt(len - 1);
        char left1 = len > 1 ? value.charAt(1) : '\0';

        if(JvalReader.isWhiteSpace(left) || JvalReader.isWhiteSpace(right) ||
        left == '"' ||
        left == '\'' ||
        left == '#' ||
        left == '/' && (left1 == '*' || left1 == '/') ||
        isPunctuatorChar(left) ||
        startsWithKeyword(value) ||
        JvalReader.tryParseNumber(value) != null ||
        containsQuoteChar(value)){

            boolean noEscape = true, noEscapeML = true, allWhite = true;
            for(int i = 0; i < len && (noEscape || noEscapeML || allWhite); i++){
                char ch = value.charAt(i);
                if(noEscape && needsEscape(ch)) noEscape = false;
                if(noEscapeML && needsEscapeML(ch)) noEscapeML = false;
                if(allWhite && !JvalReader.isWhiteSpace(ch)) allWhite = false;
            }

            if(noEscape){
                tw.write(separator);
                tw.write('"');
                tw.write(value);
                tw.write('"');
                return;
            }

            if(noEscapeML && !allWhite && value.indexOf("'''") < 0) writeMLString(value, tw, level, separator);
            else{
                tw.write(separator);
                tw.write('"');
                tw.write(escapeString(value));
                tw.write('"');
            }
        }else{
            tw.write(separator);
            tw.write(value);
        }
    }

    static boolean containsQuoteChar(String value){
        int len = value.length();
        for(int i = 0; i < len; i++){
            if(needsQuotes(value.charAt(i))) return true;
        }
        return false;
    }

    static void writeMLString(String value, Writer tw, int level, String separator) throws IOException{
        String[] lines = value.replace("\r", "").split("\n", -1);

        if(lines.length == 1){
            tw.write(separator + "'''");
            tw.write(lines[0]);
            tw.write("'''");
        }else{
            level++;
            nl(tw, level);
            tw.write("'''");

            for(String line : lines){
                nl(tw, line.length() > 0 ? level : 0);
                tw.write(line);
            }
            nl(tw, level);
            tw.write("'''");
        }
    }

    static boolean startsWithKeyword(String text){
        int p;
        if(text.startsWith("true") || text.startsWith("null")) p = 4;
        else if(text.startsWith("false")) p = 5;
        else return false;
        while(p < text.length() && JvalReader.isWhiteSpace(text.charAt(p))) p++;
        if(p == text.length()) return true;
        char ch = text.charAt(p);
        return ch == ',' || ch == '}' || ch == ']' || ch == '#' || ch == '/' && (text.length() > p + 1 && (text.charAt(p + 1) == '/' || text.charAt(p + 1) == '*'));
    }

    static boolean isPunctuatorChar(int c){
        return c == '{' || c == '}' || c == '[' || c == ']' || c == ',' || c == ':';
    }

    static boolean needsQuotes(char c){
        return c == '\t' || c == '\f' || c == '\b' || c == '\n' || c == '\r' || c == ']' || c == '[' || c == ',' || c == '/' || c == '#' || c == ':';
    }

    static boolean needsEscape(char c){
        return c == '\"' || c == '\\' || needsQuotes(c);
    }

    static boolean needsEscapeML(char c){
        switch(c){
            case '\n':
            case '\r':
            case '\t':
                return false;
            default:
                return needsQuotes(c);
        }
    }

    static void nl(Writer tw, int level) throws IOException{
        tw.write('\n');
        for(int i = 0; i < level; i++) tw.write("  ");
    }

    static String escapeString(String src){
        if(src == null) return null;

        for(int i = 0; i < src.length(); i++){
            if(getEscapedChar(src.charAt(i)) != null){
                StringBuilder sb = new StringBuilder();
                if(i > 0) sb.append(src, 0, i);
                return doEscapeString(sb, src, i);
            }
        }
        return src;
    }

    private static String doEscapeString(StringBuilder sb, String src, int cur){
        int start = cur;
        for(int i = cur; i < src.length(); i++){
            String escaped = getEscapedChar(src.charAt(i));
            if(escaped != null){
                sb.append(src, start, i);
                sb.append(escaped);
                start = i + 1;
            }
        }
        sb.append(src, start, src.length());
        return sb.toString();
    }

    private static String getEscapedChar(char c){
        switch(c){
            case '\"': return "\\\"";
            case '\t': return "\\t";
            case '\n': return "\\n";
            case '\r': return "\\r";
            case '\f': return "\\f";
            case '\b': return "\\b";
            case '\\': return "\\\\";
            default: return null;
        }
    }
}
