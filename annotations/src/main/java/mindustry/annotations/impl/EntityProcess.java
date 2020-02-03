package mindustry.annotations.impl;

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

@SupportedAnnotationTypes({
"mindustry.annotations.Annotations.EntityDef",
"mindustry.annotations.Annotations.EntityInterface",
"mindustry.annotations.Annotations.BaseComponent"
})
public class EntityProcess extends BaseProcessor{
    Array<Definition> definitions = new Array<>();
    Array<Stype> baseComponents;
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
            Array<Stype> allDefs = types(EntityDef.class);

            ObjectSet<Stype> allComponents = new ObjectSet<>();

            //find all components used...
            for(Stype type : allDefs){
                allComponents.addAll(allComponents(type));
            }

            //add all components w/ dependencies
            allComponents.addAll(types(Depends.class).map(s -> Array.withArrays(getDependencies(s), s)).flatten());

            //add component imports
            for(Stype comp : allComponents){
                imports.addAll(getImports(comp.e));
            }

            //create component interfaces
            for(Stype component : allComponents){
                TypeSpec.Builder inter = TypeSpec.interfaceBuilder(interfaceName(component)).addModifiers(Modifier.PUBLIC).addAnnotation(EntityInterface.class);

                //implement extra interfaces these components may have, e.g. position
                for(Stype extraInterface : component.interfaces()){
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
                    inter.addMethod(MethodSpec.methodBuilder("get" + cname).addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC).returns(field.tname()).build());
                    //setter
                    if(!field.is(Modifier.FINAL)) inter.addMethod(MethodSpec.methodBuilder("set" + cname).addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC).addParameter(field.tname(), field.name()).build());
                }

                //add utility methods to interface
                for(Smethod method : component.methods()){
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
                String name = type.name().replace("Def", "Gen"); //TODO remove 'gen'
                TypeSpec.Builder builder = TypeSpec.classBuilder(name).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                Array<Stype> components = allComponents(type);
                ObjectMap<String, Array<Smethod>> methods = new ObjectMap<>();

                //add all components
                for(Stype comp : components){

                    //write fields to the class; ignoring transient ones
                    Array<Svar> fields = comp.fields().select(f -> !f.is(Modifier.TRANSIENT));
                    for(Svar f : fields){
                        VariableTree tree = f.tree();
                        FieldSpec.Builder fbuilder = FieldSpec.builder(f.tname(), f.name());
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
                    //representative method
                    Smethod first = entry.value.first();
                    //build method using same params/returns
                    MethodSpec.Builder mbuilder = MethodSpec.methodBuilder(first.name()).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
                    mbuilder.addTypeVariables(first.typeVariables().map(TypeVariableName::get));
                    mbuilder.returns(first.retn());
                    mbuilder.addExceptions(first.thrownt());

                    for(Svar var : first.params()){
                        mbuilder.addParameter(var.tname(), var.name());
                    }

                    //only write the block if it's a void method with several entries
                    boolean writeBlock = first.ret().toString().equals("void") && entry.value.size > 1;

                    if(entry.value.first().is(Modifier.ABSTRACT) && entry.value.size == 1){
                        err(entry.value.first().up().getSimpleName() + " declares an abstract method. This method must be implemented in another component", entry.value.first());
                    }

                    for(Smethod elem : entry.value){
                        if(elem.is(Modifier.ABSTRACT)) continue;

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

                        //make sure to remove braces here
                        mbuilder.addCode(str);

                        //end scope
                        if(writeBlock) mbuilder.addCode("}\n");
                    }

                    builder.addMethod(mbuilder.build());
                }

                definitions.add(new Definition(builder, type));

            }
        }else{
            //round 2: generate actual classes and implement interfaces
            Array<Stype> interfaces = types(EntityInterface.class);

            //implement each definition
            for(Definition def : definitions){
                Array<Stype> components = allComponents(def.base);

                //get interface for each component
                for(Stype comp : components){
                    //implement the interface
                    Stype inter = interfaces.find(i -> i.name().equals(interfaceName(comp)));
                    def.builder.addSuperinterface(inter.tname());

                    //generate getter/setter for each method
                    for(Smethod method : inter.methods()){
                        if(method.name().length() <= 3) continue;

                        String var = Strings.camelize(method.name().substring(3));
                        //make sure it's a real variable
                        if(!Array.with(def.builder.fieldSpecs).contains(f -> f.name.equals(var))) continue;

                        if(method.name().startsWith("get")){
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
            err("All components must have names that end with 'Comp'.", comp.e);
        }
        return comp.name().substring(0, comp.name().length() - suffix.length()) + "c";
    }

    /** @return all components that a entity def has */
    Array<Stype> allComponents(Stype type){
        if(!defComponents.containsKey(type)){
            //get base defs
            Array<Stype> components = Array.with(mirrors(type)).map(Stype::of);
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
            out.addAll(component.superclasses());

            //get dependency classes
            if(component.annotation(Depends.class) != null){
                try{
                    component.annotation(Depends.class).value();
                }catch(MirroredTypesException e){
                    out.addAll(Array.with(e.getTypeMirrors()).map(Stype::of));
                }
            }

            //out now contains the base dependencies; finish constructing the tree
            ObjectSet<Stype> result = new ObjectSet<>();
            for(Stype type : out){
                result.add(type);
                result.addAll(getDependencies(type));
            }

            if(component.annotation(BaseComponent.class) == null){
                result.addAll(baseComponents);
            }

            componentDependencies.put(component, result.asArray());
        }

        return componentDependencies.get(component);
    }

    TypeMirror[] mirrors(Stype type){
        try{
            type.annotation(EntityDef.class).value();
        }catch(MirroredTypesException e){
            return e.getTypeMirrors().toArray(new TypeMirror[0]);
        }
        throw new IllegalArgumentException("Missing components: " + type);
    }

    class Definition{
        final TypeSpec.Builder builder;
        final Stype base;

        public Definition(Builder builder, Stype base){
            this.builder = builder;
            this.base = base;
        }
    }
}
