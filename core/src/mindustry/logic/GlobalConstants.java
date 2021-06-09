package mindustry.logic;

import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.logic.LExecutor.*;
import mindustry.type.*;
import mindustry.world.*;

/** Stores global constants for logic processors. */
public class GlobalConstants{
    public static final int ctrlProcessor = 1, ctrlPlayer = 2, ctrlFormation = 3;

    private ObjectIntMap<String> namesToIds = new ObjectIntMap<>();
    private Seq<Var> vars = new Seq<>(Var.class);

    public void init(){
        put("the end", null);
        //add default constants
        put("false", 0);
        put("true", 1);
        put("null", null);

        //special enums

        put("@ctrlProcessor", ctrlProcessor);
        put("@ctrlPlayer", ctrlPlayer);
        put("@ctrlFormation", ctrlFormation);

        //store base content

        for(Item item : Vars.content.items()){
            put("@" + item.name, item);
        }

        for(Liquid liquid : Vars.content.liquids()){
            put("@" + liquid.name, liquid);
        }

        for(Block block : Vars.content.blocks()){
            if(block.synthetic()){
                put("@" + block.name, block);
            }
        }

        //used as a special value for any environmental solid block
        put("@solid", Blocks.stoneWall);
        put("@air", Blocks.air);

        for(UnitType type : Vars.content.units()){
            put("@" + type.name, type);
        }

        //store sensor constants
        for(LAccess sensor : LAccess.all){
            put("@" + sensor.name(), sensor);
        }

        for(UnitCommand cmd : UnitCommand.all){
            put("@command" + Strings.capitalize(cmd.name()), cmd);
        }
    }

    /** @return a constant ID > 0 if there is a constant with this name, otherwise -1. */
    public int get(String name){
        return namesToIds.get(name, -1);
    }

    /** @return a constant variable by ID. ID is not bound checked and must be positive. */
    public Var get(int id){
        return vars.items[id];
    }

    /** Adds a constant value by name. */
    public Var put(String name, Object value){
        Var var = new Var(name);
        var.constant = true;
        if(value instanceof Number num){
            var.numval = num.doubleValue();
        }else{
            var.isobj = true;
            var.objval = value;
        }

        int index = vars.size;
        namesToIds.put(name, index);
        vars.add(var);
        return var;
    }
}
