package mindustry.annotations.entity;

import arc.files.*;
import arc.struct.*;
import arc.util.serialization.*;
import com.squareup.javapoet.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;
import mindustry.annotations.util.TypeIOResolver.*;

import javax.lang.model.element.*;

import static mindustry.annotations.BaseProcessor.instanceOf;

public class EntityIO{
    final static Json json = new Json();

    final ClassSerializer serializer;
    final String name;
    final TypeSpec.Builder type;
    final Fi directory;
    final Array<Revision> revisions = new Array<>();

    boolean write;
    MethodSpec.Builder method;
    ObjectSet<String> presentFields = new ObjectSet<>();

    EntityIO(String name, TypeSpec.Builder type, ClassSerializer serializer, Fi directory){
        this.directory = directory;
        this.type = type;
        this.serializer = serializer;
        this.name = name;

        directory.mkdirs();

        //load old revisions
        for(Fi fi : directory.list()){
            revisions.add(json.fromJson(Revision.class, fi));
        }

        //next revision to be used
        int nextRevision = revisions.isEmpty() ? 0 : revisions.max(r -> r.version).version + 1;

        //resolve preferred field order based on fields that fit
        Array<FieldSpec> fields = Array.with(type.fieldSpecs).select(spec ->
            !spec.hasModifier(Modifier.TRANSIENT) &&
            !spec.hasModifier(Modifier.STATIC) &&
            !spec.hasModifier(Modifier.FINAL) &&
            (spec.type.isPrimitive() || serializer.has(spec.type.toString())));

        //sort to keep order
        fields.sortComparing(f -> f.name);

        //keep track of fields present in the entity
        presentFields.addAll(fields.map(f -> f.name));

        //add new revision if it doesn't match or there are no revisions
        if(revisions.isEmpty() || !revisions.peek().equal(fields)){
            revisions.add(new Revision(nextRevision, fields.map(f -> new RevisionField(f.name, f.type.toString(), f.type.isPrimitive() ? BaseProcessor.typeSize(f.type.toString()) : -1))));
            //write revision
            directory.child(nextRevision + ".json").writeString(json.toJson(revisions.peek()));
        }
    }

    void write(MethodSpec.Builder method, boolean write) throws Exception{
        this.method = method;
        this.write = write;

        //subclasses *have* to call this method
        method.addAnnotation(CallSuper.class);

        if(write){
            //write short revision
            st("write.s($L)", revisions.peek().version);
            //write uses most recent revision
            for(RevisionField field : revisions.peek().fields){
                io(field.type, "this." + field.name);
            }
        }else{
            //read revision
            st("short REV = read.s()");

            for(int i = 0; i < revisions.size; i++){
                //check for the right revision
                Revision rev = revisions.get(i);
                if(i == 0){
                    cont("if(REV == $L)", rev.version);
                }else{
                    ncont("else if(REV == $L)", rev.version);
                }

                //add code for reading revision
                for(RevisionField field : rev.fields){
                    //if the field doesn't exist, the result will be an empty string, it won't get assigned
                    io(field.type, presentFields.contains(field.name) ? "this." + field.name + " = " : "");
                }
            }

            //throw exception on illegal revisions
            ncont("else");
            st("throw new IllegalArgumentException(\"Unknown revision '\" + REV + \"' for entity type '" + name + "'\")");
            econt();
        }
    }

    private void io(String type, String field) throws Exception{
        if(BaseProcessor.isPrimitive(type)){
            s(type.equals("boolean") ? "bool" : type.charAt(0) + "", field);
        }else if(instanceOf(type, "mindustry.ctype.Content")){
            if(write){
                s("s", field + ".id");
            }else{
                st(field + "mindustry.Vars.content.getByID(mindustry.ctype.ContentType.$L, read.s())", BaseProcessor.simpleName(type).toLowerCase().replace("type", ""));
            }
        }else if(serializer.writers.containsKey(type) && write){
            st("$L(write, $L)", serializer.writers.get(type), field);
        }else if(serializer.readers.containsKey(type) && !write){
            st("$L$L(read)", field, serializer.readers.get(type));
        }
    }

    private void cont(String text, Object... fmt){
        method.beginControlFlow(text, fmt);
    }

    private void econt(){
        method.endControlFlow();
    }

    private void ncont(String text, Object... fmt){
        method.nextControlFlow(text, fmt);
    }

    private void st(String text, Object... args){
        method.addStatement(text, args);
    }

    private void s(String type, String field){
        if(write){
            method.addStatement("write.$L($L)", type, field);
        }else{
            method.addStatement("$Lread.$L()", field, type);
        }
    }

    public static class Revision{
        int version;
        Array<RevisionField> fields;

        Revision(int version, Array<RevisionField> fields){
            this.version = version;
            this.fields = fields;
        }

        Revision(){}

        /** @return whether these two revisions are compatible */
        boolean equal(Array<FieldSpec> specs){
            if(fields.size != specs.size) return false;

            for(int i = 0; i < fields.size; i++){
                RevisionField field = fields.get(i);
                FieldSpec spec = specs.get(i);
                //TODO when making fields, their primitive size may be overwritten by an annotation; check for that
                if(!(field.type.equals(spec.type.toString()) && (!spec.type.isPrimitive() || BaseProcessor.typeSize(spec.type.toString()) == field.size))){
                    return false;
                }
            }
            return true;
        }
    }

    public static class RevisionField{
        String name, type;
        int size; //in bytes

        RevisionField(String name, String type, int size){
            this.name = name;
            this.size = size;
            this.type = type;
        }

        RevisionField(){}
    }
}
