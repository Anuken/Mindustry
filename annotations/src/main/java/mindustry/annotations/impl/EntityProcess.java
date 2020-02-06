package mindustry.annotations.impl;

import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.*;
import com.sun.source.tree.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;
import mindustry.annotations.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.lang.annotation.*;

@SupportedAnnotationTypes({
"mindustry.annotations.Annotations.EntityDef",
"mindustry.annotations.Annotations.EntityInterface",
"mindustry.annotations.Annotations.BaseComponent"
})
public class EntityProcess extends BaseProcessor{
    Array<EntityDefinition> definitions = new Array<>();
    Array<GroupDefinition> groupDefs = new Array<>();
    Array<Stype> baseComponents;
    ObjectMap<String, Stype> componentNames = new ObjectMap<>();
    ObjectMap<Stype, Array<Stype>> componentDependencies = new ObjectMap<>();
    ObjectMap<Stype, Array<Stype>> defComponents = new ObjectMap<>();
    ObjectSet<String> imports = new ObjectSet<>();

    {
        rounds = 2;
    }

    @Override
    public void process(RoundEnvironment env) throws Exception{

        //round 1: get component classes and generate interfaces for them
        if(round == 1){
            baseComponents = types(BaseComponent.class);
            Array<Smethod> allGroups = methods(GroupDef.class);
            Array<Stype> allDefs = types(EntityDef.class);
            Array<Stype> allComponents = types(Component.class);

            //store components
            for(Stype type : allComponents){
                componentNames.put(type.name(), type);
            }

            //add component imports
            for(Stype comp : allComponents){
                imports.addAll(getImports(comp.e));
            }

            //parse groups
            for(Smethod group : allGroups){
                GroupDef an = group.annotation(GroupDef.class);
                groupDefs.add(new GroupDefinition(group.name(), types(an, GroupDef::value), an));
            }

            //create component interfaces
            for(Stype component : allComponents){
                TypeSpec.Builder inter = TypeSpec.interfaceBuilder(interfaceName(component)).addModifiers(Modifier.PUBLIC).addAnnotation(EntityInterface.class);

                //implement extra interfaces these components may have, e.g. position
                for(Stype extraInterface : component.interfaces().select(i -> !isCompInterface(i))){
                    inter.addSuperinterface(extraInterface.mirror());
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
                    if(method.is(Modifier.PRIVATE)) continue;

                    //keep track of signatures used to prevent dupes
                    signatures.add(method.e.toString());

                    inter.addMethod(MethodSpec.methodBuilder(method.name())
                    .addExceptions(method.thrownt())
                    .addTypeVariables(method.typeVariables().map(TypeVariableName::get))
                    .returns(method.ret().toString().equals("void") ? TypeName.VOID : method.retn())
                    .addParameters(method.params().map(v -> ParameterSpec.builder(v.tname(), v.name())
                    .build())).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).build());
                }

                for(Svar field : component.fields().select(e -> !e.is(Modifier.STATIC) && !e.is(Modifier.PRIVATE) && !e.is(Modifier.TRANSIENT))){
                    String cname = field.name();

                    //getter
                    if(!signatures.contains(cname + "()")){
                        inter.addMethod(MethodSpec.methodBuilder(cname).addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                        .addAnnotations(Array.with(field.annotations()).select(a -> a.toString().contains("Null")).map(AnnotationSpec::get))
                        .returns(field.tname()).build());
                    }

                    //setter
                    if(!field.is(Modifier.FINAL) && !signatures.contains(cname + "(" + field.mirror().toString() + ")") &&
                        !field.annotations().contains(f -> f.toString().equals("@mindustry.annotations.Annotations.ReadOnly"))){
                            inter.addMethod(MethodSpec.methodBuilder(cname).addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                            .addParameter(ParameterSpec.builder(field.tname(), field.name())
                            .addAnnotations(Array.with(field.annotations())
                            .select(a -> a.toString().contains("Null")).map(AnnotationSpec::get)).build()).build());
                    }
                }

                write(inter);

                //LOGGING

                Log.info("&gGenerating interface for " + component.name());

                for(TypeName tn : inter.superinterfaces){
                    Log.info("&g> &lbextends {0}", simpleName(tn.toString()));
                }

                //log methods generated
                for(MethodSpec spec : inter.methodSpecs){
                    Log.info("&g> > &c{0} {1}({2})", simpleName(spec.returnType.toString()), spec.name, Array.with(spec.parameters).toString(", ", p -> simpleName(p.type.toString()) + " " + p.name));
                }

                Log.info("");
            }

            //look at each definition
            for(Stype type : allDefs){
                boolean isFinal = type.annotation(EntityDef.class).isFinal();
                if(!type.name().endsWith("Def")){
                    err("All entity def names must end with 'Def'", type.e);
                }
                String name = type.name().replace("Def", "Entity"); //TODO remove extra underscore
                TypeSpec.Builder builder = TypeSpec.classBuilder(name).addModifiers(Modifier.PUBLIC);
                if(isFinal) builder.addModifiers(Modifier.FINAL);

                Array<Stype> components = allComponents(type);
                Array<GroupDefinition> groups = groupDefs.select(g -> !g.components.contains(s -> !components.contains(s)));
                ObjectMap<String, Array<Smethod>> methods = new ObjectMap<>();

                //add all components
                for(Stype comp : components){

                    //write fields to the class; ignoring transient ones
                    Array<Svar> fields = comp.fields().select(f -> !f.is(Modifier.TRANSIENT));
                    for(Svar f : fields){
                        VariableTree tree = f.tree();
                        FieldSpec.Builder fbuilder = FieldSpec.builder(f.tname(), f.name());
                        //keep statics/finals
                        if(f.is(Modifier.STATIC)){
                            fbuilder.addModifiers(Modifier.STATIC);
                            if(f.is(Modifier.FINAL)) fbuilder.addModifiers(Modifier.FINAL);
                        }
                        //add initializer if it exists
                        if(tree.getInitializer() != null){
                            fbuilder.initializer(tree.getInitializer().toString());
                        }

                        if(!isFinal) fbuilder.addModifiers(Modifier.PROTECTED);
                        fbuilder.addAnnotations(f.annotations().map(AnnotationSpec::get));
                        builder.addField(fbuilder.build());
                    }

                    //get all utility methods from components
                    for(Smethod elem : comp.methods()){
                        methods.getOr(elem.toString(), Array::new).add(elem);
                    }
                }

                //add all methods from components
                for(ObjectMap.Entry<String, Array<Smethod>> entry : methods){
                    entry.value.sort(m -> m.has(MethodPriority.class) ? m.annotation(MethodPriority.class).value() : 0);

                    //representative method
                    Smethod first = entry.value.first();

                    //skip internal impl
                    if(first.has(InternalImpl.class)){
                        continue;
                    }

                    //build method using same params/returns
                    MethodSpec.Builder mbuilder = MethodSpec.methodBuilder(first.name()).addModifiers(first.is(Modifier.PRIVATE) ? Modifier.PRIVATE : Modifier.PUBLIC);
                    if(isFinal) mbuilder.addModifiers(Modifier.FINAL);
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

                    for(Smethod elem : entry.value){
                        if(elem.is(Modifier.ABSTRACT) || elem.is(Modifier.NATIVE)) continue;

                        //get all statements in the method, copy them over
                        MethodTree methodTree = elem.tree();
                        BlockTree blockTree = methodTree.getBody();
                        String str = blockTree.toString();
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

                        //SPECIAL CASE: inject group add/remove code
                        if(elem.name().equals("add") || elem.name().equals("remove")){
                            for(GroupDefinition def : groups){
                                //remove/add from each group, assume imported
                                mbuilder.addStatement("Groups.$L.$L(this)", def.name, elem.name());
                            }
                        }

                        //make sure to remove braces here
                        mbuilder.addCode(str);

                        //end scope
                        if(writeBlock) mbuilder.addCode("}\n");
                    }

                    builder.addMethod(mbuilder.build());
                }

                //make constructor private
                builder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PROTECTED).build());

                //add create() method
                builder.addMethod(MethodSpec.methodBuilder("create").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(tname(packageName + "." + name))
                .addStatement("return new $L()", name).build());

                definitions.add(new EntityDefinition("mindustry.gen." + name, builder, type, components, groups));
            }

            //generate groups
            TypeSpec.Builder groupsBuilder = TypeSpec.classBuilder("Groups").addModifiers(Modifier.PUBLIC);
            MethodSpec.Builder groupInit = MethodSpec.methodBuilder("init").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
            for(GroupDefinition group : groupDefs){
                Stype ctype = group.components.first();
                //class names for interface/group
                ClassName itype =  ClassName.bestGuess("mindustry.gen." + interfaceName(ctype));
                ClassName groupc = ClassName.bestGuess("mindustry.entities.EntityGroup");

                //add field...
                groupsBuilder.addField(ParameterizedTypeName.get(
                    ClassName.bestGuess("mindustry.entities.EntityGroup"), itype), group.name, Modifier.PUBLIC, Modifier.STATIC);

                groupInit.addStatement("$L = new $T<>($L, $L)", group.name, groupc, group.def.spatial(), group.def.mapping());
            }

            //write the groups
            groupsBuilder.addMethod(groupInit.build());

            MethodSpec.Builder groupResize = MethodSpec.methodBuilder("resize")
                .addParameter(TypeName.FLOAT, "x").addParameter(TypeName.FLOAT, "y").addParameter(TypeName.FLOAT, "w").addParameter(TypeName.FLOAT, "h")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

            for(GroupDefinition group : groupDefs){
                if(group.def.spatial()){
                    groupResize.addStatement("$L.resize(x, y, w, h)", group.name);
                }
            }

            groupsBuilder.addMethod(groupResize.build());

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

            //write assigned IDs
            PropertiesUtils.store(map, idProps.writer(false), "Maps entity names to IDs. Autogenerated.");

            //build mapping class for sync IDs
            TypeSpec.Builder idBuilder = TypeSpec.classBuilder("ClassMapping").addModifiers(Modifier.PUBLIC)
            .addField(FieldSpec.builder(TypeName.get(Prov[].class), "mapping", Modifier.PRIVATE, Modifier.STATIC).initializer("new Prov[256]").build())
            .addMethod(MethodSpec.methodBuilder("map").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.get(Prov.class)).addParameter(int.class, "id").addStatement("return mapping[id]").build());

            CodeBlock.Builder idStore = CodeBlock.builder();

            //store the mappings
            for(EntityDefinition def : definitions){
                //store mapping
                idStore.addStatement("mapping[$L] = $L::new", def.classID, def.name);
                //return mapping
                def.builder.addMethod(MethodSpec.methodBuilder("classId").addAnnotation(Override.class)
                    .returns(int.class).addModifiers(Modifier.PUBLIC).addStatement("return " + def.classID).build());
            }

            idBuilder.addStaticBlock(idStore.build());

            write(idBuilder);
        }else{
            //round 2: generate actual classes and implement interfaces
            Array<Stype> interfaces = types(EntityInterface.class);

            //implement each definition
            for(EntityDefinition def : definitions){

                //get interface for each component
                for(Stype comp : def.components){

                    //implement the interface
                    Stype inter = interfaces.find(i -> i.name().equals(interfaceName(comp)));
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

            //create mock types of all components
            for(Stype interf : interfaces){
                Array<Stype> dependencies = interf.allInterfaces();
                dependencies.add(interf);
                Log.info(interf + ": sub  " + interf.allSuperclasses() + " " + interf.allInterfaces());

                Array<Smethod> methods = dependencies.flatMap(Stype::methods);
                methods.sort(Structs.comparing(Object::toString));

                ObjectSet<String> signatures = new ObjectSet<>();

                TypeSpec.Builder nullBuilder = TypeSpec.classBuilder("Null" + interf.name().substring(0, interf.name().length() - 1))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                nullBuilder.addSuperinterface(interf.tname());

                for(Smethod method : methods){
                    String signature = method.toString();
                    if(signatures.contains(signature)) continue;

                    Stype type = method.type();
                    MethodSpec.Builder builder = MethodSpec.overriding(method.e).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                    if(!method.isVoid()){
                        builder.addStatement("return " + getDefault(method.ret().toString()));
                    }

                    nullBuilder.addMethod(builder.build());

                    signatures.add(signature);
                }

                //write(nullBuilder);

                Log.info("Methods to override for {0}:\n{1}\n\n", interf, methods.toString("\n", s -> "&lg> &lb" + s));
            }
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
    Array<Stype> allComponents(Stype type){
        if(!defComponents.containsKey(type)){
            //get base defs
            Array<Stype> components = types(type.annotation(EntityDef.class), EntityDef::value);
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
        final Array<Stype> components;
        final GroupDef def;

        public GroupDefinition(String name, Array<Stype> components, GroupDef def){
            this.components = components;
            this.name = name;
            this.def = def;
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
        final Stype base;
        final String name;
        int classID;

        public EntityDefinition(String name, Builder builder, Stype base, Array<Stype> components, Array<GroupDefinition> groups){
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
