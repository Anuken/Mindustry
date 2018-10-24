package io.anuke.mindustry.type;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.StatusController.StatusEntry;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.game.Content;

public class StatusEffect extends Content{
    /**Duration of this status effect in ticks at maximum power.*/
    public final float baseDuration;

    public float damageMultiplier = 1f; //damage dealt
    public float armorMultiplier = 1f; //armor points
    public float speedMultiplier = 1f; //speed
    public Color color = Color.WHITE.cpy(); //tint color

    /**Set of 'opposite' effects, which will decrease the duration of this effect when applied.*/
    protected ObjectSet<StatusEffect> opposites = new ObjectSet<>();
    /**The strength of time decrease when met with an opposite effect, as a fraction of the other's duration.*/
    protected float oppositeScale = 0.5f;

    public StatusEffect(float baseDuration){
        this.baseDuration = baseDuration;
    }

    /**Runs every tick on the affected unit while time is greater than 0.*/
    public void update(Unit unit, float time){
    }

    /**
     * Called when transitioning between two status effects.
     *
     * @param to The state to transition to
     * @param time The current status effect time
     * @param newTime The time that the new status effect will last
     */
    public StatusEntry getTransition(Unit unit, StatusEffect to, float time, float newTime, StatusEntry result){
        if(opposites.contains(to)){
            time -= newTime * oppositeScale;
            if(time > 0){
                return result.set(this, time);
            }
        }

        return result.set(to, newTime);
    }

    /**Called when this effect transitions to a new status effect.*/
    public void onTransition(Unit unit, StatusEffect to){
    }

    public boolean isOpposite(StatusEffect other){
        return opposites.size > 0 && opposites.contains(other);
    }

    public void setOpposites(StatusEffect... effects){
        for(StatusEffect e : effects){
            opposites.add(e);
        }
    }

    @Override
    public ContentType getContentType(){
        return ContentType.status;
    }
}
