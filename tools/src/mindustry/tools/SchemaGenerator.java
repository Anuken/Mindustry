package mindustry.tools;

import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.body.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import java.lang.reflect.*;

public class SchemaGenerator{
    static ObjectMap<String, Fi> classNameToFile = new ObjectMap<>();
    static JavaParser parser = new JavaParser();
    static ObjectMap<Class, TypeDeclaration<?>> decls = new ObjectMap<>();
    static ObjectMap<Class<?>, Seq<Cons<Jval>>> customInjectors = new ObjectMap<>();

    static{
        registerBuiltinSchemas();
    }

    //called from ScriptMainGenerator
    public static void writeSchema(Class<?> type) throws Exception{
        if(Building.class.isAssignableFrom(type) || type.getSuperclass() == null) return;

        Jval val = Jval.newObject();

        var typeDec = getTypeDecl(type);
        if(typeDec == null){
            Log.warn("No type found for class: " + type);
            return;
        }

        if(type.getSuperclass() != null) val.put("superclass", type.getSuperclass().getCanonicalName());

        if(typeDec.getJavadoc().isPresent()){
            val.put("doc", typeDec.getJavadoc().get().toText());
        }

        for(var field : type.getFields()){
            if(Modifier.isStatic(field.getModifiers())) continue;
            Jval inner = Jval.newObject();
            inner.put("type", field.getGenericType().getTypeName());

            if(field.isAnnotationPresent(Nullable.class)) inner.put("nullable", true);

            var root = getTypeDecl(field.getDeclaringClass());
            if(root != null) root.getFieldByName(field.getName()).ifPresent(fdec -> {
                String[] docAndDefault = determineJavadocAndDefault(field, fdec, fdec.getVariables().getFirst().orElseThrow());
                if(docAndDefault[0] != null) inner.put("doc", docAndDefault[0]);
                if(docAndDefault[1] != null) inner.put("default", docAndDefault[1]);
            });

            val.put(field.getName(), inner);
        }

        //apply any registered fake/extra fields for this exact class (e.g. Block#consumes)
        for(var injector : customInjectors.get(type, Seq::new)){
            injector.get(val);
        }

        Fi dir = new Fi("build/schemas/" + type.getCanonicalName() + ".json");
        dir.writeString(val.toString(Jformat.formatted));
    }

    //registers an injector adding/overwriting fields on type's generated schema, for JSON keys with no real backing field
    public static void injectCustomFields(Class<?> type, Cons<Jval> injector){
        customInjectors.get(type, Seq::new).add(injector);
    }

    //shorthand for injecting a single fake field into a class' generated schema
    public static void injectCustomField(Class<?> type, String fieldName, Jval fieldSchema){
        injectCustomFields(type, val -> val.put(fieldName, fieldSchema));
    }

    //writes a schema for a type with no backing Java class at all
    public static void writeFakeSchema(String name, String val){
        Fi file = new Fi("build/schemas/" + name + ".json");
        file.writeString(val);
    }

    private static void registerBuiltinSchemas(){

        injectCustomField(Planet.class, "sectorSize", field("int", "Planet sector subdivisions.", false));

        writeFakeSchema("Consumes", """
        {
          "doc": "Things that the block consumes.",
          "remove": {"type": "java.lang.String[]", "doc": "Removes consumers instead of adding one: 'all', or a type name from this list, e.g. 'items'."},
          "item": {"type": "mindustry.type.Item", "doc": "Adds a single-item consumer; shorthand for consumeItem(item)."},
          "itemCharged": {"type": "mindustry.world.consumers.ConsumeItemCharged"},
          "itemFlammable": {"type": "mindustry.world.consumers.ConsumeItemFlammable"},
          "itemRadioactive": {"type": "mindustry.world.consumers.ConsumeItemRadioactive"},
          "itemExplosive": {"type": "mindustry.world.consumers.ConsumeItemExplosive"},
          "itemList": {"type": "mindustry.world.consumers.ConsumeItemList"},
          "itemExplode": {"type": "mindustry.world.consumers.ConsumeItemExplode"},
          "items": {"type": "mindustry.type.ItemStack[]", "doc": "Also accepts a single ItemStack, or a full ConsumeItems object."},
          "itemsBoost": {"type": "mindustry.type.ItemStack[]", "doc": "Like 'items', but marked as a boost input (Consume#booster)."},
          "liquidFlammable": {"type": "mindustry.world.consumers.ConsumeLiquidFlammable"},
          "liquid": {"type": "mindustry.world.consumers.ConsumeLiquid"},
          "liquids": {"type": "mindustry.type.LiquidStack[]", "doc": "Also accepts a full ConsumeLiquids object."},
          "coolant": {"type": "mindustry.world.consumers.ConsumeCoolant"},
          "liquidsBoost": {"type": "mindustry.type.LiquidStack[]", "doc": "Like 'liquids', but marked as a boost input (Consume#booster)."},
          "power": {"type": "float", "doc": "Also accepts a full ConsumePower object, e.g. for buffered power."},
          "powerBuffered": {"type": "float", "doc": "Shorthand for a buffered power consumer with this capacity."}
        }
        """);

        injectCustomField(Block.class, "consumes", field("Consumes", "Things that the block consumes.", true));

        writeFakeSchema("Research", """
        {
          "doc": "Tech tree placement for this content. Also accepts a plain string, treated as 'parent'.",
          "parent": {"type": "mindustry.ctype.UnlockableContent", "doc": "Name of the parent tech tree node; required unless 'root' is true."},
          "requirements": {"type": "mindustry.type.ItemStack[]", "doc": "Overrides the default research cost, which is this content's build cost."},
          "objectives": {"type": "mindustry.type.Objective[]", "doc": "Extra objectives to complete; items/liquids get a Produce objective automatically."},
          "planet": {"type": "mindustry.type.Planet", "doc": "Name of the planet this node belongs to; inherited from the parent otherwise."},
          "root": {"type": "boolean", "doc": "If true, this is a root node and 'parent' is not required.", "default": "false"},
          "name": {"type": "java.lang.String", "doc": "Display name for a root node; ignored for non-root nodes."},
          "requiresUnlock": {"type": "boolean", "doc": "If true, a root node isn't unlocked by default.", "default": "false"}
        }
        """);

        for(var type : Seq.with(Block.class, Liquid.class, UnitType.class, Item.class, StatusEffect.class, Planet.class, Weather.class, SectorPreset.class)){
            injectCustomField(type, "research", field("Research", "Tech tree research dependencies.", true));
        }
    }

    private static Jval field(String type, String doc, boolean nullable){
        return field(type, doc, nullable, null);
    }

    private static Jval field(String type, String doc, boolean nullable, String def){
        Jval inner = Jval.newObject();
        inner.put("type", type);
        if(nullable) inner.put("nullable", true);
        if(doc != null) inner.put("doc", doc);
        if(def != null) inner.put("default", def);
        return inner;
    }

    private static @Nullable TypeDeclaration<?> getTypeDecl(Class<?> type){
        return decls.get(type, () -> {
            if(classNameToFile.isEmpty()){
                new Fi("core/src").walk(f -> {
                    if(f.extEquals("java")){
                        classNameToFile.put(f.nameWithoutExtension(), f);
                    }
                });
            }

            var file = classNameToFile.get(type.getSimpleName());
            if(file == null && type.getEnclosingClass() != null) file = classNameToFile.get(type.getEnclosingClass().getSimpleName());

            if(file == null){
                Log.warn("Missing file for class: " + type);
                return null;
            }

            var cu = parser.parse(file.read()).getResult().orElseThrow();

            return cu.findAll(TypeDeclaration.class).stream()
            .filter(t -> t.getFullyQualifiedName().isPresent()
            ? t.getFullyQualifiedName().get().equals(type.getCanonicalName())
            : t.getNameAsString().equals(type.getSimpleName()))
            .findFirst()
            .orElse(null);
        });
    }

    private static String[] determineJavadocAndDefault(Field baseField, FieldDeclaration field, VariableDeclarator variable){
        String doc = null;
        if(variable.getComment().isPresent()){
            doc = variable.getComment().get().getContent();
        }else if(field.getJavadoc().isPresent()){
            doc = field.getJavadoc().get().toText();
        }
        if(doc != null){
            if(doc.endsWith("\n")) doc = doc.substring(0, doc.length() - 1);
        }
        var initValue = variable.getInitializer().isEmpty() ? null : variable.getInitializer().get().toString();

        if(initValue != null){
            //array init
            if(initValue.equals("{}")){
                initValue = "[]";
            }

            //special array init
            if(initValue.contains("new") && initValue.contains("[") && initValue.contains("]")){
                initValue = "[]";
            }

            //field
            if(initValue.contains(".") && !(baseField.getType().isArray())){
                var split = initValue.split("\\.");
                initValue = split[split.length - 1];
            }

            //remove f suffix
            if(variable.getTypeAsString().equals("float") && initValue.endsWith("f")){
                initValue = initValue.substring(0, initValue.length() - 1);
            }

            //remove lambdas/code
            if(initValue.contains("->") || initValue.contains("(") || initValue.contains(")")){
                initValue = null;
            }
        }
        return new String[]{doc, initValue};
    }
}