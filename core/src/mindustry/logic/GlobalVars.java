package mindustry.logic;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.game.*;
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
    private static LVar varTime, varTick, varSecond, varMinute, varWave, varWaveTime, varMapW, varMapH, varServer, varClient, varClientLocale, varClientUnit, varClientName, varClientTeam, varClientMobile;

    private ObjectMap<String, LVar> vars = new ObjectMap<>();
    private Seq<VarEntry> varEntries = new Seq<>();
    private ObjectSet<String> privilegedNames = new ObjectSet<>();
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

        for(UnitType type : Vars.content.units()){
            put("@" + type.name, type);
        }

        for(Weather weather : Vars.content.weathers()){
            put("@" + weather.name, weather);
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
        varTime.numval = state.tick / 60.0 * 1000.0;
        varTick.numval = state.tick;

        //shorthands for seconds/minutes spent in save
        varSecond.numval = state.tick / 60f;
        varMinute.numval = state.tick / 60f / 60f;

        //wave state
        varWave.numval = state.wave;
        varWaveTime.numval = state.wavetime / 60f;

        varMapW.numval = world.width();
        varMapH.numval = world.height();

        //network
        varServer.numval = (net.server() || !net.active()) ? 1 : 0;
        varClient.numval = net.client() ? 1 : 0;

        //client
        if(!net.server() && player != null){
            varClientLocale.objval = player.locale();
            varClientUnit.objval = player.unit();
            varClientName.objval = player.name();
            varClientTeam.numval = player.team().id;
            varClientMobile.numval = mobile ? 1 : 0;
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
     * @return a constant variable if there is a constant with this name, otherwise null.
     * Attempt to get privileged variable from non-privileged logic executor returns null constant.
     */
    public LVar get(String name){
        return vars.get(name);
    }

    /**
     * @return a constant variable by name
     * Attempt to get privileged variable from non-privileged logic executor returns null constant.
     */
    public LVar get(String name, boolean privileged){
        if(!privileged && privilegedNames.contains(name)) return vars.get("null");
        return vars.get(name);
    }

    /** Sets a global variable by name. */
    public void set(String name, double value){
        get(name, true).numval = value;
    }

    /** Adds a constant value by name. */
    public LVar put(String name, Object value, boolean privileged){
        return put(name, value, privileged, true);
    }

    /** Adds a constant value by name. */
    public LVar put(String name, Object value, boolean privileged, boolean hidden){
        LVar existingVar = vars.get(name);
        if(existingVar != null){ //don't overwrite existing vars (see #6910)
            Log.debug("Failed to add global logic variable '@', as it already exists.", name);
            return existingVar;
        }

        LVar var = new LVar(name);
        var.constant = true;
        if(value instanceof Number num){
            var.isobj = false;
            var.numval = num.doubleValue();
        }else{
            var.isobj = true;
            var.objval = value;
        }

        vars.put(name, var);
        if(privileged) privilegedNames.add(name);

        if(!hidden){
            varEntries.add(new VarEntry(name, "", "", privileged));
        }
        return var;
    }

    public LVar put(String name, Object value){
        return put(name, value, false);
    }

    public LVar putEntry(String name, Object value){
        return put(name, value, false, false);
    }

    public LVar putEntry(String name, Object value, boolean privileged){
        return put(name, value, privileged, false);
    }

    public void putEntryOnly(String name){
        varEntries.add(new VarEntry(name, "", "", false));
    }

    /** An entry that describes a variable for documentation purposes. This is *only* used inside UI for global variables. */
    public static class VarEntry{
        public String name, description, icon;
        public boolean privileged;

        public VarEntry(String name, String description, String icon, boolean privileged){
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.privileged = privileged;
        }

        public VarEntry(){
        }
    }
}
