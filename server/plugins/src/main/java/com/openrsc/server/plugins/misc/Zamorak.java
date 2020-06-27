package com.openrsc.server.plugins.misc;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.constants.Skills;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.update.ChatMessage;
import com.openrsc.server.plugins.triggers.*;

import static com.openrsc.server.plugins.Functions.*;

public class Zamorak implements TalkNpcTrigger, TakeObjTrigger, AttackNpcTrigger, PlayerRangeNpcTrigger, SpellNpcTrigger {

	@Override
	public void onTakeObj(Player owner, GroundItem item) {
		if (item.getID() == ItemId.WINE_OF_ZAMORAK.id() && item.getX() == 333 && item.getY() == 434) {
			Npc zam = ifnearvisnpc(owner, 7, NpcId.MONK_OF_ZAMORAK.id(), NpcId.MONK_OF_ZAMORAK_MACE.id());
			if (zam != null && !zam.inCombat()) {
				owner.face(zam);
				zam.face(owner);
				applyCurse(owner, zam);
			}
			else {
				owner.groundItemTake(item);
			}
		}
	}

	@Override
	public boolean blockTakeObj(Player player, GroundItem i) {
		return i.getID() == ItemId.WINE_OF_ZAMORAK.id();
	}

	@Override
	public boolean blockAttackNpc(Player player, Npc n) {
		return n.getID() == NpcId.MONK_OF_ZAMORAK.id() || n.getID() == NpcId.MONK_OF_ZAMORAK_MACE.id();
	}

	@Override
	public void onAttackNpc(Player player, Npc zamorak) {
		if (zamorak.getID() == NpcId.MONK_OF_ZAMORAK.id() || zamorak.getID() == NpcId.MONK_OF_ZAMORAK_MACE.id()) {
			applyCurse(player, zamorak);
		}
	}

	@Override
	public boolean blockSpellNpc(Player player, Npc n) {
		return n.getID() == NpcId.MONK_OF_ZAMORAK.id() || n.getID() == NpcId.MONK_OF_ZAMORAK_MACE.id();
	}

	@Override
	public void onSpellNpc(Player player, Npc zamorak) {
		if (zamorak.getID() == NpcId.MONK_OF_ZAMORAK.id() || zamorak.getID() == NpcId.MONK_OF_ZAMORAK_MACE.id()) {
			applyCurse(player, zamorak);
		}
	}

	@Override
	public boolean blockPlayerRangeNpc(Player player, Npc n) {
		return n.getID() == NpcId.MONK_OF_ZAMORAK.id() || n.getID() == NpcId.MONK_OF_ZAMORAK_MACE.id();
	}

	@Override
	public void onPlayerRangeNpc(Player player, Npc zamorak) {
		if (zamorak.getID() == NpcId.MONK_OF_ZAMORAK.id() || zamorak.getID() == NpcId.MONK_OF_ZAMORAK_MACE.id()) {
			applyCurse(player, zamorak);
		}
	}

	private void applyCurse(Player player, Npc zam) {
		zam.getUpdateFlags().setChatMessage(new ChatMessage(zam, "A curse be upon you", player));
		delay(2200);
		player.message("You feel slightly weakened");
		int dmg = (int) Math.ceil(((player.getSkills().getMaxStat(Skills.HITS) + 20) * 0.05));
		player.damage(dmg);
		int[] stats = {Skills.ATTACK, Skills.DEFENSE, Skills.STRENGTH};
		for(int affectedStat : stats) {
			/* How much to lower the stat */
			int lowerBy = (int) Math.ceil(((player.getSkills().getMaxStat(affectedStat) + 20) * 0.05));
			/* New current level */
			final int newStat = Math.max(0, player.getSkills().getLevel(affectedStat) - lowerBy);
			player.getSkills().setLevel(affectedStat, newStat);
		}
		delay(config().GAME_TICK);
		zam.setChasing(player);
	}

	@Override
	public boolean blockTalkNpc(Player player, Npc n) {
		return n.getID() == NpcId.MONK_OF_ZAMORAK.id() || n.getID() == NpcId.MONK_OF_ZAMORAK_MACE.id();
	}

	@Override
	public void onTalkNpc(Player player, Npc n) {
		if (n.getID() == NpcId.MONK_OF_ZAMORAK.id() || n.getID() == NpcId.MONK_OF_ZAMORAK_MACE.id()) {
			if (n.getID() == NpcId.MONK_OF_ZAMORAK.id()) {
				npcsay(player, n, "Save your speech for the altar");
			} else {
				npcsay(player, n, "Who are you to dare speak to the servants of Zamorak ?");
			}
			n.setChasing(player);
		}
	}
}
