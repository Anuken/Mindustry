package io.anuke.mindustry.mod;

import io.anuke.arc.util.serialization.*;
import io.anuke.mindustry.game.*;

public abstract class TypeParser<T extends Content>{
    public abstract T parse(String name, JsonValue value);
}
