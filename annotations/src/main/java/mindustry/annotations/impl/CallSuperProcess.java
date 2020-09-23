package mindustry.annotations.impl;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import mindustry.annotations.Annotations.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic.*;
import java.lang.annotation.*;
import java.util.*;

@SupportedAnnotationTypes({"java.lang.Override"})
public class CallSuperProcess extends AbstractProcessor{
    private Trees trees;

    @Override
    public void init(ProcessingEnvironment pe){
        super.init(pe);
        trees = Trees.instance(pe);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        for(Element e : roundEnv.getElementsAnnotatedWith(Override.class)){
            if(e.getAnnotation(OverrideCallSuper.class) != null) return false;

            CodeAnalyzerTreeScanner codeScanner = new CodeAnalyzerTreeScanner();
            codeScanner.methodName = e.getSimpleName().toString();

            TreePath tp = trees.getPath(e.getEnclosingElement());
            codeScanner.scan(tp, trees);

            if(codeScanner.callSuperUsed){
                List list = codeScanner.method.getBody().getStatements();

                if(!doesCallSuper(list, codeScanner.methodName)){
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Overriding method '" + codeScanner.methodName + "' must explicitly call super method from its parent class.", e);
                }
            }
        }

        return false;
    }

    private boolean doesCallSuper(List list, String methodName){
        for(Object object : list){
            if(object instanceof JCTree.JCExpressionStatement){
                JCTree.JCExpressionStatement expr = (JCExpressionStatement)object;
                String exprString = expr.toString();
                if(exprString.startsWith("super." + methodName) && exprString.endsWith(");")) return true;
            }
        }

        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion(){
        return SourceVersion.RELEASE_8;
    }

    static class CodeAnalyzerTreeScanner extends TreePathScanner<Object, Trees>{
        String methodName;
        MethodTree method;
        boolean callSuperUsed;

        @Override
        public Object visitClass(ClassTree classTree, Trees trees){
            Tree extendTree = classTree.getExtendsClause();

            if(extendTree instanceof JCTypeApply){ //generic classes case
                JCTypeApply generic = (JCTypeApply)extendTree;
                extendTree = generic.clazz;
            }

            if(extendTree instanceof JCIdent){
                JCIdent tree = (JCIdent)extendTree;

                if(tree == null || tree.sym == null)  return super.visitClass(classTree, trees);

                com.sun.tools.javac.code.Scope members = tree.sym.members();

                if(checkScope(members))
                    return super.visitClass(classTree, trees);

                if(checkSuperTypes((ClassType)tree.type))
                    return super.visitClass(classTree, trees);

            }
            callSuperUsed = false;

            return super.visitClass(classTree, trees);
        }

        public boolean checkSuperTypes(ClassType type){
            if(type.supertype_field != null && type.supertype_field.tsym != null){
                if(checkScope(type.supertype_field.tsym.members()))
                    return true;
                else
                    return checkSuperTypes((ClassType)type.supertype_field);
            }

            return false;
        }

        @SuppressWarnings("unchecked")
        public boolean checkScope(Scope members){
            Iterable<Symbol> it;
            try{
                it = (Iterable<Symbol>)members.getClass().getMethod("getElements").invoke(members);
            }catch(Throwable t){
                try{
                    it = (Iterable<Symbol>)members.getClass().getMethod("getSymbols").invoke(members);
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
            }

            for(Symbol s : it){

                if(s instanceof MethodSymbol){
                    MethodSymbol ms = (MethodSymbol)s;

                    if(ms.getSimpleName().toString().equals(methodName)){
                        Annotation annotation = ms.getAnnotation(CallSuper.class);
                        if(annotation != null){
                            callSuperUsed = true;
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        @Override
        public Object visitMethod(MethodTree methodTree, Trees trees){
            if(methodTree.getName().toString().equals(methodName))
                method = methodTree;

            return super.visitMethod(methodTree, trees);
        }

    }
}
