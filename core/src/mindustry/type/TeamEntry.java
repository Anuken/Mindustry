package mindustry.type;

import arc.scene.ui.layout.*;
import mindustry.ctype.*;
import mindustry.game.*;

/** This class is only for displaying team lore in the content database. */
//TODO more stuff, make unlockable, don't display in campaign at all
public class TeamEntry extends UnlockableContent{
    public final Team team;

    public TeamEntry(String name, Team team){
        super(name);
        this.team = team;
    }

    public TeamEntry(Team team){
        this(team.name, team);
    }

    @Override
    public void displayExtra(Table table){
        table.add("@team." + name + ".log").pad(6).padTop(20).width(400f).wrap().fillX();
    }

    @Override
    public ContentType getContentType(){
        return ContentType.team;
    }
}
