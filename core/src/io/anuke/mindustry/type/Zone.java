package io.anuke.mindustry.type;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.content.Loadouts;
import io.anuke.mindustry.game.EventType.ZoneConfigureCompleteEvent;
import io.anuke.mindustry.game.EventType.ZoneRequireCompleteEvent;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.maps.generators.Generator;
import io.anuke.mindustry.world.Block;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;

public class Zone extends UnlockableContent{
    public final Generator generator;
    public Block[] blockRequirements = {};
    public ZoneRequirement[] zoneRequirements = {};
    public Item[] resources = {};
    public Consumer<Rules> rules = rules -> {};
    public boolean alwaysUnlocked;
    public int conditionWave = Integer.MAX_VALUE;
    public int configureWave = 15;
    public int launchPeriod = 10;
    public Loadout loadout = Loadouts.basicShard;

    protected ItemStack[] baseLaunchCost = {};
    protected Array<ItemStack> startingItems = new Array<>();
    protected ItemStack[] launchCost = null;

    private Array<ItemStack> defaultStartingItems = new Array<>();

    public Zone(String name, Generator generator){
        super(name);
        this.generator = generator;
    }

    public boolean isBossWave(int wave){
        return wave % configureWave == 0 && wave > 0;
    }

    public boolean isLaunchWave(int wave){
        return metCondition() && wave % launchPeriod == 0;
    }

    public ItemStack[] getLaunchCost(){
        if(launchCost == null){
            updateLaunchCost();
        }
        return launchCost;
    }

    public Array<ItemStack> getStartingItems(){
        return startingItems;
    }

    public void resetStartingItems(){
        startingItems.clear();
        defaultStartingItems.each(stack -> startingItems.add(new ItemStack(stack.item, stack.amount)));
    }

    public void updateWave(int wave){
        int value = Core.settings.getInt(name + "-wave", 0);
        if(value < wave){
            Core.settings.put(name + "-wave", wave);
            data.modified();

            for(Zone zone : content.zones()){
                ZoneRequirement req = Structs.find(zone.zoneRequirements, f -> f.zone == this);
                if(req != null && wave == req.wave + 1){
                    Events.fire(new ZoneRequireCompleteEvent(zone, this));
                }
            }

            if(wave == configureWave + 1){
                Events.fire(new ZoneConfigureCompleteEvent(this));
            }
        }
    }

    public int bestWave(){
        return Core.settings.getInt(name + "-wave", 0);
    }

    public boolean isCompleted(){
        return bestWave() >= conditionWave;
    }

    public void updateLaunchCost(){
        Array<ItemStack> stacks = new Array<>();

        Consumer<ItemStack> adder = stack -> {
            for(ItemStack other : stacks){
                if(other.item == stack.item){
                    other.amount += stack.amount;
                    return;
                }
            }
            stacks.add(new ItemStack(stack.item, stack.amount));
        };

        for(ItemStack stack : baseLaunchCost) adder.accept(stack);
        for(ItemStack stack : startingItems) adder.accept(stack);

        for(ItemStack stack : stacks){
            if(stack.amount < 0) stack.amount = 0;
        }

        stacks.sort();
        launchCost = stacks.toArray(ItemStack.class);
        Core.settings.putObject(name + "-starting-items", startingItems);
        data.modified();
    }

    /** Whether this zone has met its condition; if true, the player can leave. */
    public boolean metCondition(){
        return state.wave >= conditionWave;
    }

    public boolean canConfigure(){
        return bestWave() >= configureWave;
    }

    @Override
    public void init(){
        generator.init(loadout);
        Arrays.sort(resources);

        for(ItemStack stack : startingItems){
            defaultStartingItems.add(new ItemStack(stack.item, stack.amount));
        }

        @SuppressWarnings("unchecked")
        Array<ItemStack> arr = Core.settings.getObject(name + "-starting-items", Array.class, () -> null);
        if(arr != null){
            startingItems = arr;
        }
    }

    @Override
    public boolean alwaysUnlocked(){
        return alwaysUnlocked;
    }

    @Override
    public boolean isHidden(){
        return true;
    }

    //neither of these are implemented, as zones are not displayed in a normal fashion... yet
    @Override
    public void displayInfo(Table table){
    }

    @Override
    public TextureRegion getContentIcon(){
        return null;
    }

    @Override
    public String localizedName(){
        return Core.bundle.get("zone." + name + ".name");
    }

    @Override
    public ContentType getContentType(){
        return ContentType.zone;
    }

    public static class ZoneRequirement{
        public final Zone zone;
        public final int wave;

        public ZoneRequirement(Zone zone, int wave){
            this.zone = zone;
            this.wave = wave;
        }

        public static ZoneRequirement[] with(Object... objects){
            ZoneRequirement[] out = new ZoneRequirement[objects.length / 2];
            for(int i = 0; i < objects.length; i += 2){
                out[i / 2] = new ZoneRequirement((Zone)objects[i], (Integer)objects[i + 1]);
            }
            return out;
        }
    }

}
