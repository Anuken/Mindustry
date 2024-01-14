package mindustry.game;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.Rules.*;
import mindustry.game.Teams.*;
import mindustry.graphics.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class Team implements Comparable<Team>{
    public final int id;
    public final Color color;
    public final Color[] palette;
    public final int[] palettei = new int[3];
    public String emoji = "";
    public boolean hasPalette;
    public String name;
    public int[] flags = new int[256];

    /** All 256 registered teams. */
    public static final Team[] all = new Team[256];
    /** The 6 base teams used in the editor. */
    public static final Team[] baseTeams = new Team[6];

    public final static Team
        derelict = new Team(0, "derelict", Color.valueOf("4d4e58")),
        sharded = new Team(1, "sharded", Pal.accent.cpy(), Color.valueOf("ffd37f"), Color.valueOf("eab678"), Color.valueOf("d4816b")),
        crux = new Team(2, "crux", Color.valueOf("f25555"), Color.valueOf("fc8e6c"), Color.valueOf("f25555"), Color.valueOf("a04553")),
        malis = new Team(3, "malis", Color.valueOf("a27ce5"), Color.valueOf("c195fb"), Color.valueOf("665c9f"), Color.valueOf("484988")),

        //TODO temporarily no palettes for these teams.
        green = new Team(4, "green", Color.valueOf("54d67d")),//Color.valueOf("96f58c"), Color.valueOf("54d67d"), Color.valueOf("28785c")),
        blue = new Team(5, "blue", Color.valueOf("6c87fd")), //Color.valueOf("85caf9"), Color.valueOf("6c87fd"), Color.valueOf("3b3392")
        neoplastic = new Team(6, "neoplastic", Color.valueOf("e05438")); //yes, it looks very similar to crux, you're not supposed to use this team for block regions anyway

    static{
        Mathf.rand.setSeed(8);
        //fix random seed shift caused by new team
        for(int i = 0; i < 3; i++){
            Mathf.random();
        }
        //create the whole 256 placeholder teams
        for(int i = 7; i < all.length; i++){
            new Team(i, "team#" + i, Color.HSVtoRGB(360f * Mathf.random(), 100f * Mathf.random(0.4f, 1f), 100f * Mathf.random(0.6f, 1f), 1f));
        }
        Mathf.rand.setSeed(new Rand().nextLong());
        //weakly initialize team flags
        for(int i = 0; i < 256; i++){
            all[i].flags[0] = TeamFlags.derelictTarget;
            all[i].flags[i] = TeamFlags.genericSelf;
        }
    }

    public static Team get(int id){
        return all[((byte)id) & 0xff];
    }

    protected Team(int id, String name, Color color){
        this.name = name;
        this.color = color;
        this.id = id;

        if(id < 6) baseTeams[id] = this;
        all[id] = this;

        palette = new Color[3];
        palette[0] = color;
        palette[1] = color.cpy().mul(0.75f);
        palette[2] = color.cpy().mul(0.5f);

        for(int i = 0; i < 3; i++){
            palettei[i] = palette[i].rgba();
        }
    }

    /** Specifies a 3-color team palette. */
    protected Team(int id, String name, Color color, Color pal1, Color pal2, Color pal3){
        this(id, name, color);

        palette[0] = pal1;
        palette[1] = pal2;
        palette[2] = pal3;
        for(int i = 0; i < 3; i++){
            palettei[i] = palette[i].rgba();
        }
        hasPalette = true;
    }

    /** @return the core items for this team, or an empty item module.
     * Never add to the resulting item module, as it is mutable. */
    public ItemModule items(){
        return core() == null ? ItemModule.empty : core().items;
    }

    /** @return the team-specific rules. */
    public TeamRule rules(){
        return state.rules.teams.get(this);
    }

    public TeamData data(){
        return state.teams.get(this);
    }

    @Nullable
    public CoreBuild core(){
        return data().core();
    }

    public boolean active(){
        return state.teams.isActive(this);
    }

    /** @return whether this team is supposed to be AI-controlled. */
    public boolean isAI(){
        return (state.rules.waves || state.rules.attackMode) && this != state.rules.defaultTeam && !state.rules.pvp;
    }

    /** @return whether this team is solely comprised of AI (with no players possible). */
    public boolean isOnlyAI(){
        return isAI() && data().players.size == 0;
    }

    /** @return whether this team needs a flow field for "dumb" wave pathfinding. */
    public boolean needsFlowField(){
        return isAI() && !rules().rtsAi;
    }

    /** Reset all flags to their default values. **/
    public void initializeFlags(){
        for(int i = 0; i < 256; i++){
            for(int j = 1; j < 256; j++){
                all[i].flags[j] = TeamFlags.genericEnemy;
            }
            all[i].flags[0] = TeamFlags.derelictTarget;
            all[i].flags[i] = TeamFlags.genericSelf;
        }
    }

    /** Flag binding methods. */
    public boolean isEnemy(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.isNeutral) == 0;
    }

    public boolean isAlly(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.allyCheck) == TeamFlags.allyCheck;
    }

    public boolean canDamage(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.cannotDamage) == 0;
    }

    public boolean canPlaceOver(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.canPlaceOver) != 0;
    }

    public boolean canDeconstruct(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.canDeconstruct) != 0;
    }

    public boolean canAssistBuilding(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.canAssistBuilding) != 0;
    }

    public boolean canAttach(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.canAttach) != 0;
    }

    public boolean canInteract(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.canInteract) != 0;
    }

    public boolean ignoresBuildRadius(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.ignoresBuildRadius) != 0;
    }

    public boolean canLogicBlocks(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.canLogicBlocks) != 0;
    }

    public boolean canLogicUnits(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.canLogicUnits) != 0;
    }

    public boolean canGiveItems(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.canGiveItems) != 0;
    }

    public boolean canTakeItems(Team other){
        if(other == null) return false;
        return (this.flags[other.id] & TeamFlags.canTakeItems) != 0;
    }

    //TODO might be easier to just OR-bitmask in fog code?
    public boolean canSeeExplored(Team other){
        return (this.flags[other.id] & TeamFlags.canSeeExplored) != 0;
    }

    public Seq<CoreBuild> cores(){
        return state.teams.cores(this);
    }

    public String localized(){
        return Core.bundle.get("team." + name + ".name", name);
    }
    
    public String coloredName(){
        return emoji + "[#" + color + "]" + localized() + "[]";
    }

    @Override
    public int compareTo(Team team){
        return Integer.compare(id, team.id);
    }

    @Override
    public String toString(){
        return name;
    }
}
