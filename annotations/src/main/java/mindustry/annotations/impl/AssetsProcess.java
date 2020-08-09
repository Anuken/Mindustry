package mindustry.annotations.impl;

import arc.files.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.serialization.*;
import com.squareup.javapoet.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import java.util.*;

@SupportedAnnotationTypes("mindustry.annotations.Annotations.StyleDefaults")
public class AssetsProcess extends BaseProcessor{

    @Override
    public void process(RoundEnvironment env) throws Exception{
        processSounds("Sounds", rootDirectory + "/core/assets/sounds", "arc.audio.Sound");
        processSounds("Musics", rootDirectory + "/core/assets/music", "arc.audio.Music");
        processUI(env.getElementsAnnotatedWith(StyleDefaults.class));
    }

    void processUI(Set<? extends Element> elements) throws Exception{
        TypeSpec.Builder type = TypeSpec.classBuilder("Tex").addModifiers(Modifier.PUBLIC);
        TypeSpec.Builder ictype = TypeSpec.classBuilder("Icon").addModifiers(Modifier.PUBLIC);
        TypeSpec.Builder ichtype = TypeSpec.classBuilder("Iconc").addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder load = MethodSpec.methodBuilder("load").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        MethodSpec.Builder loadStyles = MethodSpec.methodBuilder("loadStyles").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        MethodSpec.Builder icload = MethodSpec.methodBuilder("load").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        String resources = rootDirectory + "/core/assets-raw/sprites/ui";
        Jval icons = Jval.read(Fi.get(rootDirectory + "/core/assets-raw/fontgen/config.json").readString());

        ictype.addField(FieldSpec.builder(ParameterizedTypeName.get(ObjectMap.class, String.class, TextureRegionDrawable.class),
                "icons", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("new ObjectMap<>()").build());

        for(Jval val : icons.get("glyphs").asArray()){
            String name = capitalize(val.getString("css", ""));
            int code = val.getInt("code", 0);
            ichtype.addField(FieldSpec.builder(char.class, name, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("(char)" + code).build());

            ictype.addField(TextureRegionDrawable.class, name + "Small", Modifier.PUBLIC, Modifier.STATIC);
            icload.addStatement(name + "Small = mindustry.ui.Fonts.getGlyph(mindustry.ui.Fonts.def, (char)" + code + ")");

            ictype.addField(TextureRegionDrawable.class, name, Modifier.PUBLIC, Modifier.STATIC);
            icload.addStatement(name + " = mindustry.ui.Fonts.getGlyph(mindustry.ui.Fonts.icon, (char)" + code + ")");

            icload.addStatement("icons.put($S, " + name + ")", name);
            icload.addStatement("icons.put($S, " + name + "Small)", name + "Small");
        }

        Fi.get(resources).walk(p -> {
            if(!p.extEquals("png")) return;

            String filename = p.name();
            filename = filename.substring(0, filename.indexOf("."));

            String sfilen = filename;
            String dtype = p.name().endsWith(".9.png") ? "arc.scene.style.NinePatchDrawable" : "arc.scene.style.TextureRegionDrawable";

            String varname = capitalize(sfilen);

            if(SourceVersion.isKeyword(varname)) varname += "s";

            type.addField(ClassName.bestGuess(dtype), varname, Modifier.STATIC, Modifier.PUBLIC);
            load.addStatement(varname + " = ("+dtype+")arc.Core.atlas.drawable($S)", sfilen);
        });

        for(Element elem : elements){
            Seq.with(((TypeElement)elem).getEnclosedElements()).each(e -> e.getKind() == ElementKind.FIELD, field -> {
                String fname = field.getSimpleName().toString();
                if(fname.startsWith("default")){
                    loadStyles.addStatement("arc.Core.scene.addStyle(" + field.asType().toString() + ".class, mindustry.ui.Styles." + fname + ")");
                }
            });
        }

        ictype.addMethod(icload.build());
        JavaFile.builder(packageName, ichtype.build()).build().writeTo(BaseProcessor.filer);
        JavaFile.builder(packageName, ictype.build()).build().writeTo(BaseProcessor.filer);

        type.addMethod(load.build());
        type.addMethod(loadStyles.build());
        JavaFile.builder(packageName, type.build()).build().writeTo(BaseProcessor.filer);
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
                BaseProcessor.err("Duplicate file name: " + p.toString() + "!");
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
            type.addField(FieldSpec.builder(ClassName.bestGuess(rtype), name, Modifier.STATIC, Modifier.PUBLIC).initializer("new arc.mock.Mock" + rtype.substring(rtype.lastIndexOf(".") + 1)+ "()").build());
        });

        if(classname.equals("Sounds")){
            type.addField(FieldSpec.builder(ClassName.bestGuess(rtype), "none", Modifier.STATIC, Modifier.PUBLIC).initializer("new arc.mock.Mock" + rtype.substring(rtype.lastIndexOf(".") + 1)+ "()").build());
        }

        type.addMethod(loadBegin.build());
        type.addMethod(dispose.build());
        JavaFile.builder(packageName, type.build()).build().writeTo(BaseProcessor.filer);
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
