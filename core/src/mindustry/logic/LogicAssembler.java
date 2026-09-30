package mindustry.logic;

import arc.func.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;

/** "Compiles" a sequence of statements into instructions. */
public class LogicAssembler{
    public static ObjectMap<String, Func<String[], LogicStatement>> customParsers = new ObjectMap<>();

    public boolean privileged;
    /** Maps names to variable. */
    public OrderedMap<String, LogicVar> vars = new OrderedMap<>();
    /** All instructions to be executed. */
    public LogicInstruction[] instructions;

    public LogicAssembler(){
        //instruction counter
        putVar("@counter").isobj = false;
        //currently controlled unit
        putConst("@unit", null);
        //reference to self
        putConst("@this", null);
    }

    public static LogicAssembler assemble(String data, boolean privileged){
        LogicAssembler asm = new LogicAssembler();
        Seq<LogicStatement> st = read(data, privileged);

        asm.privileged = privileged;

        asm.instructions = st.map(l -> l.build(asm)).retainAll(l -> l != null).toArray(LogicInstruction.class);
        return asm;
    }

    public static String write(Seq<LogicStatement> statements){
        StringBuilder out = new StringBuilder();
        for(LogicStatement s : statements){
            s.write(out);
            out.append("\n");
        }

        return out.toString();
    }

    /** Parses a sequence of statements from a string. */
    public static Seq<LogicStatement> read(String text, boolean privileged){
        //don't waste time parsing null/empty text
        if(text == null || text.isEmpty()) return new Seq<>();
        return new LogicParser(text, privileged).parse();
    }

    /**
     * @return a variable by name. This may be a constant variable referring to a number or object.
     * @param symbol the string literal, numeric literal, or variable name. Leading or trailing spaces are not allowed.
     * */
    public LogicVar var(String symbol){
        LogicVar constVar = Vars.logicVars.get(symbol, privileged);
        if(constVar != null) return constVar;

        //string case
        if(symbol.length() > 1 && symbol.charAt(0) == '\"' && symbol.charAt(symbol.length() - 1) == '\"'){
            return putConst("___" + symbol, unescape(symbol.substring(1, symbol.length() - 1)));
        }

        //use a positive invalid number if number might be negative, else use a negative invalid number
        double value = parseDouble(symbol);

        if(Double.isNaN(value)){
            return putVar(symbol);
        }else{
            if(Double.isInfinite(value)) value = 0.0;
            //this creates a hidden const variable with the specified value
            return putConst("___" + value, value);
        }
    }

    /** Decodes \n, \", \\ and uXXXX escape sequences in a string literal's contents (quotes already stripped). */
    static String unescape(String s){
        if(s.indexOf('\\') == -1) return s;

        StringBuilder out = new StringBuilder(s.length());
        for(int i = 0; i < s.length(); i++){
            char c = s.charAt(i);
            if(c == '\\' && i + 1 < s.length()){
                char next = s.charAt(i + 1);
                if(next == 'n'){
                    out.append('\n');
                    i ++;
                    continue;
                }else if(next == '"' || next == '\\'){
                    out.append(next);
                    i ++;
                    continue;
                }else if(next == 'u' && i + 5 < s.length()){
                    out.append((char)Integer.parseInt(s.substring(i + 2, i + 6), 16));
                    i += 5;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    double parseDouble(String symbol){
        //fail fast for obvious non-numbers
        if(symbol.isEmpty() || !isNumStart(symbol.charAt(0))) return Double.NaN;

        //parse hex/binary syntax
        if(symbol.startsWith("0b")) return parseHexOrBin(false, symbol, true, 2);
        if(symbol.startsWith("+0b")) return parseHexOrBin(false, symbol, true, 3);
        if(symbol.startsWith("-0b")) return parseHexOrBin(true, symbol, true, 3);
        if(symbol.startsWith("0x")) return parseHexOrBin(false, symbol, false, 2);
        if(symbol.startsWith("+0x")) return parseHexOrBin(false, symbol, false, 3);
        if(symbol.startsWith("-0x")) return parseHexOrBin(true, symbol, false, 3);
        if(symbol.startsWith("%[") && symbol.endsWith("]") && symbol.length() > 3) return parseNamedColor(symbol);
        if(symbol.startsWith("%") && (symbol.length() == 7 || symbol.length() == 9)) return parseColor(symbol);

        return Strings.parseDouble(symbol, Double.NaN);
    }

    boolean isNumStart(char c){
        //note that 'e10' isn't a valid number; '%ffffff' is. Hex numbers start with '0x'.
        return c >= '0' && c <= '9' || c == '.' || c == '-' || c == '+' || c == '%';
    }

    //parses *unsigned* hex or bin number, including negative ones (0xffffffffffffffff as -1)
    //detects overflow by input length and uses bit manipulation to avoid signed arithmetics
    double parseHexOrBin(boolean negative, String s, boolean binary, int offset){
        int end = s.length();
        if(offset >= end) return Double.NaN;

        int pos = offset;
        while(pos < end && s.charAt(pos) == '0') pos ++;    //skip leading zeros to avoid incorrect overflow detection

        int shift = binary ? 1 : 4;
        if(end - pos > 64 / shift) return Double.NaN;

        long acc = 0;
        int radix = 1 << shift;
        while(pos < end){
            int digit = Character.digit(s.charAt(pos), radix);
            if(digit < 0) return Double.NaN;
            acc = acc << shift | digit;
            pos ++;
        }
        return negative ? -acc : acc;
    }

    double parseColor(String symbol){
        int
        r = Strings.parseInt(symbol, 16, 0, 1, 3),
        g = Strings.parseInt(symbol, 16, 0, 3, 5),
        b = Strings.parseInt(symbol, 16, 0, 5, 7),
        a = symbol.length() == 9 ? Strings.parseInt(symbol, 16, 0, 7, 9) : 255;

        return Color.toDoubleBits(r, g, b, a);
    }

    double parseNamedColor(String symbol){
        Color color = Colors.get(symbol.substring(2, symbol.length() - 1));

        return color == null ? Double.NaN : color.toDoubleBits();
    }

    /** Adds a constant value by name. */
    public LogicVar putConst(String name, Object value){
        LogicVar var = putVar(name);
        if(value instanceof Number number){
            var.isobj = false;
            var.numval = number.doubleValue();
            var.objval = null;
        }else{
            var.isobj = true;
            var.objval = value;
        }
        var.constant = true;
        return var;
    }

    /** Registers a variable name mapping. */
    public LogicVar putVar(String name){
        if(vars.containsKey(name)){
            return vars.get(name);
        }else{
            //variables are null objects by default
            LogicVar var = new LogicVar(name);
            var.isobj = true;
            vars.put(name, var);
            return var;
        }
    }

    @Nullable
    public LogicVar getVar(String name){
        return vars.get(name);
    }

}
