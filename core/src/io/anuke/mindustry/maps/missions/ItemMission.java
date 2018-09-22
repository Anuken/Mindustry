package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.state;

/**A mission that is completed when the player obtains items in their core.*/
public class ItemMission extends Mission{
    private final Item item;
    private final int amount;

    public ItemMission(Item item, int amount){
        this.item = item;
        this.amount = amount;
    }

    @Override
    public GameMode getMode(){
        return GameMode.waves;
    }

    @Override
    public boolean isComplete(){
        for(Tile tile : state.teams.get(Vars.defaultTeam).cores){
            if(tile.entity.items.has(item, amount)){
                return true;
            }
        }
        return false;
    }

    @Override
    public String displayString(){
        return Bundles.format("text.mission.resource", item.localizedName(), amount);
    }
}
