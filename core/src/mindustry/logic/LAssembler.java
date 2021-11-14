package mindustry.logic;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.logic.LExecutor.*;

/** "Compiles" a sequence of statements into instructions. */
public class LAssembler{
    public static ObjectMap<String, Func<String[], LStatement>> customParsers = new ObjectMap<>();
    public static final int maxTokenLength = 36;

    private static final int invalidNum = Integer.MIN_VALUE;

    private int lastVar;
    /** Maps names to variable IDs. */
    public ObjectMap<String, BVar> vars = new ObjectMap<>();
    /** All instructions to be executed. */
    public LInstruction[] instructions;

    public LAssembler(){
        //instruction counter
        putVar("@counter").value = 0;
        //unix timestamp
        putConst("@time", 0);
        //currently controlled unit
        putConst("@unit", null);
        //reference to self
        putConst("@this", null);
        //global tick
        putConst("@tick", 0);
    }

    public static LAssembler assemble(String data){
        LAssembler asm = new LAssembler();

        Seq<LStatement> st = read(data);

        asm.instructions = st.map(l -> l.build(asm)).filter(l -> l != null).toArray(LInstruction.class);
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

    public static Seq<LStatement> read(String data){
        return LParser.parse(data);
    }

    /** @return a variable ID by name.
     * This may be a constant variable referring to a number or object. */
    public int var(String symbol){
        int constId = Vars.constants.get(symbol);
        if(constId > 0){
            //global constants are *negated* and stored separately
            return -constId;
        }

        symbol = symbol.trim();

        //string case
        if(!symbol.isEmpty() && symbol.charAt(0) == '\"' && symbol.charAt(symbol.length() - 1) == '\"'){
            return putConst("___" + symbol, symbol.substring(1, symbol.length() - 1).replace("\\n", "\n")).id;
        }

        //remove spaces for non-strings
        symbol = symbol.replace(' ', '_');

        double value = parseDouble(symbol);

        if(value == invalidNum){
            return putVar(symbol).id;
        }else{
            //this creates a hidden const variable with the specified value
            return putConst("___" + value, value).id;
        }
    }

    double parseDouble(String symbol){
        //parse hex/binary syntax
        if(symbol.startsWith("0b")) return Strings.parseLong(symbol, 2, 2, symbol.length(), invalidNum);
        if(symbol.startsWith("0x")) return Strings.parseLong(symbol, 16, 2, symbol.length(), invalidNum);

        return Strings.parseDouble(symbol, invalidNum);
    }

    /** Adds a constant value by name. */
    public BVar putConst(String name, Object value){
        BVar var = putVar(name);
        var.constant = true;
        var.value = value;
        return var;
    }

    /** Registers a variable name mapping. */
    public BVar putVar(String name){
        if(vars.containsKey(name)){
            return vars.get(name);
        }else{
            BVar var = new BVar(lastVar++);
            vars.put(name, var);
            return var;
        }
    }

    @Nullable
    public BVar getVar(String name){
        return vars.get(name);
    }

    /** A variable "builder". */
    public static class BVar{
        public int id;
        public boolean constant;
        public Object value;

        public BVar(int id){
            this.id = id;
        }

        BVar(){}

        @Override
        public String toString(){
            return "BVar{" +
            "id=" + id +
            ", constant=" + constant +
            ", value=" + value +
            '}';
        }
    }
}
