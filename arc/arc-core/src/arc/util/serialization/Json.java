package arc.util.serialization;

import arc.files.*;
import arc.struct.*;
import arc.struct.IntSet.*;
import arc.struct.ObjectMap.*;
import arc.struct.Queue;
import arc.struct.OrderedMap.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.serialization.Jval.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Reads/writes Java objects to/from JSON, automatically.
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class Json{
    private static final Object[] noDefaultValues = new Object[0];

    private final ObjectMap<Class, OrderedMap<String, FieldMetadata>> typeToFields = new ObjectMap();
    private final ObjectMap<String, Class> tagToClass = new ObjectMap();
    private final ObjectMap<Class, String> classToTag = new ObjectMap();
    private final ObjectMap<Class, JsonSerializer> classToSerializer = new ObjectMap();
    private final ObjectMap<Class, Object[]> classToDefaultValues = new ObjectMap();
    private final ObjectMap<Class, Constructor> classToConstructor = new ObjectMap();
    private final ObjectMap<String, Class> nameToClass = new ObjectMap();
    private final ObjectMap<Class, ObjectMap<String, Enum>> classToEnumConstants = new ObjectMap();
    private final Object[] equals1 = {null}, equals2 = {null};

    /** Sets the serializer to use when the type being deserialized is not known (null). */
    public @Nullable JsonSerializer<?> defaultSerializer;
    /**
     * Sets the name of the JSON field to store the Java class name or class tag when required to avoid ambiguity during
     * deserialization. Set to null to never output this information, but be warned that deserialization may fail. Default is
     * "class".
     */
    public String typeName = "class";
    /** When true, field values that are identical to a newly constructed instance are not written. Default is true. */
    public boolean skipDefaultValues = true;
    /** When true, fields in the JSON that are not found on the class will not throw a {@link SerializationException}. Default is true. */
    public boolean ignoreUnknownFields = true;
    /** When true, {@link Enum#name()} is used to write enum values. When false, {@link Enum#toString()} is used which may not be unique. Default is true. */
    public boolean enumNames = true;
    /** When true, classes not implementing Serializable are allowed. This is very unsafe! */
    public boolean allowNonSerializableClasses = false;

    /** Sets a tag to use instead of the fully qualifier class name. This can make the JSON easier to read. */
    public void addClassTag(String tag, Class type){
        tagToClass.put(tag, type);
        classToTag.put(type, tag);
    }

    /** Returns the class for the specified tag, or null. */
    public Class getClass(String tag){
        return tagToClass.get(tag);
    }

    /** Returns the tag for the specified class, or null. */
    public String getTag(Class type){
        return classToTag.get(type);
    }

    /**
     * Registers a serializer to use for the specified type instead of the default behavior of serializing all of an objects
     * fields.
     */
    public <T> void setSerializer(Class<T> type, JsonSerializer<T> serializer){
        classToSerializer.put(type, serializer);
    }

    public <T> JsonSerializer<T> getSerializer(Class<T> type){
        return classToSerializer.get(type);
    }

    /**
     * Sets the type of elements in a struct. When the element type is known, the class for each element in the struct
     * does not need to be written unless different from the element type.
     */
    public void setElementType(Class type, String fieldName, Class elementType){
        ObjectMap<String, FieldMetadata> fields = getFields(type);
        FieldMetadata metadata = fields.get(fieldName);
        if(metadata == null)
            throw new SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")");
        metadata.elementType = elementType;
    }

    @SuppressWarnings("deprecation")
    public OrderedMap<String, FieldMetadata> getFields(Class type){
        OrderedMap<String, FieldMetadata> fields = typeToFields.get(type);
        if(fields != null) return fields;

        Seq<Class> classHierarchy = new Seq();
        Class nextClass = type;
        while(nextClass != Object.class){
            classHierarchy.add(nextClass);
            nextClass = nextClass.getSuperclass();
        }
        Seq<Field> allFields = new Seq<>();
        for(int i = classHierarchy.size - 1; i >= 0; i--)
            allFields.addAll(classHierarchy.get(i).getDeclaredFields());

        OrderedMap<String, FieldMetadata> nameToField = new OrderedMap(allFields.size);

        for(Field field: allFields){
            if(Modifier.isTransient(field.getModifiers())) continue;
            if(Modifier.isStatic(field.getModifiers())) continue;
            if(field.isSynthetic() || type.isEnum() || Reflect.isWrapper(type)) continue;

            //this is deprecated, but I know what I'm doing
            if(!field.isAccessible()){
                try{
                    field.setAccessible(true);
                }catch(Exception ex){
                    continue;
                }
            }

            FieldMetadata data = new FieldMetadata(field);

            nameToField.put(field.getName(), data);
        }
        typeToFields.put(type, nameToField);
        return nameToField;
    }


    public String toJson(Object object){
        return toJson(object, object == null ? null : object.getClass(), (Class)null);
    }

    public String toJson(Object object, Class knownType){
        return toJson(object, knownType, (Class)null);
    }

    public void toUBJson(Object object, Class knownType, OutputStream stream){
        writeValue(new UBJsonWriter(stream), object, knownType, null);
    }

    public void toUBJson(Object object, Class knownType, Class elementType, OutputStream stream){
        writeValue(new UBJsonWriter(stream), object, knownType, elementType);
    }

    /**
     * @param knownType May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    public String toJson(Object object, Class knownType, Class elementType){
        StringWriter buffer = new StringWriter();
        toJson(object, knownType, elementType, buffer);
        return buffer.toString();
    }

    public void toJson(Object object, Fi file){
        toJson(object, object == null ? null : object.getClass(), null, file);
    }

    /** @param knownType May be null if the type is unknown. */
    public void toJson(Object object, Class knownType, Fi file){
        toJson(object, knownType, null, file);
    }

    /**
     * @param knownType May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    public void toJson(Object object, Class knownType, Class elementType, Fi file){
        Writer writer = null;
        try{
            writer = file.writer(false, "UTF-8");
            toJson(object, knownType, elementType, writer);
        }catch(Exception ex){
            throw new SerializationException("Error writing file: " + file, ex);
        }finally{
            Streams.close(writer);
        }
    }

    public void toJson(Object object, Writer writer){
        toJson(object, object == null ? null : object.getClass(), null, writer);
    }

    /** @param knownType May be null if the type is unknown. */
    public void toJson(Object object, Class knownType, Writer writer){
        toJson(object, knownType, null, writer);
    }

    /**
     * @param knownType May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    public void toJson(Object object, Class knownType, Class elementType, Writer writer){
        JsonWriter jsonWriter = new StringJsonWriter(writer, Jformat.minimal);
        try{
            writeValue(jsonWriter, object, knownType, elementType);
        }finally{
            Streams.close(jsonWriter);
        }
    }

    /** Writes all fields of the specified object to the current JSON object. */
    public void writeFields(JsonWriter writer, Object object){
        Class type = object.getClass();

        Object[] defaultValues = getDefaultValues(type);

        OrderedMap<String, FieldMetadata> fields = getFields(type);
        int i = 0;
        for(FieldMetadata metadata : new OrderedMapValues<>(fields)){
            Field field = metadata.field;
            try{
                Object value = field.get(object);
                if(defaultValues != null){
                    Object defaultValue = defaultValues[i++];
                    if(value == null && defaultValue == null) continue;
                    if(value != null && defaultValue != null){
                        if(value.equals(defaultValue)) continue;
                        if(value.getClass().isArray() && defaultValue.getClass().isArray()){
                            equals1[0] = value;
                            equals2[0] = defaultValue;
                            if(Arrays.deepEquals(equals1, equals2)) continue;
                        }
                    }
                }

                writer.name(field.getName());
                writeValue(writer, value, field.getType(), metadata.elementType);
            }catch(IllegalAccessException ex){
                throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", ex);
            }catch(SerializationException ex){
                ex.addTrace(field + " (" + type.getName() + ")");
                throw ex;
            }catch(Exception runtimeEx){
                SerializationException ex = new SerializationException(runtimeEx);
                ex.addTrace(field + " (" + type.getName() + ")");
                throw ex;
            }
        }
    }

    private Object[] getDefaultValues(Class type){
        if(!skipDefaultValues) return null;
        if(type.isAnonymousClass()) type = type.getSuperclass();

        Object[] cached = classToDefaultValues.get(type);
        if(cached != null) return cached == noDefaultValues ? null : cached;

        Object object;
        try{
            object = newInstance(type);
        }catch(Exception ex){
            classToDefaultValues.put(type, noDefaultValues);
            return null;
        }

        ObjectMap<String, FieldMetadata> fields = getFields(type);
        Object[] values = new Object[fields.size];
        classToDefaultValues.put(type, values);

        int i = 0;
        for(FieldMetadata metadata : fields.values()){
            Field field = metadata.field;
            try{
                values[i++] = field.get(object);
            }catch(IllegalAccessException ex){
                throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", ex);
            }catch(SerializationException ex){
                ex.addTrace(field + " (" + type.getName() + ")");
                throw ex;
            }catch(RuntimeException runtimeEx){
                SerializationException ex = new SerializationException(runtimeEx);
                ex.addTrace(field + " (" + type.getName() + ")");
                throw ex;
            }
        }
        return values;
    }

    /** @see #writeField(JsonWriter, Object, String, String, Class) */
    public void writeField(JsonWriter writer, Object object, String name){
        writeField(writer, object, name, name, null);
    }

    /**
     * @param elementType May be null if the type is unknown.
     * @see #writeField(JsonWriter, Object, String, String, Class)
     */
    public void writeField(JsonWriter writer, Object object, String name, Class elementType){
        writeField(writer, object, name, name, elementType);
    }

    /** @see #writeField(JsonWriter, Object, String, String, Class) */
    public void writeField(JsonWriter writer, Object object, String fieldName, String jsonName){
        writeField(writer, object, fieldName, jsonName, null);
    }

    /**
     * Writes the specified field to the current JSON object.
     * @param elementType May be null if the type is unknown.
     */
    public void writeField(JsonWriter writer, Object object, String fieldName, String jsonName, Class elementType){
        Class type = object.getClass();
        ObjectMap<String, FieldMetadata> fields = getFields(type);
        FieldMetadata metadata = fields.get(fieldName);
        if(metadata == null)
            throw new SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")");
        Field field = metadata.field;
        if(elementType == null) elementType = metadata.elementType;
        try{
            writer.name(jsonName);
            writeValue(writer, field.get(object), field.getType(), elementType);
        }catch(IllegalAccessException ex){
            throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", ex);
        }catch(SerializationException ex){
            ex.addTrace(field + " (" + type.getName() + ")");
            throw ex;
        }catch(Exception runtimeEx){
            SerializationException ex = new SerializationException(runtimeEx);
            ex.addTrace(field + " (" + type.getName() + ")");
            throw ex;
        }
    }

    /**
     * Writes the value as a field on the current JSON object, without writing the actual class.
     * @param value May be null.
     * @see #writeValue(JsonWriter, String, Object, Class, Class)
     */
    public void writeValue(JsonWriter writer, String name, Object value){
        writer.name(name);
        if(value == null)
            writeValue(writer, value, null, null);
        else
            writeValue(writer, value, value.getClass(), null);
    }

    /**
     * Writes the value as a field on the current JSON object, writing the class of the object if it differs from the specified
     * known type.
     * @param value May be null.
     * @param knownType May be null if the type is unknown.
     * @see #writeValue(JsonWriter, String, Object, Class, Class)
     */
    public void writeValue(JsonWriter writer, String name, Object value, Class knownType){
        writer.name(name);
        writeValue(writer, value, knownType, null);
    }

    /**
     * Writes the value as a field on the current JSON object, writing the class of the object if it differs from the specified
     * known type. The specified element type is used as the default type for collections.
     * @param value May be null.
     * @param knownType May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    public void writeValue(JsonWriter writer, String name, Object value, Class knownType, Class elementType){
        writer.name(name);
        writeValue(writer, value, knownType, elementType);
    }

    /**
     * Writes the value, without writing the class of the object.
     * @param value May be null.
     */
    public void writeValue(JsonWriter writer, Object value){
        if(value == null)
            writeValue(writer, value, null, null);
        else
            writeValue(writer, value, value.getClass(), null);
    }

    /**
     * Writes the value, writing the class of the object if it differs from the specified known type.
     * @param value May be null.
     * @param knownType May be null if the type is unknown.
     */
    public void writeValue(JsonWriter writer, Object value, Class knownType){
        writeValue(writer, value, knownType, null);
    }

    /**
     * Writes the value, writing the class of the object if it differs from the specified known type. The specified element type
     * is used as the default type for collections.
     * @param value May be null.
     * @param knownType May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    public void writeValue(JsonWriter writer, Object value, Class knownType, Class elementType){
        if(knownType != null && knownType.isAnonymousClass()){
            knownType = knownType.getSuperclass();
        }

        if(value == null){
            writer.value(null);
            return;
        }

        if((knownType != null && knownType.isPrimitive()) || knownType == String.class || Reflect.isWrapper(knownType)){
            writer.value(value);
            return;
        }

        Class actualType = value.getClass().isAnonymousClass() ? value.getClass().getSuperclass() : value.getClass();

        if(actualType.isPrimitive() || actualType == String.class || Reflect.isWrapper(actualType)){
            writeObjectStart(writer, actualType, null);
            writeValue(writer, "value", value);
            writeObjectEnd(writer);
            return;
        }

        if(value instanceof JsonSerializable){
            writeObjectStart(writer, actualType, knownType);
            ((JsonSerializable)value).write(this, writer);
            writeObjectEnd(writer);
            return;
        }

        JsonSerializer serializer = classToSerializer.get(actualType);
        if(serializer != null){
            serializer.write(this, writer, value, knownType);
            return;
        }

        // JSON array special cases.
        if(value instanceof Seq){
            if(knownType != null && actualType != knownType && actualType != Seq.class)
                throw new SerializationException("Serialization of an Array other than the known type is not supported.\n"
                + "Known type: " + knownType + "\nActual type: " + actualType);
            writer.writeArrayStart();
            Seq array = (Seq)value;
            for(int i = 0, n = array.size; i < n; i++)
                writeValue(writer, array.get(i), elementType, null);
            writer.writeArrayEnd();
            return;
        }
        if(value instanceof ObjectSet){
            if(knownType == null) knownType = ObjectSet.class;
            writeObjectStart(writer, actualType, knownType);
            writer.name("values");
            writer.writeArrayStart();
            for(Object entry : (ObjectSet)value)
                writeValue(writer, entry, elementType, null);
            writer.writeArrayEnd();
            writeObjectEnd(writer);
            return;
        }
        if(value instanceof IntSet){
            if(knownType == null) knownType = IntSet.class;
            writeObjectStart(writer, actualType, knownType);
            writer.name("values");
            writer.writeArrayStart();
            for(IntSetIterator iter = ((IntSet)value).iterator(); iter.hasNext; )
                writeValue(writer, iter.next(), Integer.class, null);
            writer.writeArrayEnd();
            writeObjectEnd(writer);
            return;
        }
        if(value instanceof IntSeq){
            writer.writeArrayStart();
            IntSeq array = (IntSeq)value;
            for(int i = 0, n = array.size; i < n; i++)
                writeValue(writer, array.get(i), Integer.class, null);
            writer.writeArrayEnd();
            return;
        }
        if(value instanceof arc.struct.Queue){
            if(knownType != null && actualType != knownType && actualType != arc.struct.Queue.class)
                throw new SerializationException("Serialization of a Queue other than the known type is not supported.\n"
                + "Known type: " + knownType + "\nActual type: " + actualType);
            writer.writeArrayStart();
            arc.struct.Queue queue = (arc.struct.Queue)value;
            for(int i = 0, n = queue.size; i < n; i++)
                writeValue(writer, queue.get(i), elementType, null);
            writer.writeArrayEnd();
            return;
        }
        if(value instanceof Collection){
            if(typeName != null && actualType != ArrayList.class && (knownType == null || knownType != actualType)){
                writeObjectStart(writer, actualType, knownType);
                writer.writeArrayStart("items");
                for(Object item : (Collection)value)
                    writeValue(writer, item, elementType, null);
                writer.writeArrayEnd();
                writeObjectEnd(writer);
            }else{
                writer.writeArrayStart();
                for(Object item : (Collection)value)
                    writeValue(writer, item, elementType, null);
                writer.writeArrayEnd();
            }
            return;
        }
        if(actualType.isArray()){
            if(elementType == null) elementType = actualType.getComponentType();
            int length = java.lang.reflect.Array.getLength(value);
            writer.writeArrayStart();
            for(int i = 0; i < length; i++)
                writeValue(writer, java.lang.reflect.Array.get(value, i), elementType, null);
            writer.writeArrayEnd();
            return;
        }

        // JSON object special cases.
        if(value instanceof ObjectMap){
            if(knownType == null) knownType = ObjectMap.class;
            writeObjectStart(writer, actualType, knownType);
            for(Entry entry : ((ObjectMap<?, ?>)value).entries()){
                writer.name(convertToString(entry.key));
                writeValue(writer, entry.value, elementType, null);
            }
            writeObjectEnd(writer);
            return;
        }
        if(value instanceof ObjectIntMap){
            if(knownType == null) knownType = ObjectIntMap.class;
            writeObjectStart(writer, actualType, knownType);
            for(ObjectIntMap.Entry entry : ((ObjectIntMap<?>)value).entries()){
                writer.name(convertToString(entry.key));
                writer.value(entry.value);
            }
            writeObjectEnd(writer);
            return;
        }
        if(value instanceof ObjectFloatMap){
            if(knownType == null) knownType = ObjectFloatMap.class;
            writeObjectStart(writer, actualType, knownType);
            for(ObjectFloatMap.Entry entry : ((ObjectFloatMap<?>)value).entries()){
                writer.name(convertToString(entry.key));
                writer.value(entry.value);
            }
            writeObjectEnd(writer);
            return;
        }
        if(value instanceof IntMap){
            if(knownType == null) knownType = IntMap.class;
            writeObjectStart(writer, actualType, knownType);
            for(IntMap.Entry entry : ((IntMap<?>)value).entries()){
                writer.name(String.valueOf(entry.key));
                writeValue(writer, entry.value, elementType, null);
            }
            writeObjectEnd(writer);
            return;
        }
        if(value instanceof ArrayMap){
            if(knownType == null) knownType = ArrayMap.class;
            writeObjectStart(writer, actualType, knownType);
            ArrayMap map = (ArrayMap)value;
            for(int i = 0, n = map.size; i < n; i++){
                writer.name(convertToString(map.keys[i]));
                writeValue(writer, map.values[i], elementType, null);
            }
            writeObjectEnd(writer);
            return;
        }
        if(value instanceof Map){
            if(knownType == null) knownType = HashMap.class;
            writeObjectStart(writer, actualType, knownType);
            for(Map.Entry entry : ((Map<?, ?>)value).entrySet()){
                writer.name(convertToString(entry.getKey()));
                writeValue(writer, entry.getValue(), elementType, null);
            }
            writeObjectEnd(writer);
            return;
        }

        // Enum special case.
        if(Enum.class.isAssignableFrom(actualType)){
            if(typeName != null && (knownType == null || knownType != actualType)){
                // Ensures that enums with specific implementations (abstract logic) serialize correctly.
                if(actualType.getEnumConstants() == null) actualType = actualType.getSuperclass();

                writeObjectStart(writer, actualType, null);
                writer.name("value");
                writer.value(convertToString((Enum)value));
                writeObjectEnd(writer);
            }else{
                writer.value(convertToString((Enum)value));
            }
            return;
        }

        writeObjectStart(writer, actualType, knownType);
        writeFields(writer, value);
        writeObjectEnd(writer);
    }

    /** @param knownType May be null if the type is unknown. */
    public void writeObjectStart(JsonWriter writer, String name, Class actualType, Class knownType){
        writer.name(name);
        writeObjectStart(writer, actualType, knownType);
    }

    /**
     * Starts writing an object, writing the actualType to a field if needed.
     * @param knownType May be null if the type is unknown.
     */
    public void writeObjectStart(JsonWriter writer, Class actualType, Class knownType){
        writer.writeObjectStart();
        if(knownType == null || knownType != actualType) writeType(writer, actualType);
    }

    public void writeObjectEnd(JsonWriter writer){
        writer.writeObjectEnd();
    }

    public void writeType(JsonWriter writer, Class type){
        if(typeName == null) return;
        String className = getTag(type);
        if(className == null) className = type.getName();
        writer.set(typeName, className);
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T fromJson(Class<T> type, Reader reader){
        return readValue(type, null, Jval.read(reader));
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T fromJson(Class<T> type, Class elementType, Reader reader){
        return readValue(type, elementType, Jval.read(reader));
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T fromJson(Class<T> type, InputStream input){
        return readValue(type, null, Jval.read(new InputStreamReader(input)));
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T fromJson(Class<T> type, Class elementType, InputStream input){
        return readValue(type, elementType, Jval.read(new InputStreamReader(input)));
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T fromJson(Class<T> type, Fi file){
        try{
            return readValue(type, null, Jval.read(file.reader()));
        }catch(Exception ex){
            throw new SerializationException("Error reading file: " + file, ex);
        }
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T fromJson(Class<T> type, Class elementType, Fi file){
        try{
            return readValue(type, elementType, Jval.read(file.reader()));
        }catch(Exception ex){
            throw new SerializationException("Error reading file: " + file, ex);
        }
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T fromJson(Class<T> type, char[] data, int offset, int length){
        return readValue(type, null, Jval.read(new String(data, offset, length)));
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T fromJson(Class<T> type, Class elementType, char[] data, int offset, int length){
        return readValue(type, elementType, Jval.read(new String(data, offset, length)));
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T fromJson(Class<T> type, String json){
        return readValue(type, null, Jval.read(json));
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T fromJson(Class<T> type, Class elementType, String json){
        return readValue(type, elementType, Jval.read(json));
    }

    public void readField(Object object, String name, Jval jsonData){
        readField(object, name, name, null, jsonData);
    }

    public void readField(Object object, String name, Class elementType, Jval jsonData){
        readField(object, name, name, elementType, jsonData);
    }

    public void readField(Object object, String fieldName, String jsonName, Jval jsonData){
        readField(object, fieldName, jsonName, null, jsonData);
    }

    /** @param elementType May be null if the type is unknown. */
    public void readField(Object object, String fieldName, String jsonName, Class elementType, Jval jsonMap){
        Class type = object.getClass();
        ObjectMap<String, FieldMetadata> fields = getFields(type);
        FieldMetadata metadata = fields.get(fieldName);
        if(metadata == null)
            throw new SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")");
        Field field = metadata.field;
        if(elementType == null) elementType = metadata.elementType;
        readField(object, field, jsonName, elementType, jsonMap);
    }

    /**
     * @param object May be null if the field is static.
     * @param elementType May be null if the type is unknown.
     */
    public void readField(Object object, Field field, String jsonName, Class elementType, Jval jsonMap){
        Jval jsonValue = jsonMap.get(jsonName);
        if(jsonValue == null) return;
        try{
            field.set(object, readValue(field.getType(), elementType, jsonValue));
        }catch(IllegalAccessException ex){
            throw new SerializationException(
            "Error accessing field: " + field.getName() + " (" + field.getDeclaringClass().getName() + ")", ex);
        }catch(SerializationException ex){
            ex.addTrace(field.getName() + " (" + field.getDeclaringClass().getName() + ")");
            throw ex;
        }catch(RuntimeException runtimeEx){
            SerializationException ex = new SerializationException(runtimeEx);
            ex.addTrace(jsonValue.toString());
            ex.addTrace(field.getName() + " (" + field.getDeclaringClass().getName() + ")");
            throw ex;
        }
    }

    public void readFields(Object object, Jval jsonMap){
        Class type = object.getClass();
        ObjectMap<String, FieldMetadata> fields = getFields(type);
        for(ObjectMap.Entry<String, Jval> entry : jsonMap.asObject()){
            String name = entry.key;
            Jval child = entry.value;
            FieldMetadata metadata = fields.get(name.indexOf(' ') < 0 ? name : name.replace(" ", "_"));
            if(metadata == null){
                if(name.equals(typeName)) continue;
                if(ignoreUnknownFields || ignoreUnknownField(type, name)){
                    continue;
                }else{
                    SerializationException ex = new SerializationException("Field not found: " + name + " (" + type.getName() + ")");
                    ex.addTrace(child.toString());
                    throw ex;
                }
            }
            Field field = metadata.field;
            try{
                field.set(object, readValue(field.getType(), metadata.elementType, child, metadata.keyType));
            }catch(IllegalAccessException ex){
                throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", ex);
            }catch(SerializationException ex){
                ex.addTrace(field.getName() + " (" + type.getName() + ")");
                throw ex;
            }catch(RuntimeException runtimeEx){
                SerializationException ex = new SerializationException(runtimeEx);
                ex.addTrace(child.toString());
                ex.addTrace(field.getName() + " (" + type.getName() + ")");
                throw ex;
            }
        }
    }

    /**
     * Called for each unknown field name encountered by {@link #readFields(Object, Jval)} when {@link #ignoreUnknownFields}
     * is false to determine whether the unknown field name should be ignored.
     * @param type The object type being read.
     * @param fieldName A field name encountered in the JSON for which there is no matching class field.
     * @return true if the field name should be ignored and an exception won't be thrown by
     * {@link #readFields(Object, Jval)}.
     */
    protected boolean ignoreUnknownField(Class type, String fieldName){
        return false;
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T readValue(String name, Class<T> type, Jval jsonMap){
        return readValue(type, null, jsonMap.get(name));
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T readValue(String name, Class<T> type, T defaultValue, Jval jsonMap){
        Jval jsonValue = jsonMap.get(name);
        if(jsonValue == null) return defaultValue;
        return readValue(type, null, jsonValue);
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T readValue(String name, Class<T> type, Class elementType, Jval jsonMap){
        return readValue(type, elementType, jsonMap.get(name));
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T readValue(String name, Class<T> type, Class elementType, T defaultValue, Jval jsonMap){
        Jval jsonValue = jsonMap.get(name);
        return readValue(type, elementType, defaultValue, jsonValue);
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T readValue(Class<T> type, Class elementType, T defaultValue, Jval jsonData){
        if(jsonData == null) return defaultValue;
        return readValue(type, elementType, jsonData);
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T readValue(Class<T> type, Jval jsonData){
        return readValue(type, null, jsonData);
    }

    public <T> T readValue(Class<T> type, Class elementType, Jval jsonData){
        return readValue(type, elementType, jsonData, null);
    }
    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    public <T> T readValue(Class<T> type, Class elementType, Jval jsonData, Class keytype){
        if(jsonData == null) return null;

        if(jsonData.isObject()){
            String className = typeName == null ? null : jsonData.getString(typeName, null);
            if(className != null){
                type = resolveClass(className);
            }

            if(type == null){
                if(defaultSerializer != null) return (T)defaultSerializer.read(this, jsonData, type);
                return (T)jsonData;
            }

            if(typeName != null && Collection.class.isAssignableFrom(type)){
                // JSON object wrapper to specify type.
                jsonData = jsonData.get("items");
                if(jsonData == null) throw new SerializationException(
                "Unable to convert object to struct: " + jsonData + " (" + type.getName() + ")");
            }else{
                JsonSerializer serializer = classToSerializer.get(type);
                if(serializer != null) return (T)serializer.read(this, jsonData, type);

                if(type == String.class || Reflect.isWrapper(type) || Enum.class.isAssignableFrom(type)){
                    return readValue("value", type, jsonData);
                }

                Object object = newInstance(type);

                if(object instanceof JsonSerializable){
                    ((JsonSerializable)object).read(this, jsonData);
                    return (T)object;
                }

                // JSON object special cases.
                if(object instanceof ObjectMap){
                    ObjectMap result = (ObjectMap)object;
                    for(ObjectMap.Entry<String, Jval> entry : jsonData.asObject()){
                        result.put(keytype != null ? readValue(keytype, null, Jval.valueOf(entry.key)) : entry.key, readValue(elementType, null, entry.value));
                    }

                    return (T)result;
                }
                if(object instanceof ObjectIntMap){
                    ObjectIntMap result = (ObjectIntMap)object;
                    for(ObjectMap.Entry<String, Jval> entry : jsonData.asObject()){
                        result.put(elementType != null ? readValue(elementType, null, Jval.valueOf(entry.key)) : entry.key, entry.value.asInt());
                    }

                    return (T)result;
                }
                if(object instanceof ObjectFloatMap){
                    ObjectFloatMap result = (ObjectFloatMap)object;
                    for(ObjectMap.Entry<String, Jval> entry : jsonData.asObject()){
                        result.put(elementType != null ? readValue(elementType, null, Jval.valueOf(entry.key)) : entry.key, entry.value.asFloat());
                    }

                    return (T)result;
                }
                if(object instanceof IntMap){
                    IntMap result = (IntMap)object;
                    for(ObjectMap.Entry<String, Jval> entry : jsonData.asObject()){
                        result.put(Integer.parseInt(entry.key), readValue(elementType, null, entry.value));
                    }

                    return (T)result;
                }
                if(object instanceof ObjectSet){
                    ObjectSet result = (ObjectSet)object;
                    Jval values = jsonData.get("values");
                    if(values != null) for(Jval child : values.asArray())
                        result.add(readValue(elementType, null, child));
                    return (T)result;
                }
                if(object instanceof IntSet){
                    IntSet result = (IntSet)object;
                    Jval values = jsonData.get("values");
                    if(values != null) for(Jval child : values.asArray())
                        result.add(child.asInt());
                    return (T)result;
                }
                if(object instanceof IntSeq){
                    IntSeq result = (IntSeq)object;
                    for(ObjectMap.Entry<String, Jval> entry : jsonData.asObject())
                        result.add(entry.value.asInt());
                    return (T)result;
                }
                if(object instanceof ArrayMap){
                    ArrayMap result = (ArrayMap)object;
                    for(ObjectMap.Entry<String, Jval> entry : jsonData.asObject())
                        result.put(entry.key, readValue(elementType, null, entry.value));

                    return (T)result;
                }
                if(object instanceof Map){
                    Map result = (Map)object;
                    for(ObjectMap.Entry<String, Jval> entry : jsonData.asObject()){
                        if(entry.key.equals(typeName)){
                            continue;
                        }
                        result.put(entry.key, readValue(elementType, null, entry.value));
                    }
                    return (T)result;
                }

                readFields(object, jsonData);
                return (T)object;
            }
        }

        if(type != null){
            JsonSerializer serializer = classToSerializer.get(type);
            if(serializer != null) return (T)serializer.read(this, jsonData, type);

            if(JsonSerializable.class.isAssignableFrom(type)){
                // A Serializable may be read as an array, string, etc, even though it will be written as an object.
                Object object = newInstance(type);
                ((JsonSerializable)object).read(this, jsonData);
                return (T)object;
            }
        }

        if(jsonData.isArray()){
            // JSON array special cases.
            if(type == null || type == Object.class) type = (Class<T>)Seq.class;
            if(Seq.class.isAssignableFrom(type)){
                Seq result = type == Seq.class ? new Seq() : (Seq)newInstance(type);
                for(Jval child : jsonData.asArray())
                    result.add(readValue(elementType, null, child));
                return (T)result;
            }
            if(IntSeq.class.isAssignableFrom(type)){
                IntSeq result = type == IntSeq.class ? new IntSeq() : (IntSeq)newInstance(type);
                for(Jval child : jsonData.asArray())
                    result.add(child.asInt());
                return (T)result;
            }
            if(ObjectSet.class.isAssignableFrom(type)){
                ObjectSet result = type == ObjectSet.class ? new ObjectSet() : (ObjectSet)newInstance(type);
                for(Jval child : jsonData.asArray())
                    result.add(readValue(elementType, null, child));
                return (T)result;
            }
            if(arc.struct.Queue.class.isAssignableFrom(type)){
                arc.struct.Queue result = type == arc.struct.Queue.class ? new arc.struct.Queue() : (Queue)newInstance(type);
                for(Jval child : jsonData.asArray())
                    result.addLast(readValue(elementType, null, child));
                return (T)result;
            }
            if(Collection.class.isAssignableFrom(type)){
                Collection result = type.isInterface() ? new ArrayList() : (Collection)newInstance(type);
                for(Jval child : jsonData.asArray())
                    result.add(readValue(elementType, null, child));
                return (T)result;
            }
            if(type.isArray()){
                Class componentType = type.getComponentType();
                if(elementType == null) elementType = componentType;
                JsonArray array = jsonData.asArray();
                Object result = java.lang.reflect.Array.newInstance(componentType, array.size);
                int i = 0;
                for(Jval child : array)
                    java.lang.reflect.Array.set(result, i++, readValue(elementType, null, child));
                return (T)result;
            }
            throw new SerializationException("Unable to convert value to required type: " + jsonData + " (" + type.getName() + ")");
        }

        if(jsonData.isNumber()){
            try{
                if(type == null || type == float.class || type == Float.class) return (T)(Float)jsonData.asFloat();
                if(type == int.class || type == Integer.class) return (T)(Integer)jsonData.asInt();
                if(type == long.class || type == Long.class) return (T)(Long)jsonData.asLong();
                if(type == double.class || type == Double.class) return (T)(Double)jsonData.asDouble();
                if(type == String.class) return (T)jsonData.asString();
                if(type == short.class || type == Short.class) return (T)(Short)jsonData.asShort();
                if(type == byte.class || type == Byte.class) return (T)(Byte)jsonData.asByte();
            }catch(NumberFormatException ignored){
            }
            jsonData = Jval.valueOf(jsonData.asString());
        }

        if(jsonData.isBoolean()){
            try{
                if(type == null || type == boolean.class || type == Boolean.class)
                    return (T)(Boolean)jsonData.asBool();
            }catch(NumberFormatException ignored){
            }
            jsonData = Jval.valueOf(jsonData.asString());
        }

        if(jsonData.isString()){
            String string = jsonData.asString();
            if(type == null || type == String.class) return (T)string;
            try{
                if(type == int.class || type == Integer.class) return (T)Integer.valueOf(string);
                if(type == float.class || type == Float.class) return string.endsWith("f") ? (T)Float.valueOf(string.substring(0, string.length() - 1)) : string.endsWith("f,") ? (T)Float.valueOf(string.substring(0, string.length() - 2)) : (T)Float.valueOf(string);
                if(type == long.class || type == Long.class) return (T)Long.valueOf(string);
                if(type == double.class || type == Double.class) return (T)Double.valueOf(string);
                if(type == short.class || type == Short.class) return (T)Short.valueOf(string);
                if(type == byte.class || type == Byte.class) return (T)Byte.valueOf(string);
            }catch(NumberFormatException ignored){
            }
            if(type == boolean.class || type == Boolean.class) return (T)Boolean.valueOf(string);
            if(type == char.class || type == Character.class) return (T)(Character)string.charAt(0);
            if(Enum.class.isAssignableFrom(type)){
                Enum result = enumValue(type, string);
                if(result != null) return (T)result;
            }
            if(type == CharSequence.class) return (T)string;
            throw new SerializationException("Unable to convert value to required type: " + jsonData + " (" + type.getName() + ")");
        }

        return null;
    }

    private Enum enumValue(Class type, String string){
        ObjectMap<String, Enum> values = classToEnumConstants.get(type);
        if(values == null){
            Enum[] constants = (Enum[])type.getEnumConstants();
            values = new ObjectMap<>(constants.length);
            for(Enum e : constants) values.put(convertToString(e), e);
            classToEnumConstants.put(type, values);
        }
        return values.get(string);
    }

    /**
     * Each field on the <code>to</code> object is set to the value for the field with the same name on the <code>from</code>
     * object. The <code>to</code> object must have at least all the fields of the <code>from</code> object with the same name and
     * type.
     */
    public void copyFields(Object from, Object to){
        copyFields(from, to, false);
    }

    public void copyFields(Object from, Object to, boolean setFinals){
        ObjectMap<String, FieldMetadata> toFields = getFields(from.getClass());
        for(ObjectMap.Entry<String, FieldMetadata> entry : getFields(from.getClass())){
            FieldMetadata toField = toFields.get(entry.key);
            Field fromField = entry.value.field;
            if(Modifier.isFinal(fromField.getModifiers()) && !setFinals) continue;

            if(toField == null) throw new SerializationException("To object is missing field" + entry.key);
            try{
                toField.field.set(to, fromField.get(from));
            }catch(IllegalAccessException ex){
                throw new SerializationException("Error copying field: " + fromField.getName(), ex);
            }
        }
    }

    protected <T> Class<T> resolveClass(String className){
        Class<T> type = getClass(className);
        if(type != null) return type;

        type = (Class<T>)nameToClass.get(className);
        if(type != null) return type;

        try{
            type = (Class<T>)Class.forName(className);
            if(allowNonSerializableClasses || JsonSerializable.class.isAssignableFrom(type) || AllowSerialization.class.isAssignableFrom(type)
                || type == String.class || type == Long.class || type == Integer.class || type == Short.class || type == Byte.class|| type == Boolean.class || type == Character.class){
                nameToClass.put(className, type);
                return type;
            }else{
                throw new SerializationException("Class deserialization not allowed: " + type);
            }
        }catch(Throwable ex){
            throw new SerializationException(ex);
        }
    }

    private String convertToString(Enum e){
        return enumNames ? e.name() : e.toString();
    }

    protected String convertToString(Object object){
        if(object instanceof Enum) return convertToString((Enum)object);
        if(object instanceof Class) return ((Class)object).getName();
        return String.valueOf(object);
    }

    protected Object newInstance(Class type){
        Constructor cached = classToConstructor.get(type);
        if(cached != null){
            try{
                return cached.newInstance();
            }catch(Exception ex){
                throw new SerializationException("Error constructing instance of class: " + type.getName(), ex);
            }
        }

        try{
            Constructor constructor = type.getDeclaredConstructor();
            Object result = constructor.newInstance();
            classToConstructor.put(type, constructor);
            return result;
        }catch(Exception ex){
            try{
                // Try a private constructor.
                Constructor constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object result = constructor.newInstance();
                classToConstructor.put(type, constructor);
                return result;
            }catch(SecurityException ignored){
            }catch(IllegalAccessException ignored){
                if(Enum.class.isAssignableFrom(type)){
                    if(type.getEnumConstants() == null) type = type.getSuperclass();
                    return type.getEnumConstants()[0];
                }
                if(type.isArray())
                    throw new SerializationException("Encountered JSON object when expected array of type: " + type.getName(), ex);
                else if(type.isMemberClass() && !Modifier.isStatic(type.getModifiers()))
                    throw new SerializationException("Class cannot be created (non-static member class): " + type.getName(), ex);
                else
                    throw new SerializationException("Class cannot be created (missing no-arg constructor): " + type.getName(), ex);
            }catch(Exception privateConstructorException){
                ex = privateConstructorException;
            }
            throw new SerializationException("Error constructing instance of class: " + type.getName(), ex);
        }
    }

    public interface JsonSerializer<T>{
        void write(Json json, JsonWriter writer, T object, Class knownType);
        T read(Json json, Jval jsonData, Class type);
    }

    public interface JsonSerializable{
        void write(Json json, JsonWriter writer);
        void read(Json json, Jval jsonData);
    }

    public static class FieldMetadata{
        public final Field field;
        public @Nullable Class elementType;
        public @Nullable Class keyType;

        public FieldMetadata(Field field){
            boolean isMap = ObjectMap.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType()) || ArrayMap.class.isAssignableFrom(field.getType());

            this.field = field;
            this.elementType = getElementType(field, isMap ? 1 : 0);
            keyType = isMap ? getElementType(field, 0) : null;
        }
    }

    static Class getElementType(Field field, int index){
        Type genericType = field.getGenericType();
        if(genericType instanceof ParameterizedType){
            Type[] actualTypes = ((ParameterizedType)genericType).getActualTypeArguments();
            if(actualTypes.length - 1 >= index){
                Type actualType = actualTypes[index];
                if(actualType instanceof Class)
                    return (Class)actualType;
                else if(actualType instanceof ParameterizedType)
                    return (Class)((ParameterizedType)actualType).getRawType();
                else if(actualType instanceof GenericArrayType){
                    Type componentType = ((GenericArrayType)actualType).getGenericComponentType();
                    if(componentType instanceof Class)
                        return java.lang.reflect.Array.newInstance((Class)componentType, 0).getClass();
                }
            }
        }
        return null;
    }
}