package mindustry.type;

import mindustry.ctype.*;
import mindustry.game.*;

/** This class is only for displaying team lore in the content database. */
//TODO
public class TeamEntry extends UnlockableContent{
    public final Team team;

    public TeamEntry(Team team){
        super(team.name);
        this.team = team;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.team;
    }
}
