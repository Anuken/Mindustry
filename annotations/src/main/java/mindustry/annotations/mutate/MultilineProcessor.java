package mindustry.annotations.mutate;

import com.sun.tools.javac.tree.JCTree.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import java.io.*;
import java.util.*;

//currently unused
@SupportedAnnotationTypes({"mindustry.annotations.Annotations.Multiline"})
public final class MultilineProcessor extends BaseProcessor{

    @Override
    public void process(RoundEnvironment env){
        Set<? extends Element> fields = env.getElementsAnnotatedWith(Multiline.class);
        for(Element field : fields){
            String docComment = elementUtils.getDocComment(field);
            if(null != docComment){
                JCVariableDecl fieldNode = (JCVariableDecl)elementUtils.getTree(field);
                fieldNode.init = maker.Literal(toString(docComment, field.getAnnotation(Multiline.class)));
            }
        }
    }

    static String toString(String value, Multiline annotation){
        if(!annotation.merge() && !annotation.trim()){
            return value;
        }

        String crnl = System.getProperty("line.separator");
        try{
            BufferedReader reader = new BufferedReader(new StringReader(value));
            StringBuilder buf = new StringBuilder();
            String line = reader.readLine();
            while(line != null){
                if(annotation.trim()){
                    line = line.trim();
                }
                if(annotation.merge() && buf.length() > 0){
                    if(annotation.mergeChar() != '\0'){
                        buf.append(annotation.mergeChar());
                    }
                }
                buf.append(line);
                if(!annotation.merge()){
                    buf.append(crnl);
                }

                line = reader.readLine();
            }
            return buf.toString();
        }catch(IOException ex){
            throw new RuntimeException("checked exceptions are disgusting", ex);
        }
    }
}