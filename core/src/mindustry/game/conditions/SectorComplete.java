package mindustry.game.conditions;

import arc.*;
import mindustry.type.*;

public class SectorComplete implements UnlockCondition{
    public SectorPreset preset;

    public SectorComplete(SectorPreset zone){
        this.preset = zone;
    }

    protected SectorComplete(){
    }

    @Override
    public boolean complete(){
        return preset.sector.save != null && preset.sector.isCaptured() && preset.sector.hasBase();
    }

    @Override
    public String display(){
        return Core.bundle.format("requirement.capture", preset.localizedName);
    }

    @Override
    public String toString(){
        return "sectorComplete: " + preset;
    }
}
