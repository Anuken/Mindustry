use arc::graphics::*;
use arc::struct::*;
use mindustry::*;
use mindustry::content::*;

#[derive(Default)]
struct ColorMapper{
    let color2block : IntMap<Block>;
}

impl ColorMapper{
    fn get(color : i32) -> Block {
        color2block.get(color, Blocks::air)
    }

    fn load() {
        color2block.clear();

        for block : Block in Vars.content.blocks() {
            color2block.put(block.mapColor.rgba(), block);
        }

        color2block.put(Color.rgba8888(0, 0, 0, 1), Blocks.air);
    }
}
