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
        ClassSerializer out = new ClassSerializer(new ObjectMap<>(), new ObjectMap<>(), new ObjectMap<>());
        for(Stype type : processor.types(TypeIOHandler.class)){
            //look at all TypeIOHandler methods
            Seq<Smethod> methods = type.methods();
            for(Smethod meth : methods){
                if(meth.is(Modifier.PUBLIC) && meth.is(Modifier.STATIC)){
                    Seq<Svar> params = meth.params();
                    //2 params, second one is type, first is writer
                    if(params.size == 2 && params.first().tname().toString().equals("arc.util.io.Writes")){
                        out.writers.put(fix(params.get(1).tname().toString()), type.fullName() + "." + meth.name());
                    }else if(params.size == 1 && params.first().tname().toString().equals("arc.util.io.Reads") && !meth.isVoid()){
                        //1 param, one is reader, returns type
                        out.readers.put(fix(meth.retn().toString()), type.fullName() + "." + meth.name());
                    }else if(params.size == 2 && params.first().tname().toString().equals("arc.util.io.Reads") && !meth.isVoid() && meth.ret().equals(meth.params().get(1).mirror())){
                        //2 params, one is reader, other is type, returns type - these are made to reduce garbage allocated
                        out.mutatorReaders.put(fix(meth.retn().toString()), type.fullName() + "." + meth.name());
                    }
                }
            }
        }

        return out;
    }

    /** makes sure type names don't contain 'gen' */
    private static String fix(String str){
        return str.replace("mindustry.gen", "");
    }

    /** Information about read/write methods for class types. */
    public static class ClassSerializer{
        public final ObjectMap<String, String> writers, readers, mutatorReaders;

        public ClassSerializer(ObjectMap<String, String> writers, ObjectMap<String, String> readers, ObjectMap<String, String> mutatorReaders){
            this.writers = writers;
            this.readers = readers;
            this.mutatorReaders = mutatorReaders;
        }
    }
}
