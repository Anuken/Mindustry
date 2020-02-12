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
    private static final String data = "eJy1V41u2zYQfoM9AydgANV4apIO61Y1HfLXzUCbdnHaYKiDgqIom41ECiRlxzX80HuD3ZFyLCfO4g2bEUQiebyf7747Ut/8WTN+zUaCVFLljXVmluQia0aprGptHGGGJ0+6g0KWwt6ZatQdoYq58fpM42S5YcYKI1kpvzIntVqtr5zh2ohN81JvmlXCJYeGnwn33uiJzIV5SOo9hC1cJ5AvbMLWtPqJkqlRYkRRCu7uLKl7wj6ir7KGaV4ya0lffYF92szrJislJ9ZBnJxMtMyJlDSen7earXA0eDRoAQHPvY4eiaZGOmGiHqHHWtn9l5sC6ZGjmRNHTVEI8yqmdY9kMfn+FZnLgtZEKrCsuNAFARUKLIZd8XxtSDg58EY6EnWageJPV8QI25Sun4OIElPiZ3evUnADbbwbJNIeqtxAaKDVR8/3QNa/JoU2Z6wSNGJBJNE2OWpkmUdxOnBGqpEfCUOy9hmsrK3ROH0tRZmDMwU+LQjxvWQknJ+2sA52qB8ECfKilYzRR/+K4hezWtCYHBy0+gPS8Rxjae0nrK6Fyld7qGrKMo7TBWeOj+nF2Ogpy0pBAMTFYtGChA/bReinqxTfzyFuulTtdDBL42TM7PhY5+BNDMm8cZhES72WOO0g7mfShSitIEu0L4EGegpu46YajAMlmBMj6T2Y05D/YIqUUgmYhY3iRnAaTSvgIbe10XnDHYEAyYcP/ZMosXUpHY1IBLFuExQqTsBG9Xg0rjFqGUmvde81dBOCLQX043vCjYAgLkRV45BGUBmja+B+lEwyGyEDSnHpC4IU09ap1RxFVUtKEdhBDqIBxKazL5dv+1BbE8nR1K/CvcuwNukwmkpVjSpnXwzhlwyHRms3HHJZTfaHUTxU0Q7xOrgu+05UiMSauuQUEP29EWZGPpMgTggBxQOBtU2ekMLoikC6nu1/PmJWHGlmclDdCr/WhpwyPkataACqdWUriFxabmTtklM+1kuxJHSKs6bKAIsgJ26kI1AD7fAMcoCvaTFNfBOhgEiMI15qC2lKN7ODRjwYJE+fnuk3eqQhpB2fJiyFY6a0kpyV76HLY647nGmp8N9SxxvOAU2HPq8TabGMQUF/AUuDmUVwRv4EqIVxMxo1cMokuA78ybQuBVOEqRlIFwwqyncNqRyRMLObwuNlp5aSUqiRG8P0zk5oEZ26XIl9ohIgQiPdWMh39zVd+XYSp8EDZxqRZkD66wdaC5T7tyAaQ6mQXGJjPMYjMRzDLLO6bACW6BjoCxGPDKtOmGPDIWasr8Cy7zi/iZu26byFpBWJgQToaiBETh9BrBNOj+z2yM/tL4YGgb6BT8AaaR3033iOo+oa/i+78R1c93ZbIFGQj6Eh0uhy8MdgG29paxdEo+Tk8AJ886wOXLml2J0t8RU07QWA50YO/NjS7vPnz2Fle2haMMBGB4xtimAD3VFJJ7B2dtGhHUoAZfIggPHhyTB3yBxv5rxRTlbi9IaLGm9WNELuLzYxDBkdqOWvKrPQByDkw7pGJp0sF+j/yCbE79aDFYHAr/ZsWC2G5G3y5R9ny/eVVboeoeuj9u9zFePamqP/ki0+iL+nixdZ48s9HuzHy5tC08h82WTg8HUwZxHTNsgI1/E8jjyGOIK7yGlVA9orygfj20QRJOO0Nbu69FE8Kn/8ARMGDUZxTFwrHKfr3tWNu/ULH3fXLZuEg+MWkfAaLlTdft61mQtvcwsgsLIWGboBtKAfhbH4MeOve3GKl83+u5ChdmvWIzyZBDFcflgImfvwamMxVm+YwjeXzrBWfiGeAvEeXH5321V0i8OnVKnNUn4ZNGB1fvxsn3DDW/j9GODi+I1W53B4dYT95jcaPMBlQOMjKxusqFZtsI3O5IA7fMZsPAU66219PRSil8PzUsbLNkfq9jKTwQz8gW5hCgb3On8RXlLQH7LEt0RLbpkOOv4Chwsv3w==";

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
