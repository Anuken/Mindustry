package io.anuke.annotations;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.annotations.IOFinder.ClassSerializer;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**The annotation processor for generating remote method call code.*/
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
    "io.anuke.annotations.Annotations.Remote",
    "io.anuke.annotations.Annotations.WriteClass",
    "io.anuke.annotations.Annotations.ReadClass",
})
public class RemoteMethodAnnotationProcessor extends AbstractProcessor {
    /**Maximum size of each event packet.*/
    public static final int maxPacketSize = 512;
    /**Name of the base package to put all the generated classes.*/
    private static final String packageName = "io.anuke.mindustry.gen";

    /**Name of class that handles reading and invoking packets on the server.*/
    private static final String readServerName = "RemoteReadServer";
    /**Name of class that handles reading and invoking packets on the client.*/
    private static final String readClientName = "RemoteReadClient";

    /**Whether the initial round is done.*/
    private boolean done;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        //put all relevant utils into utils class
        Utils.typeUtils = processingEnv.getTypeUtils();
        Utils.elementUtils = processingEnv.getElementUtils();
        Utils.filer = processingEnv.getFiler();
        Utils.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(done) return false; //only process 1 round
        done = true;

        try {

            //get serializers
            HashMap<String, ClassSerializer> serializers = new IOFinder().findSerializers(roundEnv);

            //last method ID used
            int lastMethodID = 0;
            //find all elements with the Remote annotation
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Remote.class);
            //map of all classes to generate by name
            HashMap<String, ClassEntry> classMap = new HashMap<>();
            //list of all method entries
            ArrayList<MethodEntry> methods = new ArrayList<>();
            //list of all method entries
            ArrayList<ClassEntry> classes = new ArrayList<>();

            //create methods
            for (Element element : elements) {
                Remote annotation = element.getAnnotation(Remote.class);

                //check for static
                if(!element.getModifiers().contains(Modifier.STATIC)) {
                    Utils.messager.printMessage(Kind.ERROR, "All Remote methods must be static: ", element);
                }

                //get and create class entry if needed
                if (!classMap.containsKey(annotation.target())) {
                    ClassEntry clas = new ClassEntry(annotation.target());
                    classMap.put(annotation.target(), clas);
                    classes.add(clas);
                }

                ClassEntry entry = classMap.get(annotation.target());

                //make sure that each server annotation has at least one method to generate, otherwise throw an error
                if (annotation.server() && !annotation.all() && !annotation.one()) {
                    Utils.messager.printMessage(Kind.ERROR, "A client method must not have all() and one() both be false!", element);
                    return false;
                }

                if (annotation.server() && annotation.client()) {
                    Utils.messager.printMessage(Kind.ERROR, "A method cannot be client and server simulatenously!", element);
                    return false;
                }

                //create and add entry
                MethodEntry method = new MethodEntry(entry.name, Utils.getMethodName(element), annotation.client(), annotation.server(),
                        annotation.all(), annotation.one(), annotation.local(), annotation.unreliable(), lastMethodID ++, (ExecutableElement)element);

                entry.methods.add(method);
                methods.add(method);
            }

            //create read/write generators
            RemoteReadGenerator readgen = new RemoteReadGenerator(serializers);
            RemoteWriteGenerator writegen = new RemoteWriteGenerator(serializers);

            //generate server readers
            readgen.generateFor(methods.stream().filter(method -> method.client).collect(Collectors.toList()), readServerName, packageName, true);
            //generate client readers
            readgen.generateFor(methods.stream().filter(method -> method.server).collect(Collectors.toList()), readClientName, packageName, false);

            //generate the methods to invoke (write)
            writegen.generateFor(classes, packageName);

            //create class for storing unique method hash
            TypeSpec.Builder hashBuilder = TypeSpec.classBuilder("MethodHash").addModifiers(Modifier.PUBLIC);
            hashBuilder.addField(FieldSpec.builder(int.class, "HASH", Modifier.STATIC, Modifier.PUBLIC, Modifier.FINAL)
                    .initializer("$1L", Objects.hash(methods)).build());

            //build and write resulting hash class
            TypeSpec spec = hashBuilder.build();
            JavaFile.builder(packageName, spec).build().writeTo(Utils.filer);

            return true;

        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
