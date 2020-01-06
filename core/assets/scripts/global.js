//Generated class. Do not modify.

const log = function(context, obj){
    Vars.mods.getScripts().log(context, obj ? String(obj) : "null")
}

const extendContent = function(classType, name, params){
    return new JavaAdapter(classType, params, name)
}

const extend = function(classType, params){
    return new JavaAdapter(classType, params)
}

const run = method => new java.lang.Runnable(){run: method}
const boolf = method => new Boolf(){get: method}
const boolp = method => new Boolp(){get: method}
const cons = method => new Cons(){get: method}
const prov = method => new Prov(){get: method}
const newEffect = (lifetime, renderer) => new Effects.Effect(lifetime, new Effects.EffectRenderer({render: renderer}))
Call = Packages.mindustry.gen.Call
const Calls = Call //backwards compat
importPackage(Packages.arc)
importPackage(Packages.arc.func)
importPackage(Packages.arc.graphics)
importPackage(Packages.arc.graphics.g2d)
importPackage(Packages.arc.math)
importPackage(Packages.arc.math.geom)
importPackage(Packages.arc.scene)
importPackage(Packages.arc.scene.actions)
importPackage(Packages.arc.scene.event)
importPackage(Packages.arc.scene.style)
importPackage(Packages.arc.scene.ui)
importPackage(Packages.arc.scene.ui.layout)
importPackage(Packages.arc.scene.utils)
importPackage(Packages.arc.struct)
importPackage(Packages.arc.util)
importPackage(Packages.mindustry)
importPackage(Packages.mindustry.ai)
importPackage(Packages.mindustry.content)
importPackage(Packages.mindustry.core)
importPackage(Packages.mindustry.ctype)
importPackage(Packages.mindustry.editor)
importPackage(Packages.mindustry.entities)
importPackage(Packages.mindustry.entities.bullet)
importPackage(Packages.mindustry.entities.effect)
importPackage(Packages.mindustry.entities.traits)
importPackage(Packages.mindustry.entities.type)
importPackage(Packages.mindustry.entities.type.base)
importPackage(Packages.mindustry.entities.units)
importPackage(Packages.mindustry.game)
importPackage(Packages.mindustry.gen)
importPackage(Packages.mindustry.graphics)
importPackage(Packages.mindustry.input)
importPackage(Packages.mindustry.maps)
importPackage(Packages.mindustry.maps.filters)
importPackage(Packages.mindustry.maps.generators)
importPackage(Packages.mindustry.maps.zonegen)
importPackage(Packages.mindustry.type)
importPackage(Packages.mindustry.ui)
importPackage(Packages.mindustry.ui.dialogs)
importPackage(Packages.mindustry.ui.fragments)
importPackage(Packages.mindustry.ui.layout)
importPackage(Packages.mindustry.world)
importPackage(Packages.mindustry.world.blocks)
importPackage(Packages.mindustry.world.blocks.defense)
importPackage(Packages.mindustry.world.blocks.defense.turrets)
importPackage(Packages.mindustry.world.blocks.distribution)
importPackage(Packages.mindustry.world.blocks.liquid)
importPackage(Packages.mindustry.world.blocks.logic)
importPackage(Packages.mindustry.world.blocks.power)
importPackage(Packages.mindustry.world.blocks.production)
importPackage(Packages.mindustry.world.blocks.sandbox)
importPackage(Packages.mindustry.world.blocks.storage)
importPackage(Packages.mindustry.world.blocks.units)
importPackage(Packages.mindustry.world.consumers)
importPackage(Packages.mindustry.world.meta)
importPackage(Packages.mindustry.world.meta.values)
importPackage(Packages.mindustry.world.modules)
importPackage(Packages.mindustry.world.producers)
