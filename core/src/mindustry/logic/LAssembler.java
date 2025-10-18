package mindustry.logic;

import arc.func.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.logic.LExecutor.*;

/** "Compiles" a sequence of statements into instructions. */
public class LAssembler{
    public static ObjectMap<String, Func<String[], LStatement>> customParsers = new ObjectMap<>();

    private static final long invalidNumNegative = Long.MIN_VALUE;
    private static final long invalidNumPositive = Long.MAX_VALUE;

    private boolean privileged;
    /** Maps names to variable. */
    public OrderedMap<String, LVar> vars = new OrderedMap<>();
    /** All instructions to be executed. */
    public LInstruction[] instructions;

    public LAssembler(){
        //instruction counter
        putVar("@counter").isobj = false;
        //currently controlled unit
        putConst("@unit", null);
        //reference to self
        putConst("@this", null);
    }

    public static LAssembler assemble(String data, boolean privileged){
        LAssembler asm = new LAssembler();

        Seq<LStatement> st = read(data, privileged);

        asm.privileged = privileged;

        asm.instructions = st.map(l -> l.build(asm)).retainAll(l -> l != null).toArray(LInstruction.class);
        return asm;
    }

    public static String write(Seq<LStatement> statements){
        StringBuilder out = new StringBuilder();
        for(LStatement s : statements){
            s.write(out);
            out.append("\n");
        }

        return out.toString();
    }

    /** Parses a sequence of statements from a string. */
    public static Seq<LStatement> read(String text, boolean privileged){
        //don't waste time parsing null/empty text
        if(text == null || text.isEmpty()) return new Seq<>();
        return new LParser(text, privileged).parse();
    }

    /** @return a variable by name.
     * This may be a constant variable referring to a number or object. */
    public LVar var(String symbol){
        LVar constVar = Vars.logicVars.get(symbol, privileged);
        if(constVar != null) return constVar;

        //Parse escape codes, loosely based on C escape codes (with some changes)
        int tailSpaces = 0;
        boolean string = false, frontTrimmed = false, lastQuoteEsc = false;
        StringBuilder unescapedSymbol = new StringBuilder(symbol.length());
        for(int i = 0; i < symbol.length(); i++){
            if(symbol.charAt(i) <= ' '){
                if(!frontTrimmed) continue;
                tailSpaces++;
            }else{
                if(!frontTrimmed && symbol.charAt(i) == '"') string = true;
                frontTrimmed = true;
                tailSpaces = 0;
            }
            if(symbol.charAt(i) != '\\'){
                if(symbol.charAt(i) == '"') lastQuoteEsc = false;
                unescapedSymbol.append(symbol.charAt(i));
            }else if(i < symbol.length() - 1){
                unescapedSymbol.append(switch(symbol.charAt(++i)){
                    case '\\', '#', ';', '\t' -> symbol.charAt(i);
                    case 'n' -> '\n';
                    case '"' -> {
                        lastQuoteEsc = true;
                        yield symbol.charAt(i);
                    }
                    case ' ' -> {
                        tailSpaces--;
                        yield ' ';
                    }
                    case 'x' -> {
                        char chr = ++i < symbol.length() ? symbol.charAt(i) : 0;
                        if((chr >= '0' && chr <= '9') || (chr >= 'A' && chr <= 'F') || (chr >= 'a' && chr <= 'f')){
                            int code = 0;
                            int bits = 0;
                            while(((chr >= '0' && chr <= '9') || (chr >= 'A' && chr <= 'F') || (chr >= 'a' && chr <= 'f')) && bits < 16){
                                code = code << 4 | switch(chr){
                                    case 'A', 'B', 'C', 'D', 'E', 'F' -> chr - 'A' + 10;
                                    case 'a', 'b', 'c', 'd', 'e', 'f' -> chr - 'a' + 10;
                                    default -> chr - '0';
                                };
                                bits += 4;
                                chr = ++i < symbol.length() ? symbol.charAt(i) : 0;
                            }
                            i--;
                            yield (char)code;
                        }
                        unescapedSymbol.append("\\x");
                        yield chr;
                    }
                    default -> {
                        char chr = symbol.charAt(i);
                        //Octal case, unlike C can use more than 3 digits.
                        if(chr >= '0' && chr < '8'){
                            int code = 0;
                            int bits = 0;
                            while(chr >= '0' && chr < '8' && bits < 16){
                                code = code << 3 | chr - '0';
                                bits += 3;
                                chr = ++i < symbol.length() ? symbol.charAt(i) : 0;
                            }
                            i--;
                            yield (char)code;
                        }
                        unescapedSymbol.append('\\');
                        yield chr;
                    }
                });
            }else{
                unescapedSymbol.append('\\');
            }
        }
        String unescaped = unescapedSymbol.substring(0, unescapedSymbol.length() - Math.max(tailSpaces, 0));
        //Potentially a string
        string: if(string && unescaped.length() >= 2 && !lastQuoteEsc){
            if(unescaped.charAt(unescaped.length() - 1) != '"') break string;
            boolean escaped = false;
            for(int i = unescaped.length() - 2; i > 1; i--){
                if(unescaped.charAt(i) != '\\') break;
                escaped = !escaped;
            }
            if(escaped) break string;
            return putConst("___" + unescaped, unescaped.substring(1, unescaped.length() - 1));
        }

        //use a positive invalid number if number might be negative, else use a negative invalid number
        double value = parseDouble(unescaped);

        if(Double.isNaN(value)){
            return putVar(unescaped);
        }else{
            if(Double.isInfinite(value)) value = 0.0;
            //this creates a hidden const variable with the specified value
            return putConst("___" + value, value);
        }
    }

    double parseDouble(String symbol){
        //parse hex/binary syntax
        if(symbol.startsWith("0b")) return parseLong(false, symbol, 2, 2, symbol.length());
        if(symbol.startsWith("+0b")) return parseLong(false, symbol, 2, 3, symbol.length());
        if(symbol.startsWith("-0b")) return parseLong(true,symbol,  2, 3, symbol.length());
        if(symbol.startsWith("0x")) return parseLong(false,symbol,  16, 2, symbol.length());
        if(symbol.startsWith("+0x")) return parseLong(false,symbol,  16, 3, symbol.length());
        if(symbol.startsWith("-0x")) return parseLong(true,symbol,  16, 3, symbol.length());
        if(symbol.startsWith("%[") && symbol.endsWith("]") && symbol.length() > 3) return parseNamedColor(symbol);
        if(symbol.startsWith("%") && (symbol.length() == 7 || symbol.length() == 9)) return parseColor(symbol);

        return Strings.parseDouble(symbol, Double.NaN);
    }

    double parseLong(boolean negative, String s, int radix, int start, int end) {
        long usedInvalidNum = negative ? invalidNumPositive : invalidNumNegative;
        long l = Strings.parseLong(s, radix, start, end, usedInvalidNum);
        return l == usedInvalidNum ? Double.NaN : negative ? -l : l;
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
    public LVar putConst(String name, Object value){
        LVar var = putVar(name);
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
    public LVar putVar(String name){
        if(vars.containsKey(name)){
            return vars.get(name);
        }else{
            //variables are null objects by default
            LVar var = new LVar(name);
            var.isobj = true;
            vars.put(name, var);
            return var;
        }
    }

    @Nullable
    public LVar getVar(String name){
        return vars.get(name);
    }

}
