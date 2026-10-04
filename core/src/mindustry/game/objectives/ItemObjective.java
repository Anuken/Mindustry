package mindustry.game.objectives;

import arc.*;
import mindustry.content.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** Have a certain amount of item in your core. */
public class ItemObjective extends MapObjective{
    public Item item = Items.copper;
    public int amount = 1;

    public ItemObjective(Item item, int amount){
        this.item = item;
        this.amount = amount;
    }

    public ItemObjective(){
    }

    @Override
    public boolean update(){
        return state.rules.defaultTeam.items().has(item, amount);
    }

    @Override
    public String text(){
        return Core.bundle.format("objective.item", state.rules.defaultTeam.items().get(item), amount, item.emoji() + " ", item.localizedName);
    }

    @Override
    public void validate(){
        if(item == null) item = Items.copper;
    }

    @Override
    public String toString(){
        return "item: " + item + " x" + amount;
    }
}
