const print = function(obj){
    java.lang.System.out.println(obj ? String(obj) : "null")
}

const extend = function(classType, params){
    return new JavaAdapter(classType, params)
}