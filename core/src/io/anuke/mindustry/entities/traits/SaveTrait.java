package io.anuke.mindustry.entities.traits;

/**
 * Marks an entity as serializable.
 */
public interface SaveTrait extends Entity, TypeTrait, Saveable{
    byte version();
}
