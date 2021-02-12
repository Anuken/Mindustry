package mindustry.tools;

import arc.files.*;
import arc.struct.*;
import arc.util.*;

import java.lang.reflect.*;

import static mindustry.tools.ScriptMainGenerator.*;

//experimental
public class BindingsGenerator{

    public static void main(String[] args) throws Exception{

        Seq<String> blacklist = Seq.with("mindustry.tools", "arc.backend");
        Seq<Class<?>> classes = Seq.withArrays(
            getClasses("mindustry"),
            getClasses("arc")
        );
        classes.sort(Structs.comparing(Class::getName));

        classes.removeAll(type -> type.isSynthetic() || type.isAnonymousClass() || type.getCanonicalName() == null || Modifier.isPrivate(type.getModifiers())
        || blacklist.contains(s -> type.getName().startsWith(s)));

        classes.distinct();
        classes.sortComparing(Class::getName);

        StringBuilder result = new StringBuilder();
        result.append("import jnim, jnim/java/lang\n\n{.experimental: \"codeReordering\".}\n\n");

        for(Class<?> type : classes){
            String name = type.getCanonicalName();
            result.append("jclass ").append(name).append("* of ")
                .append(type.getSuperclass() == null ? "JVMObject" : type.getSuperclass().getSimpleName()).append(":\n");

            for(Field field : type.getFields()){
                if(Modifier.isPublic(field.getModifiers())){
                    result.append("  proc `").append(field.getName()).append("`*");
                    result.append(": ").append(str(field.getType()));
                    result.append(" {.prop");
                    if(Modifier.isStatic(field.getModifiers())) result.append(", `static`");
                    if(Modifier.isStatic(field.getModifiers())) result.append(", `final`");
                    result.append(".}\n");
                }
            }

            Seq<Executable> exec = new Seq<>();

            exec.addAll(type.getDeclaredMethods());
            exec.addAll(type.getDeclaredConstructors());

            for(Executable method : exec){
                if(Modifier.isPublic(method.getModifiers())){
                    String mname = method.getName().equals("<init>") || method.getName().equals(type.getCanonicalName()) ? "new" : method.getName();
                    result.append("  proc `").append(mname).append("`*");

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
            }
            result.append("\n");
        }

        Fi.get(OS.userhome).child("mindustry.nim").writeString(result.toString());
        Log.info(result);
    }

    static String str(Class type){
        if(type.isArray()){
            return "seq[" + str(type.getComponentType()) + "]";
        }
        if(type.isPrimitive()) return "j" + type.getSimpleName();
        return type.getSimpleName();
    }
}
