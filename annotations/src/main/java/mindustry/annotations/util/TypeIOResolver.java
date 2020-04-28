package mindustry.annotations.util;

import arc.struct.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;

import javax.lang.model.element.*;

/**
 * This class finds reader and writer methods.
 */
public class TypeIOResolver{

    /**
     * Finds all class serializers for all types and returns them. Logs errors when necessary.
     * Maps fully qualified class names to their serializers.
     */
    public static ClassSerializer resolve(BaseProcessor processor){
        ClassSerializer out = new ClassSerializer(new ObjectMap<>(), new ObjectMap<>());
        for(Stype type : processor.types(TypeIOHandler.class)){
            //look at all TypeIOHandler methods
            Array<Smethod> methods = type.methods();
            for(Smethod meth : methods){
                if(meth.is(Modifier.PUBLIC) && meth.is(Modifier.STATIC)){
                    Array<Svar> params = meth.params();
                    //2 params, second one is type, first is writer
                    if(params.size == 2 && params.first().tname().toString().equals("arc.util.io.Writes")){
                        out.writers.put(params.get(1).tname().toString(), type.fullName() + "." + meth.name());
                    }else if(params.size == 1 && params.first().tname().toString().equals("arc.util.io.Reads") && !meth.isVoid()){
                        //1 param, one is reader, returns type
                        out.readers.put(meth.retn().toString(), type.fullName() + "." + meth.name());
                    }
                }
            }
        }

        return out;
    }

    /** Information about read/write methods for class types. */
    public static class ClassSerializer{
        public final ObjectMap<String, String> writers, readers;

        public ClassSerializer(ObjectMap<String, String> writers, ObjectMap<String, String> readers){
            this.writers = writers;
            this.readers = readers;
        }

        public boolean has(String type){
            return writers.containsKey(type) && readers.containsKey(type);
        }
    }
}
