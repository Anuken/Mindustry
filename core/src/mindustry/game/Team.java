package mindustry.game;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
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
    public boolean hasPalette;
    public String name;

    /** All 256 registered teams. */
    public static final Team[] all = new Team[256];
    /** The 6 base teams used in the editor. */
    public static final Team[] baseTeams = new Team[6];

    public final static Team
        derelict = new Team(0, "derelict", Color.valueOf("4d4e58")),
        sharded = new Team(1, "sharded", Pal.accent.cpy(),
            Color.valueOf("ffd37f"), Color.valueOf("eab678"), Color.valueOf("d4816b")),
        crux = new Team(2, "crux", Color.valueOf("f25555"),
            Color.valueOf("fc8e6c"), Color.valueOf("f25555"), Color.valueOf("a04553")),
        green = new Team(3, "green", Color.valueOf("4dd98b")),
        purple = new Team(4, "purple", Color.valueOf("9a4bdf")),
        blue = new Team(5, "blue", Color.royal.cpy());

    static{
        Mathf.rand.setSeed(8);
        //create the whole 256 placeholder teams
        for(int i = 6; i < all.length; i++){
            new Team(i, "team#" + i, Color.HSVtoRGB(360f * Mathf.random(), 100f * Mathf.random(0.6f, 1f), 100f * Mathf.random(0.8f, 1f), 1f));
        }
        Mathf.rand.setSeed(new Rand().nextLong());
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
    }

    /** Specifies a 3-color team palette. */
    protected Team(int id, String name, Color color, Color pal1, Color pal2, Color pal3){
        this(id, name, color);

        palette[0] = pal1;
        palette[1] = pal2;
        palette[2] = pal3;
        hasPalette = true;
    }

    /** @return the core items for this team, or an empty item module.
     * Never add to the resulting item module, as it is mutable. */
    public @NonNull ItemModule items(){
        return core() == null ? ItemModule.empty : core().items;
    }

    /** @return the team-specific rules. */
    public TeamRule rules(){
        return state.rules.teams.get(this);
    }

    public Team[] enemies(){
        return state.teams.enemiesOf(this);
    }

    public TeamData data(){
        return state.teams.get(this);
    }

    public @Nullable CoreEntity core(){
        return data().core();
    }

    public boolean active(){
        return state.teams.isActive(this);
    }

    public boolean isEnemy(Team other){
        return state.teams.areEnemies(this, other);
    }

    public Seq<CoreEntity> cores(){
        return state.teams.cores(this);
    }

    public String localized(){
        return Core.bundle.get("team." + name + ".name", name);
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
