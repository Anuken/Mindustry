package mindustry.game;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.Rules.*;
import mindustry.game.Teams.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class Team implements Comparable<Team>, Senseable{
    public final int id;
    public final Color color = new Color();
    public final Color[] palette = {new Color(), new Color(), new Color()};
    public final int[] palettei = new int[3];
    public boolean ignoreUnitCap = false;
    public String emoji = "";
    public boolean hasPalette;
    public String name;

    /** All 256 registered teams. */
    public static final Team[] all = new Team[256];
    /** The 6 base teams used in the editor. */
    public static final Team[] baseTeams = new Team[6];

    public final static Team
        derelict = new Team(0, "derelict", Color.valueOf("4d4e58")),
        sharded = new Team(1, "sharded", Pal.accent.cpy(), Color.valueOf("ffd37f"), Color.valueOf("eab678"), Color.valueOf("d4816b")),
        crux = new Team(2, "crux", Color.valueOf("f25555"), Color.valueOf("fc8e6c"), Color.valueOf("f25555"), Color.valueOf("a04553")),
        malis = new Team(3, "malis", Color.valueOf("a27ce5"), Color.valueOf("c7a4f5"), Color.valueOf("896fd6"), Color.valueOf("504cba")),

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

        neoplastic.ignoreUnitCap = true;
    }

    public static Team get(int id){
        return all[((byte)id) & 0xff];
    }

    protected Team(int id, String name, Color color){
        this.name = name;
        this.color.set(color);
        this.id = id;

        if(id < 6) baseTeams[id] = this;
        all[id] = this;

        setPalette(color);
    }

    /** Specifies a 3-color team palette. */
    protected Team(int id, String name, Color color, Color pal1, Color pal2, Color pal3){
        this(id, name, color);

        setPalette(pal1, pal2, pal3);
        this.color.set(color);
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

    /** @return whether this team has any buildings on this map; in waves mode, this is always true for the enemy team. */
    public boolean active(){
        return state.teams.isActive(this);
    }

    /** @return whether this team has any active cores. Not the same as active()! */
    public boolean isAlive(){
        return data().isAlive();
    }

    /** @return whether this team is supposed to be AI-controlled. */
    public boolean isAI(){
        return (state.rules.waves || state.rules.attackMode || state.isCampaign()) && this != state.rules.defaultTeam && !state.rules.pvp;
    }

    /** @return whether this team is solely comprised of AI (with no players possible). */
    public boolean isOnlyAI(){
        return isAI() && data().players.size == 0;
    }

    /** @return whether this team needs a flow field for "dumb" wave pathfinding. */
    public boolean needsFlowField(){
        return isAI() && !rules().rtsAi;
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

    public void setPalette(Color color){
        setPalette(color, color.cpy().mul(0.75f), color.cpy().mul(0.5f));
        hasPalette = false;
    }

    public void setPalette(Color pal1, Color pal2, Color pal3){
        color.set(pal1);
        palette[0].set(pal1);
        palette[1].set(pal2);
        palette[2].set(pal3);
        for(int i = 0; i < 3; i++){
            palettei[i] = palette[i].rgba();
        }
        hasPalette = true;
    }

    @Override
    public int compareTo(Team team){
        return Integer.compare(id, team.id);
    }

    @Override
    public String toString(){
        return name;
    }

    @Override
    public double sense(LAccess sensor){
        return switch(sensor){
            case id -> id;
            case color -> color.toDoubleBits();
            default -> Double.NaN;
        };
    }

    @Override
    public Object senseObject(LAccess sensor){
        if(sensor == LAccess.name) return name;
        return Senseable.noSensed;
    }
}
