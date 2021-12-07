package mindustry.world.blocks.defense;

public class DirectionalForceProjector extends ForceProjector{

    public DirectionalForceProjector(String name){
        super(name);

        consumeCoolant = false;
    }

    public class DirectionalForceProjectorBuild extends ForceBuild{

        @Override
        public void deflectBullets(){
            //TODO
        }
    }
}
