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

            if ((action.type == Administration.ActionType.depositItem || action.type == Administration.ActionType.withdrawItem) && action.tile != null && action.tile.block() != null && action.tile.block() instanceof CoreBlock) {
                if (player.con != null)
                    Call.infoToast(player.con, "[accent]You can not interact with the core.", 8f);
            }
            return true;
        });


        // Spiders
        drops.put(UnitTypes.crawler, ItemStack.with(Items.copper, 20, Items.lead, 10, Items.graphite, 3));
        drops.put(UnitTypes.atrax, ItemStack.with(Items.copper, 30, Items.lead, 40, Items.graphite, 10, Items.titanium, 5));
        drops.put(UnitTypes.spiroct, ItemStack.with(Items.lead, 100, Items.silicon, 40, Items.graphite, 40, Items.thorium, 10));
        drops.put(UnitTypes.arkyid, ItemStack.with(Items.copper, 300, Items.graphite, 80, Items.metaglass, 80, Items.titanium, 80, Items.thorium, 20, Items.phaseFabric, 10));
        drops.put(UnitTypes.toxopid, ItemStack.with(Items.copper, 400, Items.lead, 400, Items.silicon, 120, Items.graphite, 120, Items.thorium, 40, Items.plastanium, 40, Items.phaseFabric, 5, Items.surgeAlloy, 15));

        // Shooters
        drops.put(UnitTypes.dagger, ItemStack.with(Items.copper, 20, Items.lead, 10, Items.silicon, 3));
        drops.put(UnitTypes.mace, ItemStack.with(Items.copper, 30, Items.lead, 40, Items.silicon, 10, Items.titanium, 5));
        drops.put(UnitTypes.fortress, ItemStack.with(Items.lead, 100, Items.silicon, 40, Items.graphite, 40, Items.thorium, 10));
        drops.put(UnitTypes.scepter, ItemStack.with(Items.copper, 300, Items.silicon, 80, Items.metaglass, 80, Items.titanium, 80, Items.thorium, 20, Items.phaseFabric, 10));
        drops.put(UnitTypes.reign, ItemStack.with(Items.copper, 400, Items.lead, 400, Items.silicon, 120, Items.graphite, 120, Items.thorium, 40, Items.plastanium, 40, Items.phaseFabric, 5, Items.surgeAlloy, 15));

        // Lazers
        drops.put(UnitTypes.nova, ItemStack.with(Items.copper, 20, Items.lead, 10, Items.metaglass, 3));
        drops.put(UnitTypes.pulsar, ItemStack.with(Items.copper, 30, Items.lead, 40, Items.metaglass, 10));
        drops.put(UnitTypes.quasar, ItemStack.with(Items.lead, 100, Items.metaglass, 40, Items.silicon, 40, Items.titanium, 80, Items.thorium, 10));
        drops.put(UnitTypes.vela, ItemStack.with(Items.copper, 300, Items.metaglass, 80, Items.graphite, 80, Items.titanium, 60, Items.plastanium, 20, Items.surgeAlloy, 5));
        drops.put(UnitTypes.corvus, ItemStack.with(Items.copper, 400, Items.lead, 400, Items.silicon, 100, Items.metaglass, 120, Items.graphite, 100, Items.titanium, 120, Items.thorium, 60, Items.phaseFabric, 10, Items.surgeAlloy, 10));

        // Flyers
        drops.put(UnitTypes.flare, ItemStack.with(Items.copper, 20, Items.lead, 10, Items.graphite, 3));
        drops.put(UnitTypes.horizon, ItemStack.with(Items.copper, 30, Items.lead, 40, Items.graphite, 10));
        drops.put(UnitTypes.zenith, ItemStack.with(Items.lead, 100, Items.silicon, 40, Items.graphite, 40, Items.titanium, 30, Items.plastanium, 10));
        drops.put(UnitTypes.antumbra, ItemStack.with(Items.copper, 300, Items.graphite, 80, Items.metaglass, 80, Items.titanium, 60, Items.surgeAlloy, 15));
        drops.put(UnitTypes.eclipse, ItemStack.with(Items.copper, 400, Items.lead, 400, Items.silicon, 120, Items.graphite, 120, Items.titanium, 120, Items.thorium, 40, Items.plastanium, 40, Items.phaseFabric, 10, Items.surgeAlloy, 5));

        // Support
        drops.put(UnitTypes.mono, ItemStack.with(Items.copper, 20, Items.lead, 10, Items.silicon, 3));
        drops.put(UnitTypes.poly, ItemStack.with(Items.copper, 30, Items.lead, 40, Items.silicon, 10, Items.titanium, 5));
        drops.put(UnitTypes.mega, ItemStack.with(Items.lead, 100, Items.silicon, 40, Items.graphite, 40, Items.thorium, 10));
        drops.put(UnitTypes.quad, ItemStack.with(Items.copper, 300, Items.silicon, 80, Items.metaglass, 80, Items.titanium, 80, Items.thorium, 20, Items.phaseFabric, 10));
        drops.put(UnitTypes.oct, ItemStack.with(Items.copper, 400, Items.lead, 400, Items.silicon, 120, Items.graphite, 120, Items.thorium, 40, Items.plastanium, 40, Items.phaseFabric, 5, Items.surgeAlloy, 15));

        // Ships
        drops.put(UnitTypes.risso, ItemStack.with(Items.copper, 20, Items.lead, 10, Items.metaglass, 3));
        drops.put(UnitTypes.minke, ItemStack.with(Items.copper, 30, Items.lead, 40, Items.metaglass, 10));
        drops.put(UnitTypes.bryde, ItemStack.with(Items.lead, 100, Items.metaglass, 40, Items.silicon, 40, Items.titanium, 80, Items.thorium, 10));
        drops.put(UnitTypes.sei, ItemStack.with(Items.copper, 300, Items.metaglass, 80, Items.graphite, 80, Items.titanium, 60, Items.plastanium, 20, Items.surgeAlloy, 5));
        drops.put(UnitTypes.omura, ItemStack.with(Items.copper, 400, Items.lead, 400, Items.silicon, 100, Items.metaglass, 120, Items.graphite, 100, Items.titanium, 120, Items.thorium, 60, Items.phaseFabric, 10, Items.surgeAlloy, 10));
        
        // Basic
        drops.put(UnitTypes.alpha, ItemStack.with(Items.copper, 30, Items.lead, 30, Items.silicon, 20, Items.graphite, 20, Items.metaglass, 20));
        drops.put(UnitTypes.beta, ItemStack.with(Items.titanium, 40, Items.thorium, 20));
        drops.put(UnitTypes.gamma, ItemStack.with(Items.plastanium, 20, Items.phaseFabric, 10, Items.surgeAlloy, 10));


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
