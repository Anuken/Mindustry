package io.anuke.mindustry.entities.traits;

import io.anuke.ucore.entities.trait.Entity;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**Marks an entity as serializable.*/
public interface SaveTrait extends Entity, TypeTrait{
    void writeSave(DataOutput stream) throws IOException;
    void readSave(DataInput stream) throws IOException;
}
