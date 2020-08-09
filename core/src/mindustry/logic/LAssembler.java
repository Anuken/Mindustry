package mindustry.logic;

import arc.func.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.logic.LExecutor.*;
import mindustry.type.*;

/** "Compiles" a sequence of statements into instructions. */
public class LAssembler{
    public static ObjectMap<String, Func<String[], LStatement>> customParsers = new ObjectMap<>();

    private int lastVar;
    /** Maps names to variable IDs. */
    ObjectMap<String, BVar> vars = new ObjectMap<>();
    /** All instructions to be executed. */
    LInstruction[] instructions;

    public LAssembler(){
        putVar("@counter").value = 0;
        putConst("@time", 0);

        //add default constants
        putConst("false", 0);
        putConst("true", 1);
        putConst("null", null);

        //store base content (TODO hacky?)

        for(Item item : Vars.content.items()){
            putConst("@" + item.name, item);
        }

        for(Liquid liquid : Vars.content.liquids()){
            putConst("@" + liquid.name, liquid);
        }

        //store sensor constants

        for(LSensor sensor : LSensor.all){
            putConst("@" + sensor.name(), sensor);
        }
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
        Seq<LStatement> statements = new Seq<>();
        String[] lines = data.split("[;\n]+");
        for(String line : lines){
            //comments
            //if(line.startsWith("#")) continue;

            String[] tokens = line.split(" ");
            LStatement st = LogicIO.read(tokens);
            if(st != null){
                statements.add(st);
            }else{
                //attempt parsing using custom parser if a match is found - this is for mods
                String first = tokens[0];
                if(customParsers.containsKey(first)){
                    statements.add(customParsers.get(first).get(tokens));
                }
            }
        }
        return statements;
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

    public @Nullable BVar getVar(String name){
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
