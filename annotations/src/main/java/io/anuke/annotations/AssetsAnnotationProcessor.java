package io.anuke.annotations;

import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic.*;
import java.nio.file.*;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AssetsAnnotationProcessor extends AbstractProcessor{
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
            process("Sounds", "core/assets/sounds", "io.anuke.arc.audio.Sound", "newSound");
            process("Musics", "core/assets/music", "io.anuke.arc.audio.Music", "newMusic");

            return true;
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    void process(String classname, String path, String rtype, String loadMethod) throws Exception{
        TypeSpec.Builder type = TypeSpec.classBuilder(classname).addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder load = MethodSpec.methodBuilder("load").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        MethodSpec.Builder dispose = MethodSpec.methodBuilder("dispose").addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        HashSet<String> names = new HashSet<>();
        Files.list(Paths.get(path)).forEach(p -> {
            String fname = p.getFileName().toString();
            String name = p.getFileName().toString();
            name = name.substring(0, name.indexOf("."));

            if(names.contains(name)){
                Utils.messager.printMessage(Kind.ERROR, "Duplicate file name: " + p.toString() + "!");
            }else{
                names.add(name);
            }

            if(SourceVersion.isKeyword(name)){
                name = name + "s";
            }

            load.addStatement(name + " = io.anuke.arc.Core.audio."+loadMethod+"(io.anuke.arc.Core.files.internal($S))", path.substring(path.lastIndexOf("/") + 1) + "/" + fname);
            dispose.addStatement(name + ".dispose()");
            dispose.addStatement(name + " = null");
            type.addField(FieldSpec.builder(ClassName.bestGuess(rtype), name, Modifier.STATIC, Modifier.PUBLIC).initializer("new io.anuke.arc.audio.mock.Mock" + rtype.substring(rtype.lastIndexOf(".") + 1)+ "()").build());
            //cons.consume(type, fname, name);
        });

        type.addMethod(load.build());
        type.addMethod(dispose.build());
        JavaFile.builder(packageName, type.build()).build().writeTo(Utils.filer);
    }
}
