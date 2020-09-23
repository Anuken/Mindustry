package mindustry.annotations.misc;

import arc.func.*;
import arc.struct.*;
import com.squareup.javapoet.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;
import mindustry.annotations.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;

@SupportedAnnotationTypes("mindustry.annotations.Annotations.RegisterStatement")
public class LogicStatementProcessor extends BaseProcessor{

    @Override
    public void process(RoundEnvironment env) throws Exception{
        TypeSpec.Builder type = TypeSpec.classBuilder("LogicIO")
            .addModifiers(Modifier.PUBLIC);

        MethodSpec.Builder writer = MethodSpec.methodBuilder("write")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(Object.class, "obj")
            .addParameter(StringBuilder.class, "out");

        MethodSpec.Builder reader = MethodSpec.methodBuilder("read")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(tname("mindustry.logic.LStatement"))
            .addParameter(String[].class, "tokens");

        Seq<Stype> types = types(RegisterStatement.class);

        type.addField(FieldSpec.builder(
            ParameterizedTypeName.get(
            ClassName.get(Seq.class),
            ParameterizedTypeName.get(ClassName.get(Prov.class),
                tname("mindustry.logic.LStatement"))), "allStatements", Modifier.PUBLIC, Modifier.STATIC)
            .initializer("Seq.with(" + types.toString(", ", t -> "" + t.toString() + "::new") + ")").build());

        boolean beganWrite = false, beganRead = false;

        for(Stype c : types){
            String name = c.annotation(RegisterStatement.class).value();

            if(beganWrite){
                writer.nextControlFlow("else if(obj instanceof $T)", c.mirror());
            }else{
                writer.beginControlFlow("if(obj instanceof $T)", c.mirror());
                beganWrite = true;
            }

            //write the name & individual fields
            writer.addStatement("out.append($S)", name);

            Seq<Svar> fields = c.fields();

            String readSt = "if(tokens[0].equals($S))";
            if(beganRead){
                reader.nextControlFlow("else " + readSt, name);
            }else{
                reader.beginControlFlow(readSt, name);
                beganRead = true;
            }

            reader.addStatement("$T result = new $T()", c.mirror(), c.mirror());

            int index = 0;

            for(Svar field : fields){
                if(field.is(Modifier.TRANSIENT)) continue;

                writer.addStatement("out.append(\" \")");
                writer.addStatement("out.append((($T)obj).$L$L)", c.mirror(), field.name(),
                    Seq.with(typeu.directSupertypes(field.mirror())).contains(t -> t.toString().contains("java.lang.Enum")) ? ".name()" :
                    "");

                //reading primitives, strings and enums is supported; nothing else is
                reader.addStatement("if(tokens.length > $L) result.$L = $L(tokens[$L])",
                index + 1,
                field.name(),
                field.mirror().toString().equals("java.lang.String") ?
                "" : (field.tname().isPrimitive() ? field.tname().box().toString() :
                field.mirror().toString()) + ".valueOf", //if it's not a string, it must have a valueOf method
                index + 1
                );

                index ++;
            }

            reader.addStatement("result.afterRead()");
            reader.addStatement("return result");
        }

        reader.endControlFlow();
        writer.endControlFlow();

        reader.addStatement("return null");

        type.addMethod(writer.build());
        type.addMethod(reader.build());

        write(type);
    }
}
