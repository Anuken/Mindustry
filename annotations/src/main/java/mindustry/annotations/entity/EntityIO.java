package mindustry.annotations.entity;

import arc.util.*;
import com.squareup.javapoet.*;
import com.squareup.javapoet.MethodSpec.*;
import mindustry.annotations.*;

import javax.lang.model.element.*;

public class EntityIO{
    final TypeElement contentElem = BaseProcessor.elementu.getTypeElement("mindustry.ctype.Content");
    final MethodSpec.Builder builder;
    final boolean write;

    EntityIO(Builder builder, boolean write){
        this.builder = builder;
        this.write = write;
    }

    void io(TypeName type, String field) throws Exception{
        TypeElement element = BaseProcessor.elementu.getTypeElement(type.toString());

        if(type.isPrimitive()){
            s(type.toString(), field);
        }else if(type.toString().equals("java.lang.String")){
            s("UTF", field);
        }else if(element != null && BaseProcessor.typeu.isSubtype(element.asType(), contentElem.asType())){
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
