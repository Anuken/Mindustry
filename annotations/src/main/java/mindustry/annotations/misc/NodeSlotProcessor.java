package mindustry.annotations.misc;

import arc.struct.*;
import arc.util.*;
import com.squareup.javapoet.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;
import mindustry.annotations.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;

@SupportedAnnotationTypes("mindustry.annotations.Annotations.Slot")
public class NodeSlotProcessor extends BaseProcessor{

    @Override
    public void process(RoundEnvironment env) throws Exception{
        TypeSpec.Builder slotClass = TypeSpec.classBuilder("LogicSlotMap")
            .addModifiers(Modifier.PUBLIC);

        ObjectMap<Stype, Seq<String>> fields = new ObjectMap<>();
        for(Svar var : fields(Slot.class)){
            String type = var.mirror().toString();

            boolean overrideInput = var.annotation(Slot.class).input();
            boolean output = (type.contains("SetObj") || type.contains("SetNum") || type.contains("Runnable")) && !overrideInput;

            String objType = output ?
                                type.contains("SetNum") ? "double" :
                                type.contains("SetObj") ? ((DeclaredType)var.mirror()).getTypeArguments().get(0).toString() :
                                type : type;

            String dataType = objType.equals("double") ? "number" :
                            objType.contains("Content") ? "content" :
                            objType.equals("mindustry.gen.Building") ? "building" :
                            objType.equals("mindustry.gen.Unit") ? "unit" :
                            objType.equals("java.lang.Void") || objType.equals("java.lang.Runnable") ? "control" :
                            objType.equals("java.lang.String") ? "string" :
                            "<<invalid>>";

            if(dataType.equals("<<invalid>>")) err("Unknown logic node type: " + objType, var);

            boolean numeric = dataType.equals("number");

            String name = Strings.capitalize(var.name());

            String lambda = output ?
                "(" + var.enclosingType() + " node, " + objType + " val__) -> node." + var.name() + (objType.contains("Runnable") ? ".run()" : ".set(val__)") :
                "(" + var.enclosingType() + " node, " + objType + " val__) -> node." + var.name() + " = val__";

            //NodeSlot(String name, boolean input, DataType type, NumOutput<N> numOutput, ObjOutput<N, T> setObject)
            String constructed = Strings.format(
                "new mindustry.logic.LogicNode.NodeSlot(\"@\", @, mindustry.logic.LogicNode.DataType.@, @, @)",
                name,
                !output,
                dataType,
                numeric ? lambda : "null",
                !numeric ? lambda : "null"
            );

            fields.get(var.enclosingType(), Seq::new).add(constructed);
        }

        slotClass.addField(FieldSpec.builder(
        ParameterizedTypeName.get(ClassName.get(ObjectMap.class),
            TypeName.get(Class.class),
            TypeName.OBJECT), //screw type safety, I don't care anymore
            "map", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer("new ObjectMap<>()").build());


        CodeBlock.Builder code = CodeBlock.builder();
        fields.each((type, inits) -> code.addStatement("map.put($L.class, new mindustry.logic.LogicNode.NodeSlot[]{$L})", type.toString(), inits.toString(",")));

        slotClass.addStaticBlock(code.build());

        write(slotClass);
    }
}
