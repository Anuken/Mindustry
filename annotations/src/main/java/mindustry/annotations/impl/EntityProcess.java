package mindustry.annotations.impl;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
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
    Array<Definition> definitions = new Array<>();
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
                Log.info("&yGenerating interface for " + component);
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

                for(Svar field : component.fields().select(e -> !e.is(Modifier.STATIC) && !e.is(Modifier.PRIVATE) && !e.is(Modifier.TRANSIENT))){
                    String cname = Strings.capitalize(field.name());
                    //getter
                    inter.addMethod(MethodSpec.methodBuilder((field.mirror().toString().equals("boolean") ? "is" : "get") + cname).addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC).returns(field.tname()).build());
                    //setter
                    if(!field.is(Modifier.FINAL)) inter.addMethod(MethodSpec.methodBuilder("set" + cname).addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC).addParameter(field.tname(), field.name()).build());
                }

                //add utility methods to interface
                for(Smethod method : component.methods()){
                    //skip private methods, those are for internal use.
                    if(method.is(Modifier.PRIVATE)) continue;

                    inter.addMethod(MethodSpec.methodBuilder(method.name())
                    .addExceptions(method.thrownt())
                    .addTypeVariables(method.typeVariables().map(TypeVariableName::get))
                    .returns(method.ret().toString().equals("void") ? TypeName.VOID : method.retn())
                    .addParameters(method.params().map(v -> ParameterSpec.builder(v.tname(), v.name())
                    .build())).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).build());
                }

                write(inter);
            }

            //look at each definition
            for(Stype type : allDefs){
                if(!type.name().endsWith("Def")){
                    err("All entity def names must end with 'Def'", type.e);
                }
                String name = type.name().replace("Def", "_"); //TODO remove extra underscore
                TypeSpec.Builder builder = TypeSpec.classBuilder(name).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

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
                    //build method using same params/returns
                    MethodSpec.Builder mbuilder = MethodSpec.methodBuilder(first.name()).addModifiers(first.is(Modifier.PRIVATE) ? Modifier.PRIVATE : Modifier.PUBLIC, Modifier.FINAL);
                    mbuilder.addTypeVariables(first.typeVariables().map(TypeVariableName::get));
                    mbuilder.returns(first.retn());
                    mbuilder.addExceptions(first.thrownt());

                    for(Svar var : first.params()){
                        mbuilder.addParameter(var.tname(), var.name());
                    }

                    //only write the block if it's a void method with several entries
                    boolean writeBlock = first.ret().toString().equals("void") && entry.value.size > 1;

                    if((entry.value.first().is(Modifier.ABSTRACT) || entry.value.first().is(Modifier.NATIVE)) && entry.value.size == 1){
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

                definitions.add(new Definition(builder, type, components, groups));
            }

            //generate groups
            TypeSpec.Builder groupsBuilder = TypeSpec.classBuilder("Groups").addModifiers(Modifier.PUBLIC);
            MethodSpec.Builder groupInit = MethodSpec.methodBuilder("init").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
            for(GroupDefinition group : groupDefs){
                Stype ctype = group.components.first();
                ClassName itype =  ClassName.bestGuess("mindustry.gen." + interfaceName(ctype));
                ClassName groupc = ClassName.bestGuess("mindustry.entities.EntityGroup");

                //add field...
                groupsBuilder.addField(ParameterizedTypeName.get(
                    ClassName.bestGuess("mindustry.entities.EntityGroup"), itype), group.name, Modifier.PUBLIC, Modifier.STATIC);

                groupInit.addStatement("$L = new $T<>($L, $L)", group.name, groupc, group.def.spatial(), group.def.mapping());
            }

            groupsBuilder.addMethod(groupInit.build());

            write(groupsBuilder);

        }else{
            //round 2: generate actual classes and implement interfaces
            Array<Stype> interfaces = types(EntityInterface.class);

            //implement each definition
            for(Definition def : definitions){

                //get interface for each component
                for(Stype comp : def.components){
                    //implement the interface
                    Stype inter = interfaces.find(i -> i.name().equals(interfaceName(comp)));
                    if(inter == null){
                        err("Failed to generate interface for component. Interfaces are " + interfaces + "\nComponent", comp);
                        return;
                    }

                    def.builder.addSuperinterface(inter.tname());

                    //generate getter/setter for each method
                    for(Smethod method : inter.methods()){
                        if(method.name().length() <= 3) continue;

                        String var = Strings.camelize(method.name().substring(method.name().startsWith("is") ? 2 : 3));
                        //make sure it's a real variable
                        if(!Array.with(def.builder.fieldSpecs).contains(f -> f.name.equals(var))) continue;

                        if(method.name().startsWith("get") || method.name().startsWith("is")){
                            def.builder.addMethod(MethodSpec.overriding(method.e).addStatement("return " + var).build());
                        }else if(method.name().startsWith("set")){
                            def.builder.addMethod(MethodSpec.overriding(method.e).addStatement("this." + var + " = " + var).build());
                        }
                    }
                }

                write(def.builder, imports.asArray());
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

    class Definition{
        final Array<GroupDefinition> groups;
        final Array<Stype> components;
        final TypeSpec.Builder builder;
        final Stype base;

        public Definition(Builder builder, Stype base, Array<Stype> components, Array<GroupDefinition> groups){
            this.builder = builder;
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
