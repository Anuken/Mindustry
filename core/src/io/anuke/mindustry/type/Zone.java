package io.anuke.mindustry.type;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.function.Supplier;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.content.Loadouts;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.game.EventType.ZoneCompleteEvent;
import io.anuke.mindustry.game.EventType.ZoneConfigureCompleteEvent;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.maps.generators.Generator;
import io.anuke.mindustry.maps.generators.MapGenerator;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.Vars.data;
import static io.anuke.mindustry.Vars.state;

public class Zone extends UnlockableContent{
    public final Generator generator;
    public Block[] blockRequirements = {};
    public ItemStack[] itemRequirements = {};
    public Zone[] zoneRequirements = {};
    public Item[] resources = {};
    public Supplier<Rules> rules = Rules::new;
    public boolean alwaysUnlocked;
    public int conditionWave = Integer.MAX_VALUE;
    public int configureWave = 40;
    public int launchPeriod = 10;
    public Loadout loadout = Loadouts.basicShard;

    protected ItemStack[] baseLaunchCost = {};
    protected Array<ItemStack> startingItems = new Array<>();
    protected ItemStack[] launchCost = null;

    public Zone(String name, MapGenerator generator){
        super(name);
        this.generator = generator;
    }

    protected SpawnGroup bossGroup(UnitType type){
        return new SpawnGroup(type){{
            begin = configureWave-1;
            effect = StatusEffects.boss;
            unitScaling = 1;
            spacing = configureWave;
        }};
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

    public void updateWave(int wave){
        int value = Core.settings.getInt(name + "-wave", 0);
        if(value < wave){
            Core.settings.put(name + "-wave", wave);
            data.modified();

            if(wave == conditionWave + 1){
                Events.fire(new ZoneCompleteEvent(this));
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

        //TODO optimize
        for(ItemStack stack : baseLaunchCost){
            ItemStack out = new ItemStack(stack.item, stack.amount);
            for(ItemStack other : startingItems){
                if(other.item == out.item){
                    out.amount += other.amount;
                    out.amount = Math.max(out.amount, 0);
                }
            }
            stacks.add(out);
        }

        for(ItemStack other : startingItems){
            if(stacks.find(s -> s.item == other.item) == null){
                stacks.add(other);
            }
        }

        stacks.sort();
        launchCost = stacks.toArray(ItemStack.class);
        Core.settings.putObject(name + "-starting-items", startingItems);
        data.modified();
    }

    /**Whether this zone has met its condition; if true, the player can leave.*/
    public boolean metCondition(){
        return state.wave >= conditionWave;
    }

    public boolean canConfigure(){
        return bestWave() >= configureWave;
    }

    @Override
    public void init(){
        generator.init(loadout);

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
    public void displayInfo(Table table){}

    @Override
    public TextureRegion getContentIcon(){ return null; }

    @Override
    public String localizedName(){
        return Core.bundle.get("zone."+name+".name");
    }

    @Override
    public ContentType getContentType(){
        return ContentType.zone;
    }

}
