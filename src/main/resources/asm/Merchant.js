var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')
var AbstractInsnNode = Java.type('org.objectweb.asm.tree.AbstractInsnNode')
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode')
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode')
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode')
var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode')
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode')

function initializeCoreMod() {
    return {
    	'Merchant': {
    		'target': {
    			'type': 'CLASS',
    			'name': 'net.minecraft.world.inventory.MerchantMenu'
    		},
    		'transformer': function(classNode) {
    			var count = 0
    			var fn = "playTradeSound"
    			for (var i = 0; i < classNode.methods.size(); ++i) {
    				var obj = classNode.methods.get(i)
    				if (obj.name == fn) {
    					patch_playSound(obj)
    					count++
    				}
    			}
    			if (count < 1)
    				asmapi.log("ERROR", "Failed to modify MerchantMenu: Method not found")
    			return classNode;
    		}
    	}
    }
}

// add the test: if (!(this.trader instanceof Entity)) return;
function patch_playSound(obj) {
	var f1 = "trader"
	var n1 = "net/minecraft/world/entity/Entity"
	var node = asmapi.findFirstInstruction(obj, opc.CHECKCAST)
	if (node && node.desc == n1) {
		var node2 = node.getPrevious()
		if (node2 && node2.getOpcode() == opc.GETFIELD && node2.name == f1)
		{
			var op5 = new LabelNode()
			var op1 = new InsnNode(opc.DUP)
			var op2 = new TypeInsnNode(opc.INSTANCEOF, n1)
			var op3 = new JumpInsnNode(opc.IFNE, op5)
			var op4 = new InsnNode(opc.RETURN)
			var list = asmapi.listOf(op1, op2, op3, op4, op5)
			obj.instructions.insert(node2, list)
		}
		else
			asmapi.log("ERROR", "Failed to modify MerchantMenu: GETFIELD not found")
	}
	else
		asmapi.log("ERROR", "Failed to modify MerchantMenu: CHECKCAST not found")
}
