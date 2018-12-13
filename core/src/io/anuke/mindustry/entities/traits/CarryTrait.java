package io.anuke.mindustry.entities.traits;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.fx.UnitFx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.gen.Call;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.trait.SolidTrait;

public interface CarryTrait extends TeamTrait, SolidTrait, TargetTrait{

    @Remote(called = Loc.both, targets = Loc.both, forward = true)
    static void dropSelf(Player player){
        if(player != null && player.getCarrier() != null){
            player.getCarrier().dropCarry();
        }
    }

    @Remote(called = Loc.both, targets = Loc.both, forward = true)
    static void setCarryOf(Player player, CarryTrait trait, CarriableTrait unit){
        if(trait == null) return;
        if(player != null){ //when a server recieves this called from a player, set the carrier to the player.
            trait = player;
        }

        if(trait.getCarry() != null){ //already carrying something, drop it
            //drop current
            Effects.effect(UnitFx.unitDrop, trait.getCarry());
            trait.getCarry().setCarrier(null);
            trait.setCarry(null);

            if(unit != null){
                trait.carry(unit); //now carry this new thing
            }
        }else if(unit != null){ //not currently carrying anything, make sure it's not null
            trait.setCarry(unit);
            unit.setCarrier(trait);

            Effects.effect(UnitFx.unitPickup, trait);
        }
    }

    /**Returns the thing this carrier is carrying.*/
    CarriableTrait getCarry();

    /**Sets the carrying unit. Internal use only! Use {@link #carry(CarriableTrait)} to set state.*/
    void setCarry(CarriableTrait unit);

    /**Returns maximum mass this carrier can carry.*/
    float getCarryWeight();

    /**Drops the unit that is being carried, if applicable.*/
    default void dropCarry(){
        carry(null);
    }

    default void dropCarryLocal(){
        setCarryOf(null, this, null);
    }

    /**
     * Do not override unless absolutely necessary.
     * Carries a unit. To drop a unit, call with {@code null}.
     */
    default void carry(CarriableTrait unit){
        Call.setCarryOf(this instanceof Player ? (Player) this : null, this, unit);
    }
}
