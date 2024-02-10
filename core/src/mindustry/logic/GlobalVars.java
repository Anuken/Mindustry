package mindustry.logic;

import arc.*;
import arc.files.*;
import arc.graphics.*;
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
import mindustry.world.blocks.legacy.*;

import java.io.*;

import static mindustry.Vars.*;

/** Stores global logic variables for logic processors. */
public class GlobalVars{
    public static final int ctrlProcessor = 1, ctrlPlayer = 2, ctrlCommand = 3;
    public static final ContentType[] lookableContent = {ContentType.block, ContentType.unit, ContentType.item, ContentType.liquid};
    /** Global random state. */
    public static final Rand rand = new Rand();

    //non-constants that depend on state
    private static int varTime, varTick, varSecond, varMinute, varWave, varWaveTime, varMapW, varMapH, varServer, varClient, varClientLocale, varClientUnit, varClientName, varClientTeam, varClientMobile;

    private ObjectIntMap<String> namesToIds = new ObjectIntMap<>();
    private Seq<Var> vars = new Seq<>(Var.class);
    private Seq<VarEntry> varEntries = new Seq<>();
    private IntSet privilegedIds = new IntSet();
    private UnlockableContent[][] logicIdToContent;
    private int[][] contentIdToLogicId;

    public void init(){
        putEntryOnly("sectionProcessor");

        putEntryOnly("@this");
        putEntryOnly("@thisx");
        putEntryOnly("@thisy");
        putEntryOnly("@links");
        putEntryOnly("@ipt");

        putEntryOnly("sectionGeneral");

        put("the end", null, false, true);
        //add default constants
        putEntry("false", 0);
        putEntry("true", 1);
        put("null", null, false, true);

        //math
        putEntry("@pi", Mathf.PI);
        put("π", Mathf.PI, false, true); //for the "cool" kids
        putEntry("@e", Mathf.E);
        putEntry("@degToRad", Mathf.degRad);
        putEntry("@radToDeg", Mathf.radDeg);

        putEntryOnly("sectionMap");

        //time
        varTime = putEntry("@time", 0);
        varTick = putEntry("@tick", 0);
        varSecond = putEntry("@second", 0);
        varMinute = putEntry("@minute", 0);
        varWave = putEntry("@waveNumber", 0);
        varWaveTime = putEntry("@waveTime", 0);

        varMapW = putEntry("@mapw", 0);
        varMapH = putEntry("@maph", 0);

        putEntryOnly("sectionNetwork");

        varServer = putEntry("@server", 0, true);
        varClient = putEntry("@client", 0, true);

        //privileged desynced client variables
        varClientLocale = putEntry("@clientLocale", null, true);
        varClientUnit = putEntry("@clientUnit", null, true);
        varClientName = putEntry("@clientName", null, true);
        varClientTeam = putEntry("@clientTeam", 0, true);
        varClientMobile = putEntry("@clientMobile", 0, true);

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
            if(content.item(block.name) == null & !(block instanceof LegacyBlock)){
                put("@" + block.name, block);
            }
        }

        for(var entry : Colors.getColors().entries()){
            //ignore uppercase variants, they are duplicates
            if(Character.isUpperCase(entry.key.charAt(0))) continue;

            put("@color" + Strings.capitalize(entry.key), entry.value.toDoubleBits());
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

        putEntryOnly("sectionLookup");

        Fi ids = Core.files.internal("logicids.dat");
        if(ids.exists()){
            //read logic ID mapping data (generated in ImagePacker)
            try(DataInputStream in = new DataInputStream(ids.readByteStream())){
                for(ContentType ctype : lookableContent){
                    short amount = in.readShort();
                    logicIdToContent[ctype.ordinal()] = new UnlockableContent[amount];
                    contentIdToLogicId[ctype.ordinal()] = new int[Vars.content.getBy(ctype).size];

                    //store count constants
                    putEntry("@" + ctype.name() + "Count", amount);

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

        vars.items[varMapW].numval = world.width();
        vars.items[varMapH].numval = world.height();

        //network
        vars.items[varServer].numval = (net.server() || !net.active()) ? 1 : 0;
        vars.items[varClient].numval = net.client() ? 1 : 0;

        //client
        if(!net.server() && player != null){
            vars.items[varClientLocale].objval = player.locale();
            vars.items[varClientUnit].objval = player.unit();
            vars.items[varClientName].objval = player.name();
            vars.items[varClientTeam].numval = player.team().id;
            vars.items[varClientMobile].numval = mobile ? 1 : 0;
        }
    }

    public Seq<VarEntry> getEntries(){
        return varEntries;
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

    /**
     * @return a constant ID > 0 if there is a constant with this name, otherwise -1.
     * Attempt to get privileged variable id from non-privileged logic executor returns null constant id.
     */
    public int get(String name){
        return namesToIds.get(name, -1);
    }

    /**
     * @return a constant variable by ID. ID is not bound checked and must be positive.
     * Attempt to get privileged variable from non-privileged logic executor returns null constant
     */
    public Var get(int id, boolean privileged){
        if(!privileged && privilegedIds.contains(id)) return vars.get(namesToIds.get("null"));
        return vars.items[id];
    }

    /** Sets a global variable by an ID returned from put(). */
    public void set(int id, double value){
        get(id, true).numval = value;
    }

    /** Adds a constant value by name. */
    public int put(String name, Object value, boolean privileged){
        return put(name, value, privileged, true);
    }

    /** Adds a constant value by name. */
    public int put(String name, Object value, boolean privileged, boolean hidden){
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
        if(privileged) privilegedIds.add(index);
        vars.add(var);

        if(!hidden){
            varEntries.add(new VarEntry(index, name, "", "", privileged));
        }
        return index;
    }

    public int put(String name, Object value){
        return put(name, value, false);
    }

    public int putEntry(String name, Object value){
        return put(name, value, false, false);
    }

    public int putEntry(String name, Object value, boolean privileged){
        return put(name, value, privileged, false);
    }

    public void putEntryOnly(String name){
        varEntries.add(new VarEntry(0, name, "", "", false));
    }

    /** An entry that describes a variable for documentation purposes. This is *only* used inside UI for global variables. */
    public static class VarEntry{
        public int id;
        public String name, description, icon;
        public boolean privileged;

        public VarEntry(int id, String name, String description, String icon, boolean privileged){
            this.id = id;
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.privileged = privileged;
        }

        public VarEntry(){
        }
    }
}
