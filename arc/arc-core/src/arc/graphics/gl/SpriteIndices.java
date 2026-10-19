package arc.graphics.gl;

public class SpriteIndices{
    private static final int maxIndices = 8192*2;
    private static IndexBufferObject indexData;

    /** @return a globally shared IndexData instance for drawing sprites in the same pattern as SpriteBatch/SpriteCache does. */
    public static IndexBufferObject get(){
        if(indexData == null){
            int j = 0;
            short[] indices = new short[maxIndices * 6];
            for(int i = 0; i < indices.length; i += 6, j += 4){
                indices[i] = (short)j;
                indices[i + 1] = (short)(j + 1);
                indices[i + 2] = (short)(j + 2);
                indices[i + 3] = (short)(j + 2);
                indices[i + 4] = (short)(j + 3);
                indices[i + 5] = (short)j;
            }

            indexData = new IndexBufferObject(true, indices.length){
                @Override
                public void dispose(){
                    //there is never a need to dispose this index buffer
                }
            };
            indexData.set(indices, 0, indices.length);
        }

        return indexData;
    }
}
