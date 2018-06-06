package io.anuke.annotations;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class Utils {
    public static Types typeUtils;
    public static Elements elementUtils;
    public static Filer filer;
    public static Messager messager;

    public static boolean isPrimitive(String type){
        return type.equals("boolean") || type.equals("byte") || type.equals("short") || type.equals("int")
                || type.equals("long") || type.equals("float") || type.equals("double") || type.equals("char");
    }
}
