package io.anuke.annotations;

import io.anuke.annotations.Annotations.ReadClass;
import io.anuke.annotations.Annotations.WriteClass;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Stream;

/**This class finds reader and writer methods annotated by the {@link io.anuke.annotations.Annotations.WriteClass}
 * and {@link io.anuke.annotations.Annotations.ReadClass} annotations.*/
public class IOFinder {

    /**Finds all class serializers for all types and returns them. Logs errors when necessary.
     * Maps fully qualified class names to their serializers.*/
    public HashMap<String, ClassSerializer> findSerializers(RoundEnvironment env){
        HashMap<String, ClassSerializer> result = new HashMap<>();

        //get methods with the types
        Set<? extends Element> writers = env.getElementsAnnotatedWith(WriteClass.class);
        Set<? extends Element> readers = env.getElementsAnnotatedWith(ReadClass.class);

        //look for writers first
        for(Element writer : writers){
            WriteClass writean = writer.getAnnotation(WriteClass.class);
            Class<?> type = writean.value();

            //make sure there's only one read method
            if(readers.stream().filter(elem -> elem.getAnnotation(ReadClass.class).value() == type).count() > 1){
                Utils.messager.printMessage(Kind.ERROR, "Multiple writer methods for type: ", writer);
            }

            //make sure there's only one write method
            Stream<? extends Element> stream = readers.stream().filter(elem -> elem.getAnnotation(ReadClass.class).value() == type);
            if(stream.count() == 0){
                Utils.messager.printMessage(Kind.ERROR, "Writer method does not have an accompanying reader: ", writer);
            }else if(stream.count() > 1){
                Utils.messager.printMessage(Kind.ERROR, "Writer method has multiple reader for type: ", writer);
            }

            Element reader = stream.findFirst().get();

            //add to result list
            result.put(type.getName(), new ClassSerializer(Utils.getMethodName(reader), Utils.getMethodName(writer), type.getName()));
        }

        return result;
    }

    /**Information about read/write methods for a specific class type.*/
    public static class ClassSerializer{
        /**Fully qualified method name of the reader.*/
        public final String readMethod;
        /**Fully qualified method name of the writer.*/
        public final String writeMethod;
        /**Fully qualified class type name.*/
        public final String classType;

        public ClassSerializer(String readMethod, String writeMethod, String classType) {
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;
            this.classType = classType;
        }
    }
}
