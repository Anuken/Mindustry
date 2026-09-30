package mindustry.world;

import arc.struct.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import mindustry.maps.filters.GenerateFilter.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** World context that applies filters after generation end. */
public class FilterContext extends DefaultWorldContext{
    final Map map;

    public FilterContext(Map map){
        this.map = map;
    }

    public FilterContext(Map map, Sector sector){
        super(sector);
        this.map = map;
    }

    @Override
    public void end(){
        applyFilters();

        super.end();
    }

    @Override
    public boolean isMap(){
        return true;
    }

    public void applyFilters(){
        Seq<GenerateFilter> filters = map.filters();

        if(!filters.isEmpty()){
            //input for filter queries
            GenerateInput input = new GenerateInput();

            for(GenerateFilter filter : filters){
                filter.randomize();
                input.begin(state.world.width, state.world.height, state.world::rawTile);
                filter.apply(state.world, input);
            }
        }
    }
}