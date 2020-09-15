package mindustry.logic;

import arc.func.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.logic.LExecutor.*;
import mindustry.logic.LStatements.*;
import mindustry.type.*;
import mindustry.world.*;

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

        for(Block block : Vars.content.blocks()){
            if(block.synthetic()){
                putConst("@" + block.name, block);
            }
        }

        //store sensor constants

        for(LAccess sensor : LAccess.all){
            putConst("@" + sensor.name(), sensor);
        }
    }

    public static LAssembler assemble(String data, int maxInstructions){
        LAssembler asm = new LAssembler();

        Seq<LStatement> st = read(data, maxInstructions);

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
        return read(data, LExecutor.maxInstructions);
    }

    public static Seq<LStatement> read(String data, int max){
        //empty data check
        if(data == null || data.isEmpty()) return new Seq<>();

        Seq<LStatement> statements = new Seq<>();
        String[] lines = data.split("[;\n]+");
        int index = 0;
        for(String line : lines){
            //comments
            if(line.startsWith("#")) continue;

            if(index++ > max) break;

            line = line.replace("\t", "").trim();
            
            try{
                String[] arr;

                //yes, I am aware that this can be split with regex, but that's slow and even more incomprehensible
                if(line.contains(" ")){
                    Seq<String> tokens = new Seq<>();
                    boolean inString = false;
                    int lastIdx = 0;

                    for(int i = 0; i < line.length() + 1; i++){
                        char c = i == line.length() ? ' ' : line.charAt(i);
                        if(c == '"'){
                            inString = !inString;
                        }else if(c == ' ' && !inString){
                            tokens.add(line.substring(lastIdx, i));
                            lastIdx = i + 1;
                        }
                    }

                    arr = tokens.toArray(String.class);
                }else{
                    arr = new String[]{line};
                }

                String type = arr[0];

                //legacy stuff
                if(type.equals("bop")){
                    arr[0] = "op";

                    //field order for bop used to be op a, b, result, but now it's op result a b
                    String res = arr[4];
                    arr[4] = arr[3];
                    arr[3] = arr[2];
                    arr[2] = res;
                }else if(type.equals("uop")){
                    arr[0] = "op";

                    if(arr[1].equals("negate")){
                        arr = new String[]{
                            "op", "mul", arr[3], arr[2], "-1"
                        };
                    }else{
                        //field order for uop used to be op a, result, but now it's op result a
                        String res = arr[3];
                        arr[3] = arr[2];
                        arr[2] = res;
                    }
                }

                LStatement st = LogicIO.read(arr);

                if(st != null){
                    statements.add(st);
                }else{
                    //attempt parsing using custom parser if a match is found - this is for mods
                    String first = arr[0];
                    if(customParsers.containsKey(first)){
                        statements.add(customParsers.get(first).get(arr));
                    }else{
                        //unparseable statement
                        statements.add(new InvalidStatement());
                    }
                }
            }catch(Exception parseFailed){
                parseFailed.printStackTrace();
                //when parsing fails, add a dummy invalid statement
                statements.add(new InvalidStatement());
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
            return putConst("___" + symbol, symbol.substring(1, symbol.length() - 1).replace("\\n", "\n")).id;
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
