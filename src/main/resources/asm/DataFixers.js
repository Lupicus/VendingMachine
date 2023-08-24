var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode')

function initializeCoreMod() {
    return {
    	'DataFixers': {
    		'target': {
    			'type': 'CLASS',
    			'name': 'net.minecraft.util.datafix.DataFixers'
    		},
    		'transformer': function(classNode) {
    			var count = 0
    			var fn = asmapi.mapMethod('m_274588_') // createFixerUpper
    			for (var i = 0; i < classNode.methods.size(); ++i) {
    				var obj = classNode.methods.get(i)
    				if (obj.name == fn) {
    					patch_m_274588_(obj)
    					count++
    				}
    			}
    			if (count < 1)
    				asmapi.log("ERROR", "Failed to modify DataFixers: Method not found")
    			return classNode;
    		}
    	}
    }
}

// add hook
function patch_m_274588_(obj) {
	var n1 = asmapi.mapMethod('m_14513_') // addFixers
	var o1 = "net/minecraft/util/datafix/DataFixers"
	var d1 = "(Lcom/mojang/datafixers/DataFixerBuilder;)V"
	var node = asmapi.findFirstMethodCall(obj, asmapi.MethodType.STATIC, o1, n1, d1)
	if (node) {
		var o2 = "com/lupicus/vm/datafix/ModFixers"
		var n2 = "apply"
		var d2 = "(Lcom/mojang/datafixers/DataFixerBuilder;)V"
		var op2 = asmapi.buildMethodCall(o2, n2, d2, asmapi.MethodType.STATIC)
		var op1 = new VarInsnNode(opc.ALOAD, 1) // datafixerbuilder
		var list = asmapi.listOf(op1, op2)
		obj.instructions.insert(node, list)
	}
	else
		asmapi.log("ERROR", "Failed to modify DataFixers: call not found")
}
