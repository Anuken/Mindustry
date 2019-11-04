package io.anuke.mindustry.type;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.func.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.ctype.UnlockableContent;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.Objectives.*;
import io.anuke.mindustry.maps.generators.*;

import static io.anuke.mindustry.Vars.*;

public class Zone extends UnlockableContent{
    public @NonNull Generator generator;
    public @NonNull Objective configureObjective = new ZoneWave(this, 15);
    public Array<Objective> requirements = new Array<>();
    //TODO autogenerate
    public Array<Item> resources = new Array<>();

    public Cons<Rules> rules = rules -> {};
    public boolean alwaysUnlocked;
    public int conditionWave = Integer.MAX_VALUE;
    public int launchPeriod = 10;
    public Schematic loadout = Loadouts.basicShard;
    public TextureRegion preview;

    protected Array<ItemStack> baseLaunchCost = new Array<>();
    protected Array<ItemStack> startingItems = new Array<>();
    protected Array<ItemStack> launchCost;

    private Array<ItemStack> defaultStartingItems = new Array<>();

    public Zone(String name, Generator generator){
        super(name);
        this.generator = generator;
    }

    public Zone(String name){
        this(name, new MapGenerator(name));
    }

    @Override
    public void load(){
        preview = Core.atlas.find("zone-" + name, Core.atlas.find(name + "-zone"));
    }

    public Rules getRules(){
        if(generator instanceof MapGenerator){
            return ((MapGenerator)generator).getMap().rules();
        }else{
            Rules rules = new Rules();
            this.rules.get(rules);
            return rules;
        }
    }

    public boolean isLaunchWave(int wave){
        return metCondition() && wave % launchPeriod == 0;
    }

    public boolean canUnlock(){
        return data.isUnlocked(this) || !requirements.contains(r -> !r.complete());
    }

    public Array<ItemStack> getLaunchCost(){
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

    public boolean hasLaunched(){
        return Core.settings.getBool(name + "-launched", false);
    }

    public void setLaunched(){
        updateObjectives(() -> {
            Core.settings.put(name + "-launched", true);
            data.modified();
        });
    }

    public void updateWave(int wave){
        int value = Core.settings.getInt(name + "-wave", 0);

        if(value < wave){
            updateObjectives(() -> {
                Core.settings.put(name + "-wave", wave);
                data.modified();
            });
        }
    }

    public void updateObjectives(Runnable closure){
        Array<ZoneObjective> incomplete = content.zones()
            .map(z -> z.requirements).<Objective>flatten()
            .select(o -> o.zone() == this && !o.complete())
            .as(ZoneObjective.class);

        boolean wasConfig = configureObjective.complete();

        closure.run();
        for(ZoneObjective objective : incomplete){
            if(objective.complete()){
                Events.fire(new ZoneRequireCompleteEvent(objective.zone, content.zones().find(z -> z.requirements.contains(objective)), objective));
            }
        }

        if(!wasConfig && configureObjective.complete()){
            Events.fire(new ZoneConfigureCompleteEvent(this));
        }
    }

    public int bestWave(){
        return Core.settings.getInt(name + "-wave", 0);
    }

    /** @return whether initial conditions to launch are met. */
    public boolean isLaunchMet(){
        return bestWave() >= conditionWave;
    }

    public void updateLaunchCost(){
        Array<ItemStack> stacks = new Array<>();

        Cons<ItemStack> adder = stack -> {
            for(ItemStack other : stacks){
                if(other.item == stack.item){
                    other.amount += stack.amount;
                    return;
                }
            }
            stacks.add(new ItemStack(stack.item, stack.amount));
        };

        for(ItemStack stack : baseLaunchCost) adder.get(stack);
        for(ItemStack stack : startingItems) adder.get(stack);

        for(ItemStack stack : stacks){
            if(stack.amount < 0) stack.amount = 0;
        }

        stacks.sort();
        launchCost = stacks;
        Core.settings.putObject(name + "-starting-items", startingItems);
        data.modified();
    }

    /** Whether this zone has met its condition; if true, the player can leave. */
    public boolean metCondition(){
        //players can't leave in attack mode.
        return state.wave >= conditionWave && !state.rules.attackMode;
    }

    public boolean canConfigure(){
        return configureObjective.complete();
    }

    @Override
    public void init(){
        if(generator instanceof MapGenerator && mod != null){
            ((MapGenerator)generator).removePrefix(mod.name);
        }

        generator.init(loadout);
        resources.sort();

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
    public String localizedName(){
        return Core.bundle.get("zone." + name + ".name");
    }

    @Override
    public ContentType getContentType(){
        return ContentType.zone;
    }

}
