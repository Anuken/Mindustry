package mindustry.annotations.remote;

import arc.struct.*;
import mindustry.annotations.remote.IOFinder.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

public class SerializerResolver{
    private static final ClassSerializer entitySerializer = new ClassSerializer("mindustry.io.TypeIO.readEntity", "mindustry.io.TypeIO.writeEntity", "Entityc");

    public static ClassSerializer locate(ExecutableElement elem, TypeMirror mirror){
        //generic type
        if(mirror.toString().equals("T")){
            TypeParameterElement param = elem.getTypeParameters().get(0);
            if(Array.with(param.getBounds()).contains(SerializerResolver::isEntity)){
                return entitySerializer;
            }
        }
        if(isEntity(mirror)){
            return entitySerializer;
        }
        return null;
    }

    private static boolean isEntity(TypeMirror mirror){
        return !mirror.toString().contains(".") && mirror.toString().endsWith("c");
    }
}
