package mindustry.type;

import arc.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Objectives.*;
import mindustry.maps.generators.*;

import static mindustry.Vars.*;

public class SectorPreset extends UnlockableContent{
    public @NonNull FileMapGenerator generator;
    public @NonNull Planet planet;
    public @NonNull Sector sector;
    public Seq<Objective> requirements = new Seq<>();

    public Cons<Rules> rules = rules -> {};
    public int conditionWave = Integer.MAX_VALUE;
    public int launchPeriod = 10;
    public Schematic loadout = Loadouts.basicShard;

    protected Seq<ItemStack> baseLaunchCost = new Seq<>();
    protected Seq<ItemStack> startingItems = new Seq<>();
    protected Seq<ItemStack> launchCost;
    protected Seq<ItemStack> defaultStartingItems = new Seq<>();

    public SectorPreset(String name, Planet planet, int sector){
        super(name);
        this.generator = new FileMapGenerator(name);
        this.planet = planet;
        this.sector = planet.sectors.get(sector);

        planet.preset(sector, this);
    }

    public Rules getRules(){
        return generator.map.rules();
    }

    public boolean isLaunchWave(int wave){
        return metCondition() && wave % launchPeriod == 0;
    }

    public boolean canUnlock(){
        return data.isUnlocked(this) || !requirements.contains(r -> !r.complete());
    }

    public Seq<ItemStack> getLaunchCost(){
        if(launchCost == null){
            updateLaunchCost();
        }
        return launchCost;
    }

    public Seq<ItemStack> getStartingItems(){
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
        Seq<SectorObjective> incomplete = content.sectors()
            .flatMap(z -> z.requirements)
            .filter(o -> o.zone() == this && !o.complete()).as();

        closure.run();
        for(SectorObjective objective : incomplete){
            if(objective.complete()){
                Events.fire(new ZoneRequireCompleteEvent(objective.preset, content.sectors().find(z -> z.requirements.contains(objective)), objective));
            }
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
        Seq<ItemStack> stacks = new Seq<>();

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
        data.modified();
    }

    /** Whether this zone has met its condition; if true, the player can leave. */
    public boolean metCondition(){
        //players can't leave in attack mode.
        return state.wave >= conditionWave && !state.rules.attackMode;
    }

    public boolean canConfigure(){
        return true;
    }

    @Override
    public void init(){

        for(ItemStack stack : startingItems){
            defaultStartingItems.add(new ItemStack(stack.item, stack.amount));
        }
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
    public ContentType getContentType(){
        return ContentType.sector;
    }

}
