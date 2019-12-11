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
const newEffect = (lifetime, renderer) => new Effects.Effect(lifetime, new Effects.EffectRenderer({render: renderer}))
const Calls = Packages.io.anuke.mindustry.gen.Call
importPackage(Packages.io.anuke.arc)
importPackage(Packages.io.anuke.arc.collection)
importPackage(Packages.io.anuke.arc.func)
importPackage(Packages.io.anuke.arc.graphics)
importPackage(Packages.io.anuke.arc.graphics.g2d)
importPackage(Packages.io.anuke.arc.math)
importPackage(Packages.io.anuke.arc.scene)
importPackage(Packages.io.anuke.arc.scene.actions)
importPackage(Packages.io.anuke.arc.scene.event)
importPackage(Packages.io.anuke.arc.scene.style)
importPackage(Packages.io.anuke.arc.scene.ui)
importPackage(Packages.io.anuke.arc.scene.ui.layout)
importPackage(Packages.io.anuke.arc.scene.utils)
importPackage(Packages.io.anuke.arc.util)
importPackage(Packages.io.anuke.mindustry)
importPackage(Packages.io.anuke.mindustry.ai)
importPackage(Packages.io.anuke.mindustry.content)
importPackage(Packages.io.anuke.mindustry.core)
importPackage(Packages.io.anuke.mindustry.ctype)
importPackage(Packages.io.anuke.mindustry.editor)
importPackage(Packages.io.anuke.mindustry.entities)
importPackage(Packages.io.anuke.mindustry.entities.bullet)
importPackage(Packages.io.anuke.mindustry.entities.effect)
importPackage(Packages.io.anuke.mindustry.entities.traits)
importPackage(Packages.io.anuke.mindustry.entities.type)
importPackage(Packages.io.anuke.mindustry.entities.type.base)
importPackage(Packages.io.anuke.mindustry.entities.units)
importPackage(Packages.io.anuke.mindustry.game)
importPackage(Packages.io.anuke.mindustry.gen)
importPackage(Packages.io.anuke.mindustry.graphics)
importPackage(Packages.io.anuke.mindustry.input)
importPackage(Packages.io.anuke.mindustry.maps)
importPackage(Packages.io.anuke.mindustry.maps.filters)
importPackage(Packages.io.anuke.mindustry.maps.generators)
importPackage(Packages.io.anuke.mindustry.maps.zonegen)
importPackage(Packages.io.anuke.mindustry.type)
importPackage(Packages.io.anuke.mindustry.ui)
importPackage(Packages.io.anuke.mindustry.ui.dialogs)
importPackage(Packages.io.anuke.mindustry.ui.fragments)
importPackage(Packages.io.anuke.mindustry.ui.layout)
importPackage(Packages.io.anuke.mindustry.world)
importPackage(Packages.io.anuke.mindustry.world.blocks)
importPackage(Packages.io.anuke.mindustry.world.blocks.defense)
importPackage(Packages.io.anuke.mindustry.world.blocks.defense.turrets)
importPackage(Packages.io.anuke.mindustry.world.blocks.distribution)
importPackage(Packages.io.anuke.mindustry.world.blocks.liquid)
importPackage(Packages.io.anuke.mindustry.world.blocks.logic)
importPackage(Packages.io.anuke.mindustry.world.blocks.power)
importPackage(Packages.io.anuke.mindustry.world.blocks.production)
importPackage(Packages.io.anuke.mindustry.world.blocks.sandbox)
importPackage(Packages.io.anuke.mindustry.world.blocks.storage)
importPackage(Packages.io.anuke.mindustry.world.blocks.units)
importPackage(Packages.io.anuke.mindustry.world.consumers)
importPackage(Packages.io.anuke.mindustry.world.meta)
importPackage(Packages.io.anuke.mindustry.world.meta.values)
importPackage(Packages.io.anuke.mindustry.world.modules)
importPackage(Packages.io.anuke.mindustry.world.producers)
