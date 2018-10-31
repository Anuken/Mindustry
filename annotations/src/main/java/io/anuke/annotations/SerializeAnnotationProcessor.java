package io.anuke.annotations;

import com.squareup.javapoet.*;
import io.anuke.annotations.Annotations.Serialize;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
"io.anuke.annotations.Annotations.Serialize"
})
public class SerializeAnnotationProcessor extends AbstractProcessor{
    /**Target class name.*/
    private static final String className = "Serialization";
    /** Name of the base package to put all the generated classes. */
    private static final String packageName = "io.anuke.mindustry.gen";

    private int round;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv){
        super.init(processingEnv);
        //put all relevant utils into utils class
        Utils.typeUtils = processingEnv.getTypeUtils();
        Utils.elementUtils = processingEnv.getElementUtils();
        Utils.filer = processingEnv.getFiler();
        Utils.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        if(round++ != 0) return false; //only process 1 round

        try{
            Set<TypeElement> elements = ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Serialize.class));

            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
            MethodSpec.Builder method = MethodSpec.methodBuilder("init").addModifiers(Modifier.PUBLIC, Modifier.STATIC);

            for(TypeElement elem : elements){
                TypeName type = TypeName.get(elem.asType());

                TypeSpec.Builder serializer = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(ParameterizedTypeName.get(
                ClassName.bestGuess("io.anuke.ucore.io.TypeSerializer"), type));

                MethodSpec.Builder writeMethod = MethodSpec.methodBuilder("write")
                .returns(void.class)
                .addParameter(DataOutput.class, "stream")
                .addParameter(type, "object")
                .addAnnotation(Override.class)
                .addException(IOException.class)
                .addModifiers(Modifier.PUBLIC);

                MethodSpec.Builder readMethod = MethodSpec.methodBuilder("read")
                .returns(type)
                .addParameter(DataInput.class, "stream")
                .addAnnotation(Override.class)
                .addException(IOException.class)
                .addModifiers(Modifier.PUBLIC);

                readMethod.addStatement("$L object = new $L()", type, type);

                List<VariableElement> fields = ElementFilter.fieldsIn(Utils.elementUtils.getAllMembers(elem));
                for(VariableElement field : fields){
                    if(field.getModifiers().contains(Modifier.STATIC) || field.getModifiers().contains(Modifier.TRANSIENT) || field.getModifiers().contains(Modifier.PRIVATE)) continue;

                    String name = field.getSimpleName().toString();
                    String typeName = Utils.typeUtils.erasure(field.asType()).toString().replace('$', '.');
                    String capName = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

                    if(field.asType().getKind().isPrimitive()){
                        writeMethod.addStatement("stream.write" + capName + "(object." + name + ")");
                        readMethod.addStatement("object." + name + "= stream.read" + capName + "()");
                    }else{
                        writeMethod.addStatement("io.anuke.ucore.core.Settings.getSerializer(" + typeName+ ".class).write(stream, object." + name + ")");
                        readMethod.addStatement("object." + name + " = (" +typeName+")io.anuke.ucore.core.Settings.getSerializer(" + typeName+ ".class).read(stream)");
                    }
                }

                readMethod.addStatement("return object");

                serializer.addMethod(writeMethod.build());
                serializer.addMethod(readMethod.build());

                method.addStatement("io.anuke.ucore.core.Settings.setSerializer($N, $L)",  Utils.elementUtils.getBinaryName(elem).toString().replace('$', '.') + ".class", serializer.build());
            }

            classBuilder.addMethod(method.build());

            //write result
            JavaFile.builder(packageName, classBuilder.build()).build().writeTo(Utils.filer);

            return true;
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
