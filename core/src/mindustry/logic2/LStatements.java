package mindustry.logic2;

import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.logic.*;
import mindustry.ui.*;

public class LStatements{

    public static class AssignStatement extends LStatement{
        public int output;
        public double value;

        @Override
        public void run(LExecutor exec){

        }

        @Override
        public void build(Table table){
            table.field(value + "", Styles.nodeField, str -> value = parseDouble(str))
                .size(100f, 40f).pad(2f).color(table.color).padRight(20);

            table.field(value + "", Styles.nodeField, str -> value = parseDouble(str))
                .size(100f, 40f).pad(2f).color(table.color);
        }

        @Override
        public LCategory category(){
            return LCategory.operations;
        }

        static double parseDouble(String s){
            return s.equals("yes") || s.equals("true") ? 1 :
            s.equals("no") || s.equals("false") ? 0 :
            Strings.parseDouble(s, 0);
        }
    }

    public static class OpStatement extends LStatement{
        public BinaryOp op = BinaryOp.add;
        public int output;
        public double result;

        @Override
        public void run(LExecutor exec){

        }

        @Override
        public void build(Table table){

        }

        @Override
        public LCategory category(){
            return LCategory.operations;
        }
    }
}
