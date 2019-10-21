package io.anuke.annotations;

import com.squareup.javapoet.*;
import io.anuke.annotations.Annotations.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic.*;
import javax.tools.*;
import java.nio.file.*;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("io.anuke.annotations.Annotations.StyleDefaults")
public class AssetsAnnotationProcessor extends AbstractProcessor{
    /** Name of the base package to put all the generated classes. */
    private static final String packageName = "io.anuke.mindustry.gen";
    private String path;
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
            path = Paths.get(Utils.filer.createResource(StandardLocation.CLASS_OUTPUT, "no", "no")
            .toUri().toURL().toString().substring(System.getProperty("os.name").contains("Windows") ? 6 : "file:".length()))
            .getParent().getParent().getParent().getParent().getParent().getParent().toString();
            path = path.replace("%20", " ");

            processSounds("Sounds", path + "/assets/sounds", "io.anuke.arc.audio.Sound");
            processSounds("Musics", path + "/assets/music", "io.anuke.arc.audio.Music");
            processUI(roundEnv.getElementsAnnotatedWith(StyleDefaults.class));

            return true;
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    void processUI(Set<? extends Element> elements) throws Exception{
        String[] iconSizes = {"small", "smaller", "tiny"};

        TypeSpec.Builder type = TypeSpec.classBuilder("Tex").addModifiers(Modifier.PUBLIC);
        TypeSpec.Builder ictype = TypeSpec.classBuilder("Icon").addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder load = MethodSpec.methodBuilder("load").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        MethodSpec.Builder loadStyles = MethodSpec.methodBuilder("loadStyles").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        MethodSpec.Builder icload = MethodSpec.methodBuilder("load").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        String resources = path + "/assets-raw/sprites/ui";
        Files.walk(Paths.get(resources)).forEach(p -> {
            if(Files.isDirectory(p) || p.getFileName().toString().equals(".DS_Store")) return;

            String filename = p.getFileName().toString();
            filename = filename.substring(0, filename.indexOf("."));

            ArrayList<String> names = new ArrayList<>();
            names.add("");
            if(filename.contains("icon")){
                names.addAll(Arrays.asList(iconSizes));
            }

            for(String suffix : names){
                suffix = suffix.isEmpty() ? "" : "-" + suffix;

                String sfilen = filename + suffix;
                String dtype = p.getFileName().toString().endsWith(".9.png") ? "io.anuke.arc.scene.style.NinePatchDrawable" : "io.anuke.arc.scene.style.TextureRegionDrawable";

                String varname = capitalize(sfilen);
                TypeSpec.Builder ttype = type;
                MethodSpec.Builder tload = load;
                if(varname.startsWith("icon")){
                    varname = varname.substring("icon".length());
                    varname = Character.toLowerCase(varname.charAt(0)) + varname.substring(1);
                    ttype = ictype;
                    tload = icload;
                    if(SourceVersion.isKeyword(varname)) varname += "i";
                }

                if(SourceVersion.isKeyword(varname)) varname += "s";

                ttype.addField(ClassName.bestGuess(dtype), varname, Modifier.STATIC, Modifier.PUBLIC);
                tload.addStatement(varname + " = ("+dtype+")io.anuke.arc.Core.atlas.drawable($S)", sfilen);
            }
        });

        for(Element elem : elements){
            TypeElement t = (TypeElement)elem;
            t.getEnclosedElements().stream().filter(e -> e.getKind() == ElementKind.FIELD).forEach(field -> {
                String fname = field.getSimpleName().toString();
                if(fname.startsWith("default")){
                    loadStyles.addStatement("io.anuke.arc.Core.scene.addStyle(" + field.asType().toString() + ".class, io.anuke.mindustry.ui.Styles." + fname + ")");
                }
            });
        }

        ictype.addMethod(icload.build());
        JavaFile.builder(packageName, ictype.build()).build().writeTo(Utils.filer);

        type.addMethod(load.build());
        type.addMethod(loadStyles.build());
        JavaFile.builder(packageName, type.build()).build().writeTo(Utils.filer);
    }

    void processSounds(String classname, String path, String rtype) throws Exception{
        TypeSpec.Builder type = TypeSpec.classBuilder(classname).addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder dispose = MethodSpec.methodBuilder("dispose").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        MethodSpec.Builder loadBegin = MethodSpec.methodBuilder("load").addModifiers(Modifier.PUBLIC, Modifier.STATIC);

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

            String filepath = path.substring(path.lastIndexOf("/") + 1) + "/" + fname;

            String filename = "io.anuke.arc.Core.app.getType() != io.anuke.arc.Application.ApplicationType.iOS ? \"" + filepath + "\" : \"" + filepath.replace(".ogg", ".mp3")+"\"";

            loadBegin.addStatement("io.anuke.arc.Core.assets.load("+filename +", "+rtype+".class).loaded = a -> " + name + " = ("+rtype+")a", filepath, filepath.replace(".ogg", ".mp3"));

            dispose.addStatement("io.anuke.arc.Core.assets.unload(" + filename + ")");
            dispose.addStatement(name + " = null");
            type.addField(FieldSpec.builder(ClassName.bestGuess(rtype), name, Modifier.STATIC, Modifier.PUBLIC).initializer("new io.anuke.arc.audio.mock.Mock" + rtype.substring(rtype.lastIndexOf(".") + 1)+ "()").build());
        });

        if(classname.equals("Sounds")){
            type.addField(FieldSpec.builder(ClassName.bestGuess(rtype), "none", Modifier.STATIC, Modifier.PUBLIC).initializer("new io.anuke.arc.audio.mock.Mock" + rtype.substring(rtype.lastIndexOf(".") + 1)+ "()").build());
        }

        type.addMethod(loadBegin.build());
        type.addMethod(dispose.build());
        JavaFile.builder(packageName, type.build()).build().writeTo(Utils.filer);
    }

    static String capitalize(String s){
        StringBuilder result = new StringBuilder(s.length());

        for(int i = 0; i < s.length(); i++){
            char c = s.charAt(i);
            if(c != '_' && c != '-'){
                if(i > 0 && (s.charAt(i - 1) == '_' || s.charAt(i - 1) == '-')){
                    result.append(Character.toUpperCase(c));
                }else{
                    result.append(c);
                }
            }
        }

        return result.toString();
    }
}
