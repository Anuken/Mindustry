package mindustry.tools;

import arc.files.*;
import arc.struct.*;

import java.lang.reflect.*;
import java.util.*;

/** Generates a .d.ts file for JS type hints. */
public class TypescriptDtsGenerator{

    /** Java types with no reasonable TS analogue - erased to `any`. */
    private static final Set<Class<?>> erase = new HashSet<>(Arrays.asList(
    Class.class, Object.class // Object is intentionally erased, not mapped to `object`, since Java's Object methods (equals/hashCode/toString) are rarely what someone typing `.` after an `any`-ish value wants
    ));

    private static final String rhinoPrelude =
    "// --- Rhino script-runtime globals (not reflected - hand-written) ---\n" +
    "declare function extend(base: any, ...args: any[]): any;\n" +
    "declare function extendContent(base: any, name: string, fields: object): any;\n" +
    "declare function require(module: string): any;\n" +
    "declare function print(text: string): void;\n" +
    "declare function output(...args: any[]): void;\n" +
    "declare function importPackage(...args: any[]): void;\n" +
    "declare function importClass(...args: any[]): void;\n" +
    "declare const Packages: any;\n\n";

    public static void writeDts(Seq<Class<?>> classes, String outPath) throws Exception{
        //keep only public top-level-ish members; also drop the couple of guaranteed-duplicate
        //simple names up front, otherwise later `declare class Foo` collides with an earlier one.
        var byName = new HashMap<String, Class<?>>();
        var dropped = new Seq<String>();

        for(Class<?> c : classes){
            if(!Modifier.isPublic(c.getModifiers())) continue;
            if(c.getSimpleName().isEmpty()) continue; //anonymous, shouldn't happen post-filter but be safe

            Class<?> existing = byName.get(c.getSimpleName());
            if(existing != null){
                //keep whichever one is NOT in the more "internal" looking package, as a heuristic;
                //in practice these collisions are rare enough to just log & keep the first seen.
                dropped.add(c.getName() + " (duplicate simple name of " + existing.getName() + ")");
                continue;
            }
            byName.put(c.getSimpleName(), c);
        }

        if(dropped.any()){
            arc.util.Log.warn("Dropped @ classes from .d.ts due to simple-name collisions:\n@",
            dropped.size, dropped.toString("\n"));
        }

        Set<String> known = byName.keySet();
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated file. Do not modify.\n");
        sb.append("// Ambient global declarations for Mindustry/Arc JS modding (Rhino).\n");
        sb.append("// This file is for editor tooling ONLY - it is never compiled or executed.\n");
        sb.append("// Classes are declared as globals (not modules) because importPackage()\n");
        sb.append("// puts them directly in global scope at runtime.\n\n");

        sb.append(rhinoPrelude);

        var sorted = Seq.with(byName.values());
        sorted.sortComparing(Class::getName);

        for(Class<?> c : sorted){
            try{
                emitType(sb, c, known);
            }catch(Throwable t){
                //never let one weird class (e.g. something with exotic generics) kill the whole run
                arc.util.Log.warn("Skipped @ in .d.ts generation: @", c.getName(), t.toString());
            }
        }

        new Fi(outPath).writeString(sb.toString());
        arc.util.Log.info("Generated .d.ts @ (@ classes, @ dropped).", outPath, sorted.size, dropped.size);
    }

    private static void emitType(StringBuilder sb, Class<?> c, Set<String> known){
        boolean isInterface = c.isInterface();
        boolean isEnum = c.isEnum();

        sb.append("declare ").append(isInterface ? "interface" : "class").append(" ").append(c.getSimpleName());
        appendTypeParams(sb, c.getTypeParameters());

        if(!isInterface){
            Class<?> sup = c.getSuperclass();
            if(sup != null && sup != Object.class && known.contains(sup.getSimpleName())){
                sb.append(" extends ").append(sup.getSimpleName());
            }
            //TS `declare class` can't implement multiple interfaces meaningfully for hint purposes;
            //skip `implements` entirely rather than emit something that reads as a real contract.
        }else{
            var exts = new Seq<String>();
            for(Class<?> i : c.getInterfaces()){
                if(known.contains(i.getSimpleName())) exts.add(i.getSimpleName());
            }
            if(exts.any()) sb.append(" extends ").append(exts.toString(", "));
        }

        sb.append("{\n");

        if(isEnum){
            for(Object constant : c.getEnumConstants()){
                sb.append("    static readonly ").append(((Enum<?>)constant).name())
                .append(": ").append(c.getSimpleName()).append(";\n");
            }
            sb.append("    static readonly values: () => ").append(c.getSimpleName()).append("[];\n");
            sb.append("    readonly name: string;\n");
            sb.append("    readonly ordinal: number;\n");
        }

        var seenSigs = new ObjectSet<String>();

        //fields
        for(Field f : c.getDeclaredFields()){
            if(!Modifier.isPublic(f.getModifiers()) || f.isSynthetic()) continue;
            if(isEnum && f.isEnumConstant()) continue;

            String mod = Modifier.isStatic(f.getModifiers()) ? "static " : "";
            String ro = Modifier.isFinal(f.getModifiers()) ? "readonly " : "";
            String sig = mod + ro + f.getName();
            if(!seenSigs.add(sig)) continue;

            sb.append("    ").append(mod).append(ro).append(sanitizeName(f.getName()))
            .append(": ").append(typeName(f.getGenericType(), known)).append(";\n");
        }

        //constructors (skipped for interfaces)
        if(!isInterface){
            for(Constructor<?> ctor : c.getDeclaredConstructors()){
                if(!Modifier.isPublic(ctor.getModifiers())) continue;
                String params = paramList(ctor.getParameters(), known);
                String sig = "new(" + params + ")";
                if(!seenSigs.add(sig)) continue;
                sb.append("    constructor(").append(params).append(");\n");
            }
        }

        //methods
        for(Method m : c.getDeclaredMethods()){
            if(!Modifier.isPublic(m.getModifiers()) || m.isSynthetic() || m.isBridge()) continue;
            //skip Object-inherited noise that clutters completion (still callable at runtime, just not worth listing)
            if(isObjectMethod(m)) continue;

            String mod = Modifier.isStatic(m.getModifiers()) ? "static " : "";
            String params = paramList(m.getParameters(), known);
            String ret = typeName(m.getGenericReturnType(), known);

            String sig = mod + m.getName() + "(" + params + ")";
            if(!seenSigs.add(sig)) continue; //identical erasure to an already-emitted overload

            sb.append("    ").append(mod).append(sanitizeName(m.getName()))
            .append("(").append(params).append("): ").append(ret).append(";\n");
        }

        sb.append("}\n\n");
    }

    private static boolean isObjectMethod(Method m){
        try{
            Object.class.getDeclaredMethod(m.getName(), m.getParameterTypes());
            return true;
        }catch(NoSuchMethodException e){
            return false;
        }
    }

    private static void appendTypeParams(StringBuilder sb, TypeVariable<?>[] params){
        if(params.length == 0) return;
        sb.append("<");
        for(int i = 0; i < params.length; i++){
            if(i > 0) sb.append(", ");
            sb.append(params[i].getName());
        }
        sb.append(">");
    }

    private static String paramList(Parameter[] params, Set<String> known){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < params.length; i++){
            if(i > 0) sb.append(", ");
            Parameter p = params[i];
            String name = p.isNamePresent() ? sanitizeName(p.getName()) : "arg" + i;
            if(p.isVarArgs()){
                Class<?> comp = p.getType().getComponentType();
                sb.append("...").append(name).append(": ").append(typeName(comp, known)).append("[]");
            }else{
                sb.append(name).append(": ").append(typeName(p.getParameterizedType(), known));
            }
        }
        return sb.toString();
    }

    private static String sanitizeName(String name){
        return switch(name){
            case "in", "function", "package", "class", "new", "delete", "export",
                 "import", "typeof", "var", "constructor" -> "_" + name;
            default -> name;
        };
    }

    private static String typeName(Type t, Set<String> known){
        if(t instanceof Class){
            return className((Class<?>)t, known);
        }
        if(t instanceof GenericArrayType){
            return typeName(((GenericArrayType)t).getGenericComponentType(), known) + "[]";
        }
        if(t instanceof TypeVariable){
            return "any"; //bounded type vars erased - not worth reproducing bounds for hint purposes
        }
        if(t instanceof WildcardType){
            return "any";
        }
        if(t instanceof ParameterizedType){
            ParameterizedType pt = (ParameterizedType)t;
            Class<?> raw = (Class<?>)pt.getRawType();
            Type[] args = pt.getActualTypeArguments();

            //common Arc/Java containers get mapped to native TS shapes
            String rawName = raw.getSimpleName();
            if(isCollectionLike(raw) && args.length == 1){
                return typeName(args[0], known) + "[]";
            }
            if(isMapLike(raw) && args.length == 2){
                return "Record<string, " + typeName(args[1], known) + ">"; //key type erased to string, close enough for hints
            }

            if(!known.contains(rawName)) return "any";
            StringBuilder sb = new StringBuilder(rawName).append("<");
            for(int i = 0; i < args.length; i++){
                if(i > 0) sb.append(", ");
                sb.append(typeName(args[i], known));
            }
            return sb.append(">").toString();
        }
        return "any";
    }

    private static boolean isCollectionLike(Class<?> raw){
        String n = raw.getSimpleName();
        return n.equals("Seq") || n.equals("Array") || n.equals("List") || n.equals("ArrayList")
        || n.equals("ObjectSet") || n.equals("Set") || n.equals("Queue") || n.equals("SnapshotSeq");
    }

    private static boolean isMapLike(Class<?> raw){
        String n = raw.getSimpleName();
        return n.equals("ObjectMap") || n.equals("Map") || n.equals("HashMap") || n.equals("IntMap");
    }

    private static String className(Class<?> c, Set<String> known){
        if(c == void.class || c == Void.class) return "void";
        if(c == boolean.class || c == Boolean.class) return "boolean";
        if(c == int.class || c == Integer.class || c == float.class || c == Float.class
        || c == double.class || c == Double.class || c == long.class || c == Long.class
        || c == short.class || c == Short.class || c == byte.class || c == Byte.class) return "number";
        if(c == char.class || c == Character.class || c == String.class || c == CharSequence.class) return "string";
        if(c.isArray()) return className(c.getComponentType(), known) + "[]";
        if(erase.contains(c)) return "any";
        if(c.isPrimitive()) return "any"; //shouldn't hit, but stay safe
        return known.contains(c.getSimpleName()) ? c.getSimpleName() : "any";
    }
}