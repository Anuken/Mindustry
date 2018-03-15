package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Rectangle;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.game.Team;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.function.Consumer;

import static io.anuke.mindustry.Vars.playerGroup;
import static io.anuke.mindustry.Vars.unitGroups;

/**Utility class for unit-based interactions.*/
public class Units {

    public static void allUnits(Consumer<Unit> cons){
        for(EntityGroup<BaseUnit> group : unitGroups){
            if(!group.isEmpty()){
                for(BaseUnit unit : group.all()){
                    cons.accept(unit);
                }
            }
        }

        for(Player player : playerGroup.all()){
            cons.accept(player);
        }
    }

    public static void getNearbyEnemies(Team team, Rectangle rect, Consumer<Unit> cons){

    }
}
