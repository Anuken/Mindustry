package mindustry.logic;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.logic.LExecutor.*;
import mindustry.type.*;
import mindustry.world.*;

import java.io.*;

/** Stores global constants for logic processors. */
public class GlobalConstants{
    public static final int ctrlProcessor = 1, ctrlPlayer = 2, ctrlFormation = 3;
    public static final ContentType[] lookableContent = {ContentType.block, ContentType.unit, ContentType.item, ContentType.liquid};

    private ObjectIntMap<String> namesToIds = new ObjectIntMap<>();
    private Seq<Var> vars = new Seq<>(Var.class);
    private UnlockableContent[][] logicIdToContent;
    private int[][] contentIdToLogicId;

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

        logicIdToContent = new UnlockableContent[ContentType.all.length][];
        contentIdToLogicId = new int[ContentType.all.length][];

        Fi ids = Core.files.internal("logicids.dat");
        if(ids.exists()){
            //read logic ID mapping data (generated in ImagePacker)
            try(DataInputStream in = new DataInputStream(ids.readByteStream())){
                for(ContentType ctype : lookableContent){
                    short amount = in.readShort();
                    logicIdToContent[ctype.ordinal()] = new UnlockableContent[amount];
                    contentIdToLogicId[ctype.ordinal()] = new int[Vars.content.getBy(ctype).size];

                    //store count constants
                    put("@" + ctype.name() + "Count", amount);

                    for(int i = 0; i < amount; i++){
                        String name = in.readUTF();
                        UnlockableContent fetched = Vars.content.getByName(ctype, name);

                        if(fetched != null){
                            logicIdToContent[ctype.ordinal()][i] = fetched;
                            contentIdToLogicId[ctype.ordinal()][fetched.id] = i;
                        }
                    }
                }
            }catch(IOException e){
                //don't crash?
                Log.err("Error reading logic ID mapping", e);
            }
        }
    }

    /** @return a piece of content based on its logic ID. This is not equivalent to content ID. */
    public @Nullable Content lookupContent(ContentType type, int id){
        var arr = logicIdToContent[type.ordinal()];
        return arr != null && id >= 0 && id < arr.length ? arr[id] : null;
    }

    /** @return the integer logic ID of content, or -1 if invalid. */
    public int lookupLogicId(UnlockableContent content){
        var arr = contentIdToLogicId[content.getContentType().ordinal()];
        return arr != null && content.id >= 0 && content.id < arr.length ? arr[content.id] : -1;
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
