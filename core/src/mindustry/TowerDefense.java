
package mindustry;

import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Strings;
import arc.util.Timer;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.EventType.UnitSpawnEvent;
import mindustry.game.EventType.WaveEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;
import mindustry.net.Administration.ActionType;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

import java.awt.*;

import static mindustry.Vars.netServer;
import static mindustry.Vars.state;

public class TowerDefense {

    public static StringMap icons;
    public static ObjectMap<UnitType, ItemStack[]> drops;

    public static float multiplier = 1f;

    public static void init() {
        icons = StringMap.of(
                "copper", Iconc.itemCopper,
                "lead", Iconc.itemLead,
                "metaglass", Iconc.itemMetaglass,
                "graphite", Iconc.itemGraphite,
                "sand", Iconc.itemSand,
                "coal", Iconc.itemCoal,
                "titanium", Iconc.itemTitanium,
                "thorium", Iconc.itemThorium,
                "scrap", Iconc.itemScrap,
                "silicon", Iconc.itemSilicon,
                "plastanium", Iconc.itemPlastanium,
                "phase-fabric", Iconc.itemPhaseFabric,
                "surge-alloy", Iconc.itemSurgeAlloy,
                "spore-pod", Iconc.itemSporePod,
                "blast-compound", Iconc.itemBlastCompound,
                "pyratite", Iconc.itemPyratite
        );

        drops = ObjectMap.of(
                UnitTypes.crawler, ItemStack.with(Items.copper, 20, Items.lead, 10, Items.silicon, 3),
                UnitTypes.atrax, ItemStack.with(Items.copper, 30, Items.lead, 40, Items.graphite, 10, Items.titanium, 5),
                UnitTypes.spiroct, ItemStack.with(Items.lead, 100, Items.silicon, 40, Items.graphite, 40, Items.thorium, 10),
                UnitTypes.arkyid, ItemStack.with(Items.copper, 300, Items.graphite, 80, Items.metaglass, 80, Items.titanium, 80, Items.thorium, 20, Items.phaseFabric, 10),
                UnitTypes.toxopid, ItemStack.with(Items.copper, 400, Items.lead, 400, Items.silicon, 120, Items.graphite, 120, Items.thorium, 40, Items.plastanium, 40, Items.phaseFabric, 5, Items.surgeAlloy, 15),

                UnitTypes.dagger, ItemStack.with(Items.copper, 20, Items.lead, 10, Items.silicon, 3),
                UnitTypes.mace, ItemStack.with(Items.copper, 30, Items.lead, 40, Items.silicon, 10, Items.titanium, 5),
                UnitTypes.fortress, ItemStack.with(Items.lead, 100, Items.silicon, 40, Items.graphite, 40, Items.thorium, 10),
                UnitTypes.scepter, ItemStack.with(Items.copper, 300, Items.silicon, 80, Items.metaglass, 80, Items.titanium, 80, Items.thorium, 20, Items.phaseFabric, 10),
                UnitTypes.reign, ItemStack.with(Items.copper, 400, Items.lead, 400, Items.silicon, 120, Items.graphite, 120, Items.thorium, 40, Items.plastanium, 40, Items.phaseFabric, 5, Items.surgeAlloy, 15),

                UnitTypes.nova, ItemStack.with(Items.copper, 20, Items.lead, 10, Items.metaglass, 3),
                UnitTypes.pulsar, ItemStack.with(Items.copper, 30, Items.lead, 40, Items.metaglass, 10),
                UnitTypes.quasar, ItemStack.with(Items.lead, 100, Items.metaglass, 40, Items.silicon, 40, Items.titanium, 80, Items.thorium, 10),
                UnitTypes.vela, ItemStack.with(Items.copper, 300, Items.metaglass, 80, Items.graphite, 80, Items.titanium, 60, Items.plastanium, 20, Items.surgeAlloy, 5),
                UnitTypes.corvus, ItemStack.with(Items.copper, 400, Items.lead, 400, Items.silicon, 100, Items.metaglass, 120, Items.graphite, 100, Items.titanium, 120, Items.thorium, 60, Items.phaseFabric, 10, Items.surgeAlloy, 10),

                UnitTypes.flare, ItemStack.with(Items.copper, 20, Items.lead, 10, Items.graphite, 3, Items.scrap, 1),
                UnitTypes.horizon, ItemStack.with(Items.copper, 30, Items.lead, 40, Items.graphite, 10, Items.scrap, 2),
                UnitTypes.zenith, ItemStack.with(Items.lead, 100, Items.silicon, 40, Items.graphite, 40, Items.titanium, 30, Items.plastanium, 10, Items.scrap, 3),
                UnitTypes.antumbra, ItemStack.with(Items.copper, 300, Items.graphite, 80, Items.metaglass, 80, Items.titanium, 60, Items.surgeAlloy, 15, Items.scrap, 4),
                UnitTypes.eclipse, ItemStack.with(Items.copper, 400, Items.lead, 400, Items.silicon, 120, Items.graphite, 120, Items.titanium, 120, Items.thorium, 40, Items.plastanium, 40, Items.phaseFabric, 10, Items.surgeAlloy, 5, Items.scrap, 5),

                UnitTypes.mono, ItemStack.with(Items.copper, 20, Items.lead, 10, Items.silicon, 3),
                UnitTypes.poly, ItemStack.with(Items.copper, 30, Items.lead, 40, Items.silicon, 10, Items.titanium, 5),
                UnitTypes.mega, ItemStack.with(Items.lead, 100, Items.silicon, 40, Items.graphite, 40, Items.thorium, 10),
                UnitTypes.quad, ItemStack.with(Items.copper, 300, Items.silicon, 80, Items.metaglass, 80, Items.titanium, 80, Items.thorium, 20, Items.phaseFabric, 10),
                UnitTypes.oct, ItemStack.with(Items.copper, 400, Items.lead, 400, Items.silicon, 120, Items.graphite, 120, Items.thorium, 40, Items.plastanium, 40, Items.phaseFabric, 5, Items.surgeAlloy, 15),

                UnitTypes.risso, ItemStack.with(Items.copper, 20, Items.lead, 10, Items.metaglass, 3),
                UnitTypes.minke, ItemStack.with(Items.copper, 30, Items.lead, 40, Items.metaglass, 10),
                UnitTypes.bryde, ItemStack.with(Items.lead, 100, Items.metaglass, 40, Items.silicon, 40, Items.titanium, 80, Items.thorium, 10),
                UnitTypes.sei, ItemStack.with(Items.copper, 300, Items.metaglass, 80, Items.graphite, 80, Items.titanium, 60, Items.plastanium, 20, Items.surgeAlloy, 5),
                UnitTypes.omura, ItemStack.with(Items.copper, 400, Items.lead, 400, Items.silicon, 100, Items.metaglass, 120, Items.graphite, 100, Items.titanium, 120, Items.thorium, 60, Items.phaseFabric, 10, Items.surgeAlloy, 10),

                UnitTypes.retusa, ItemStack.with(Items.copper, 8, Items.lead, 2, Items.scrap, 8),
                UnitTypes.oxynoe, ItemStack.with(Items.copper, 12, Items.lead, 4, Items.scrap, 16, Items.silicon, 8, Items.plastanium, 2),
                UnitTypes.cyerce, ItemStack.with(Items.lead, 23, Items.metaglass, 27, Items.scrap, 86, Items.phaseFabric, 2, Items.thorium, 4),
                UnitTypes.aegires, ItemStack.with(Items.silicon, 47, Items.phaseFabric, 8, Items.surgeAlloy, 4, Items.plastanium, 18, Items.thorium, 18),
                UnitTypes.navanax, ItemStack.with(Items.surgeAlloy, 50, Items.phaseFabric, 50),

                UnitTypes.alpha, ItemStack.with(Items.copper, 30, Items.lead, 30, Items.silicon, 20, Items.graphite, 20, Items.metaglass, 20),
                UnitTypes.beta, ItemStack.with(Items.titanium, 40, Items.thorium, 20),
                UnitTypes.gamma, ItemStack.with(Items.plastanium, 20, Items.phaseFabric, 10, Items.surgeAlloy, 10)
        );

        netServer.admins.addActionFilter(action -> {
            if (action.type == ActionType.placeBlock || action.type == ActionType.breakBlock) {
                if (!(proximityCheck(action.tile, action.block) || action.block == Blocks.shockMine || action.block instanceof CoreBlock)) {
                    Call.label(action.player.con, "[scarlet]\uE868", 4f, action.tile.worldx() + (1 - (action.block.size % 2)) * 4, action.tile.worldy() + (1 - (action.block.size % 2)) * 4);
                    return false;
                }
            }

            if ((action.type == ActionType.depositItem || action.type == ActionType.withdrawItem) && action.tile.block() != null && action.tile.block() instanceof CoreBlock) {
                Call.label(action.player.con, "[scarlet]\uE868", 4f, action.tile.worldx() + (1 - (action.tile.block().size % 2)) * 4, action.tile.worldy() + (1 - (action.tile.block().size % 2)) * 4);
                return false;
            }

            return true;
        });

        Events.on(WorldLoadEvent.class, event -> multiplier = 1f);

        Events.on(WaveEvent.class, event -> multiplier = Mathf.clamp(((state.wave * state.wave / 3175f) + 0.5f), multiplier, 100f));

        Events.on(UnitDestroyEvent.class, event -> {
            Unit unit = event.unit;

            if (unit.team == state.rules.waveTeam) {
                CoreBuild core = state.rules.defaultTeam.core();
                ItemStack[] stacks = drops.get(unit.type);

                if (core == null || stacks == null || state.gameOver) return;

                StringBuilder message = new StringBuilder();

                for (int i = 0; i < stacks.length; i++) {
                    ItemStack stack = stacks[i];

                    Item item = stack.item;
                    int amount = Mathf.random(stack.amount - stack.amount / 2, stack.amount + stack.amount / 2);

                    message.append("[accent]+").append(amount).append(" [white]").append(icons.get(item.name, "[scarlet]?")).append("  ");
                    if (i > 0 && i % 3 == 0) {
                        message.append("\n");
                    }

                    Call.transferItemTo(unit, item, core.tile.build.acceptStack(item, amount, core), unit.x + Mathf.range(8f), unit.y + Mathf.range(8f), core);
                }

                Call.label(message.toString(), Strings.stripColors(message.toString().trim()).length() / 12f, unit.x + Mathf.range(4f), unit.y + Mathf.range(4f));
            }
        });

        Events.on(UnitSpawnEvent.class, event -> {
            if (event.unit.team() == state.rules.waveTeam) {
                event.unit.maxHealth = event.unit.maxHealth * multiplier;
                event.unit.health = event.unit.maxHealth;
                event.unit.damageMultiplier = 0f;
                event.unit.apply(StatusEffects.disarmed, Float.MAX_VALUE);
            }
        });

        Timer.schedule(() -> Groups.player.each(player -> Call.infoPopup(player.con, Strings.format("[yellow]\uE86D[accent] Units health multiplier: @@x", trafficLightColor(multiplier), String.valueOf(multiplier).length() > 3 ? String.valueOf(multiplier).substring(0, 4) : multiplier), 1f, 20, 50, 20, 450, 0)), 0f, 1f);
    }

    public static String trafficLightColor(float value) {
        return "[#" + Integer.toHexString(Color.HSBtoRGB(value / 3f, 1f, 1f)).substring(2) + "]";
    }

    public static boolean proximityCheck(Tile tile, Block block) {
        return !tile.getLinkedTilesAs(block, new Seq<>()).contains(check -> check.floor() == Blocks.darkPanel4 || check.floor() == Blocks.darkPanel5);
    }
}
