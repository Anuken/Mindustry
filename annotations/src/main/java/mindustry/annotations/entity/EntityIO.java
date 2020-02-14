package mindustry.annotations.entity;

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
            s(type == TypeName.BOOLEAN ? "bool" : type.toString().charAt(0) + "", field);
        }else if(type.toString().equals("java.lang.String")){
            s("str", field);
        }else if(instanceOf(type.toString(), "mindustry.ctype.Content")){
            if(write){
                s("s", field + ".id");
            }else{
                st(field + " = mindustry.Vars.content.getByID(mindustry.ctype.ContentType.$L, read.s())", BaseProcessor.simpleName(type.toString()).toLowerCase().replace("type", ""));
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
            builder.addStatement("write.$L($L)", type, field);
        }else{
            builder.addStatement("$L = read.$L()", field, type);
        }
    }
}
