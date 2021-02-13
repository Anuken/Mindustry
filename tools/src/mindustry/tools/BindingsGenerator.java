package mindustry.tools;

import arc.files.*;
import arc.struct.*;
import arc.util.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

import static mindustry.tools.ScriptMainGenerator.*;

//experimental
public class BindingsGenerator{
    //list of touchy class names that lead to conflicts or should not be referenced; all typedefs and procs containing these are ignored
    static Seq<String> ignored = Seq.with(".Entry", ".MapIterator", "arc.freetype.FreeType.Glyph", "FrameBufferBuilder", "Spliterator",
        "java.util.function", "java.util.stream", "ArrayList", "Constable", "Optional", "java.lang.reflect", "rhino.Context", "arc.graphics.GL20", "arc.graphics.GL30");
    static ObjectSet<Class<?>> classSet = new ObjectSet<>();
    static ObjectSet<String> keywords = ObjectSet.with("object", "string");

    public static void main(String[] args) throws Exception{

        //37800
        Seq<String> blacklist = Seq.with("mindustry.tools", "arc.backend", "arc.maps", "arc.util.serialization.Xml", "arc.fx", "arc.net", "arc.Net", "arc.freetype");
        Seq<Class<?>> classes = Seq.withArrays(
            getClasses("mindustry"),
            getClasses("arc")
        );

        classes.removeAll(type -> type.isSynthetic() || type.isAnonymousClass() || type.getCanonicalName() == null || Modifier.isPrivate(type.getModifiers())
        || blacklist.contains(s -> type.getName().startsWith(s)));

        classes.addAll(Enum.class, FloatBuffer.class, IntBuffer.class, ByteBuffer.class, StringBuilder.class,
            Comparator.class, Comparable.class, Reader.class, Writer.class, PrintStream.class, PrintWriter.class, File.class, Charset.class,
            ClassLoader.class, DoubleBuffer.class, CharBuffer.class, Locale.class,
            LongBuffer.class, DataInputStream.class, DataOutputStream.class);
        classes.distinct();

        classes = sorted(classes);
        classes.removeAll(BindingsGenerator::ignore);
        classes.removeAll(c -> !Modifier.isPublic(c.getModifiers()));

        ObjectMap<String, Seq<Class<?>>> similars = new ObjectMap<>();
        classes.each(c -> similars.get(c.getSimpleName(), Seq::new).add(c));
        similars.each((key, val) -> {
            if(val.size > 1){
                Log.info("\n" + key + ":\n" + val.toString("\n", v -> "- " + v.getCanonicalName()));
            }
        });

        classSet = classes.asSet();
        classSet.addAll(Object.class, Number.class, Integer.class, Double.class, Short.class, Float.class, Byte.class, Long.class, String.class, Boolean.class, Throwable.class, Exception.class);
        classSet.addAll(int.class, float.class, long.class, char.class, byte.class, boolean.class, double.class, short.class);

        /*
        //Cons<Class<?>> logger = c -> {
            //if(!c.isPrimitive() && !c.isArray() && !ignore(c) && Modifier.isPublic(c.getModifiers()) && classSet.add(c)){
            //    Log.info(c);
            //}
        //};

        for(Class<?> type : classes){
            for(Field f : type.getDeclaredFields()){
                if(Modifier.isPublic(f.getModifiers())){
                    logger.get(f.getType());
                }
            }

            for(Method f : type.getDeclaredMethods()){
                if(Modifier.isPublic(f.getModifiers())){
                    logger.get(f.getReturnType());
                    for(Class<?> c : f.getParameterTypes()){
                        logger.get(c);
                    }
                }
            }
        }*/

        StringBuilder result = new StringBuilder();
        result.append("import jnim, jnim/java/lang\n\n{.experimental: \"codeReordering\".}\n{.push hint[ConvFromXtoItselfNotNeeded]: off.}\n\n");

        for(Class<?> type : classes){
            result.append("jclassDef ").append(type.getCanonicalName()).append(" of `")
            .append(repr(type.getSuperclass())).append("`\n");
        }

        result.append("\n");

        for(Class<?> type : classes){

            Seq<Executable> exec = new Seq<>();
            Seq<Method> methods = Seq.with(type.getDeclaredMethods());

            methods.removeAll(m -> ignore(m.getReturnType()) || !discovered(m.getReturnType()));

            exec.addAll(methods);
            exec.addAll(type.getDeclaredConstructors());

            exec.removeAll(e -> !Modifier.isPublic(e.getModifiers()) || keywords.contains(e.getName()));
            exec.removeAll(e -> Structs.contains(e.getParameterTypes(), BindingsGenerator::ignore));
            exec.removeAll(e -> Structs.contains(e.getParameterTypes(), p -> !discovered(p)));

            Seq<Field> fields = Seq.select(type.getDeclaredFields(), f ->
                Modifier.isPublic(f.getModifiers()) &&
                !keywords.contains(f.getName()) &&
                !ignore(f.getType()) && Modifier.isPublic(f.getType().getModifiers()) &&
                !(f.getType().isArray() &&
                f.getType().getComponentType().isArray())
            );

            if(exec.size + fields.size <= 0) continue;

            result.append("jclassImpl ").append(type.getCanonicalName()).append(" of `")
                .append(repr(type.getSuperclass())).append("`").append(":").append("\n");

            for(Field field : fields){
                result.append("  proc `").append(field.getName()).append("`");
                result.append(": ").append(str(field.getType()));
                result.append(" {.prop");
                if(Modifier.isStatic(field.getModifiers())) result.append(", `static`");
                if(Modifier.isStatic(field.getModifiers())) result.append(", `final`");
                result.append(".}\n");
            }

            for(Executable method : exec){
                String mname = method.getName().equals("<init>") || method.getName().equals(type.getName()) ? "new" : method.getName();

                if(method instanceof Method){
                    Method m = (Method)method;

                    //if a field by the same name exists, use it to prevent ambiguity
                    if(method.getParameterCount() == 0 && fields.contains(f -> f.getName().equals(method.getName()))){
                        continue;
                    }

                    //check if this is a less specific method that was overridden by another method
                    if(methods.contains(other ->
                        other != m &&
                        m.getName().equals(other.getName()) &&
                        m.getParameterCount() == other.getParameterCount() &&
                        Arrays.equals(m.getParameterTypes(), other.getParameterTypes()) &&
                        m.getReturnType().isAssignableFrom(other.getReturnType())
                    )){
                        continue;
                    }
                }


                result.append("  proc `").append(mname).append("`");

                if(method.getParameterCount() > 0){
                    result.append("(");

                    for(int i = 0; i < method.getParameterCount(); i++){
                        Class p = method.getParameterTypes()[i];

                        result.append(method.getParameters()[i].getName()).append(": ").append(str(p));

                        if(i != method.getParameterCount() - 1){
                            result.append(", ");
                        }
                    }

                    result.append(")");
                }

                if(method instanceof Method){
                    Method m = (Method)method;
                    if(!m.getReturnType().equals(void.class)){
                        result.append(": ").append(str(m.getReturnType()));
                    }
                }

                if(Modifier.isStatic(method.getModifiers())){
                    //result.append(" {.`static`.}");
                }
                result.append("\n");
            }
            result.append("\n");
        }
        result.append("{.pop.}\n");

        Fi.get("/home/anuke/Projects/Nimdustry-java/mindustry_bindings.nim").writeString(result.toString());
        Log.info("Done. Classes found: @", classes.size);
    }

    static boolean discovered(Class<?> type){
        return classSet.contains(type) || (type.isArray() && discovered(type.getComponentType()));
    }

    static boolean ignore(Class<?> type){
        if(type == null) return false;
        if(type.isArray() && type.getComponentType().isArray()) return true;
        return ignored.contains(s -> type.getCanonicalName().contains(s)) || ignore(type.getSuperclass());
    }

    static Seq<Class<?>> sorted(Seq<Class<?>> classes){
        ObjectSet<Class<?>> visited = new ObjectSet<>();
        Seq<Class<?>> result = new Seq<>();
        for(Class<?> c : classes){
            if(!visited.contains(c)){
                topoSort(c, result, visited);
            }
        }
        return result;
    }

    static void topoSort(Class<?> c, Seq<Class<?>> stack, ObjectSet<Class<?>> visited){
        visited.add(c);
        for(Class<?> sup : c.getInterfaces()){
            if(!visited.contains(sup)){
                topoSort(sup, stack, visited);
            }
        }
        if(c.getSuperclass() != null && !c.getSuperclass().equals(Object.class) && !visited.contains(c.getSuperclass())){
            topoSort(c.getSuperclass(), stack, visited);
        }
        stack.add(c);
    }

    static String repr(Class type){
        if(type == null || type.equals(Object.class)) return "JVMObject";
        if(type.equals(String.class)) return "string";
        if(!Modifier.isPublic(type.getModifiers())) return "Object";
        return type.getSimpleName();
    }

    static String str(Class type){
        if(type.equals(Object.class)) return "JVMObject";
        if(type.equals(String.class)) return "string";
        if(type.isArray()) return "seq[" + str(type.getComponentType()) + "]";
        if(type.isPrimitive()) return "j" + type.getSimpleName();
        return type.getSimpleName();
    }
}
