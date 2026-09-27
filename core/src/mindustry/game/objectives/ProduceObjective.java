package mindustry.game.objectives;

import arc.*;
import mindustry.content.*;
import mindustry.game.objectives.MapObjectives.*;
import mindustry.type.*;

/** Produce a specific piece of content in the tech tree (essentially research with different text). */
public class ProduceObjective extends MapObjective{
    public @Researchable UnlockableContent content = Items.copper;

    public ProduceObjective(UnlockableContent content){
        this.content = content;
    }

    public ProduceObjective(){
    }

    @Override
    public boolean update(){
        return content.unlocked();
    }

    @Override
    public String text(){
        return Core.bundle.format("objective.produce", content.emoji() + " ", content.localizedName);
    }

    @Override
    public void validate(){
        if(content == null) content = Items.copper;
    }

    @Override
    public String toString(){
        return "produce: " + content;
    }
}
