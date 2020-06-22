package mindustry.entities.traits;

public interface DamageTrait{
    float damage();

    default void killed(Entity other){

    }
}
