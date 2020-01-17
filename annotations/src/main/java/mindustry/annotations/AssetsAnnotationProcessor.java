package mindustry.annotations;

import arc.files.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.serialization.*;
import com.squareup.javapoet.*;
import mindustry.annotations.Annotations.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic.*;
import javax.tools.*;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("mindustry.annotations.Annotations.StyleDefaults")
public class AssetsAnnotationProcessor extends AbstractProcessor{
    /** Name of the base package to put all the generated classes. */
    private static final String packageName = "mindustry.gen";
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
            path = Fi.get(Utils.filer.createResource(StandardLocation.CLASS_OUTPUT, "no", "no")
            .toUri().toURL().toString().substring(System.getProperty("os.name").contains("Windows") ? 6 : "file:".length()))
            .parent().parent().parent().parent().parent().parent().toString();
            path = path.replace("%20", " ");

            processSounds("Sounds", path + "/assets/sounds", "arc.audio.Sound");
            processSounds("Musics", path + "/assets/music", "arc.audio.Music");
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
        TypeSpec.Builder ictype = TypeSpec.classBuilder("Icon").addModifiers(Modifier.PUBLIC); //TODO remove and replace
        TypeSpec.Builder ictype_ = TypeSpec.classBuilder("Icon_").addModifiers(Modifier.PUBLIC);
        TypeSpec.Builder ichtype = TypeSpec.classBuilder("Iconc").addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder load = MethodSpec.methodBuilder("load").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        MethodSpec.Builder loadStyles = MethodSpec.methodBuilder("loadStyles").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        MethodSpec.Builder icload = MethodSpec.methodBuilder("load").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        MethodSpec.Builder icload_ = MethodSpec.methodBuilder("load").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        String resources = path + "/assets-raw/sprites/ui";
        Jval icons = Jval.read(Fi.get(path + "/assets-raw/fontgen/config.json").readString());

        for(Jval val : icons.get("glyphs").asArray()){
            String name = capitalize(val.getString("css", ""));
            int code = val.getInt("code", 0);
            ichtype.addField(FieldSpec.builder(char.class, name, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("(char)" + code).build());
            ictype_.addField(TextureRegionDrawable.class, name, Modifier.PUBLIC, Modifier.STATIC);
            icload_.addStatement(name + " = mindustry.Vars.ui.getGlyph(mindustry.ui.Fonts.def, (char)" + code + ")");
        }

        Fi.get(resources).walk(p -> {
            if(p.nameWithoutExtension().equals(".DS_Store")) return;

            String filename = p.name();
            filename = filename.substring(0, filename.indexOf("."));

            ArrayList<String> names = new ArrayList<>();
            names.add("");
            if(filename.contains("icon")){
                names.addAll(Arrays.asList(iconSizes));
            }

            for(String suffix : names){
                suffix = suffix.isEmpty() ? "" : "-" + suffix;

                String sfilen = filename + suffix;
                String dtype = p.name().endsWith(".9.png") ? "arc.scene.style.NinePatchDrawable" : "arc.scene.style.TextureRegionDrawable";

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
                tload.addStatement(varname + " = ("+dtype+")arc.Core.atlas.drawable($S)", sfilen);
            }
        });

        for(Element elem : elements){
            Array.with(((TypeElement)elem).getEnclosedElements()).each(e -> e.getKind() == ElementKind.FIELD, field -> {
                String fname = field.getSimpleName().toString();
                if(fname.startsWith("default")){
                    loadStyles.addStatement("arc.Core.scene.addStyle(" + field.asType().toString() + ".class, mindustry.ui.Styles." + fname + ")");
                }
            });
        }

        ictype.addMethod(icload.build());
        ictype_.addMethod(icload_.build());
        JavaFile.builder(packageName, ictype.build()).build().writeTo(Utils.filer);
        JavaFile.builder(packageName, ichtype.build()).build().writeTo(Utils.filer);
        JavaFile.builder(packageName, ictype_.build()).build().writeTo(Utils.filer);

        type.addMethod(load.build());
        type.addMethod(loadStyles.build());
        JavaFile.builder(packageName, type.build()).build().writeTo(Utils.filer);
    }

    void processSounds(String classname, String path, String rtype) throws Exception{
        TypeSpec.Builder type = TypeSpec.classBuilder(classname).addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder dispose = MethodSpec.methodBuilder("dispose").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        MethodSpec.Builder loadBegin = MethodSpec.methodBuilder("load").addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        HashSet<String> names = new HashSet<>();
        Fi.get(path).walk(p -> {
            String fname = p.name();
            String name = p.nameWithoutExtension();

            if(names.contains(name)){
                Utils.messager.printMessage(Kind.ERROR, "Duplicate file name: " + p.toString() + "!");
            }else{
                names.add(name);
            }

            if(SourceVersion.isKeyword(name)){
                name = name + "s";
            }

            String filepath = path.substring(path.lastIndexOf("/") + 1) + "/" + fname;

            String filename = "arc.Core.app.getType() != arc.Application.ApplicationType.iOS ? \"" + filepath + "\" : \"" + filepath.replace(".ogg", ".mp3")+"\"";

            loadBegin.addStatement("arc.Core.assets.load("+filename +", "+rtype+".class).loaded = a -> " + name + " = ("+rtype+")a", filepath, filepath.replace(".ogg", ".mp3"));

            dispose.addStatement("arc.Core.assets.unload(" + filename + ")");
            dispose.addStatement(name + " = null");
            type.addField(FieldSpec.builder(ClassName.bestGuess(rtype), name, Modifier.STATIC, Modifier.PUBLIC).initializer("new arc.audio.mock.Mock" + rtype.substring(rtype.lastIndexOf(".") + 1)+ "()").build());
        });

        if(classname.equals("Sounds")){
            type.addField(FieldSpec.builder(ClassName.bestGuess(rtype), "none", Modifier.STATIC, Modifier.PUBLIC).initializer("new arc.audio.mock.Mock" + rtype.substring(rtype.lastIndexOf(".") + 1)+ "()").build());
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
