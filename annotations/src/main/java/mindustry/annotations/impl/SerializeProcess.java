package mindustry.annotations.impl;

import com.squareup.javapoet.*;
import mindustry.annotations.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.remote.*;

import javax.annotation.processing.*;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;

@SupportedAnnotationTypes("mindustry.annotations.Annotations.Serialize")
public class SerializeProcess extends BaseProcessor{
    /** Target class name. */
    private static final String className = "Serialization";
    /** Name of the base package to put all the generated classes. */
    private static final String data = "eJztV0tvGzcQvvfQ3zDRIeDCKhsbQVDUsgP5UViH2IHl9BIEBsUdSYxX3C3Jlawm+XH9Z53hUg/bkuOmOfRQwfDuDme++ebBWe6PfwU3/wTwUU2VLJQdSYfDAnWQvxkschjCAUyMzWtPetJikF2nzzG8deXU5OjkW6VvMPTRGVWYP0mgC+W9HGE4Qbp1mEcg0Zo5E9C1sn2AofQYulqj92ZQoAiuxqVc2Loo2iCU03JYWy2PS+v3OndJNF7bDW1rSnk0D3hUD4foDjNRtWGQwU+HQIGZoajAWB+U1VgOYROOZx+Wgm4eMzJ7ghpoyo14Cl5FsQ2I4PsPcE2/XXpssk7kOMw6mEJe9KXxXZu70uTM4Jjz2Hl9CJ79xCc5LN25mqBoqUZPVosy9DEEY0eebnTtMKZ5iaDddgRd2oA2MGO+XqIvi2mq0xJAqQ0ARHzA8dncywWar91QaZwanMkUS7eqCqNVMKW9x+qRuO6wug3R8GGLvsEwLnMYMZBS6z3XrIgWidYhLgYfyQ50IyKrkZbGTssbjHU4Lh1KVVWbvaUNEf8fUFXYX+rt7vnJ5UXv5Lp3Et30g6NagDK55RZpHrNoyUaxwx+PyA+XLtZCaYBabSpoOzlptttX0uM8oen7aJsqnhLkkixmyPlFjlLe1kL0a/ER6YVis4UXKO2YCbYyNkCBnBQv6ToKY5Gt9kauAveZxVkjYc2fYe8DT4bSCTY2tP5iny4dxuGbnQPY4+3Cxu9N1GdODJAJcTxWTmmaOzI3IxOEl5ok3SBM1obdVxl0OvAyA9iB7Zq0uNtoM9cvy9gpvLoIiXAjW+1mnwZi7Ht5pDy+enlc8k5Fq+kqmG8EpBnQIEn8o1aFp25a/C66B60sgzB25Sx6uaxtMBM8vdVYMbBoHakc3r3rnchYvjhdiBGDM1csPD4cMr3Sc8ZSGHVtuJ+X/e8Xk2TZcKLFOtR2rVYizM8EdDqpwlxkDJZKeCcnUfYLl4+f2MFEhbG8pE0uMhqXt4Gntk/hM3Ti8k0JTSgM8zCWqg7LKPiyWcurKYr1PDaYi0x+Wi08gVaOkdYT85paa+Enbubo4NTWE3QRvtO87eg1Qy/gWeluerQd47w9BCRSsHWdfd6XebGcGptMoKw58Dhe4IwrXJYFKkspEKnYfImdRB0R7+GAasezjRIXamdhSP2M+1/rjv7cB5xI5Zya67KaN2BteNFOFvE2CtPUYObJxbN/1Sxb9hw8f/7dgbsMnKoMcAbjlIezWAcecJRxkmHcGacFTmg48xrLuYBnyuUzerl185y8UPkW6YbPn+HZWFJhtmlmMSKUY+XfUC8m8NgBG52uDeXrVFnYhv3Py3u9sb7X9wu8eMUE9x1GArUoAW0rNyVw42r3WwfwanDQHx1+9FhcMYii4y6E/6fvf3T6UiaZLA3BtXO9Zvvf0Xn2MahNEfmv1unr42peYe9Cxk+chD6gU5qcNla8/GQbSwfhJyvXvslmpC2oxOXAUIe9TgegXfgVXizXOSxN4RSlW9nEnK4eGzsGolO9pw+6xXC6d/pa0yDBzs7db6ZHGEczPgSbO+88qBpVMYjSbH/Trgn0vUM8+oE+O67otMbt8uWHvwGqGwCj";

    @Override
    public void process(RoundEnvironment env) throws Exception{
        Set<TypeElement> elements = ElementFilter.typesIn(env.getElementsAnnotatedWith(Serialize.class));

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
        classBuilder.addStaticBlock(CodeBlock.of(new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)))).readUTF()));
        classBuilder.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "\"unchecked\"").build());
        classBuilder.addJavadoc(RemoteProcess.autogenWarning);

        MethodSpec.Builder method = MethodSpec.methodBuilder("init").addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        for(TypeElement elem : elements){
            TypeName type = TypeName.get(elem.asType());
            String simpleTypeName = type.toString().substring(type.toString().lastIndexOf('.') + 1);

            TypeSpec.Builder serializer = TypeSpec.anonymousClassBuilder("")
            .addSuperinterface(ParameterizedTypeName.get(
            ClassName.bestGuess("arc.Settings.TypeSerializer"), type));

            MethodSpec.Builder writeMethod = MethodSpec.methodBuilder("write")
            .returns(void.class)
            .addParameter(DataOutput.class, "stream")
            .addParameter(type, "object")
            .addException(IOException.class)
            .addModifiers(Modifier.PUBLIC);

            MethodSpec.Builder readMethod = MethodSpec.methodBuilder("read")
            .returns(type)
            .addParameter(DataInput.class, "stream")
            .addException(IOException.class)
            .addModifiers(Modifier.PUBLIC);

            readMethod.addStatement("$L object = new $L()", type, type);

            List<VariableElement> fields = ElementFilter.fieldsIn(BaseProcessor.elementu.getAllMembers(elem));
            for(VariableElement field : fields){
                if(field.getModifiers().contains(Modifier.STATIC) || field.getModifiers().contains(Modifier.TRANSIENT) || field.getModifiers().contains(Modifier.PRIVATE))
                    continue;

                String name = field.getSimpleName().toString();
                String typeName = BaseProcessor.typeu.erasure(field.asType()).toString().replace('$', '.');
                String capName = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

                if(field.asType().getKind().isPrimitive()){
                    writeMethod.addStatement("stream.write" + capName + "(object." + name + ")");
                    readMethod.addStatement("object." + name + "= stream.read" + capName + "()");
                }else{
                    writeMethod.addStatement("arc.Core.settings.getSerializer(" + typeName + ".class).write(stream, object." + name + ")");
                    readMethod.addStatement("object." + name + " = (" + typeName + ")arc.Core.settings.getSerializer(" + typeName + ".class).read(stream)");
                }
            }

            readMethod.addStatement("return object");

            serializer.addMethod(writeMethod.build());
            serializer.addMethod(readMethod.build());

            method.addStatement("arc.Core.settings.setSerializer($N, $L)", BaseProcessor.elementu.getBinaryName(elem).toString().replace('$', '.') + ".class", serializer.build());

            name(writeMethod, "write" + simpleTypeName);
            name(readMethod, "read" + simpleTypeName);

            writeMethod.addModifiers(Modifier.STATIC);
            readMethod.addModifiers(Modifier.STATIC);

            classBuilder.addMethod(writeMethod.build());
            classBuilder.addMethod(readMethod.build());
        }

        classBuilder.addMethod(method.build());

        //write result
        JavaFile.builder(packageName, classBuilder.build()).build().writeTo(BaseProcessor.filer);
    }

    static void name(MethodSpec.Builder builder, String name){
        try{
            Field field = builder.getClass().getDeclaredField("name");
            field.setAccessible(true);
            field.set(builder, name);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
