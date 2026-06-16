package mindustry.annotations.entity;

import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;

import javax.annotation.processing.*;
import javax.lang.model.type.*;
import javax.tools.*;
import java.io.*;

@SupportedAnnotationTypes("mindustry.annotations.Annotations.BuildingListDefs")
public class BuildingListProcess extends BaseProcessor{
    static final String template = """
    package mindustry.gen;
    
    import ELEMENT_TYPE;
    
    public class CLASS_NAME{
        public ELEMENT_TYPE[] values = new ELEMENT_TYPE[50];
        public int size, index;
        
        public void update(){
            for(index = 0; index < size; index++){
                values[index].UPDATE_METHOD_NAME();
            }
        }
        
        public void add(ELEMENT_TYPE building){
            ELEMENT_TYPE[] items = values;
            if(size == items.length){
                int newLength = (int)(size * 1.75f);
                ELEMENT_TYPE[] newItems = new ELEMENT_TYPE[newLength];
                System.arraycopy(items, 0, newItems, 0, size);
                values = items = newItems;
            }
            items[building.buildingArrayIndex = size++] = building;
        }
        
        public void remove(ELEMENT_TYPE type){
            int position = type.buildingArrayIndex;
        
            if(position != -1 && position < size){
        
                //rarely the entity index is wrong; fallback to slow implementation
                if(values[position] != type){
                    type.buildingArrayIndex = -1;
                    for(int i = 0; i < size; i++){
                        if(values[position] == type){
                            type.buildingArrayIndex = i;
                            break;
                        }
                    }
                    //not found in the array (attempt to remove a building that does not exist?)
                    if(type.buildingArrayIndex == -1) return;
                }
        
                //swap head with current
                if(size > 1){
                    var head = values[size - 1];
                    head.buildingArrayIndex = position;
                    values[position] = head;
                }
        
                size--;
                values[size] = null;
        
                //fix iteration index when removing
                if(index >= position){
                    index--;
                }
            }
        }
    }
    
    """;

    @Override
    public void process(RoundEnvironment env) throws Exception{
        var types = types(BuildingListDefs.class);
        for(var type : types){
            for(var listDef : type.annotation(BuildingListDefs.class).value()){
                String fullName;

                if(!listDef.qualifiedType().isEmpty()){
                    fullName = listDef.qualifiedType();
                }else{
                    try{
                        fullName = listDef.type().getCanonicalName();
                    }catch(MirroredTypeException e){
                        fullName = e.getTypeMirror().toString();
                    }
                }

                String simpleName = simpleName(fullName);

                var listMethod = listDef.method();

                String listClassName = (simpleName.endsWith("Build") ? simpleName.replace("Build","") : simpleName) + "List";
                String resultClass = template
                .replaceAll("ELEMENT_TYPE", fullName)
                .replaceAll("CLASS_NAME", listClassName)
                .replaceAll("UPDATE_METHOD_NAME", listMethod)
                ;

                JavaFileObject object = filer.createSourceFile(packageName + "." + listClassName);
                Writer stream = object.openWriter();
                stream.write(resultClass);
                stream.close();
            }
        }
    }
}
