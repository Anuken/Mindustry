package mindustry.type;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.maps.generators.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class SectorPreset extends UnlockableContent{
    public @NonNull FileMapGenerator generator;
    public @NonNull Planet planet;
    public @NonNull Sector sector;

    public Cons<Rules> rules = rules -> {};
    public int conditionWave = Integer.MAX_VALUE;
    public int launchPeriod = 10;

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

    public boolean hasLaunched(){
        //TODO implement
        return Core.settings.getBool(name + "-launched", false);
    }

    public int bestWave(){
        //TODO implement
        return Core.settings.getInt(name + "-wave", 0);
    }

    /** @return whether initial conditions to launch are met. */
    public boolean isLaunchMet(){
        return bestWave() >= conditionWave;
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
    public TextureRegion icon(Cicon c){
        return Icon.terrain.getRegion();
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
