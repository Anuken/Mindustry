const print = function(obj){
    java.lang.System.out.println(obj ? String(obj) : "null")
}

const extend = function(classType, name, params){
    return new JavaAdapter(classType, params, name)
}
