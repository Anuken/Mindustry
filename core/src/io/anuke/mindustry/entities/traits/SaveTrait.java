package io.anuke.mindustry.entities.traits;

import io.anuke.ucore.entities.component.Entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**Marks an entity as serializable.*/
public interface SaveTrait extends Entity{
    void writeSave(DataOutputStream stream) throws IOException;
    void readSave(DataInputStream stream) throws IOException;
}
