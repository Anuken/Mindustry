package mindustry.annotations.remote;

import mindustry.annotations.*;
import mindustry.annotations.Annotations.ReadClass;
import mindustry.annotations.Annotations.WriteClass;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.Diagnostic.Kind;
import java.util.HashMap;
import java.util.Set;

/**
 * This class finds reader and writer methods annotated by the {@link WriteClass}
 * and {@link ReadClass} annotations.
 */
public class IOFinder{

    /**
     * Finds all class serializers for all types and returns them. Logs errors when necessary.
     * Maps fully qualified class names to their serializers.
     */
    public HashMap<String, ClassSerializer> findSerializers(RoundEnvironment env){
        HashMap<String, ClassSerializer> result = new HashMap<>();

        //get methods with the types
        Set<? extends Element> writers = env.getElementsAnnotatedWith(WriteClass.class);
        Set<? extends Element> readers = env.getElementsAnnotatedWith(ReadClass.class);

        //look for writers first
        for(Element writer : writers){
            WriteClass writean = writer.getAnnotation(WriteClass.class);
            String typeName = getValue(writean);

            //make sure there's only one read method
            if(readers.stream().filter(elem -> getValue(elem.getAnnotation(ReadClass.class)).equals(typeName)).count() > 1){
                BaseProcessor.messager.printMessage(Kind.ERROR, "Multiple writer methods for type '" + typeName + "'", writer);
            }

            //make sure there's only one write method
            long count = readers.stream().filter(elem -> getValue(elem.getAnnotation(ReadClass.class)).equals(typeName)).count();
            if(count == 0){
                BaseProcessor.messager.printMessage(Kind.ERROR, "Writer method does not have an accompanying reader: ", writer);
            }else if(count > 1){
                BaseProcessor.messager.printMessage(Kind.ERROR, "Writer method has multiple reader for type: ", writer);
            }

            Element reader = readers.stream().filter(elem -> getValue(elem.getAnnotation(ReadClass.class)).equals(typeName)).findFirst().get();

            //add to result list
            result.put(typeName, new ClassSerializer(BaseProcessor.getMethodName(reader), BaseProcessor.getMethodName(writer), typeName));
        }

        return result;
    }

    private String getValue(WriteClass write){
        try{
            Class<?> type = write.value();
            return type.getName();
        }catch(MirroredTypeException e){
            return e.getTypeMirror().toString();
        }
    }

    private String getValue(ReadClass read){
        try{
            Class<?> type = read.value();
            return type.getName();
        }catch(MirroredTypeException e){
            return e.getTypeMirror().toString();
        }
    }

    /** Information about read/write methods for a specific class type. */
    public static class ClassSerializer{
        /** Fully qualified method name of the reader. */
        public final String readMethod;
        /** Fully qualified method name of the writer. */
        public final String writeMethod;
        /** Fully qualified class type name. */
        public final String classType;

        public ClassSerializer(String readMethod, String writeMethod, String classType){
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;
            this.classType = classType;
        }
    }
}
