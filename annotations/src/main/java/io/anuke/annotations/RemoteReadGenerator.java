package io.anuke.annotations;

import com.squareup.javapoet.*;
import io.anuke.annotations.IOFinder.ClassSerializer;

import javax.lang.model.element.*;
import javax.tools.Diagnostic.Kind;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

/** Generates code for reading remote invoke packets on the client and server. */
public class RemoteReadGenerator{
    private final HashMap<String, ClassSerializer> serializers;

    /** Creates a read generator that uses the supplied serializer setup. */
    public RemoteReadGenerator(HashMap<String, ClassSerializer> serializers){
        this.serializers = serializers;
    }

    /**
     * Generates a class for reading remote invoke packets.
     * @param entries List of methods to use.
     * @param className Simple target class name.
     * @param packageName Full target package name.
     * @param needsPlayer Whether this read method requires a reference to the player sender.
     */
    public void generateFor(List<MethodEntry> entries, String className, String packageName, boolean needsPlayer)
    throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, IOException{

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
        classBuilder.addJavadoc(RemoteMethodAnnotationProcessor.autogenWarning);

        //create main method builder
        MethodSpec.Builder readMethod = MethodSpec.methodBuilder("readPacket")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(ByteBuffer.class, "buffer") //buffer to read form
        .addParameter(int.class, "id") //ID of method type to read
        .returns(void.class);

        if(needsPlayer){
            //since the player type isn't loaded yet, creating a type def is necessary
            //this requires reflection since the TypeName constructor is private for some reason
            Constructor<TypeName> cons = TypeName.class.getDeclaredConstructor(String.class);
            cons.setAccessible(true);

            TypeName playerType = cons.newInstance("io.anuke.mindustry.entities.type.Player");
            //add player parameter
            readMethod.addParameter(playerType, "player");
        }

        CodeBlock.Builder readBlock = CodeBlock.builder(); //start building block of code inside read method
        boolean started = false; //whether an if() statement has been written yet

        for(MethodEntry entry : entries){
            //write if check for this entry ID
            if(!started){
                started = true;
                readBlock.beginControlFlow("if(id == " + entry.id + ")");
            }else{
                readBlock.nextControlFlow("else if(id == " + entry.id + ")");
            }

            readBlock.beginControlFlow("try");

            //concatenated list of variable names for method invocation
            StringBuilder varResult = new StringBuilder();

            //go through each parameter
            for(int i = 0; i < entry.element.getParameters().size(); i++){
                VariableElement var = entry.element.getParameters().get(i);

                if(!needsPlayer || i != 0){ //if client, skip first parameter since it's always of type player and doesn't need to be read
                    //full type name of parameter
                    String typeName = var.asType().toString();
                    //name of parameter
                    String varName = var.getSimpleName().toString();
                    //captialized version of type name for reading primitives
                    String capName = typeName.equals("byte") ? "" : Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

                    //write primitives automatically
                    if(Utils.isPrimitive(typeName)){
                        if(typeName.equals("boolean")){
                            readBlock.addStatement("boolean " + varName + " = buffer.get() == 1");
                        }else{
                            readBlock.addStatement(typeName + " " + varName + " = buffer.get" + capName + "()");
                        }
                    }else{
                        //else, try and find a serializer
                        ClassSerializer ser = serializers.get(typeName);

                        if(ser == null){ //make sure a serializer exists!
                            Utils.messager.printMessage(Kind.ERROR, "No @ReadClass method to read class type: '" + typeName + "'", var);
                            return;
                        }

                        //add statement for reading it
                        readBlock.addStatement(typeName + " " + varName + " = " + ser.readMethod + "(buffer)");
                    }

                    //append variable name to string builder
                    varResult.append(var.getSimpleName());
                    if(i != entry.element.getParameters().size() - 1) varResult.append(", ");
                }else{
                    varResult.append("player");
                    if(i != entry.element.getParameters().size() - 1) varResult.append(", ");
                }
            }

            //execute the relevant method before the forward
            //if it throws a ValidateException, the method won't be forwarded
            readBlock.addStatement("$N." + entry.element.getSimpleName() + "(" + varResult.toString() + ")", ((TypeElement)entry.element.getEnclosingElement()).getQualifiedName().toString());

            //call forwarded method, don't forward on the client reader
            if(entry.forward && entry.where.isServer && needsPlayer){
                //call forwarded method
                readBlock.addStatement(packageName + "." + entry.className + "." + entry.element.getSimpleName() +
                "__forward(player.con" + (varResult.length() == 0 ? "" : ", ") + varResult.toString() + ")");
            }

            readBlock.nextControlFlow("catch (java.lang.Exception e)");
            readBlock.addStatement("throw new java.lang.RuntimeException(\"Failed to to read remote method '" + entry.element.getSimpleName() + "'!\", e)");
            readBlock.endControlFlow();
        }

        //end control flow if necessary
        if(started){
            readBlock.nextControlFlow("else");
            readBlock.addStatement("throw new $1N(\"Invalid read method ID: \" + id + \"\")", RuntimeException.class.getName()); //handle invalid method IDs
            readBlock.endControlFlow();
        }

        //add block and method to class
        readMethod.addCode(readBlock.build());
        classBuilder.addMethod(readMethod.build());

        //build and write resulting class
        TypeSpec spec = classBuilder.build();
        JavaFile.builder(packageName, spec).build().writeTo(Utils.filer);
    }
}
