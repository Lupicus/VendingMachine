var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')

function initializeCoreMod() {
    return {
    	'CreativeModeTab': {
    		'target': {
    			'type': 'CLASS',
    			'name': 'net.minecraft.world.item.CreativeModeTab'
    		},
    		'transformer': function(classNode) {
    			var count = 0
    			var fn = asmapi.mapMethod('m_269498_') // buildContents
    			for (var i = 0; i < classNode.methods.size(); ++i) {
    				var obj = classNode.methods.get(i)
    				if (obj.name == fn) {
    					patch_m_269498_(obj)
    					count++
    				}
    			}
    			if (count < 1)
    				asmapi.log("ERROR", "Failed to modify CreativeModeTab: Method not found")
    			return classNode;
    		}
    	}
    }
}

// change owner class to ours
function patch_m_269498_(obj) {
	var node = asmapi.findFirstInstruction(obj, opc.INVOKESTATIC)
	while (node && node.name != 'onCreativeModeTabBuildContents') {
		var index = obj.instructions.indexOf(node)
		node = asmapi.asmapi.findFirstInstructionAfter(obj, opc.INVOKESTATIC, index + 1)
	}
	if (node) {
		if (node.owner == 'net/minecraftforge/client/ForgeHooksClient')
			node.owner = 'com/lupicus/vm/hook/ForgeHooks'
	}
	else
		asmapi.log("ERROR", "Failed to modify CreativeModeTab: INVOKESTATIC not found")
}
