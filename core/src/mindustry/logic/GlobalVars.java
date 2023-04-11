package mindustry.logic;

import arc.*;
import arc.files.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.logic.LExecutor.*;
import mindustry.type.*;
import mindustry.world.*;

import java.io.*;

import static mindustry.Vars.*;

/** Stores global logic variables for logic processors. */
public class GlobalVars{
    public static final int ctrlProcessor = 1, ctrlPlayer = 2, ctrlCommand = 3;
    public static final ContentType[] lookableContent = {ContentType.block, ContentType.unit, ContentType.item, ContentType.liquid};
    /** Global random state. */
    public static final Rand rand = new Rand();

    //non-constants that depend on state
    private static int varTime, varTick, varSecond, varMinute, varWave, varWaveTime;

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

        //math
        put("@pi", Mathf.PI);
        put("π", Mathf.PI); //for the "cool" kids
        put("@e", Mathf.E);
        put("@degToRad", Mathf.degRad);
        put("@radToDeg", Mathf.radDeg);

        //time
        varTime = put("@time", 0);
        varTick = put("@tick", 0);
        varSecond = put("@second", 0);
        varMinute = put("@minute", 0);
        varWave = put("@waveNumber", 0);
        varWaveTime = put("@waveTime", 0);

        //special enums
        put("@ctrlProcessor", ctrlProcessor);
        put("@ctrlPlayer", ctrlPlayer);
        put("@ctrlCommand", ctrlCommand);

        //store base content

        for(Team team : Team.baseTeams){
            put("@" + team.name, team);
        }

        for(Item item : Vars.content.items()){
            put("@" + item.name, item);
        }

        for(Liquid liquid : Vars.content.liquids()){
            put("@" + liquid.name, liquid);
        }

        for(Block block : Vars.content.blocks()){
            //only register blocks that have no item equivalent (this skips sand)
            if(content.item(block.name) == null){
                put("@" + block.name, block);
            }
        }

        //used as a special value for any environmental solid block
        put("@solid", Blocks.stoneWall);

        for(UnitType type : Vars.content.units()){
            put("@" + type.name, type);
        }

        //store sensor constants
        for(LAccess sensor : LAccess.all){
            put("@" + sensor.name(), sensor);
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

    /** Updates global time and other state variables. */
    public void update(){
        //set up time; note that @time is now only updated once every invocation and directly based off of @tick.
        //having time be based off of user system time was a very bad idea.
        vars.items[varTime].numval = state.tick / 60.0 * 1000.0;
        vars.items[varTick].numval = state.tick;

        //shorthands for seconds/minutes spent in save
        vars.items[varSecond].numval = state.tick / 60f;
        vars.items[varMinute].numval = state.tick / 60f / 60f;

        //wave state
        vars.items[varWave].numval = state.wave;
        vars.items[varWaveTime].numval = state.wavetime / 60f;
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

    /** Sets a global variable by an ID returned from put(). */
    public void set(int id, double value){
        get(id).numval = value;
    }

    /** Adds a constant value by name. */
    public int put(String name, Object value){
        int existingIdx = namesToIds.get(name, -1);
        if(existingIdx != -1){ //don't overwrite existing vars (see #6910)
            Log.debug("Failed to add global logic variable '@', as it already exists.", name);
            return existingIdx;
        }

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
        return index;
    }
}
