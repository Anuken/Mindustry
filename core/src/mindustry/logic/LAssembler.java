package mindustry.logic;

import arc.struct.*;
import arc.util.ArcAnnotate.*;
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
    /** Statement UI elements being processed. */
    @Nullable Seq<StatementElem> elements;

    public LAssembler(){
        //add default constants
        putConst("false", 0);
        putConst("true", 1);
        putConst("null", null);
    }

    public static LAssembler assemble(Seq<StatementElem> seq){
        LAssembler out = new LAssembler();

        out.elements = seq;
        out.instructions = seq.map(s -> s.st.build(out)).toArray(LInstruction.class);

        return out;
    }

    //TODO this is awful and confusing
    public static LAssembler fromJson(String json){
        LAssembler asm = new LAssembler();

        LStatement[] statements = JsonIO.read(LStatement[].class, json);
        for(LStatement s : statements){
            s.afterLoad(asm);
        }
        asm.instructions = Seq.with(statements).map(l -> l.build(asm)).toArray(LInstruction.class);

        return asm;
    }

    public String toJson(){
        Seq<LStatement> states = elements.map(s -> s.st);
        states.each(s -> s.beforeSave(this));
        return JsonIO.write(states.toArray(LStatement.class));
    }

    /** @return a variable ID by name.
     * This may be a constant variable referring to a number or object. */
    public int var(String symbol){
        symbol = symbol.trim();
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
    private BVar putConst(String name, Object value){
        BVar var = putVar(name);
        var.constant = true;
        var.value = value;
        return var;
    }

    /** Registers a variable name mapping. */
    private BVar putVar(String name){
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
