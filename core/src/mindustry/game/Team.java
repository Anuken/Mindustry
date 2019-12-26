package mindustry.game;

import arc.*;
import arc.graphics.*;
import arc.struct.*;
import mindustry.graphics.*;

public class Team{
    /** All registered teams. */
    public final static Array<Team> all = new Array<>();
    public final static Team
    derelict = new Team("derelict", Color.valueOf("4d4e58")),
    sharded = new Team("sharded", Pal.accent.cpy()),
    crux = new Team("crux", Color.valueOf("e82d2d")),
    green = new Team("green", Color.valueOf("4dd98b")),
    purple = new Team("purple", Color.valueOf("9a4bdf")),
    blue = new Team("blue", Color.royal.cpy());

    public final Color color;
    public final int intColor;
    public final String name;
    public final int id;

    public Team(String name, Color color){
        this.name = name;
        this.color = color;
        this.intColor = Color.rgba8888(color);
        this.id = all.size;
        all.add(this);
    }

    public String localized(){
        return Core.bundle.get("team." + name + ".name");
    }
}
