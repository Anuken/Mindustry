package io.anuke.mindustry.entities.traits;

import com.badlogic.gdx.utils.Array;
import io.anuke.ucore.function.Supplier;

public interface TypeTrait {
    int[] lastRegisteredID = {0};
    Array<Supplier<? extends TypeTrait>> registeredTypes = new Array<>();

    /**Register and return a type ID. The supplier should return a fresh instace of that type.*/
    static int registerType(Supplier<? extends TypeTrait> supplier){
        registeredTypes.add(supplier);
        int result = lastRegisteredID[0];
        lastRegisteredID[0] ++;
        return result;
    }

    /**Registers a syncable type by ID.*/
    static Supplier<? extends TypeTrait> getTypeByID(int id){
        if(id == -1){
            throw new IllegalArgumentException("Attempt to retrieve invalid entity type ID! Did you forget to set it in ContentLoader.registerTypes()?");
        }
        return registeredTypes.get(id);
    }

    /**Returns the type ID of this entity used for intstantiation. Should be < BYTE_MAX.*/
    int getTypeID();
}
