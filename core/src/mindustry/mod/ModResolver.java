package mindustry.mod;

import arc.struct.*;
import mindustry.mod.Mods.*;

/**
 * Utility class for loading or sorting mods in the correct order for dependency resolution.
 */
public class ModResolver{

    public ResolutionContext resolveDependencies(Seq<ModMeta> metas){
        ResolutionContext context = new ResolutionContext();

        for(ModMeta meta : metas){
            Seq<Dependency> dependencies = new Seq<>();
            for(String dependency : meta.dependencies){
                dependencies.add(new Dependency(dependency, true));
            }
            for(String dependency : meta.softDependencies){
                dependencies.add(new Dependency(dependency, false));
            }
            context.dependencies.put(meta.name, dependencies);
        }

        for (String key : context.dependencies.keys()) {
            if (context.ordered.contains(key)) {
                continue;
            }
            resolve(key, context);
            context.visited.clear();
        }

        return context;
    }

    protected boolean resolve(String element, ResolutionContext context){
        context.visited.add(element);
        for (final var dependency : context.dependencies.get(element)) {
            // Circular dependencies ?
            if (context.visited.contains(dependency.name) && !context.ordered.contains(dependency.name)) {
                context.invalid.put(dependency.name, InvalidReason.circular);
                return false;
                // If dependency present, resolve it, or if it's not required, ignore it
            } else if (context.dependencies.containsKey(dependency.name)) {
                if (!context.ordered.contains(dependency.name) && !resolve(dependency.name, context) && dependency.required) {
                    context.invalid.put(element, InvalidReason.incomplete);
                    return false;
                }
                // The dependency is missing, but if not required, skip
            } else if (dependency.required) {
                context.invalid.put(element, InvalidReason.incomplete);
                context.invalid.put(dependency.name, InvalidReason.missing);
                return false;
            }
        }
        if (!context.ordered.contains(element)) {
            context.ordered.add(element);
        }
        return true;
    }

    public static final class ResolutionContext {
        public final ObjectMap<String, Seq<Dependency>> dependencies = new ObjectMap<>();
        public final OrderedSet<String> ordered = new OrderedSet<>();
        public final ObjectSet<String> visited = new ObjectSet<>();
        public final ObjectMap<String, InvalidReason> invalid = new ObjectMap<>();
    }

    public static final class Dependency{
        public final String name;
        public final boolean required;

        public Dependency(String name, boolean required){
            this.name = name;
            this.required = required;
        }
    }

    public enum InvalidReason {
        circular, missing, incomplete
    }
}
