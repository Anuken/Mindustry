package mindustry.logic;

import arc.struct.*;

/** "Compiles" a sequence of statements into instructions. */
public class LBuilder{
    private int lastVar;
    /** Maps names to variable IDs. */
    private ObjectIntMap<String> vars = new ObjectIntMap<>();
    /** Maps variable IDs to their constant value. */
    private IntMap<Object> constants = new IntMap<>();

    public LBuilder(){
        //add default constant variables
        putConst("false", 0);
        putConst("true", 1);
    }

    /** @return a variable ID by name.
     * This may be a constant variable referring to a number or object. */
    public int var(String symbol){
        try{
            double value = Double.parseDouble(symbol);
            //this creates a hidden const variable with the specified value
            String key = "___" + value;
            return putConst(key, value);
        }catch(NumberFormatException e){
            return putVar(symbol);
        }
    }

    /** Adds a constant value by name. */
    private int putConst(String name, double value){
        int id = putVar(name);
        constants.put(id, value);
        return id;
    }

    /** Registers a variable name mapping. */
    private int putVar(String name){
        if(vars.containsKey(name)){
            return vars.get(name);
        }else{
            int id = lastVar++;
            vars.put(name, id);
            return id;
        }
    }
}
