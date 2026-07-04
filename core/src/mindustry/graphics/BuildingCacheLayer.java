package mindustry.graphics;

public enum BuildingCacheLayer{
    under(Layer.block - 0.5f),
    normal(Layer.block);

    public final float layer;

    public final static BuildingCacheLayer[] all = values();
    public final static int amount = all.length;
    public final static float[] layers = new float[all.length];

    static{
        for(int i = 0; i < amount; i++){
            layers[i] = all[i].layer;
        }
    }

    BuildingCacheLayer(float layer){
        this.layer = layer;
    }
}
