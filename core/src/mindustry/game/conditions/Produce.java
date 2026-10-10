package mindustry.game.conditions;

import arc.*;
import mindustry.type.*;

public class Produce implements UnlockCondition{
    public UnlockableContent content;

    public Produce(UnlockableContent content){
        this.content = content;
    }

    protected Produce(){
    }

    @Override
    public boolean complete(){
        return content.unlockedHost();
    }

    @Override
    public String display(){
        return Core.bundle.format("requirement.produce",
        content.unlockedHost() ? (content.emoji() + " " + content.localizedName) : "???");
    }

    @Override
    public String toString(){
        return "produce: " + content;
    }
}
