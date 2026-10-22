package mindustry.mod.patch;

import arc.util.*;
import arc.util.serialization.Json.*;

/** Declared type information of a patchable value, used for deserialization. */
public class PatchType{
    public final @Nullable Class<?> type, elementType, keyType;

    public PatchType(@Nullable Class<?> type, @Nullable Class<?> elementType, @Nullable Class<?> keyType){
        this.type = type;
        this.elementType = elementType;
        this.keyType = keyType;
    }

    public PatchType(FieldMetadata data){
        this(data.field.getType(), data.elementType, data.keyType);
    }

    public static PatchType of(@Nullable Class<?> type){
        return new PatchType(type, null, null);
    }
}
