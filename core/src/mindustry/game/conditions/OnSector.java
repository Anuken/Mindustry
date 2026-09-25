package mindustry.game.conditions;

import arc.*;
import mindustry.type.*;

public class OnSector implements UnlockCondition{
    public SectorPreset preset;

    public OnSector(SectorPreset zone){
        this.preset = zone;
    }

    protected OnSector(){
    }

    @Override
    public boolean complete(){
        return preset.sector.hasBase();
    }

    @Override
    public String display(){
        return Core.bundle.format("requirement.onsector", preset.localizedName);
    }

    @Override
    public String toString(){
        return "onSector: " + preset;
    }
}
