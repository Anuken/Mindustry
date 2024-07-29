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
    public static final int maxTokenLength = 36;

    private static final int invalidNum = Integer.MIN_VALUE;

    private boolean privileged;
    /** Maps names to variable. */
    public OrderedMap<String, LVar> vars = new OrderedMap<>();
    /** All instructions to be executed. */
    public LInstruction[] instructions;

    public LAssembler(){
        //instruction counter
        putVar("@counter");
        //currently controlled unit
        putConst("@unit", null);
        //reference to self
        putConst("@this", null);
    }

    public static LAssembler assemble(String data, boolean privileged){
        LAssembler asm = new LAssembler();

        Seq<LStatement> st = read(data, privileged);

        asm.instructions = st.map(l -> l.build(asm)).retainAll(l -> l != null).toArray(LInstruction.class);
        asm.privileged = privileged;
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
        LVar constVar = Vars.logicVars.get(symbol);
        if(constVar != null) return constVar;

        symbol = symbol.trim();

        //string case
        if(!symbol.isEmpty() && symbol.charAt(0) == '\"' && symbol.charAt(symbol.length() - 1) == '\"'){
            return putConst("___" + symbol, symbol.substring(1, symbol.length() - 1).replace("\\n", "\n"));
        }

        //remove spaces for non-strings
        symbol = symbol.replace(' ', '_');

        double value = parseDouble(symbol);

        if(value == invalidNum){
            return putVar(symbol);
        }else{
            //this creates a hidden const variable with the specified value
            return putConst("___" + value, value);
        }
    }

    double parseDouble(String symbol){
        //parse hex/binary syntax
        if(symbol.startsWith("0b")) return Strings.parseLong(symbol, 2, 2, symbol.length(), invalidNum);
        if(symbol.startsWith("0x")) return Strings.parseLong(symbol, 16, 2, symbol.length(), invalidNum);
        if(symbol.startsWith("%") && (symbol.length() == 7 || symbol.length() == 9)) return parseColor(symbol);

        return Strings.parseDouble(symbol, invalidNum);
    }

    double parseColor(String symbol){
        int
        r = Strings.parseInt(symbol, 16, 0, 1, 3),
        g = Strings.parseInt(symbol, 16, 0, 3, 5),
        b = Strings.parseInt(symbol, 16, 0, 5, 7),
        a = symbol.length() == 9 ? Strings.parseInt(symbol, 16, 0, 7, 9) : 255;

        return Color.toDoubleBits(r, g, b, a);
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
            LVar var = new LVar(name);
            vars.put(name, var);
            return var;
        }
    }

    @Nullable
    public LVar getVar(String name){
        return vars.get(name);
    }

}
