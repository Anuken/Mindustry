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
    private static final String data = "eJzNWI1u2zYQfoi9ACdgANWmqpP+JK3aDvnrZqxNu7htMNRFQVG0zUYSNZKKk7p+5z3C7kjalhN7dTcUmJFY0vF4vx/vTv7hr5rxczYUpJRV3hirr5JcZM0wlWWttCVM8+RW+2EgC2GukZrqGlPJ7GiZApIbbpdpjZXFCooRWrJCfmZWqmqxvjCQKy1W0aVaRa2ETfY1PxH2tVYXMhd6HddrCIWwLec+sQu2JNURClYNEy0GhWg75JaqG8zOo8+yBjIvmDGkW32CfUpP6iYrJCfGgp+cXCiZEylpPDkNko2w1FvUCwEBy52MLRKNtbRCR1uEHqrK7DxZ5cgWObiy4qAZDIR+FtN6i2QxufOMTOSA1kRWoLniQg0IiKhAo98VT5YeCSdPnZIWR51mIPj9B6KFaQrbzYGlEmPiqJ0PKZiBOl71Emn2q1yDayDVec+3gdfdJgOlT1gpaMQ8S6JMctDIIo/itGe1rIbuSWiShavXsrRG4/S5FEUOxgzwaoCJbydDYR3ZwDrooe7Bc5DHgTNGG90tsr+5qgWNydOnQb6PdDxBX4L+hNW1qPLFHlo1RRHH6ZQzy0f0zUirMcsKQSCI0+k0BAkvph2hvQ8p3p+C33TEzIjO5FvlddM4jiGPlxbzZ6gTEKetYDtKOhWFEWQW6DNAgBqDxbipBr2ABmbFUDrlE+pT7xWQQlYCqLBRXApOo3EJEOSm1iqHU0rAN/L2bfcoSkxdSEsjEoFFsLWkcWKazHgzI89TiGpoR2A0HIq6YBxS2q8AmlHUohBPSMHcQljArqFoREyekAfkyxdn0VwSEO/tzKlWvVBjoQ+ZgQzB4a8sA/DSSGitdBTP+cSfDStMd1hBeXDMUedep9PZ6XTudO7j1wP86nQe4tcu/O/B/6OWgHWKoOQVIt+A0UKYiEUYuGx3YduQFft62JSisseXXNRY02iUMUS5FrbRFUEMQF58PNLp1gapOur2fjs67b47dqny9eGkKTMoCV9J2TLvd0nd9v1vTt3/K/LPocUR7HMQd7xPuBZwlN6IssZHGkFpHp5jVJKLzERYggpx5ioyGYzDUV/QKIqCOjROXNXGHFiisk9nL7uQjQvJUc8vwr7KsDPQfjSWVTksrXnch0/S72ulbL/PZXmx049iSNBt4mRwVXStKPF8L4lLjgEpvzdCX5GPxLMTQkBwT2BnIbfIQKuSQMW4t/PxAEJ5oJjOQXRgfq40OWZ8hFJRAfSKhS7Pcma4lrVNjvlIzdiSNrYCn7iUlkAFDo8nUNTw1kWDFwqzmK4BOvcqMMx3755A3ocK7jGWWHv3M6OKxorXzOM3IP0GjFO3IQfHLepCDF+D6943oXXtAfjvEFabYhgs0BL6zFogT2cxraDBQkx7VwbzM3QjUC20vaJRA2NWgusgJVOqEKwirLoC7gFUUeHapqwskUDppHB50uooIX5Avn3b98hWd1qwvaeS3HZGJGjhocrB5Zj8dFPSB9dP49RbALOiSDM4dOdreiuk8UdgjeGoklziZHCIM6GfTVmABo0O4QSBx0PNyiNmWb8fgTndCjS7bvuruAwN9yWgaJBo6Miq7AmR069ErOXOFulskUfhA40bIfYj2AQolsbCABJP8Kk8h28cR8JUwLS+NhPgPEDQhEB380G8Kg/bnRB45G4NCiAzTlEZH8FAQaOz3h+9TTymwXZgjZKj/Tfgn6tULanTKUTaDi0YsaGC3d1dWNk8jiFyoKMVuU1GqBWjEgppeRCo0xZGkQPwlXsG9A+Hqcni9J02lZWlaJ06PCjTVXBE+HscusH+yhcxrFF1jbA7mi3Q7wg9jN/cggXawK7QyBaLPnmrbPnmbLnieiNd3xPbX/XhJrAxNjcB/S+h5Tz+Z2w5liVw3QDNTjwb75pG5rPyBWOFBZrBBARvIlxfzF/4BLP+cVlDahYB98o38cJzxmlQu3ifojgHPLyP2YXSVXHMcmCO02Xr6sbO7cLL9XXDLrDVtiLib/0LS7tTtHXmwuncIBB4DKcZmgH5p++gHePvBO4lKk7xPa77ymcobM22CE8uPBsur2dCmK9fbQz66hRTnpQqw4P1M3EQiLfhvbITVtEsDp29UHrGP3MaYnV6CO82XPMQfvcM4eL480edQ1tsMbvNLxRYgMsQjXesaPD4BbFeNxqTQ9zlZ7HyTLXWw+Fa56Ljw04s41lNJHUYWzOgwF/4vSIgbzZ4tGa4zaq2e/F17Ctfdd1UE15zg0Z0ajZ+BXXwtguxxlEYEBM0ecLq4mLmc1+IA2xLWJ5TAwWF6f3gd1CPiy5mMwsKSITzeK7fXWIy8SugaHt758EelJzOw737Ow8e7b5IvdmVn4vwh4W5CTgWX7cQFr1pZILi7m3fGkEXDTvnNs5NHKVTECD0AGZe92tPPQu+G6f8NGnIvPJATv8GxS7XDA==";

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
