package mindustry.io;

import mindustry.*;
import mindustry.type.*;

import java.io.*;

/** Reads that resolve content IDs through a save-specific ID -> content mapping instead of the global IDs. */
@SuppressWarnings("unchecked")
public class MappedReads extends Reads{
    /** Indexed by content type ordinal, then by the ID stored in the save. */
    private final MappableContent[][] map;

    public MappedReads(DataInput input, MappableContent[][] map){
        super(input);
        this.map = map;
    }

    @Override
    public <T extends Content> T content(ContentType type, int id){
        MappableContent[] mapped = map[type.ordinal()];

        if(mapped == null || mapped.length == 0){
            return super.content(type, id);
        }

        //-1 = invalid content
        if(id < 0){
            return null;
        }

        //default value is always ID 0
        if(mapped.length <= id || mapped[id] == null){
            return Vars.content.getByID(type, 0);
        }

        return (T)mapped[id];
    }
}
