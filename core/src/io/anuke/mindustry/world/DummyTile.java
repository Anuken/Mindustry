package io.anuke.mindustry.world;

import io.anuke.mindustry.game.Team;

public class DummyTile extends Tile{

    public DummyTile(int x, int y){
        super(x, y);
    }

    @Override
    public Team getTeam(){
        return Team.all[getTeamID()];
    }

    @Override
    protected void changed(){
        //nothing matters
    }

    @Override
    protected void preChanged(){
        //it really doesn't
    }
}
