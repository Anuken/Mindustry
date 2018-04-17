package io.anuke.mindustry.entities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**Marks an entity as serializable.*/
public interface SerializableEntity {
    void writeSave(DataOutputStream stream) throws IOException;
    void readSave(DataInputStream stream) throws IOException;
}
