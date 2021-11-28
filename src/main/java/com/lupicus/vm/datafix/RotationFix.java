package com.lupicus.vm.datafix;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

import net.minecraft.util.datafix.fixes.References;

public class RotationFix extends DataFix
{
	private static final Map<String, String> RENAMES = ImmutableMap.<String, String>builder()
			.put("north", "south").put("south", "north").put("west", "east")
			.put("east", "west").build();

	public RotationFix(Schema schema, boolean p_16192_) {
		super(schema, p_16192_);
	}

	private static Dynamic<?> fix(Dynamic<?> p_16196_) {
		Optional<String> optional = p_16196_.get("Name").asString().result();
		return optional.equals(Optional.of("vm:vending_machine")) ? p_16196_.update("Properties", (p_16198_) -> {
			String s = p_16198_.get("facing").asString("");
			return p_16198_.set("facing", p_16198_.createString(RENAMES.getOrDefault(s, "north")));
		}) : p_16196_;
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return this.fixTypeEverywhereTyped("vm_rotation_fix", this.getInputSchema().getType(References.BLOCK_STATE),
				(p_16194_) -> {
					return p_16194_.update(DSL.remainderFinder(), RotationFix::fix);
				});
	}
}
