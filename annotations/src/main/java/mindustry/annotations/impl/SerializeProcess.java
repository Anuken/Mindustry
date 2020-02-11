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
    private static final String data = "eJy1V21v2zYQ/gf7DZyAAVTjqUk6rFvVdMhbNwNt2sVpg6EOCoqibSYSKZCUHdfwH9+n3ZF07KT24g2bEUQiebx77rkXUt/82TB+w4aC1FKVrXVmmpWiaIe5rBttHGGGZ09WBwNZCftgqlUPhGrmRvdnWierNTNWGMkq+YU5qdVyfQmGayPWzUu9blYJlx0afibce6PHshRmk9R7cFu4FUeu2Zjd0+onKqaGmRGDSnD3YEl9Jew9+iIbmOYVs5Z01TXs02bWtEUlObEO/ORkrGVJpKTp7DxqtsLRgKgXCQHkXkeHJBMjnTBJh9Bjrez+y3WOdMjR1ImjdjAQ5lVKmw4pUvL9KzKTA9oQqcCy4kIPCKhQYDHsSmf3hoSTA29kRaLJC1D86YoYYdvKdUsQUWJC/OzuVQ4w0Ma7XibtoSoNuAZavfd8D2T9azbQ5ozVgiYsiGTaZketrMokzXvOSDX0I2FIEZ/Byr01muavpahKADPApwUhvpcNhfPTFtbBDvWDIEFeRMkUMfpXFL+YNoKm5OAg6g9MpzP0JdrPWNMIVS73UNVWVZrmc84cH9GLkdETVlSCAInz+TyShA+7ytBPVzm+n4PfdKHa6WCWptmI2dGxLgFNCsG8dRhES72WNF9h3M/kc1FZQRZsX0Ia6AnAxk0NGIeUYE4MpUcwoyH+wRSppBIwCxvFreA0mdSQh9w2RpctdwQcJB8+dE8gHNt4gtoyUFw/7oJrjVrA70RMr6GFEOwjoB/fM24EIL8QdYNDmkA5DG8g4ZNsXNgEw16JS18FZDCJoJZzFFUt8ojADnKQ9MAhXVxfvu1CQY0lR1O/CveuwIKk/WQiVT2snX3Rh1/W7xutXb/PZT3e7ydpXyU7xOvguuo6USMT99Rlp0Dj760wU/KZBHFCCCjuCSxo8oQMjK4JxOjZ/ucjZsWRZqYE1VH4tTbklPERakUDUKJLW0Hk0nIjG5ed8pFeiGWhPZy1dQFcBDlxKx2BxI/DM4gBvuaDSeY7BwVGUhzxSlsIU74hJXiwR54+PdNv9FCDRzs+Spj+x0xpJTmr3kNnx1CH6P+32eKNlUCgQ5ibcoeUEtvDMR4M4TBihdVVC5uSY4gndP6hYfUJc6zfRx+6CqrC191v4jaW3ltwY5AZgKfrnhAl7U0t8jv0J0cjjJvSpIXTKVPQt5JV4B2y2yE/x18KLQEK8lvABDRK66ALpTMc1Tfwf9GTpHJEAujdHB4vyR4+d3aCIB9BW6DJZe+P3jZoabQLokl2cngB2HyYA5N3AXiwJb2C1jUH8tzQAY4t7T5//hxWtqcmkgE2VsjYJkXWJAMqWXEszs5jVuAqNI0yLKJvi7RGUGDmEdB5oXUlmCJMTUF6wKCzrovUsqdmlVBDN4qBw6NipT8vxT5RCZShkVVeyHdfa7ryx0qaBwTOtCIvwKWbDUcMEguiKXE472k8b5WTtTi95aLB+xPygEfE7G9EEnR+vs4EuhSqy99ZpqE3AMbDpsFiOlks0P+7oO4gLIsIgMUDY7kYEngdmH+csb7zLFP2kZJ91P66et2+TP9lwXgf1lbMXZp6kXtl81Ue7KeLK0PbynLRZ+FAdjBnkdLoY4LreEYnnkIcwaXktG6A7GXVB+PbeBEk0zyaXd7+KB6fP/6A8YIeqzjGLQqn+X10TevucOHj4bpl43Cy3DESXmPZrMyv2iyFt7kFEVhZ8wJhQFbQj8JY/Krx9740x1tn912IUNxadAjPxkEMlzcLYeJuXm0t+uoNU/j40gWWyi/Ep0C6B7fg3biKsDh8U1XaLOQXTgNX58fP9gk3PNLvx0AXx4+1poTutSLsN7/RgACXgY2PrGqxoKLaYBvBlMA7fM+sPQhX1mN5bXLRy2HDlJ5lfxNu4gWngBn4A93CDBjc9fyNeJGCvsuGrmnJXaaDjr8ApwI0lQ==";

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
