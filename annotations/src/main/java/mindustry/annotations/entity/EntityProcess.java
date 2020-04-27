package mindustry.annotations.entity;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.pooling.Pool.*;
import arc.util.pooling.*;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.*;
import com.sun.source.tree.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;
import mindustry.annotations.util.*;
import mindustry.annotations.util.TypeIOResolver.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.lang.annotation.*;

@SupportedAnnotationTypes({
"mindustry.annotations.Annotations.EntityDef",
"mindustry.annotations.Annotations.GroupDef",
"mindustry.annotations.Annotations.EntityInterface",
"mindustry.annotations.Annotations.BaseComponent",
"mindustry.annotations.Annotations.TypeIOHandler"
})
public class EntityProcess extends BaseProcessor{
    Array<EntityDefinition> definitions = new Array<>();
    Array<GroupDefinition> groupDefs = new Array<>();
    Array<Stype> baseComponents;
    ObjectMap<String, Stype> componentNames = new ObjectMap<>();
    ObjectMap<Stype, Array<Stype>> componentDependencies = new ObjectMap<>();
    ObjectMap<Selement, Array<Stype>> defComponents = new ObjectMap<>();
    ObjectMap<Svar, String> varInitializers = new ObjectMap<>();
    ObjectMap<Smethod, String> methodBlocks = new ObjectMap<>();
    ObjectSet<String> imports = new ObjectSet<>();
    Array<Selement> allGroups = new Array<>();
    Array<Selement> allDefs = new Array<>();
    Array<Stype> allInterfaces = new Array<>();
    ClassSerializer serializer;

    {
        rounds = 3;
    }

    @Override
    public void process(RoundEnvironment env) throws Exception{
        allGroups.addAll(elements(GroupDef.class));
        allDefs.addAll(elements(EntityDef.class));
        allInterfaces.addAll(types(EntityInterface.class));

        //round 1: generate component interfaces
        if(round == 1){
            serializer = TypeIOResolver.resolve(this);
            baseComponents = types(BaseComponent.class);
            Array<Stype> allComponents = types(Component.class);

            //store code
            for(Stype component : allComponents){
                for(Svar f : component.fields()){
                    VariableTree tree = f.tree();

                    //add initializer if it exists
                    if(tree.getInitializer() != null){
                        String init = tree.getInitializer().toString();
                        varInitializers.put(f, init);
                    }
                }

                for(Smethod elem : component.methods()){
                    if(elem.is(Modifier.ABSTRACT) || elem.is(Modifier.NATIVE)) continue;
                    //get all statements in the method, store them
                    methodBlocks.put(elem, elem.tree().getBody().toString());
                }
            }

            //store components
            for(Stype type : allComponents){
                componentNames.put(type.name(), type);
            }

            //add component imports
            for(Stype comp : allComponents){
                imports.addAll(getImports(comp.e));
            }

            //create component interfaces
            for(Stype component : allComponents){
                TypeSpec.Builder inter = TypeSpec.interfaceBuilder(interfaceName(component))
                .addModifiers(Modifier.PUBLIC).addAnnotation(EntityInterface.class);

                inter.addJavadoc("Interface for {@link $L}", component.fullName());

                //implement extra interfaces these components may have, e.g. position
                for(Stype extraInterface : component.interfaces().select(i -> !isCompInterface(i))){
                    //javapoet completely chokes on this if I add `addSuperInterface` or create the type name with TypeName.get
                    inter.superinterfaces.add(tname(extraInterface.fullName()));
                }

                //implement super interfaces
                Array<Stype> depends = getDependencies(component);
                for(Stype type : depends){
                    inter.addSuperinterface(ClassName.get(packageName, interfaceName(type)));
                }

                ObjectSet<String> signatures = new ObjectSet<>();

                //add utility methods to interface
                for(Smethod method : component.methods()){
                    //skip private methods, those are for internal use.
                    if(method.isAny(Modifier.PRIVATE, Modifier.STATIC)) continue;

                    //keep track of signatures used to prevent dupes
                    signatures.add(method.e.toString());

                    inter.addMethod(MethodSpec.methodBuilder(method.name())
                    .addJavadoc(method.doc() == null ? "" : method.doc())
                    .addExceptions(method.thrownt())
                    .addTypeVariables(method.typeVariables().map(TypeVariableName::get))
                    .returns(method.ret().toString().equals("void") ? TypeName.VOID : method.retn())
                    .addParameters(method.params().map(v -> ParameterSpec.builder(v.tname(), v.name())
                    .build())).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).build());
                }

                for(Svar field : component.fields().select(e -> !e.is(Modifier.STATIC) && !e.is(Modifier.PRIVATE) && !e.has(Import.class))){
                    String cname = field.name();

                    //getter
                    if(!signatures.contains(cname + "()")){
                        inter.addMethod(MethodSpec.methodBuilder(cname).addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                        .addAnnotations(Array.with(field.annotations()).select(a -> a.toString().contains("Null")).map(AnnotationSpec::get))
                        .addJavadoc(field.doc() == null ? "" : field.doc())
                        .returns(field.tname()).build());
                    }

                    //setter
                    if(!field.is(Modifier.FINAL) && !signatures.contains(cname + "(" + field.mirror().toString() + ")") &&
                    !field.annotations().contains(f -> f.toString().equals("@mindustry.annotations.Annotations.ReadOnly"))){
                        inter.addMethod(MethodSpec.methodBuilder(cname).addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                        .addJavadoc(field.doc() == null ? "" : field.doc())
                        .addParameter(ParameterSpec.builder(field.tname(), field.name())
                        .addAnnotations(Array.with(field.annotations())
                        .select(a -> a.toString().contains("Null")).map(AnnotationSpec::get)).build()).build());
                    }
                }

                write(inter);

                //LOGGING

                Log.debug("&gGenerating interface for " + component.name());

                for(TypeName tn : inter.superinterfaces){
                    Log.debug("&g> &lbextends {0}", simpleName(tn.toString()));
                }

                //log methods generated
                for(MethodSpec spec : inter.methodSpecs){
                    Log.debug("&g> > &c{0} {1}({2})", simpleName(spec.returnType.toString()), spec.name, Array.with(spec.parameters).toString(", ", p -> simpleName(p.type.toString()) + " " + p.name));
                }

                Log.debug("");
            }

        }else if(round == 2){ //round 2: get component classes and generate interfaces for them

            //parse groups
            for(Selement<?> group : allGroups){
                GroupDef an = group.annotation(GroupDef.class);
                Array<Stype> types = types(an, GroupDef::value).map(this::interfaceToComp);
                Array<Stype> collides = types(an, GroupDef::collide);
                groupDefs.add(new GroupDefinition(group.name().startsWith("g") ? group.name().substring(1) : group.name(),
                    ClassName.bestGuess(packageName + "." + interfaceName(types.first())), types, an.spatial(), an.mapping(), collides));
            }

            ObjectMap<String, Selement> usedNames = new ObjectMap<>();
            ObjectMap<Selement, ObjectSet<String>> extraNames = new ObjectMap<>();

            //look at each definition
            for(Selement<?> type : allDefs){
                EntityDef ann = type.annotation(EntityDef.class);
                boolean isFinal = ann.isFinal();

                if(type.isType() && (!type.name().endsWith("Def") && !type.name().endsWith("Comp"))){
                    err("All entity def names must end with 'Def'/'Comp'", type.e);
                }

                String name = type.isType() ?
                    type.name().replace("Def", "Entity").replace("Comp", "Entity") :
                    createName(type);

                //skip double classes
                if(usedNames.containsKey(name)){
                    extraNames.getOr(usedNames.get(name), ObjectSet::new).add(type.name());
                    continue;
                }

                usedNames.put(name, type);
                extraNames.getOr(type, ObjectSet::new).add(name);
                if(!type.isType()){
                    extraNames.getOr(type, ObjectSet::new).add(type.name());
                }

                TypeSpec.Builder builder = TypeSpec.classBuilder(name).addModifiers(Modifier.PUBLIC);
                if(isFinal) builder.addModifiers(Modifier.FINAL);

                Array<Stype> components = allComponents(type);
                Array<GroupDefinition> groups = groupDefs.select(g -> (!g.components.isEmpty() && !g.components.contains(s -> !components.contains(s))) || g.manualInclusions.contains(type));
                ObjectMap<String, Array<Smethod>> methods = new ObjectMap<>();
                ObjectMap<FieldSpec, Svar> specVariables = new ObjectMap<>();
                ObjectSet<String> usedFields = new ObjectSet<>();

                //add serialize() boolean
                builder.addMethod(MethodSpec.methodBuilder("serialize").addModifiers(Modifier.PUBLIC, Modifier.FINAL).returns(boolean.class).addStatement("return " + ann.serialize()).build());

                //add all components
                for(Stype comp : components){

                    //write fields to the class; ignoring transient ones
                    Array<Svar> fields = comp.fields().select(f -> !f.has(Import.class));
                    for(Svar f : fields){
                        if(!usedFields.add(f.name())){
                            err("Field '" + f.name() + "' of component '" + comp.name() + "' re-defines a field in entity '" + type.name() + "'");
                            continue;
                        }

                        FieldSpec.Builder fbuilder = FieldSpec.builder(f.tname(), f.name());
                        //keep statics/finals
                        if(f.is(Modifier.STATIC)){
                            fbuilder.addModifiers(Modifier.STATIC);
                            if(f.is(Modifier.FINAL)) fbuilder.addModifiers(Modifier.FINAL);
                        }
                        //add transient modifier for serialization
                        if(f.is(Modifier.TRANSIENT)){
                            fbuilder.addModifiers(Modifier.TRANSIENT);
                        }
                        //add initializer if it exists
                        if(varInitializers.containsKey(f)){
                            fbuilder.initializer(varInitializers.get(f));
                        }

                        if(!isFinal) fbuilder.addModifiers(Modifier.PROTECTED);
                        fbuilder.addAnnotations(f.annotations().map(AnnotationSpec::get));
                        builder.addField(fbuilder.build());
                        specVariables.put(builder.fieldSpecs.get(builder.fieldSpecs.size() - 1), f);
                    }

                    //get all utility methods from components
                    for(Smethod elem : comp.methods()){
                        methods.getOr(elem.toString(), Array::new).add(elem);
                    }
                }

                //override toString method
                builder.addMethod(MethodSpec.methodBuilder("toString")
                    .addAnnotation(Override.class)
                    .returns(String.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return $S + $L", name + "#", "id").build());

                EntityIO io = new EntityIO(type.name(), builder, serializer, rootDirectory.child("annotations/src/main/resources/revisions").child(name));

                //add all methods from components
                for(ObjectMap.Entry<String, Array<Smethod>> entry : methods){
                    if(entry.value.contains(m -> m.has(Replace.class))){
                        //check replacements
                        if(entry.value.count(m -> m.has(Replace.class)) > 1){
                            err("Type " + type + " has multiple components replacing method " + entry.key + ".");
                        }
                        Smethod base = entry.value.find(m -> m.has(Replace.class));
                        entry.value.clear();
                        entry.value.add(base);
                    }

                    //check multi return
                    if(entry.value.count(m -> !m.isAny(Modifier.NATIVE, Modifier.ABSTRACT) && !m.isVoid()) > 1){
                        err("Type " + type + " has multiple components implementing non-void method " + entry.key + ".");
                    }

                    entry.value.sort(Structs.comps(Structs.comparingFloat(m -> m.has(MethodPriority.class) ? m.annotation(MethodPriority.class).value() : 0), Structs.comparing(Selement::name)));

                    //representative method
                    Smethod first = entry.value.first();

                    //skip internal impl
                    if(first.has(InternalImpl.class)){
                        continue;
                    }

                    //build method using same params/returns
                    MethodSpec.Builder mbuilder = MethodSpec.methodBuilder(first.name()).addModifiers(first.is(Modifier.PRIVATE) ? Modifier.PRIVATE : Modifier.PUBLIC);
                    if(isFinal || entry.value.contains(s -> s.has(Final.class))) mbuilder.addModifiers(Modifier.FINAL);
                    if(first.is(Modifier.STATIC)) mbuilder.addModifiers(Modifier.STATIC);
                    mbuilder.addTypeVariables(first.typeVariables().map(TypeVariableName::get));
                    mbuilder.returns(first.retn());
                    mbuilder.addExceptions(first.thrownt());

                    for(Svar var : first.params()){
                        mbuilder.addParameter(var.tname(), var.name());
                    }

                    //only write the block if it's a void method with several entries
                    boolean writeBlock = first.ret().toString().equals("void") && entry.value.size > 1;

                    if((entry.value.first().is(Modifier.ABSTRACT) || entry.value.first().is(Modifier.NATIVE)) && entry.value.size == 1 && !entry.value.first().has(InternalImpl.class)){
                        err(entry.value.first().up().getSimpleName() + "#" + entry.value.first() + " is an abstract method and must be implemented in some component", type);
                    }

                    //SPECIAL CASE: inject group add/remove code
                    if(first.name().equals("add") || first.name().equals("remove")){
                        mbuilder.addStatement("if(added == $L) return", first.name().equals("add"));

                        for(GroupDefinition def : groups){
                            //remove/add from each group, assume imported
                            mbuilder.addStatement("Groups.$L.$L(this)", def.name, first.name());
                        }
                    }

                    //SPECIAL CASE: I/O code
                    //note that serialization is generated even for non-serializing entities for manual usage
                    if((first.name().equals("read") || first.name().equals("write")) && ann.genio()){
                        io.write(mbuilder, first.name().equals("write"));
                    }

                    for(Smethod elem : entry.value){
                        if(elem.is(Modifier.ABSTRACT) || elem.is(Modifier.NATIVE) || !methodBlocks.containsKey(elem)) continue;

                        //get all statements in the method, copy them over
                        String str = methodBlocks.get(elem);
                        //name for code blocks in the methods
                        String blockName = elem.up().getSimpleName().toString().toLowerCase().replace("comp", "");

                        //skip empty blocks
                        if(str.replace("{", "").replace("\n", "").replace("}", "").replace("\t", "").replace(" ", "").isEmpty()){
                            continue;
                        }

                        //wrap scope to prevent variable leakage
                        if(writeBlock){
                            //replace return; with block break
                            str = str.replace("return;", "break " + blockName + ";");
                            mbuilder.addCode(blockName + ": {\n");
                        }

                        //trim block
                        str = str.substring(2, str.length() - 1);

                        //make sure to remove braces here
                        mbuilder.addCode(str);

                        //end scope
                        if(writeBlock) mbuilder.addCode("}\n");
                    }

                    //add free code to remove methods - always at the end
                    //this only gets called next frame.
                    if(first.name().equals("remove") && ann.pooled()){
                        mbuilder.addStatement("$T.app.post(() -> $T.free(this))", Core.class, Pools.class);
                    }

                    builder.addMethod(mbuilder.build());
                }

                //add pool reset method and implement Poolable
                if(ann.pooled()){
                    builder.addSuperinterface(Poolable.class);
                    //implement reset()
                    MethodSpec.Builder resetBuilder = MethodSpec.methodBuilder("reset").addModifiers(Modifier.PUBLIC);
                    for(FieldSpec spec : builder.fieldSpecs){
                        Svar variable = specVariables.get(spec);
                        if(variable.isAny(Modifier.STATIC, Modifier.FINAL)) continue;

                        if(spec.type.isPrimitive()){
                            //set to primitive default
                            resetBuilder.addStatement("$L = $L", spec.name, varInitializers.containsKey(variable) ? varInitializers.get(variable) : getDefault(spec.type.toString()));
                        }else{
                            //set to default null
                            if(!varInitializers.containsKey(variable)){
                                resetBuilder.addStatement("$L = null", spec.name);
                            } //else... TODO reset if poolable
                        }
                    }

                    builder.addMethod(resetBuilder.build());
                }

                //make constructor private
                builder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PROTECTED).build());

                //add create() method
                builder.addMethod(MethodSpec.methodBuilder("create").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(tname(packageName + "." + name))
                .addStatement(ann.pooled() ? "return Pools.obtain($L.class, " +name +"::new)" : "return new $L()", name).build());

                definitions.add(new EntityDefinition(packageName + "." + name, builder, type, components, groups));
            }

            //generate groups
            TypeSpec.Builder groupsBuilder = TypeSpec.classBuilder("Groups").addModifiers(Modifier.PUBLIC);
            MethodSpec.Builder groupInit = MethodSpec.methodBuilder("init").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
            for(GroupDefinition group : groupDefs){
                //class names for interface/group
                ClassName itype =  group.baseType;
                ClassName groupc = ClassName.bestGuess("mindustry.entities.EntityGroup");

                //add field...
                groupsBuilder.addField(ParameterizedTypeName.get(
                    ClassName.bestGuess("mindustry.entities.EntityGroup"), itype), group.name, Modifier.PUBLIC, Modifier.STATIC);

                groupInit.addStatement("$L = new $T<>($L.class, $L, $L)", group.name, groupc, itype, group.spatial, group.mapping);
            }

            //write the groups
            groupsBuilder.addMethod(groupInit.build());

            //add method for resizing all necessary groups
            MethodSpec.Builder groupResize = MethodSpec.methodBuilder("resize")
                .addParameter(TypeName.FLOAT, "x").addParameter(TypeName.FLOAT, "y").addParameter(TypeName.FLOAT, "w").addParameter(TypeName.FLOAT, "h")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

            MethodSpec.Builder groupUpdate = MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

            //method resize
            for(GroupDefinition group : groupDefs){
                if(group.spatial){
                    groupResize.addStatement("$L.resize(x, y, w, h)", group.name);
                    groupUpdate.addStatement("$L.updatePhysics()", group.name);
                }
            }

            groupUpdate.addStatement("all.update()");

            for(GroupDefinition group : groupDefs){
                for(Stype collide : group.collides){
                    groupUpdate.addStatement("$L.collide($L)", group.name, collide.name().startsWith("g") ? collide.name().substring(1) : collide.name());
                }
            }

            groupsBuilder.addMethod(groupResize.build());
            groupsBuilder.addMethod(groupUpdate.build());

            write(groupsBuilder);

            //load map of sync IDs
            StringMap map = new StringMap();
            Fi idProps = rootDirectory.child("annotations/src/main/resources/classids.properties");
            if(!idProps.exists()) idProps.writeString("");
            PropertiesUtils.load(map, idProps.reader());
            //next ID to be used in generation
            Integer max = map.values().toArray().map(Integer::parseInt).max(i -> i);
            int maxID = max == null ? 0 : max + 1;

            //assign IDs
            definitions.sort(Structs.comparing(t -> t.base.toString()));
            for(EntityDefinition def : definitions){
                String name = def.base.fullName();
                if(map.containsKey(name)){
                    def.classID = map.getInt(name);
                }else{
                    def.classID = maxID++;
                    map.put(name, def.classID + "");
                }
            }

            OrderedMap<String, String> res = new OrderedMap<>();
            res.putAll(map);
            res.orderedKeys().sort();

            //write assigned IDs
            PropertiesUtils.store(res, idProps.writer(false), "Maps entity names to IDs. Autogenerated.");

            //build mapping class for sync IDs
            TypeSpec.Builder idBuilder = TypeSpec.classBuilder("EntityMapping").addModifiers(Modifier.PUBLIC)
            .addField(FieldSpec.builder(TypeName.get(Prov[].class), "idMap", Modifier.PRIVATE, Modifier.STATIC).initializer("new Prov[256]").build())
            .addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(ObjectMap.class),
                tname(String.class), tname(Prov.class)),
                "nameMap", Modifier.PRIVATE, Modifier.STATIC).initializer("new ObjectMap<>()").build())
            .addMethod(MethodSpec.methodBuilder("map").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.get(Prov.class)).addParameter(int.class, "id").addStatement("return idMap[id]").build())
            .addMethod(MethodSpec.methodBuilder("map").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.get(Prov.class)).addParameter(String.class, "name").addStatement("return nameMap.get(name)").build());

            CodeBlock.Builder idStore = CodeBlock.builder();

            //store the mappings
            for(EntityDefinition def : definitions){
                //store mapping
                idStore.addStatement("idMap[$L] = $L::new", def.classID, def.name);
                extraNames.get(def.base).each(extra -> {
                    idStore.addStatement("nameMap.put($S, $L::new)", extra, def.name);
                });

                //return mapping
                def.builder.addMethod(MethodSpec.methodBuilder("classId").addAnnotation(Override.class)
                    .returns(int.class).addModifiers(Modifier.PUBLIC).addStatement("return " + def.classID).build());
            }


            idBuilder.addStaticBlock(idStore.build());

            write(idBuilder);
        }else{
            //round 3: generate actual classes and implement interfaces

            //implement each definition
            for(EntityDefinition def : definitions){

                //get interface for each component
                for(Stype comp : def.components){

                    //implement the interface
                    Stype inter = allInterfaces.find(i -> i.name().equals(interfaceName(comp)));
                    if(inter == null){
                        err("Failed to generate interface for", comp);
                        return;
                    }

                    def.builder.addSuperinterface(inter.tname());

                    //generate getter/setter for each method
                    for(Smethod method : inter.methods()){
                        String var = method.name();
                        FieldSpec field = Array.with(def.builder.fieldSpecs).find(f -> f.name.equals(var));
                        //make sure it's a real variable AND that the component doesn't already implement it with custom logic
                        if(field == null || comp.methods().contains(m -> m.simpleString().equals(method.simpleString()))) continue;

                        //getter
                        if(!method.isVoid()){
                            def.builder.addMethod(MethodSpec.overriding(method.e).addStatement("return " + var).addModifiers(Modifier.FINAL).build());
                        }

                        //setter
                        if(method.isVoid() && !Array.with(field.annotations).contains(f -> f.type.toString().equals("@mindustry.annotations.Annotations.ReadOnly"))){
                            def.builder.addMethod(MethodSpec.overriding(method.e).addModifiers(Modifier.FINAL).addStatement("this." + var + " = " + var).build());
                        }
                    }
                }

                write(def.builder, imports.asArray());
            }

            //store nulls
            TypeSpec.Builder nullsBuilder = TypeSpec.classBuilder("Nulls").addModifiers(Modifier.PUBLIC).addModifiers(Modifier.FINAL);

            //create mock types of all components
            for(Stype interf : allInterfaces){
                //indirect interfaces to implement methods for
                Array<Stype> dependencies = interf.allInterfaces().and(interf);
                Array<Smethod> methods = dependencies.flatMap(Stype::methods);
                methods.sortComparing(Object::toString);

                //used method signatures
                ObjectSet<String> signatures = new ObjectSet<>();

                //create null builder
                String baseName = interf.name().substring(0, interf.name().length() - 1);
                String className = "Null" + baseName;
                TypeSpec.Builder nullBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.FINAL);

                nullBuilder.addSuperinterface(interf.tname());

                for(Smethod method : methods){
                    String signature = method.toString();
                    if(signatures.contains(signature)) continue;

                    Stype compType = interfaceToComp(method.type());
                    MethodSpec.Builder builder = MethodSpec.overriding(method.e).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                    if(!method.isVoid()){
                        if(method.name().equals("isNull")){
                            builder.addStatement("return true");
                        }else if(method.name().equals("id")){
                                builder.addStatement("return -1");
                        }else{
                            Svar variable = compType == null || method.params().size > 0 ? null : compType.fields().find(v -> v.name().equals(method.name()));
                            if(variable == null || !varInitializers.containsKey(variable)){
                                builder.addStatement("return " + getDefault(method.ret().toString()));
                            }else{
                                String init = varInitializers.get(variable);
                                builder.addStatement("return " + (init.equals("{}") ? "new " + variable.mirror().toString() : "") + init);
                            }
                        }
                    }

                    nullBuilder.addMethod(builder.build());

                    signatures.add(signature);
                }

                nullsBuilder.addField(FieldSpec.builder(interf.cname(), Strings.camelize(baseName)).initializer("new " + className + "()").addModifiers(Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC).build());

                write(nullBuilder);
            }

            write(nullsBuilder);
        }
    }

    Array<String> getImports(Element elem){
        return Array.with(trees.getPath(elem).getCompilationUnit().getImports()).map(Object::toString);
    }

    /** @return interface for a component type */
    String interfaceName(Stype comp){
        String suffix = "Comp";
        if(!comp.name().endsWith(suffix)){
            err("All components must have names that end with 'Comp'", comp.e);
        }
        return comp.name().substring(0, comp.name().length() - suffix.length()) + "c";
    }

    /** @return all components that a entity def has */
    Array<Stype> allComponents(Selement<?> type){
        if(!defComponents.containsKey(type)){
            //get base defs
            Array<Stype> components = types(type.annotation(EntityDef.class), EntityDef::value).map(this::interfaceToComp);
            ObjectSet<Stype> out = new ObjectSet<>();
            for(Stype comp : components){
                //get dependencies for each def, add them
                out.add(comp);
                out.addAll(getDependencies(comp));
            }

            defComponents.put(type, out.asArray());
        }

        return defComponents.get(type);
    }

    Array<Stype> getDependencies(Stype component){
        if(!componentDependencies.containsKey(component)){
            ObjectSet<Stype> out = new ObjectSet<>();
            //add base component interfaces
            out.addAll(component.interfaces().select(this::isCompInterface).map(this::interfaceToComp));
            //remove self interface
            out.remove(component);

            //out now contains the base dependencies; finish constructing the tree
            ObjectSet<Stype> result = new ObjectSet<>();
            for(Stype type : out){
                result.add(type);
                result.addAll(getDependencies(type));
            }

            if(component.annotation(BaseComponent.class) == null){
                result.addAll(baseComponents);
            }

            //remove it again just in case
            out.remove(component);
            componentDependencies.put(component, result.asArray());
        }

        return componentDependencies.get(component);
    }

    boolean isCompInterface(Stype type){
        return interfaceToComp(type) != null;
    }

    Stype interfaceToComp(Stype type){
        String name = type.name().substring(0, type.name().length() - 1) + "Comp";
        return componentNames.get(name);
    }

    String createName(Selement<?> elem){
        Array<Stype> comps = types(elem.annotation(EntityDef.class), EntityDef::value).map(this::interfaceToComp);;
        comps.sortComparing(Selement::name);
        return comps.toString("", s -> s.name().replace("Comp", "")) + "Entity";
    }

    boolean isComponent(Stype type){
        return type.annotation(Component.class) != null;
    }

    <T extends Annotation> Array<Stype> types(T t, Cons<T> consumer){
        try{
            consumer.get(t);
        }catch(MirroredTypesException e){
            return Array.with(e.getTypeMirrors()).map(Stype::of);
        }
        throw new IllegalArgumentException("Missing types.");
    }

    class GroupDefinition{
        final String name;
        final ClassName baseType;
        final Array<Stype> components;
        final Array<Stype> collides;
        final boolean spatial, mapping;
        final ObjectSet<Selement> manualInclusions = new ObjectSet<>();

        public GroupDefinition(String name, ClassName bestType, Array<Stype> components, boolean spatial, boolean mapping, Array<Stype> collides){
            this.baseType = bestType;
            this.components = components;
            this.name = name;
            this.spatial = spatial;
            this.mapping = mapping;
            this.collides = collides;
        }

        @Override
        public String toString(){
            return name;
        }
    }

    class EntityDefinition{
        final Array<GroupDefinition> groups;
        final Array<Stype> components;
        final TypeSpec.Builder builder;
        final Selement base;
        final String name;
        int classID;

        public EntityDefinition(String name, Builder builder, Selement base, Array<Stype> components, Array<GroupDefinition> groups){
            this.builder = builder;
            this.name = name;
            this.base = base;
            this.groups = groups;
            this.components = components;
        }

        @Override
        public String toString(){
            return "Definition{" +
            "groups=" + groups +
            "components=" + components +
            ", base=" + base +
            '}';
        }
    }
}
