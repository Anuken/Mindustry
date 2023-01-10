package mindustry.mod;

import arc.struct.*;
import mindustry.mod.Mods.*;

/**
 * Utility class for loading or sorting mods in the correct order for dependency resolution.
 */
public class ModDependencyResolver{

    public OrderedMap<String, ModState> resolveDependencies(Seq<ModMeta> metas){
        var context = new ResolutionContext();

        for(var meta : metas){
            Seq<Dependency> dependencies = new Seq<>();
            for(var dependency : meta.dependencies){
                dependencies.add(new Dependency(dependency, true));
            }
            for(var dependency : meta.softDependencies){
                dependencies.add(new Dependency(dependency, false));
            }
            context.dependencies.put(meta.name, dependencies);
        }

        for (var key : context.dependencies.keys()) {
            if (context.ordered.contains(key)) {
                continue;
            }
            resolve(key, context);
            context.visited.clear();
        }

        var result = new OrderedMap<String, ModState>();
        for(var name : context.ordered){
            result.put(name, ModState.enabled);
        }
        result.putAll(context.invalid);
        return result;
    }

    protected boolean resolve(String element, ResolutionContext context){
        context.visited.add(element);
        for(final var dependency : context.dependencies.get(element)){
            // Circular dependencies ?
            if(context.visited.contains(dependency.name) && !context.ordered.contains(dependency.name)){
                context.invalid.put(dependency.name, ModState.circularDependencies);
                return false;
            // If dependency present, resolve it, or if it's not required, ignore it
            }else if(context.dependencies.containsKey(dependency.name)){
                if(!context.ordered.contains(dependency.name) && !resolve(dependency.name, context) && dependency.required) {
                    context.invalid.put(element, ModState.incompleteDependencies);
                    return false;
                }
            // The dependency is missing, but if not required, skip
            }else if(dependency.required){
                context.invalid.put(element, ModState.missingDependencies);
                return false;
            }
        }
        if (!context.ordered.contains(element)) {
            context.ordered.add(element);
        }
        return true;
    }

    protected static class ResolutionContext{
        public final ObjectMap<String, Seq<Dependency>> dependencies = new ObjectMap<>();
        public final ObjectSet<String> visited = new ObjectSet<>();
        public final OrderedSet<String> ordered = new OrderedSet<>();
        public final ObjectMap<String, ModState> invalid = new OrderedMap<>();
    }

    protected static final class Dependency{
        public final String name;
        public final boolean required;

        public Dependency(String name, boolean required){
            this.name = name;
            this.required = required;
        }
    }
}
