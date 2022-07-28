package mindustry.maps.filters;

import arc.scene.ui.layout.*;
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
    LExecutor executor;

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
        executor = new LExecutor();
        executor.privileged = true;
        executor.isFilter = true;
        configure(code);

        //limited run
        for(int i = 1; i < maxInstructionsExecution; i++){
            if(!loop && (executor.counter.numval >= executor.instructions.length || executor.counter.numval < 0)) break;
            executor.runOnce();
        }
    }

    @Override
    public char icon(){
        return Iconc.blockMicroProcessor;
    }

    void configure(String code){
        try{
            //create assembler to store extra variables
            LAssembler asm = LAssembler.assemble(code, true);

            asm.putConst("@mapw", world.width());
            asm.putConst("@maph", world.height());
            asm.putConst("@links", executor.links.length);
            asm.putConst("@ipt", 1);

            asm.putConst("@thisx", 0);
            asm.putConst("@thisy", 0);

            executor.load(asm);
        }catch(Exception e){
            //handle malformed code and replace it with nothing
            executor.load(LAssembler.assemble(code = "", true));
        }
    }
}
