package io.anuke.annotations;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.anuke.annotations.Annotations.Struct;
import io.anuke.annotations.Annotations.StructField;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import java.util.Collections;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
    "io.anuke.annotations.Annotations.Struct"
})
public class StructAnnotationProcessor extends AbstractProcessor{
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
            Set<TypeElement> elements = ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Struct.class));

            for(TypeElement elem : elements){
                TypeName type = TypeName.get(elem.asType());

                if(!type.toString().endsWith("Struct")){
                    Utils.messager.printMessage(Kind.ERROR, "All classes annotated with @Struct must have their class names end in 'Struct'.", elem);
                    continue;
                }

                TypeSpec.Builder classBuilder = TypeSpec.classBuilder(packageName + "." + elem.getSimpleName().toString());

                int offset = 0;

                for(VariableElement var : ElementFilter.fieldsIn(Collections.singletonList(elem))){
                    if(!var.asType().getKind().isPrimitive()){
                        Utils.messager.printMessage(Kind.ERROR, "All struct fields must be primitives.", var);
                        continue;
                    }

                    StructField an = var.getAnnotation(StructField.class);
                    int size = an == null ? typeSize(var.asType().getKind()) : an.value();

                    MethodSpec.Builder getter = MethodSpec.methodBuilder(var.getSimpleName().toString());
                    MethodSpec.Builder setter = MethodSpec.methodBuilder(var.getSimpleName().toString());
                }

                JavaFile.builder(packageName, classBuilder.build()).build().writeTo(Utils.filer);
            }

            return true;
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**returns a type's element size in bits.*/
    static int typeSize(TypeKind kind){
        switch(kind){
            case BOOLEAN:
                return 1;
            case BYTE:
                return 8;
            case SHORT:
                return 16;
            case FLOAT:
            case CHAR:
            case INT:
                return 32;
            case LONG:
            case DOUBLE:
                return 64;
            default:
                throw new IllegalArgumentException("Invalid type kind: " + kind);
        }
    }
}
