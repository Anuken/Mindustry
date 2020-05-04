//Generated class. Do not modify.

const log = function(context, obj){
    Vars.mods.getScripts().log(context, String(obj))
}

const onEvent = function(event, handler){
    Vars.mods.getScripts().onEvent(EventType[event], handler)
}

var scriptName = "base.js"
var modName = "none"

const print = text => log(modName + "/" + scriptName, text);

const extendContent = function(classType, name, params){
    return new JavaAdapter(classType, params, name)
}

const extend = function(classType, params){
    return new JavaAdapter(classType, params)
}

const newEffect = (lifetime, renderer) => new Effects.Effect(lifetime, new Effects.EffectRenderer({render: renderer}))
Call = Packages.mindustry.gen.Call

importPackage(Packages.mindustry.game)
importPackage(Packages.arc.func)
importPackage(Packages.mindustry.entities)
importPackage(Packages.mindustry.gen)
importPackage(Packages.mindustry.core)
importPackage(Packages.mindustry.world.blocks.storage)
importPackage(Packages.arc.scene.ui.layout)
importPackage(Packages.arc.scene.ui)
importPackage(Packages.mindustry.ai.formations.patterns)
importPackage(Packages.mindustry.world.blocks.distribution)
importPackage(Packages.mindustry.ui)
importPackage(Packages.mindustry.content)
importPackage(Packages.mindustry.world.blocks.liquid)
importPackage(Packages.arc.struct)
importPackage(Packages.mindustry.world.modules)
importPackage(Packages.arc.util)
importPackage(Packages.mindustry.maps.generators)
importPackage(Packages.arc.graphics)
importPackage(Packages.mindustry.entities.def)
importPackage(Packages.arc.scene.actions)
importPackage(Packages.mindustry.graphics)
importPackage(Packages.mindustry.entities.bullet)
importPackage(Packages.mindustry.async)
importPackage(Packages.mindustry.world.blocks.legacy)
importPackage(Packages.mindustry.world.blocks.experimental)
importPackage(Packages.mindustry.editor)
importPackage(Packages.mindustry.world.blocks.defense.turrets)
importPackage(Packages.mindustry.world.blocks.power)
importPackage(Packages.mindustry.world.blocks.production)
importPackage(Packages.mindustry.world.blocks.sandbox)
importPackage(Packages.mindustry.input)
importPackage(Packages.mindustry.world.consumers)
importPackage(Packages.mindustry.world.blocks.defense)
importPackage(Packages.mindustry.ai.formations)
importPackage(Packages.mindustry.type)
importPackage(Packages.arc.scene.event)
importPackage(Packages.mindustry.ui.fragments)
importPackage(Packages.mindustry.world.blocks.units)
importPackage(Packages.arc.scene.utils)
importPackage(Packages.mindustry.ui.dialogs)
importPackage(Packages.mindustry.graphics.g3d)
importPackage(Packages.mindustry.world.meta)
importPackage(Packages.arc.math)
importPackage(Packages.mindustry.world.blocks.payloads)
importPackage(Packages.mindustry.world)
importPackage(Packages.arc.scene.style)
importPackage(Packages.mindustry.world.blocks)
importPackage(Packages.arc.math.geom)
importPackage(Packages.mindustry.ai)
importPackage(Packages.mindustry.maps.filters)
importPackage(Packages.arc.graphics.g2d)
importPackage(Packages.mindustry.world.blocks.environment)
importPackage(Packages.mindustry)
importPackage(Packages.mindustry.entities.units)
importPackage(Packages.mindustry.ctype)
importPackage(Packages.mindustry.ai.types)
importPackage(Packages.mindustry.maps)
importPackage(Packages.mindustry.world.meta.values)
importPackage(Packages.mindustry.world.producers)
importPackage(Packages.mindustry.ui.layout)
importPackage(Packages.arc.scene)
importPackage(Packages.mindustry.maps.planet)
importPackage(Packages.mindustry.world.blocks.logic)
importPackage(Packages.arc)
