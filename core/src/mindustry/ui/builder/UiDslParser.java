package mindustry.ui.builder;

import arc.util.*;
import arc.util.serialization.*;
import mindustry.ui.builder.UiBuilder.*;

/** Parses String -> UiBuilders. */
public class UiDslParser{
    private final String src;
    private int pos;
    private boolean lastQuoted; // whether the most recently parsed value was a quoted string

    private UiDslParser(String src){
        this.src = src;
    }

    /** Parses source text into a root TableBuilder (the implicit root table's body). */
    public static TableBuilder parse(String source){
        TableBuilder root = new TableBuilder();
        UiDslParser p = new UiDslParser(source);
        p.parseStatementsInto(root);
        p.skipWs();
        if(!p.atEnd()) throw new SerializationException("Unexpected character '" + p.peek() + "' at index " + p.pos);
        return root;
    }

    private void parseStatementsInto(NodeBuilder<?> node){
        skipWs();
        while(!atEnd() && peek() != '}'){
            parseStatement(node);
            skipWs();
        }
    }

    private void parseStatement(NodeBuilder<?> node){
        skipWs();
        if(atEnd() || peek() == '"') throw new SerializationException("Expected identifier at index " + pos);
        String ident = readBareToken();
        if(ident.isEmpty()) throw new SerializationException("Unexpected character '" + peek() + "' at index " + pos);

        if(ident.equals("row")){
            node.entries.add(new Entry(UiKey.row, true));
            return;
        }

        //TODO: throws on the client upon invalid value, that's probably fine?
        UiKey key = UiKey.valueOf(ident);

        //TODO hack: everything before the row ordinal is a node type
        boolean isNodeType = key.ordinal() < UiKey.row.ordinal();
        skipWs();

        //"ident: value" - shorthand node (label: "text") or plain property (width: 100)
        if(!atEnd() && peek() == ':'){
            pos++;
            String raw = parseValue();

            if(isNodeType){
                NodeBuilder<?> child = NodeBuilder.create(key);
                child.entries.add(new Entry(getShorthandType(key), raw));
                skipWs();
                if(!atEnd() && peek() == '{'){
                    pos++;
                    parseStatementsInto(child);
                    expect('}');
                }
                node.entries.add(new Entry(child.type, child));
            }else{
                node.entries.add(new Entry(key, coerce(raw)));
            }
            return;
        }

        // "ident { ... }" - a node with a block
        if(!atEnd() && peek() == '{'){
            pos++;
            if(!isNodeType) throw new SerializationException("Unknown node type '" + ident + "' at index " + pos);
            NodeBuilder<?> child = NodeBuilder.create(key);
            parseStatementsInto(child);
            expect('}');
            node.entries.add(new Entry(child.type, child));
            return;
        }

        // bare "ident" - childless node (e.g. "space")
        if(!isNodeType) throw new SerializationException("Unknown node type '" + ident + "' at index " + pos);
        node.entries.add(new Entry(key, NodeBuilder.create(key)));
    }

    /** for statements such as image: "cat", the key type is region, not text. */
    private static UiKey getShorthandType(UiKey key){
        return switch(key){
            case image -> UiKey.region;
            default -> UiKey.text;
        };
    }

    /** Converts a parsed bare token into the most specific type it looks like; quoted strings stay strings. */
    private Object coerce(String raw){
        if(lastQuoted) return raw;
        if(raw.equals("true")) return Boolean.TRUE;
        if(raw.equals("false")) return Boolean.FALSE;

        float value = Strings.parseFloat(raw, Float.NaN);
        if(!Float.isNaN(value)){
            return value;
        }else{
            return raw;
        }
    }

    private String parseValue(){
        skipWs();
        if(!atEnd() && peek() == '"'){
            lastQuoted = true;
            return parseString();
        }
        lastQuoted = false;
        String tok = readBareToken();
        if(tok.isEmpty()) throw new SerializationException("Expected value at index " + pos);
        return tok;
    }

    private String parseString(){
        pos++; // opening quote
        StringBuilder sb = new StringBuilder();
        while(!atEnd() && peek() != '"'){
            char c = src.charAt(pos);
            if(c == '\\' && pos + 1 < src.length()){
                char next = src.charAt(pos + 1);
                sb.append(next == 'n' ? '\n' : next); //parse \n
                pos += 2;
                continue;
            }
            sb.append(c);
            pos++;
        }
        if(atEnd()) throw new SerializationException("Unterminated string");
        pos++; // closing quote
        return sb.toString();
    }

    // reads until whitespace/brace/colon; covers identifiers, numbers, and bare-word values alike
    private String readBareToken(){
        int start = pos;
        while(!atEnd()){
            char c = src.charAt(pos);
            if(Character.isWhitespace(c) || c == '{' || c == '}' || c == ':') break;
            pos++;
        }
        return src.substring(start, pos);
    }

    private void skipWs(){
        while(!atEnd()){
            char c = src.charAt(pos);
            if(Character.isWhitespace(c)){ pos++; continue; }
            if(c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == '/'){ // line comment
                while(!atEnd() && src.charAt(pos) != '\n') pos++;
                continue;
            }
            break;
        }
    }

    private void expect(char c){
        if(atEnd() || peek() != c) throw new SerializationException("Expected '" + c + "', found '" + (atEnd() ? "<eof>" : peek()) + "'");
        pos++;
    }

    private boolean atEnd(){
        return pos >= src.length();
    }

    private char peek(){
        return src.charAt(pos);
    }
}