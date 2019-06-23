package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.Core;
import io.anuke.arc.collection.EnumSet;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.storage.StorageBlock;
import io.anuke.mindustry.world.meta.BlockFlag;

import static io.anuke.mindustry.Vars.logic;
import static io.anuke.mindustry.Vars.state;

public class ItemsEater extends StorageBlock{
    public static final ItemStack[][] requirementsInRound = {
            ItemStack.with(Items.copper, 50),
            ItemStack.with(Items.copper, 50, Items.lead, 100),
            ItemStack.with(Items.copper, 100, Items.lead, 300),
            ItemStack.with(Items.copper, 100, Items.lead, 300, Items.graphite, 50, Items.silicon, 50)
    };

    public ItemsEater(String name) {
        super(name);
        solid = true;
        update = true;
        hasItems = true;
        itemCapacity = 5000;
        destructible = true;
        health = 150;
        consumes.all();
        flags = EnumSet.of(BlockFlag.target);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("points.bar", e -> new Bar(() -> Core.bundle.format("points.regular", logic.calcPoints(e.tile)), () -> Pal.ammo, () ->
                (state.points[e.getTeam().ordinal()]==0) ? 1f : (float)logic.calcPoints(e.tile) / (float)state.points[e.getTeam().ordinal()]));
    }

    @Override
    public void placed(Tile tile){
        super.placed(tile);
        state.teams.get(tile.getTeam()).cannons.add(tile);
    }

    @Override
    public void removed(Tile tile){
        super.removed(tile);
        state.teams.get(tile.getTeam()).cannons.remove(tile);
    }

    @Override
    public void onProximityUpdate(Tile tile){
        state.teams.get(tile.getTeam()).cannons.add(tile);
    }
}
