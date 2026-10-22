package mindustry.game.objectives;

import arc.*;
import mindustry.content.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** Get a certain item in your core (through a block, not manually.) */
public class CoreItemObjective extends MapObjective{
    public Item item = Items.copper;
    public int amount = 2;

    public CoreItemObjective(Item item, int amount){
        this.item = item;
        this.amount = amount;
    }

    public CoreItemObjective(){
    }

    @Override
    public boolean update(){
        return state.stats.coreItemCount.get(item) >= amount;
    }

    @Override
    public String text(){
        return Core.bundle.format("objective.coreitem", state.stats.coreItemCount.get(item), amount, item.emoji() + " ", item.localizedName);
    }

    @Override
    public void validate(){
        if(item == null) item = Items.copper;
    }

    @Override
    public String toString(){
        return "coreItem: " + item + " x" + amount;
    }
}
