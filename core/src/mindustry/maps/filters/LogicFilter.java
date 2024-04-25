package mindustry.maps.filters;

import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.maps.filters.FilterOption.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class LogicFilter extends GenerateFilter{
    /** max available execution for logic filter */
    public static int maxInstructionsExecution = 500 * 500 * 25;
    public String code;
    public boolean loop;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
            new FilterOption(){
                final String name;
                {
                    name = "code";
                }
                @Override
                public void build(Table table){
                    table.button(b -> b.image(Icon.pencil).size(iconSmall), () -> {
                        ui.logic.show(code, null, true, code -> LogicFilter.this.code = code);
                    }).pad(4).margin(12f);

                    table.add("@filter.option." + name);
                }
            },
            new ToggleOption("loop", () -> loop, f -> loop = f)
        };
    }

    @Override
    public void apply(Tiles tiles, GenerateInput in){
        LExecutor executor = new LExecutor();
        executor.privileged = true;

        try{
            //assembler has no variables, all the standard ones are null
            executor.load(LAssembler.assemble(code, true));
        }catch(Throwable ignored){
            //if loading code
            return;
        }

        //this updates map width/height global variables
        logicVars.update();

        //NOTE: all tile operations will call setNet for tiles, but that should have no overhead during world loading
        //executions are limited to prevent infinite generation
        for(int i = 1; i < maxInstructionsExecution; i++){
            if(!loop && (executor.counter.numval >= executor.instructions.length || executor.counter.numval < 0)) break;
            executor.runOnce();
        }
    }

    @Override
    public char icon(){
        return Iconc.blockMicroProcessor;
    }

    @Override
    public boolean isPost(){
        return true;
    }
}
