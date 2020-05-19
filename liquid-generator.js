var xx = "water"
var yy = "sand-water"
var zz = "darksand-water"
var cc = "darksand-tainted-water"
const liquidGenerator = extendContent(SolarGenerator,"liquidGenerator",{
getPowerProduction(tile){
    if(tile.floor() == xx || tile.floor() == yy || tile.floor() == zz || tile.floor() == cc){
        return 1.4
    } else {
        if(tile.entity.liquids.get(Liquids.water) > 0.1){
            if(tile.entity.timer.get(60)){
            tile.entity.liquids.remove(Liquids.water, 0.1)
            }
        return 1
        } else {
            return 0
        }
    }
}
});
liquidGenerator.consumes.liquid(Liquids.water, 0.1)
