package mindustry.tools;

import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.body.*;
import mindustry.*;
import mindustry.ctype.*;
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
                var arr = Jval.newArray();
                all.<UnlockableContent>as().each(c -> arr.asArray().add(Jval.valueOf(c.name)));
                root.asObject().put(type.name(), arr);
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
            if(Modifier.isStatic(field.getModifiers())) continue;
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

    static void writePartSchema(String name, String val){
        Jval base = Jval.read(val);
        base.add("ops", field("Seq<mindustry.entities.part.DrawPart$PartProgress>", "Consecutive operations."));

        writeSchema(name, base.toString(Jformat.formatted));
    }

    static void finalizeSchemas(Seq<Class<?>> allClasses){
        writeSchema("TemplateUnitType", """
        {
          "doc": "Template for unit types.",
          "superclass": "Enum",
          "values": [%TYPES%]
        }
        """.replace("%TYPES%",  allClasses.select(UnitType.class::isAssignableFrom).toString(", ", t -> "\"" + t.getSimpleName() + "\"")));

        writePartSchema("mindustry.entities.part.DrawPart$PartProgress", """
        {
          "doc": "Base class for progress functions used to animate part transforms. The concrete operation is chosen by 'type' (see PartProgress's static methods for the full list: inv, slope, clamp, delay, sustain, shorten, compress, add, blend, mul, min, sin, absin, mod, loop, curve)."
        }
        """);

        writePartSchema("reload", """
        {
          "doc": "Reload of the weapon - 1 right after shooting, 0 when ready to fire.",
          "superclass": "PartProgress"
        }
        """);

        writePartSchema("smoothReload", """
        {
          "doc": "Reload, but smoothed out, so there is no sudden jump between 0-1.",
          "superclass": "PartProgress"
        }
        """);

        writePartSchema("warmup", """
        {
          "doc": "Weapon warmup, 0 when not firing, 1 when actively shooting. Not equivalent to heat.",
          "superclass": "PartProgress"
        }
        """);

        writePartSchema("charge", """
        {
          "doc": "Weapon charge, 0 when beginning to charge, 1 when finished.",
          "superclass": "PartProgress"
        }
        """);

        writePartSchema("recoil", """
        {
          "doc": "Weapon recoil with no curve applied.",
          "superclass": "PartProgress"
        }
        """);

        writePartSchema("heat", """
        {
          "doc": "Weapon heat, 1 when just fired, 0 when it has cooled down (duration depends on weapon).",
          "superclass": "PartProgress"
        }
        """);

        writePartSchema("life", """
        {
          "doc": "Lifetime fraction, 0 to 1. Only for missiles.",
          "superclass": "PartProgress"
        }
        """);

        writePartSchema("time", """
        {
          "doc": "Current unscaled value of Time.time.",
          "superclass": "PartProgress"
        }
        """);

        writePartSchema("inv", """
        {
          "doc": "Inverts progress (1 - progress).",
          "superclass": "PartProgress"
        }
        """);

        writePartSchema("slope", """
        {
          "doc": "Slopes progress: rises to 1 then falls back to 0.",
          "superclass": "PartProgress"
        }
        """);

        writePartSchema("clamp", """
        {
          "doc": "Clamps progress to the [0, 1] range.",
          "superclass": "PartProgress"
        }
        """);

        writePartSchema("delay", """
        {
          "doc": "Delays progress by a fixed amount.",
          "superclass": "PartProgress",
          "amount": { "type": "float", "doc": "Amount to delay progress by." }
        }
        """);

        writePartSchema("sustain", """
        {
          "doc": "Rises to and sustains progress at a plateau.",
          "superclass": "PartProgress",
          "offset": { "type": "float", "doc": "Progress offset before rising.", "default": "0" },
          "grow": { "type": "float", "doc": "Duration of the rise to the plateau.", "default": "0" },
          "sustain": { "type": "float", "doc": "Length of the sustained plateau." }
        }
        """);

        writePartSchema("shorten", """
        {
          "doc": "Shortens the progress range by a fixed amount.",
          "superclass": "PartProgress",
          "amount": { "type": "float", "doc": "Amount to shorten progress by." }
        }
        """);

        writePartSchema("compress", """
        {
          "doc": "Compresses progress into the [start, end] sub-range.",
          "superclass": "PartProgress",
          "start": { "type": "float", "doc": "Start of the compressed range." },
          "end": { "type": "float", "doc": "End of the compressed range." }
        }
        """);

        writePartSchema("add", """
        {
          "doc": "Adds a constant or another PartProgress's value to this one. Set exactly one of 'amount' or 'other'.",
          "superclass": "PartProgress",
          "amount": { "type": "float", "doc": "Constant amount to add." },
          "other": { "type": "PartProgress", "doc": "Another progress function whose value is added instead of a constant." }
        }
        """);

        writePartSchema("blend", """
        {
          "doc": "Blends between this progress and another PartProgress by a fixed amount.",
          "superclass": "PartProgress",
          "other": { "type": "PartProgress", "doc": "Progress function to blend with." },
          "amount": { "type": "float", "doc": "Blend factor toward 'other'." }
        }
        """);

        writePartSchema("mul", """
        {
          "doc": "Multiplies by a constant or another PartProgress's value. Set exactly one of 'amount' or 'other'.",
          "superclass": "PartProgress",
          "amount": { "type": "float", "doc": "Constant multiplier." },
          "other": { "type": "PartProgress", "doc": "Another progress function whose value is multiplied instead of a constant." }
        }
        """);

        writePartSchema("min", """
        {
          "doc": "Takes the minimum of this progress and another PartProgress.",
          "superclass": "PartProgress",
          "other": { "type": "PartProgress", "doc": "Progress function to compare against." }
        }
        """);

        writePartSchema("sin", """
        {
          "doc": "Applies a sine wave to progress.",
          "superclass": "PartProgress",
          "offset": { "type": "float", "doc": "Phase offset.", "default": "0" },
          "scl": { "type": "float", "doc": "Scale (frequency) of the wave." },
          "mag": { "type": "float", "doc": "Magnitude (amplitude) of the wave." }
        }
        """);

        writePartSchema("absin", """
        {
          "doc": "Applies an absolute-value sine wave to progress.",
          "superclass": "PartProgress",
          "scl": { "type": "float", "doc": "Scale (frequency) of the wave." },
          "mag": { "type": "float", "doc": "Magnitude (amplitude) of the wave." }
        }
        """);

        writePartSchema("mod", """
        {
          "doc": "Wraps progress modulo a fixed amount.",
          "superclass": "PartProgress",
          "amount": { "type": "float", "doc": "Modulus to wrap progress by." }
        }
        """);

        writePartSchema("loop", """
        {
          "doc": "Loops progress over a fixed time period.",
          "superclass": "PartProgress",
          "time": { "type": "float", "doc": "Length of one loop cycle." }
        }
        """);

        writePartSchema("curve", """
        {
          "doc": "Applies an interpolation curve to progress, or slices out a sub-range and interpolates within it. Set either 'interp', or both 'offset' and 'duration'.",
          "superclass": "PartProgress",
          "interp": { "type": "arc.util.Interp", "doc": "Interpolation curve to apply directly to progress." },
          "offset": { "type": "float", "doc": "Start of the sub-range to slice out." },
          "duration": { "type": "float", "doc": "Length of the sub-range to slice out." }
        }
        """);
    }

    private static void registerBuiltinSchemas(){
        injectCustomField(UnitType.class, "type", field("JsonUnitType", "Type of the unit that is created.", false));
        injectCustomField(UnitType.class, "template", field("TemplateUnitType", "UnitType template class.", false));
        injectCustomField(UnitType.class, "defaultController", field(AIController.class.getCanonicalName(), "Unconditional controller; always assigned, even if on the player team. This overwrites RTS AI.", false));
        injectCustomField(UnitType.class, "aiController", field(AIController.class.getCanonicalName(), "Controller used when the unit is not on the player team.", false));

        writeSchema("JsonUnitType", """
        {
          "doc": "Type of unit that is created.",
          "superclass": "Enum",
          "values": ["flying", "mech", "legs", "naval", "payload", "missile", "tank", "hover", "tether", "crawl"]
        }
        """);

        writeSchema(Interp.class.getCanonicalName(), """
        {
          "doc": "Various types of interpolation.",
          "superclass": "Enum",
          "values": [%VALUES%]
        }
        """.replace("%VALUES%",  Seq.with(Interp.class.getFields()).toString(", ", t -> "\"" + t.getName() + "\"")));

        writeSchema(Blending.class.getCanonicalName(), """
        {
          "doc": "Handles sprite blending.",
          "superclass": "Enum",
          "values": ["normal", "additive", "disabled"]
        }
        """);

        injectCustomField(Planet.class, "sectorSize", field("int", "Planet sector subdivisions.", false));

        writeSchema("Consumes", """
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

        writeSchema("Research", """
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