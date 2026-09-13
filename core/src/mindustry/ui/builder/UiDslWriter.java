package mindustry.ui.builder;

import arc.struct.*;
import arc.util.*;
import mindustry.ui.builder.UiBuilder.*;

/** Converts a parsed NodeBuilder tree back into UI DSL source text. */
public class UiDslWriter{

    /** Writes the root node's entries (no wrapping block, since root is an implicit table body). */
    public static String write(NodeBuilder<?> root){
        StringBuilder sb = new StringBuilder();
        writeEntries(sb, root.entries, 0);
        return sb.toString();
    }

    private static void writeEntries(StringBuilder sb, Seq<Entry> entries, int depth){
        for(Entry e : entries){
            // "row" marker
            if(e.key == UiKey.row && e.value instanceof Boolean){
                indent(sb, depth);
                sb.append("row\n");
                continue;
            }

            // nested node (child added via block, shorthand, or bare)
            if(e.value instanceof NodeBuilder<?> child){
                writeChildNode(sb, child, depth);
                continue;
            }

            // plain property: key: value
            indent(sb, depth);
            sb.append(e.key.name()).append(": ").append(formatValue(e.value)).append('\n');
        }
    }

    private static void writeChildNode(StringBuilder sb, NodeBuilder<?> child, int depth){
        indent(sb, depth);
        sb.append(child.type.name());

        UiKey shorthandKey = shorthandType(child.type);
        Entry shorthand = null;
        for(Entry e : child.entries){
            if(e.key == shorthandKey && e.value instanceof String){
                shorthand = e;
                break;
            }
        }

        Seq<Entry> rest = new Seq<>();
        for(Entry e : child.entries){
            if(e != shorthand) rest.add(e);
        }

        if(shorthand != null){
            sb.append(": ").append(formatValue(shorthand.value));
        }

        if(rest.isEmpty()){
            sb.append('\n');
        }else{
            if(shorthand != null) sb.append(' ');
            sb.append("{\n");
            writeEntries(sb, rest, depth + 1);
            indent(sb, depth);
            sb.append("}\n");
        }
    }

    /** Mirrors UiDslParser's getShorthandType: image uses region, everything else uses text. */
    private static UiKey shorthandType(UiKey key){
        return switch(key){
            case image -> UiKey.region;
            default -> UiKey.text;
        };
    }

    private static String formatValue(Object value){
        if(value instanceof Boolean b) return b ? "true" : "false";
        if(value instanceof Float f) return formatFloat(f);
        if(value instanceof String s) return needsQuotes(s) ? quote(s) : s;
        throw new IllegalArgumentException("Unsupported value type: " + value);
    }

    private static String formatFloat(float f){
        if(f == (long)f) return Long.toString((long)f);
        return Float.toString(f);
    }

    /** A bare token round-trips correctly only if the parser wouldn't reinterpret it as a bool/number/keyword. */
    private static boolean needsQuotes(String s){
        if(s.isEmpty()) return true;
        if(s.equals("true") || s.equals("false")) return true;
        if(!Float.isNaN(Strings.parseFloat(s, Float.NaN))) return true;

        for(int i = 0; i < s.length(); i++){
            char c = s.charAt(i);
            if(Character.isWhitespace(c) || c == '{' || c == '}' || c == ':' || c == '"' || c == '\\'){
                return true;
            }
        }
        // avoid accidentally starting a line comment
        return s.startsWith("//");
    }

    private static String quote(String s){
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for(int i = 0; i < s.length(); i++){
            char c = s.charAt(i);
            if(c == '"' || c == '\\'){
                sb.append('\\').append(c);
            }else if(c == '\n'){
                sb.append("\\n");
            }else{
                sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static void indent(StringBuilder sb, int depth){
        for(int i = 0; i < depth; i++) sb.append("  ");
    }
}