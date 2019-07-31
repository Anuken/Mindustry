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
            process("Sounds", "core/assets/sounds", (type, fname, name) ->
                type.addField(FieldSpec.builder(ClassName.bestGuess("io.anuke.arc.audio.Sound"), name, Modifier.STATIC, Modifier.PUBLIC, Modifier.FINAL)
                .initializer(CodeBlock.builder().add("io.anuke.arc.Core.audio.newSound(io.anuke.arc.Core.files.internal($S))", "sounds/" + fname).build()).build()));

            process("Musics", "core/assets/music", (type, fname, name) ->
                type.addField(FieldSpec.builder(ClassName.bestGuess("io.anuke.arc.audio.Music"), name, Modifier.STATIC, Modifier.PUBLIC, Modifier.FINAL)
                .initializer(CodeBlock.builder().add("io.anuke.arc.Core.audio.newMusic(io.anuke.arc.Core.files.internal($S))", "music/" + fname).build()).build()));

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

    void process(String classname, String path, Conser cons) throws Exception{
        TypeSpec.Builder type = TypeSpec.classBuilder(classname).addModifiers(Modifier.PUBLIC);

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

            cons.consume(type, fname, name);

        });


        JavaFile.builder(packageName, type.build()).build().writeTo(Utils.filer);
    }

    interface Conser{
        void consume(TypeSpec.Builder type, String fname, String name);
    }
}
