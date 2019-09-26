package io.anuke.mindustry.entities.traits;

import io.anuke.mindustry.game.TypeID;

public interface TypeTrait{

    TypeID getTypeID();
    /*
    int[] lastRegisteredID = {0};
    Array<Supplier<? extends TypeTrait>> registeredTypes = new Array<>();
    ObjectIntMap<Class<? extends TypeTrait>> typeToID = new ObjectIntMap<>();

    /**
     * Register and return a type ID. The supplier should return a fresh instace of that type.

    static <T extends TypeTrait> void registerType(Class<T> type, Supplier<T> supplier){
        if(typeToID.get(type, -1) != -1){
            return; //already registered
        }

        registeredTypes.add(supplier);
        int result = lastRegisteredID[0];
        typeToID.put(type, result);
        lastRegisteredID[0]++;
    }

    /**Gets a syncable type by ID.
    static Supplier<? extends TypeTrait> getTypeByID(int id){
        if(id == -1){
            throw new IllegalArgumentException("Attempt to retrieve invalid entity type ID! Did you forget to set it in ContentLoader.registerTypes()?");
        }
        return registeredTypes.get(id);
    }

    /**
     * Returns the type ID of this entity used for intstantiation. Should be < BYTE_MAX.
     * Do not override!

    default int getTypeID(){
        int id = typeToID.get(getClass(), -1);
        if(id == -1)
            throw new RuntimeException("Class of type '" + getClass() + "' is not registered! Did you forget to register it in ContentLoader#registerTypes()?");
        return id;
    }*/
}
