package mindustry.game.conditions;

import arc.*;
import mindustry.type.*;

public class OnPlanet implements UnlockCondition{
    public Planet planet;

    public OnPlanet(Planet planet){
        this.planet = planet;
    }

    protected OnPlanet(){
    }

    @Override
    public boolean complete(){
        return planet.sectors.contains(Sector::hasBase);
    }

    @Override
    public String display(){
        return Core.bundle.format("requirement.onplanet", planet.localizedName);
    }

    @Override
    public String toString(){
        return "onPlanet: " + planet;
    }
}
