package mindustry.mod.patch;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.consumers.*;

import java.lang.reflect.*;

/** An arbitrary object, with children as its (reflective) fields. */
public class ObjectNode extends KeyedNode<FieldMetadata>{

    public ObjectNode(PatchContext context, @Nullable PatchNode parent, String name, Object value, PatchType type){
        super(context, parent, name, value, type);
    }

    @Override
    public void set(String name, Jval json){
        if(!setSpecial(name, json)){
            super.set(name, json);
        }
    }

    /** Handles values that do not map directly to a field. */
    boolean setSpecial(String name, Jval json){
        var parser = context.parser;

        if(value instanceof UnitType && name.equals("controller")){
            put(name, (Func<Unit, UnitController>)(u -> parser.resolveController(json.asString()).get()));
        }else if(value instanceof UnitType && name.equals("aiController")){
            put(name, parser.resolveController(json.asString()));
        }else if(value instanceof UnitType && name.equals("type")){
            put("constructor", parser.unitType(json));
        }else if(value instanceof Block block && name.equals("consumes") && json.isObject()){
            setConsumers(block, json);
        }else{
            return false;
        }
        return true;
    }

    void setConsumers(Block block, Jval json){
        Seq<Consume> prevBuilder = Reflect.<Seq<Consume>>get(Block.class, block, "consumeBuilder").copy();
        boolean hadItems = block.hasItems, hadLiquids = block.hasLiquids, hadPower = block.hasPower, acceptedItems = block.acceptsItems;
        Runnable revert = () -> {
            if(block.isPatchContent()) return; //useless
            Reflect.set(Block.class, block, "consumeBuilder", prevBuilder);
            block.reinitializeConsumers();
            block.hasItems = hadItems;
            block.hasLiquids = hadLiquids;
            block.hasPower = hadPower;
            block.acceptsItems = acceptedItems;
        };
        context.reset(revert);

        try{
            block.hasPower = false; //if a block doesn't have a power consumer, hasPower should be false. if it does, it will get set to true in reinitializeConsumers
            context.parser.readBlockConsumers(block, json);
            block.reinitializeConsumers();
        }catch(Throwable e){
            revert.run();
            Log.err(e);
            warn("Failed to read consumers for '@': @", block, Strings.getSimpleMessage(e));
        }
    }

    @Override
    protected @Nullable FieldMetadata key(String name){
        Class<?> actualType = value.getClass();
        if(actualType.isAnonymousClass()) actualType = actualType.getSuperclass();

        FieldMetadata data = context.parser.getJson().getFields(actualType).get(name);
        if(data == null){
            warn("Unknown field '@' for class '@'", name, actualType.getSimpleName());
            return null;
        }

        Field field = data.field;
        if(field.isAnnotationPresent(NoPatch.class) || field.getDeclaringClass().isAnnotationPresent(NoPatch.class)){
            warn("Field '@' cannot be edited.", field);
            return null;
        }
        return data;
    }

    @Override
    protected PatchType childType(FieldMetadata key){
        return new PatchType(key);
    }

    @Override
    protected @Nullable Object get(FieldMetadata key){
        return Reflect.get(value, key.field);
    }

    @Override
    protected void put(FieldMetadata key, @Nullable Object value){
        Field field = key.field;
        if(value == null && !field.isAnnotationPresent(Nullable.class) && !(Vars.headless && ContentParser.implicitNullable.contains(field.getType()))){
            warn("Field '@' cannot be null.", field);
            return;
        }

        Object target = this.value;
        context.record(target, field, () -> {
            Object prev = Reflect.get(target, field);
            return () -> Reflect.set(target, field, prev);
        });
        Reflect.set(target, field, value);
    }
}
