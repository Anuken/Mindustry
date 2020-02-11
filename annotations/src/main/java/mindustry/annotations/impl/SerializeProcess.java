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
    private static final String data = "eJy1V41u2zYQfoM9AydgANV4apIO61Y1HfLXzUCbdnHaYKiDgqJom4lECiRlxzX80HuD3ZFSLKfO4g2bEUTi3fF+vztS3/xZMX7DxoKUUuW1dWae5CKrx6ksK20cYYYnT7qLkSyEvUeq1T2hkrnJOqV2sthAscJIVsgvzEmtVvyVM1wbsYku9SaqEi45NPxMuPdGT2UuzENS7yFs4TqBXLMpW9PqCQVT48SIUSG4u8dSXwn7iL7ICsi8YNaSvrqGfdosqjorJCfWQZycTLXMiZQ0Xpw3mq1wNHg0aBICnnsdPRLNjHTCRD1Cj7Wy+y83BdIjR3MnjurRSJhXMa16JIvJ96/IQo5oRaQCy4oLPSKgQoHFsCterC0JJwfeSEeiSjNQ/OmKGGHrwvVzEFFiRjx19yoFN9DGu0Ei7aHKDYQGWn30fA9k/Wsy0uaMlYJGLIgk2iZHtSzyKE4Hzkg19ithSNY8g5U1Ho3T11IUOTgzwqcFIb6XjIXzZAt8sEP9IkiQF41kjD76VxS/mFeCxuTgoNEfMh0vMJbGfsKqSqh8tYequijiOF1y5viEXkyMnrGsEASSuFwumyThw3Yz9NNViu/nEDdtVTsdzNI4mTA7OdY5eBNDMW8dFtFSryVOOxn3lHQpCitIm+1LgIGegdu4qQLjAAnmxFh6DxY01D+YIoVUAqiwUdwKTqNZCTjktjI6r7kjECD58KF/EiW2KqSjEYkg1m2CQsUJ2Cgfj8bVRrWR9Br3XsM0IThSQD++J9wICOJClBUuaQSdMb4B7EfJNLMRIqAQl74hyGjWOLWiUVTVQorADnIQDSA2nV1fvu1Db00lR1O/Cvcuw96kw2gmVTkunX0xhF8yHBqt3XDIZTndH0bxUEU7xOvguug7UWIm1tQlp5DR32th5uQzCeKEEFA8ENjb5AkZGV0SKNez/c9HzIojzUwOqhvh19qQU8YnqBUNQLeubAWRS8uNrFxyyie6FUvCpDirywxyEeTErXQEeqBZnkEN8DUdzRI/RChkJMYVL7SFMqWb0UEjHgySp0/P9Bs91hDSji8TtsIxU1pJzor3MOWx1h3MNFD4b6HjDeeQTYc+rwNp2cagYL6ApcHcYnLG/gSohHFzGtVwyiTIB/xkWheCKcLUHKRHDDrKTw2pHJFA2U3h8bLTS0kh1NhNgLyzE0ZEpy9XYp+ohBShkW4s5LuvNV35cRKnwQNnapFmAPqbB0YLtPu3IBpDq5Bc4mA8xiMxHMMss7qoIS3RMcAXIh4bVp4wx4ZDrFhfgWU/cX4Tt83QeQtFGyUGCqDLgRA5fSRjnXB6ZLdHfm5+MQwI9A18AtRI62D+xgtclTfwv53G9/K6t9skEgX5BAYijS4Hfwy28ZY2dkE0Sk4OL8A3j+qAlTuI3dsSX8HQXkLy3NiBH1vaff78OXC2T02TDLDRScY2TbAB7qikE1hDXXZghxIAmTwIYHx4MiwcIsebOa+Vk6U4veWiwpsVjRD7y00IQ0QHaPmryjzMAQj5sKoQSSctg/7faLpzYYUgcKw5HFbMUL1NzvzjcvnBsqrXI3h91P4msG6P0X+JFh/D38PFi6zh5Ssc7MftTaGuZd4OGTh8HdAsprSJMUI+nseRTyGu4C5yWlaQ7BXkg/FtogiScdqYXV36KB6VP/6A9YIBozjWrRGO03Xvqtrd+YWP+3zLpuHguMtIeA0Xqu4879rMhbe5RSKws5YZugGooB+Fsfgx4697cYqXzf67UKFma9YjPJkGMWQ/LITAfZhbW4zVG6bwzaUzbJVfiIdAvAeX392Gi25x+JQqtGnl26AhV+fHz/YJN7xJv19Dujh+o1U5HF4dYb/5jQYPkA3Z+MiKGhuqURtsozM55B0+YzaeAh1+014Phejl8LyUcTvmSNVcZjKgwB/oFmbE4F7nL8ItBP0hS/xItOQO6aDjL203L98=";

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
