package io.anuke.annotations;

import com.squareup.javapoet.*;
import io.anuke.annotations.IOFinder.ClassSerializer;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

/**Generates code for writing remote invoke packets on the client and server.*/
public class RemoteWriteGenerator {
    private final HashMap<String, ClassSerializer> serializers;

    /**Creates a write generator that uses the supplied serializer setup.*/
    public RemoteWriteGenerator(HashMap<String, ClassSerializer> serializers) {
        this.serializers = serializers;
    }

    /**Generates all classes in this list.*/
    public void generateFor(List<ClassEntry> entries, String packageName) throws IOException {

        for(ClassEntry entry : entries){
            //create builder
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(entry.name).addModifiers(Modifier.PUBLIC);

            //add temporary write buffer
            classBuilder.addField(FieldSpec.builder(ByteBuffer.class, "TEMP_BUFFER", Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("ByteBuffer.allocate($1L)", RemoteMethodAnnotationProcessor.maxPacketSize).build());

            //go through each method entry in this class
            for(MethodEntry methodEntry : entry.methods){
                //write the 'send event to all players' variant: always happens for clients, but only happens if 'all' is enabled on the server method
                if(!methodEntry.server || methodEntry.allVariant){
                    writeMethodVariant(classBuilder, methodEntry, true);
                }

                //write the 'send even to one player' variant, which is only applicable on the server
                if(methodEntry.server && methodEntry.oneVariant){
                    writeMethodVariant(classBuilder, methodEntry, true);
                }
            }

            //build and write resulting class
            TypeSpec spec = classBuilder.build();
            JavaFile.builder(packageName, spec).build().writeTo(Utils.filer);
        }
    }

    private void writeMethodVariant(TypeSpec.Builder classBuilder, MethodEntry methodEntry, boolean toAll){
        ExecutableElement elem = methodEntry.element;

        //create builder
        MethodSpec.Builder method = MethodSpec.methodBuilder(elem.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class);

        //validate client methods to make sure
        if(methodEntry.client){
            if(elem.getParameters().isEmpty()){
                Utils.messager.printMessage(Kind.ERROR, "Client invoke methods must have a first parameter of type Player.", elem);
                return;
            }

            if(!elem.getParameters().get(0).asType().toString().equals("io.anuke.mindustry.entities.Player")){
                Utils.messager.printMessage(Kind.ERROR, "Client invoke methods should have a first parameter of type Player.", elem);
                return;
            }
        }

        //if toAll is false, it's a 'send to one player' variant, so add the player as a parameter
        if(!toAll){
            method.addParameter(int.class, "playerClientID");
        }

        //add all other parameters to method
        for(VariableElement var : elem.getParameters()){
            method.addParameter(TypeName.get(var.asType()), var.getSimpleName().toString());
        }

        //call local method if applicable
        if(methodEntry.local){
            //concatenate parameters
            int index = 0;
            StringBuilder results = new StringBuilder();
            for(VariableElement var : elem.getParameters()){
                results.append(var.getSimpleName());
                if(index != elem.getParameters().size() - 1) results.append(", ");
                index ++;
            }

            //add the statement to call it
            method.addStatement("$N." + elem.getSimpleName() + "(" + results.toString() + ")",
                    ((TypeElement)elem.getEnclosingElement()).getQualifiedName().toString());
        }

        //start control flow to check if it's actually client/server so no netcode is called
        method.beginControlFlow("if(io.anuke.mindustry.net.Net." + (methodEntry.client ? "client" : "server")+"())");

        //add statement to create packet from pool
        method.addStatement("$1N packet = $2N.obtain($1N.class)", "io.anuke.mindustry.net.Packets.InvokePacket", "com.badlogic.gdx.utils.Pools");
        //assign buffer
        method.addStatement("packet.writeBuffer = TEMP_BUFFER");
        //rewind buffer
        method.addStatement("TEMP_BUFFER.position(0)");

        for(VariableElement var : elem.getParameters()){
            //name of parameter
            String varName = var.getSimpleName().toString();
            //name of parameter type
            String typeName = var.asType().toString();
            //captialized version of type name for writing primitives
            String capName = typeName.equals("byte") ? "" : Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

            if(Utils.isPrimitive(typeName)) { //check if it's a primitive, and if so write it
                if(typeName.equals("boolean")){ //booleans are special
                    method.addStatement("TEMP_BUFFER.put(" + varName + " ? (byte)1 : 0)");
                }else{
                    method.addStatement("TEMP_BUFFER.put" +
                            capName + "(" + varName + ")");
                }
            }else{
                //else, try and find a serializer
                ClassSerializer ser = serializers.get(typeName);

                if(ser == null){ //make sure a serializer exists!
                    Utils.messager.printMessage(Kind.ERROR, "No @WriteClass method to write class type: ", var);
                    return;
                }

                //add statement for writing it
                method.addStatement(ser.writeMethod + "(buffer, " + varName +")");
            }
        }

        //assign packet length
        method.addStatement("packet.writeLength = TEMP_BUFFER.position()");

        //send the actual packet
        if(toAll){
            //send to all players / to server
            method.addStatement("io.anuke.mindustry.net.Net.send(packet, "+
                    (methodEntry.unreliable ? "io.anuke.mindustry.net.Net.SendMode.udp" : "io.anuke.mindustry.net.Net.SendMode.tcp")+")");
        }else{
            //send to specific client from server
            method.addStatement("io.anuke.mindustry.net.Net.sendTo(playerClientID, packet, "+
                    (methodEntry.unreliable ? "io.anuke.mindustry.net.Net.SendMode.udp" : "io.anuke.mindustry.net.Net.SendMode.tcp")+")");
        }

        //end check for server/client
        method.endControlFlow();

        //add method to class, finally
        classBuilder.addMethod(method.build());
    }
}
