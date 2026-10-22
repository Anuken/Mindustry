package mindustry.game.objectives;

import arc.*;
import mindustry.content.*;
import mindustry.game.objectives.MapObjectives.*;
import mindustry.type.*;

/** Research a specific piece of content in the tech tree. */
public class ResearchObjective extends MapObjective{
    public @Researchable UnlockableContent content = Items.copper;

    public ResearchObjective(UnlockableContent content){
        this.content = content;
    }

    public ResearchObjective(){
    }

    @Override
    public boolean update(){
        return content.unlocked();
    }

    @Override
    public String text(){
        return Core.bundle.format("objective.research", content.emoji() + " ", content.localizedName);
    }

    @Override
    public void validate(){
        if(content == null) content = Items.copper;
    }

    @Override
    public String toString(){
        return "research: " + content;
    }
}
