package mindustry.tools;

import arc.files.*;
import arc.struct.*;
import arc.util.*;

import java.lang.reflect.*;

import static mindustry.tools.ScriptMainGenerator.*;

//experimental
public class BindingsGenerator{
    //list of touchy class names that lead to conflicts; all typedefs and procs containing these are ignored
    static Seq<String> ignored = Seq.with(".Entry", ".MapIterator");

    public static void main(String[] args) throws Exception{

        Seq<String> blacklist = Seq.with("mindustry.tools", "arc.backend");
        Seq<Class<?>> classes = Seq.withArrays(
            getClasses("mindustry"),
            getClasses("arc")
        );

        classes.removeAll(type -> type.isSynthetic() || type.isAnonymousClass() || type.getCanonicalName() == null || Modifier.isPrivate(type.getModifiers())
        || blacklist.contains(s -> type.getName().startsWith(s)));

        classes.add(Enum.class);

        classes.distinct();

        classes = sorted(classes);
        classes.removeAll(BindingsGenerator::ignore);

        StringBuilder result = new StringBuilder();
        result.append("import jnim, jnim/java/lang\n\n{.experimental: \"codeReordering\".}\n\n");

        for(Class<?> type : classes){
            result.append("jclassDef ").append(type.getCanonicalName()).append(" of `")
            .append(repr(type.getSuperclass())).append("`\n");
        }

        result.append("\n");

        for(Class<?> type : classes){

            Seq<Executable> exec = new Seq<>();

            exec.addAll(type.getDeclaredMethods());
            exec.addAll(type.getDeclaredConstructors());

            exec.removeAll(e -> !Modifier.isPublic(e.getModifiers()));
            exec.removeAll(e -> Structs.contains(e.getParameterTypes(), BindingsGenerator::ignore));

            Seq<Field> fields = Seq.select(type.getDeclaredFields(), f -> Modifier.isPublic(f.getModifiers()));

            result.append("jclassImpl ").append(type.getCanonicalName()).append(" of `")
                .append(repr(type.getSuperclass())).append("`").append(exec.size + fields.size > 0 ? ":" : "").append("\n");

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

                //result.append(" {.");
                //if(Modifier.isStatic(field.getModifiers())) result.append(", `static`");
                //if(Modifier.isStatic(field.getModifiers())) result.append(", `final`");
                //result.append(".}\n");
                result.append("\n");
            }
            result.append("\n");
        }

        Fi.get("/home/anuke/Projects/Nimdustry-java/mindustry_bindings.nim").writeString(result.toString());
        //Fi.get(OS.userhome).child("mindustry.nim").writeString(result.toString());
        Log.info(result);
    }

    static boolean ignore(Class<?> type){
        if(type == null) return false;
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
        return type == null ? "JVMObject" : type.getSimpleName();
    }

    static String str(Class type){
        if(type.isArray()){
            return "seq[" + str(type.getComponentType()) + "]";
        }
        if(type.isPrimitive()) return "j" + type.getSimpleName();
        return type.getSimpleName();
    }
}
