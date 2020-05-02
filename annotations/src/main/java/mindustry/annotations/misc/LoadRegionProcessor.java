package mindustry.annotations.misc;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import com.squareup.javapoet.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;
import mindustry.annotations.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;

@SupportedAnnotationTypes({"mindustry.annotations.Annotations.LoadRegion",})
public class LoadRegionProcessor extends BaseProcessor{

    @Override
    public void process(RoundEnvironment env) throws Exception{
        TypeSpec.Builder regionClass = TypeSpec.classBuilder("ContentRegions")
            .addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder method = MethodSpec.methodBuilder("loadRegions")
            .addParameter(tname("mindustry.ctype.MappableContent"), "content")
            .addModifiers(Modifier.STATIC, Modifier.PUBLIC);

        ObjectMap<Stype, Array<Svar>> fieldMap = new ObjectMap<>();

        for(Svar field : fields(LoadRegion.class)){
            if(!field.is(Modifier.PUBLIC)){
                err("@LoadRegion field must be public", field);
            }

            fieldMap.getOr(field.enclosingType(), Array::new).add(field);
        }

        int index = 0;

        for(Entry<Stype, Array<Svar>> entry : fieldMap){
            if(index == 0){
                method.beginControlFlow("if(content instanceof $T)", entry.key.tname());
            }else{
                method.nextControlFlow("else if(content instanceof $T)", entry.key.tname());
            }

            //go through each supertype
            for(Stype stype : entry.key.superclasses().and(entry.key)){
                if(fieldMap.containsKey(stype)){
                    for(Svar field : fieldMap.get(stype)){
                        LoadRegion an = field.annotation(LoadRegion.class);
                        //get # of array dimensions
                        int dims = count(field.mirror().toString(), "[]");

                        //not an array
                        if(dims == 0){
                            method.addStatement("(($T)content).$L = $T.atlas.find($L)", entry.key.tname(), field.name(), Core.class, parse(an.value()));
                        }else{
                            //is an array, create length string
                            int[] lengths = an.lengths();
                            if(lengths.length == 0) lengths = new int[]{an.length()};

                            if(dims != lengths.length){
                                err("Length dimensions must match array dimensions: " + dims + " != " + lengths.length, field);
                            }

                            StringBuilder lengthString = new StringBuilder();
                            for(int value : lengths) lengthString.append("[").append(value).append("]");

                            method.addStatement("(($T)content).$L = new $T$L", entry.key.tname(), field.name(), TextureRegion.class, lengthString.toString());

                            for(int i = 0; i < dims; i++){
                                int length = lengths[0];

                                method.beginControlFlow("for(int INDEX$L = 0; INDEX$L < $L; INDEX$L ++)", i, i, length, i);
                            }

                            StringBuilder indexString = new StringBuilder();
                            for(int i = 0; i < dims; i++){
                                indexString.append("[INDEX").append(i).append("]");
                            }

                            method.addStatement("(($T)content).$L$L = $T.atlas.find($L)", entry.key.tname(), field.name(), indexString.toString(), Core.class, parse(an.value()));

                            for(int i = 0; i < dims; i++){
                                method.endControlFlow();
                            }
                        }
                    }
                }
            }

            index ++;
        }

        if(index > 0){
            method.endControlFlow();
        }

        regionClass.addMethod(method.build());

        write(regionClass);
    }

    private static int count(String str, String substring){
        int lastIndex = 0;
        int count = 0;

        while(lastIndex != -1){

            lastIndex = str.indexOf(substring, lastIndex);

            if(lastIndex != -1){
                count ++;
                lastIndex += substring.length();
            }
        }
        return count;
    }

    private String parse(String value){
        value = '"' + value + '"';
        value = value.replace("@", "\" + content.name + \"");
        value = value.replace("#1", "\" + INDEX0 + \"");
        value = value.replace("#2", "\" + INDEX1 + \"");
        value = value.replace("#", "\" + INDEX0 + \"");
        return value;
    }

}
