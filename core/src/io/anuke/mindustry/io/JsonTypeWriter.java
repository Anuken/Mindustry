package io.anuke.mindustry.io;

import io.anuke.arc.util.serialization.Json;

public interface JsonTypeWriter<T>{
    void write(Json json, T object, String name);
}
