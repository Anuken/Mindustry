package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.type.Item;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

public class ResourceMission implements Mission{
    private final Item item;
    private final int amount;

    public ResourceMission(Item item, int amount){
        this.item = item;
        this.amount = amount;
    }

    @Override
    public void display(Table table){

    }

    @Override
    public GameMode getMode(){
        return GameMode.waves;
    }

    @Override
    public boolean isComplete(){
        return Vars.state.teams.get(Vars.defaultTeam).cores.first().entity.items.has(item, amount);
    }

    @Override
    public String displayString(){
        return Bundles.format("text.mission.resource", item.localizedName(), amount);
    }
}
