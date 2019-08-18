package io.anuke.annotations;

import com.squareup.javapoet.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

            String path = Paths.get(Utils.filer.createResource(StandardLocation.CLASS_OUTPUT, "no", "no")
                        .toUri().toURL().toString().substring(System.getProperty("os.name").contains("Windows") ? 6 : "file:".length()))
                        .getParent().getParent().getParent().getParent().getParent().getParent().toString();

            process("Sounds", path + "/assets/sounds", "io.anuke.arc.audio.Sound", "newSound");
            process("Musics", path + "/assets/music", "io.anuke.arc.audio.Music", "newMusic");

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

            load.addStatement(name + " = io.anuke.arc.Core.audio."+loadMethod+"(io.anuke.arc.Core.files.internal(io.anuke.arc.Core.app.getType() != io.anuke.arc.Application.ApplicationType.iOS ? $S : $S))",
                path.substring(path.lastIndexOf("/") + 1) + "/" + fname, (path.substring(path.lastIndexOf("/") + 1) + "/" + fname).replace(".ogg", ".mp3"));
            dispose.addStatement(name + ".dispose()");
            dispose.addStatement(name + " = null");
            type.addField(FieldSpec.builder(ClassName.bestGuess(rtype), name, Modifier.STATIC, Modifier.PUBLIC).initializer("new io.anuke.arc.audio.mock.Mock" + rtype.substring(rtype.lastIndexOf(".") + 1)+ "()").build());
            //cons.consume(type, fname, name);
        });

        if(classname.equals("Sounds")){
            type.addField(FieldSpec.builder(ClassName.bestGuess(rtype), "none", Modifier.STATIC, Modifier.PUBLIC).initializer("new io.anuke.arc.audio.mock.Mock" + rtype.substring(rtype.lastIndexOf(".") + 1)+ "()").build());
        }

        type.addMethod(load.build());
        type.addMethod(dispose.build());
        JavaFile.builder(packageName, type.build()).build().writeTo(Utils.filer);
    }
}
