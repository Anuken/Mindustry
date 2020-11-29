package mindustry.server;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.net.Administration;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.HashMap;

public class UnitDrops {
    public static HashMap<UnitType, ItemStack[]> drops = new HashMap<>();
    public static HashMap<Item, String> itemIcons = new HashMap<>();

    public static void init(){

        Vars.netServer.admins.addActionFilter(action -> {
            Player player = action.player;
            if (player == null) return true;

            if(action.type == Administration.ActionType.placeBlock){
                boolean b = (action.tile.floor() != Blocks.darkPanel4 && action.tile.floor() != Blocks.darkPanel5) || action.block == Blocks.shockMine;
                if(!b && player.con != null) Call.infoToast(player.con, "[accent]You can not build on the enemy path.", 8f);
                return b;
            }

            boolean b = action.tile.block() != null && !(action.tile.block() instanceof CoreBlock);
            if (action.type == Administration.ActionType.depositItem || action.type == Administration.ActionType.withdrawItem) {
                if (!b && player.con != null)
                    Call.infoToast(player.con, "[accent]You can not interact with the core.", 8f);
                return b;
            }
            return true;
        });


        // Spiders
        drops.put(UnitTypes.crawler, ItemStack.with(Items.copper, 14, Items.lead, 12, Items.coal, 2, Items.blastCompound, 1));
        drops.put(UnitTypes.atrax, ItemStack.with(Items.lead, 33, Items.silicon, 22, Items.graphite, 24, Items.titanium, 24));
        drops.put(UnitTypes.spiroct, ItemStack.with(Items.copper, 81, Items.silicon, 47, Items.metaglass, 32, Items.plastanium, 17));
        drops.put(UnitTypes.arkyid, ItemStack.with(Items.copper, 260, Items.lead, 229, Items.silicon, 167, Items.graphite, 182, Items.titanium, 300, Items.thorium, 38, Items.plastanium, 47, Items.phaseFabric, 23, Items.surgeAlloy, 11));
        drops.put(UnitTypes.toxopid, ItemStack.with(Items.copper, 360, Items.lead, 329, Items.silicon, 267, Items.graphite, 382,Items.thorium, 118, Items.plastanium, 127, Items.phaseFabric, 83, Items.surgeAlloy, 51));

        // Shooters
        drops.put(UnitTypes.dagger, ItemStack.with(Items.copper, 27, Items.silicon, 13));
        drops.put(UnitTypes.mace, ItemStack.with(Items.copper, 17, Items.lead, 43, Items.graphite, 17, Items.titanium, 17, Items.pyratite, 5));
        drops.put(UnitTypes.fortress, ItemStack.with(Items.copper, 58, Items.metaglass, 83, Items.thorium, 12, Items.pyratite, 5));
        drops.put(UnitTypes.scepter, ItemStack.with(Items.phaseFabric, 53, Items.thorium, 21));
        drops.put(UnitTypes.reign, ItemStack.with(Items.copper, 517, Items.silicon, 583, Items.silicon, 384, Items.titanium, 382, Items.thorium, 50, Items.plastanium, 12, Items.surgeAlloy, 21));


        // Lazers
        drops.put(UnitTypes.nova, ItemStack.with(Items.copper, 12, Items.lead, 12, Items.metaglass, 8));
        drops.put(UnitTypes.pulsar, ItemStack.with(Items.lead, 25, Items.silicon, 28, Items.graphite, 24, Items.metaglass, 17));
        drops.put(UnitTypes.quasar, ItemStack.with(Items.metaglass, 108, Items.plastanium, 2));
        drops.put(UnitTypes.vela, ItemStack.with(Items.phaseFabric, Items.surgeAlloy, 8, Items.plastanium, 8, Items.metaglass, 74, Items.plastanium, 34));
        drops.put(UnitTypes.corvus, ItemStack.with(Items.phaseFabric, 200, Items.surgeAlloy, 150));

        // Flyers
        drops.put(UnitTypes.flare, ItemStack.with(Items.copper, 12, Items.lead, 12, Items.metaglass, 12, Items.graphite, 24, Items.titanium, 12));
        drops.put(UnitTypes.horizon, ItemStack.with(Items.copper, 12, Items.metaglass, 8, Items.metaglass, 24, Items.plastanium, 8));
        drops.put(UnitTypes.zenith, ItemStack.with( Items.lead, 12, Items.metaglass, 8, Items.thorium, 14, Items.plastanium, 9, Items.silicon, 4));
        drops.put(UnitTypes.antumbra, ItemStack.with( Items.copper, 255, Items.silicon, 100, Items.thorium, 23, Items.plastanium, 37, Items.surgeAlloy, 17));
        drops.put(UnitTypes.eclipse, ItemStack.with( Items.surgeAlloy, 50, Items.phaseFabric, 35, Items.thorium, 46, Items.titanium, 200, Items.metaglass, 100, Items.graphite, 244));

        // Support
        drops.put(UnitTypes.mono, ItemStack.with(Items.silicon, 8, Items.metaglass, 8, Items.titanium, 4, Items.surgeAlloy, 1));
        drops.put(UnitTypes.poly, ItemStack.with(Items.copper, 8, Items.lead, 4, Items.surgeAlloy, 1, Items.graphite, 18));
        drops.put(UnitTypes.mega, ItemStack.with(Items.surgeAlloy, 1, Items.silicon, 30, Items.lead, 45, Items.plastanium, 12));
        drops.put(UnitTypes.quad, ItemStack.with(Items.surgeAlloy, 11, Items.copper, 299, Items.metaglass, 188, Items.silicon, 144, Items.plastanium, 22));
        drops.put(UnitTypes.oct, ItemStack.with(Items.lead, 300, Items.graphite, 255, Items.metaglass,300, Items.silicon, 200, Items.thorium, 268, Items.surgeAlloy, 30));

        // Ships
        drops.put(UnitTypes.risso, ItemStack.with(Items.copper, 10, Items.lead, 9, Items.titanium, 6));
        drops.put(UnitTypes.minke, ItemStack.with(Items.lead, 55, Items.graphite, 40, Items.metaglass, 25));
        drops.put(UnitTypes.bryde, ItemStack.with(Items.copper, 77, Items.silicon, 44, Items.titanium, 44, Items.plastanium, 9, Items.phaseFabric, 16));
        drops.put(UnitTypes.sei, ItemStack.with(Items.lead, 229, Items.metaglass, 103, Items.thorium, 32, Items.phaseFabric, 16));
        drops.put(UnitTypes.omura, ItemStack.with(Items.copper, 491, Items.lead, 432, Items.graphite, 313, Items.thorium, 268, Items.surgeAlloy, 30));


        itemIcons.put(Items.copper, "\uF838");
        itemIcons.put(Items.lead, "\uF837");
        itemIcons.put(Items.metaglass, "\uF836");
        itemIcons.put(Items.graphite, "\uF835");
        itemIcons.put(Items.sand, "\uF834");
        itemIcons.put(Items.coal, "\uF833");
        itemIcons.put(Items.titanium, "\uF832");
        itemIcons.put(Items.thorium, "\uF831");
        itemIcons.put(Items.scrap, "\uF830");
        itemIcons.put(Items.silicon, "\uF82F");
        itemIcons.put(Items.plastanium, "\uF82E");
        itemIcons.put(Items.phaseFabric, "\uF82D");
        itemIcons.put(Items.surgeAlloy, "\uF82C");
        itemIcons.put(Items.sporePod, "\uF82B");
        itemIcons.put(Items.blastCompound, "\uF82A");
        itemIcons.put(Items.pyratite, "\uF829");

    }
}
