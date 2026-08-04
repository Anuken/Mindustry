package mindustry.tools;

import arc.files.*;
import arc.func.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.body.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import java.lang.reflect.*;

public class SchemaGenerator{
    static JavaParser parser = new JavaParser();
    static ObjectMap<String, Fi> classNameToFile = new ObjectMap<>();
    static ObjectMap<Class<?>, TypeDeclaration<?>> decls = new ObjectMap<>();
    static ObjectMap<Class<?>, Seq<Cons<Jval>>> customInjectors = new ObjectMap<>();
    static ObjectSet<Class<?>> generatedEnums = new ObjectSet<>();
    static Fi outputDir = new Fi("../../build/schemas/");

    static{
        registerBuiltinSchemas();
        outputDir.mkdirs();
    }

    public static void writeDefaultContent(){
        HeadlessSetup.setup();
        var root = Jval.newObject();
        var mappings = Jval.newObject();
        for(ContentType type : ContentType.all){
            var all = Vars.content.getBy(type);
            if(type.contentClass != null && all.size > 0 && all.first() instanceof UnlockableContent){
                mappings.put(type.name(), type.contentClass.getCanonicalName());
                var values = Jval.newObject();
                all.<UnlockableContent>as().each(c -> {
                    values.put(c.name, Jval.valueOf((c.getClass().isAnonymousClass() ? c.getClass().getSuperclass() : c.getClass()).getCanonicalName()));
                });
                root.asObject().put(type.name(), values);
            }
        }
        root.put("mappings", mappings);
        outputDir.child("allContent.json").writeString(root.toString(Jformat.formatted));
    }

    //called from ScriptMainGenerator
    public static void writeSchema(Class<?> type){
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
            if(Modifier.isStatic(field.getModifiers()) || field.getDeclaringClass() != type) continue;
            Jval inner = Jval.newObject();
            inner.put("type", field.getGenericType().getTypeName());

            if(field.isAnnotationPresent(Nullable.class)) inner.put("nullable", true);

            if(field.getType().isEnum()){
                writeEnumSchema(field.getType());
            }

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

        outputDir.child(type.getCanonicalName() + ".json").writeString(val.toString(Jformat.formatted));
    }

    //writes a schema for an enum type the first time it's encountered as a field value; subsequent calls are no-ops
    private static void writeEnumSchema(Class<?> enumType){
        if(!generatedEnums.add(enumType)) return;

        Jval val = Jval.newObject();
        val.put("superclass", "Enum"); //TODO: should this be java.lang.Enum?

        var typeDec = getTypeDecl(enumType);
        if(typeDec != null && typeDec.getJavadoc().isPresent()){
            val.put("doc", typeDec.getJavadoc().get().toText());
        }

        Jval values = Jval.newArray();
        for(var constant : enumType.getEnumConstants()){
            values.add(((Enum<?>)constant).name());
        }
        val.put("values", values);

        outputDir.child(enumType.getCanonicalName() + ".json").writeString(val.toString(Jformat.formatted));
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
    public static void writeSchema(String name, String val){
        Fi file = outputDir.child(name + ".json");
        file.writeString(val);
    }

    static void finalizeSchemas(Seq<Class<?>> allClasses){
        writeSchema(UnlockableContent.class);
        writeSchema(MappableContent.class);
        writeSchema(Effect.class);

        writeSchema("TemplateUnitType", """
        {
          "doc": "Template for unit types.",
          "superclass": "Enum",
          "values": [%TYPES%]
        }
        """.replace("%TYPES%",  allClasses.select(UnitType.class::isAssignableFrom).toString(", ", t -> "\"" + t.getSimpleName() + "\"")));

        writeSchema(Interp.class.getCanonicalName(), """
        {
          "doc": "Various types of interpolation.",
          "superclass": "Enum",
          "values": [%VALUES%]
        }
        """.replace("%VALUES%",  Seq.with(Interp.class.getFields()).toString(", ", t -> "\"" + t.getName() + "\"")));

        new Fi("../../tools/extra-schemas").copyFilesTo(outputDir);
    }

    private static void registerBuiltinSchemas(){
        injectCustomField(UnitType.class, "type", field("JsonUnitType", "Type of the unit that is created.", false));
        injectCustomField(UnitType.class, "template", field("TemplateUnitType", "UnitType template class.", false));
        injectCustomField(UnitType.class, "defaultController", field(AIController.class.getCanonicalName(), "Unconditional controller; always assigned, even if on the player team. This overwrites RTS AI.", false));
        injectCustomField(UnitType.class, "aiController", field(AIController.class.getCanonicalName(), "Controller used when the unit is not on the player team.", false));

        injectCustomField(Planet.class, "sectorSize", field("int", "Planet sector subdivisions.", false));

        injectCustomField(Block.class, "consumes", field("Consumes", "Things that the block consumes.", true));

        for(var type : Seq.with(Block.class, Liquid.class, UnitType.class, Item.class, StatusEffect.class, Planet.class, Weather.class, SectorPreset.class)){
            injectCustomField(type, "research", field("Research", "Tech tree research dependencies.", true));
        }
    }

    private static Jval field(String type, String doc){
        return field(type, doc, false, null);
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
                new Fi("../../core/src").walk(f -> {
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