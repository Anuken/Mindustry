package mindustry.mod.patch;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.entities.part.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.draw.*;

/** State shared by all nodes of the patches applied by one patcher. Tracks how to revert every edit. */
public class PatchContext{
    public final ContentParser parser;

    private final Cons<String> warner;
    private final Seq<Runnable> resetters = new Seq<>();
    private final Seq<Runnable> afterCallbacks = new Seq<>();
    private final ObjectSet<Object> patched = new ObjectSet<>();
    private final ObjectSet<Edit> edits = new ObjectSet<>();

    public PatchContext(ContentParser parser, Cons<String> warner){
        this.parser = parser;
        this.warner = warner;
    }

    public void warn(String text, Object... args){
        warner.get(Strings.format(text, args));
    }

    public boolean isPatched(Object object){
        return patched.contains(object);
    }

    public void reset(Runnable run){
        resetters.add(run);
    }

    public void after(Runnable run){
        afterCallbacks.add(run);
    }

    /** Registers a revert action, created only on the first edit of this slot of the target. */
    public void record(Object target, @Nullable Object slot, Prov<Runnable> revert){
        if(edits.add(new Edit(target, slot))){
            resetters.add(revert.get());
        }
    }

    public void visit(Object object){
        if(object instanceof Content c && patched.add(c)){
            after(c::afterPatch);
        }
    }

    /** Parses a value, initializing any objects that get created. */
    public Object read(PatchNode owner, PatchType type, Jval json){
        if(type.type == null){
            throw new IllegalArgumentException("Unknown type for value: " + json);
        }
        if(json.isObject() && UnlockableContent.class.isAssignableFrom(type.type)){
            throw new IllegalArgumentException("New content must not be instantiated: " + json);
        }

        parser.listeners.add((t, data, result) -> created(owner, result));
        try{
            return parser.getJson().readValue(type.type, type.elementType, json, type.keyType);
        }finally{
            parser.listeners.pop();
        }
    }

    void created(PatchNode owner, Object object){
        if(object instanceof Weapon weapon){
            weapon.init();
        }else if(object instanceof Content cont){
            cont.init();
            cont.postInit();
        }

        if(Vars.headless) return;

        Object parent = null;
        for(PatchNode node = owner; node != null; node = node.parent){
            if(node.value instanceof Content || node.value instanceof Weapon){
                parent = node.value;
                break;
            }
        }

        if(object instanceof DrawPart part && parent instanceof MappableContent cont){
            part.load(cont.name);
        }else if(object instanceof DrawPart part && parent instanceof Weapon w){
            part.load(w.name);
        }else if(object instanceof DrawBlock draw && parent instanceof Block block){
            draw.load(block);
        }else if(object instanceof Weapon weapon){
            weapon.load();
        }else if(object instanceof Content cont){
            cont.load();
        }
    }

    public void finish(){
        afterCallbacks.each(Runnable::run);
    }

    /** Reverts all recorded edits in reverse order. */
    public void revert(){
        resetters.reverse();
        for(var reset : resetters){
            try{
                reset.run();
            }catch(Throwable e){
                Log.err("Failed to un-apply patch!", e);
            }
        }

        finish();

        resetters.clear();
        afterCallbacks.clear();
        patched.clear();
        edits.clear();
    }

    private static class Edit{
        final Object target;
        final @Nullable Object slot;

        Edit(Object target, @Nullable Object slot){
            this.target = target;
            this.slot = slot;
        }

        //targets are compared by identity, as collections have value-based equality
        @Override
        public boolean equals(Object o){
            return o instanceof Edit e && e.target == target && Structs.eq(e.slot, slot);
        }

        @Override
        public int hashCode(){
            return System.identityHashCode(target) * 31 + (slot == null ? 0 : slot.hashCode());
        }
    }
}
