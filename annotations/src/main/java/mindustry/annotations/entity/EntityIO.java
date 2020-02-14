package mindustry.annotations.entity;

import arc.util.*;
import com.squareup.javapoet.*;
import com.squareup.javapoet.MethodSpec.*;
import mindustry.annotations.*;

import static mindustry.annotations.BaseProcessor.instanceOf;

public class EntityIO{
    final MethodSpec.Builder builder;
    final boolean write;

    EntityIO(Builder builder, boolean write){
        this.builder = builder;
        this.write = write;
    }

    void io(TypeName type, String field) throws Exception{

        if(type.isPrimitive()){
            s(type.toString(), field);
        }else if(type.toString().equals("java.lang.String")){
            s("UTF", field);
        }else if(instanceOf(type.toString(), "mindustry.ctype.Content")){
            if(write){
                s("short", field + ".id");
            }else{
                st(field + " = mindustry.Vars.content.getByID(mindustry.ctype.ContentType.$L, input.readShort())", BaseProcessor.simpleName(type.toString()).toLowerCase().replace("type", ""));
            }
        }
    }

    private void cont(String text, Object... fmt){
        builder.beginControlFlow(text, fmt);
    }

    private void cont(){
        builder.endControlFlow();
    }

    private void st(String text, Object... args){
        builder.addStatement(text, args);
    }

    private void s(String type, String field){
        if(write){
            builder.addStatement("output.write$L($L)", Strings.capitalize(type), field);
        }else{
            builder.addStatement("$L = input.read$L()", field, Strings.capitalize(type));
        }
    }
}
