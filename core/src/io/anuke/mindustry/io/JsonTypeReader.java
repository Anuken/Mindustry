package io.anuke.mindustry.io;

import io.anuke.arc.util.serialization.JsonValue;

public interface JsonTypeReader<T>{
    T read(JsonValue json, String name);
}
