package mindustry.game.conditions;

import arc.*;
import mindustry.type.*;

public class Research implements UnlockCondition{
    public UnlockableContent content;

    public Research(UnlockableContent content){
        this.content = content;
    }

    protected Research(){
    }

    @Override
    public boolean complete(){
        return content.unlockedHost();
    }

    @Override
    public String display(){
        return Core.bundle.format("requirement.research",
        //TODO broken for multi tech nodes.
        (content.techNode == null || content.techNode.parent == null || content.techNode.parent.content.unlockedHost()) ?
        (content.emoji() + " " + content.localizedName) : "???");
    }

    @Override
    public String toString(){
        return "research: " + content;
    }
}
