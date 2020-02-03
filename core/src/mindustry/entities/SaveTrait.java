package mindustry.entities;

/**
 * Marks an entity as serializable.
 */
public interface SaveTrait extends Saveable{
    byte version();
}
