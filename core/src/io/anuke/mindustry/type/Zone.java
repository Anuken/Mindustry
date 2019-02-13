package io.anuke.mindustry.type;

import io.anuke.arc.Core;
import io.anuke.arc.function.Supplier;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.maps.generators.MapGenerator;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.Vars.data;
import static io.anuke.mindustry.Vars.state;

public class Zone extends UnlockableContent{
    public final String name;
    public final MapGenerator generator;
    public ItemStack[] deployCost = {};
    public ItemStack[] startingItems = {};
    public Block[] blockRequirements = {};
    public ItemStack[] itemRequirements = {};
    public Zone[] zoneRequirements = {};
    public Item[] resources = {};
    public Supplier<Rules> rules = Rules::new;
    public boolean alwaysUnlocked;
    public int conditionWave = Integer.MAX_VALUE;
    public int configureWave = 50;
    public int launchPeriod = 10;

    public Zone(String name, MapGenerator generator){
        this.name = name;
        this.generator = generator;
    }

    /**Whether this zone has met its condition; if true, the player can leave.*/
    public boolean metCondition(){
        return state.wave >= conditionWave;
    }

    public boolean canConfigure(){
        return data.getWaveScore(this) >= configureWave;
    }

    @Override
    public void init(){
        generator.init();
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
    public String getContentName(){
        return name;
    }

    @Override
    public String localizedName(){
        return Core.bundle.get("zone."+name+".name");
    }

    @Override
    public ContentType getContentType(){
        return ContentType.zone;
    }

}
