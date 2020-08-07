package mindustry.logic;

import arc.struct.*;
import mindustry.io.*;
import mindustry.logic.LCanvas.*;
import mindustry.logic.LExecutor.*;

/** "Compiles" a sequence of statements into instructions. */
public class LAssembler{
    private transient int lastVar;
    /** Maps names to variable IDs. */
    ObjectMap<String, BVar> vars = new ObjectMap<>();
    /** All instructions to be executed. */
    LInstruction[] instructions;

    public LAssembler(){
        //add default constants
        putConst("false", 0);
        putConst("true", 1);
        putConst("null", null);
    }

    public static LAssembler assemble(Seq<StatementElem> seq){
        LAssembler out = new LAssembler();

        seq.each(s -> s.st.saveUI());
        out.instructions = seq.map(s -> s.st.build(out)).toArray(LInstruction.class);

        return out;
    }

    public static String toJson(Seq<StatementElem> seq){
        seq.each(s -> s.st.saveUI());
        LStatement[] statements = seq.map(s -> s.st).toArray(LStatement.class);

        return JsonIO.write(statements);
    }

    //TODO this is awful and confusing
    public static LAssembler fromJson(String json){
        LAssembler asm = new LAssembler();

        LStatement[] statements = JsonIO.read(LStatement[].class, json);
        for(LStatement s : statements){
            s.setupUI();
        }
        asm.instructions = Seq.with(statements).map(l -> l.build(asm)).toArray(LInstruction.class);

        return asm;
    }

    /** @return a variable ID by name.
     * This may be a constant variable referring to a number or object. */
    public int var(String symbol){
        symbol = symbol.trim();

        //string case
        if(symbol.startsWith("\"") && symbol.endsWith("\"")){
            return putConst("___" + symbol, symbol.substring(1, symbol.length() - 1)).id;
        }

        try{
            double value = Double.parseDouble(symbol);
            //this creates a hidden const variable with the specified value
            String key = "___" + value;
            return putConst(key, value).id;
        }catch(NumberFormatException e){
            return putVar(symbol).id;
        }
    }

    /** Adds a constant value by name. */
    BVar putConst(String name, Object value){
        BVar var = putVar(name);
        var.constant = true;
        var.value = value;
        return var;
    }

    /** Registers a variable name mapping. */
    BVar putVar(String name){
        if(vars.containsKey(name)){
            return vars.get(name);
        }else{
            BVar var = new BVar(lastVar++);
            vars.put(name, var);
            return var;
        }
    }

    /** A saved variable. */
    public static class BVar{
        public int id;
        public boolean constant;
        public Object value;

        public BVar(int id){
            this.id = id;
        }

        BVar(){}
    }
}
