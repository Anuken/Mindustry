package mindustry.annotations.remote;

import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;
import mindustry.annotations.util.*;
import mindustry.annotations.util.TypeIOResolver.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;


/** The annotation processor for generating remote method call code. */
@SupportedAnnotationTypes({
"mindustry.annotations.Annotations.Remote",
"mindustry.annotations.Annotations.TypeIOHandler"
})
public class RemoteProcess extends BaseProcessor{
    /** Simple class name of generated class name. */
    public static final String callLocation = "Call";

    @Override
    public void process(RoundEnvironment roundEnv) throws Exception{
        //get serializers
        //class serializers
        ClassSerializer serializer = TypeIOResolver.resolve(this);
        //last method ID used
        int lastMethodID = 0;
        //find all elements with the Remote annotation
        //all elements with the Remote annotation
        Seq<Smethod> elements = methods(Remote.class);
        //list of all method entries
        Seq<MethodEntry> methods = new Seq<>();

        Seq<Smethod> orderedElements = elements.copy();
        orderedElements.sortComparing(Selement::toString);

        //create methods
        for(Smethod element : orderedElements){
            Remote annotation = element.annotation(Remote.class);

            //check for static
            if(!element.is(Modifier.STATIC) || !element.is(Modifier.PUBLIC)){
                err("All @Remote methods must be public and static", element);
            }

            //can't generate none methods
            if(annotation.targets() == Loc.none){
                err("A @Remote method's targets() cannot be equal to 'none'", element);
            }

            String packetName = Strings.capitalize(element.name()) + "CallPacket";
            int[] index = {1};

            while(methods.contains(m -> m.packetClassName.equals(packetName + (index[0] == 1 ? "" : index[0])))){
                index[0] ++;
            }

            //create and add entry
            MethodEntry method = new MethodEntry(
                callLocation, BaseProcessor.getMethodName(element.e), packetName + (index[0] == 1 ? "" : index[0]),
                annotation.targets(), annotation.variants(),
                annotation.called(), annotation.unreliable(), annotation.forward(), lastMethodID++,
                element, annotation.priority()
            );

            methods.add(method);
        }

        //generate the methods to invoke, as well as the packet classes
        CallGenerator.generate(serializer, methods);
    }
}
