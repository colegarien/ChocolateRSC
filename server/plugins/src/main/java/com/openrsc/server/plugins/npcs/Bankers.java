package com.openrsc.server.plugins.npcs;

import com.openrsc.server.constants.IronmanMode;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.plugins.triggers.OpNpcTrigger;
import com.openrsc.server.plugins.triggers.TalkNpcTrigger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.openrsc.server.plugins.Functions.*;

public class Bankers implements TalkNpcTrigger, OpNpcTrigger {
	private static final Logger LOGGER = LogManager.getLogger(Bankers.class);
	public static int[] BANKERS = {NpcId.BANKER_GEN1.id(), NpcId.FAIRY_BANKER.id(), NpcId.BANKER_GEN2.id(),
		NpcId.GNOME_BANKER.id(), NpcId.JUNGLE_BANKER.id()};

	@Override
	public boolean blockTalkNpc(final Player player, final Npc npc) {
		if (inArray(npc.getID(), BANKERS)) {
			return true;
		}
		return false;
	}

	@Override
	public void onTalkNpc(Player player, final Npc npc) {
		if (npc.getID() == NpcId.GNOME_BANKER.id()) {
			say(player, npc, "hello");
			npcsay(player, npc, "good day to you sir");
		} else {
			npcsay(player, npc, "Good day" + (npc.getID() == NpcId.JUNGLE_BANKER.id() ? " Bwana" : "") + ", how may I help you?");
		}

		int menu;

		if (config().SPAWN_AUCTION_NPCS && config().WANT_BANK_PINS)
			menu = multi(player, npc,
				"I'd like to access my bank account please",
				"What is this place?",
				"I'd like to talk about bank pin",
				"I'd like to collect my items from auction");
		else if (config().WANT_BANK_PINS)
			menu = multi(player, npc,
				"I'd like to access my bank account please",
				"What is this place?",
				"I'd like to talk about bank pin");
		else if (config().SPAWN_AUCTION_NPCS)
			menu = multi(player, npc,
				"I'd like to access my bank account please",
				"What is this place?",
				"I'd like to collect my items from auction");
		else
			menu = multi(player, npc,
				"I'd like to access my bank account please",
				"What is this place?");

		if (menu == 0) {
			if (player.isIronMan(IronmanMode.Ultimate.id())) {
				player.message("As an Ultimate Iron Man, you cannot use the bank.");
				return;
			}

			if(validatebankpin(player)) {
				if (npc.getID() == NpcId.GNOME_BANKER.id()) {
					npcsay(player, npc, "absolutely sir");
				} else {
					npcsay(player, npc, "Certainly " + (player.isMale() ? "Sir" : "Miss"));
				}
				player.setAccessingBank(true);
				ActionSender.showBank(player);
			}
		} else if (menu == 1) {
			if (npc.getID() == NpcId.GNOME_BANKER.id()) {
				npcsay(player, npc, "well it's the tree gnome bank off course", "a lot of custom passes through here",
					"so a bank is essential in encouraging visitors");
			} else {
				npcsay(player, npc, "This is a branch of the bank of Runescape", "We have branches in many towns");
				int branchMenu = multi(player, npc, false, //do not send over
					"And what do you do?",
					"Didn't you used to be called the bank of Varrock");
				if (branchMenu == 0) {
					say(player, npc, "And what do you do?");
					npcsay(player, npc, "We will look after your items and money for you",
						"So leave your valuables with us if you want to keep them safe");
				} else if (branchMenu == 1) {
					say(player, npc, "Didn't you used to be called the bank of Varrock?");
					npcsay(player, npc, "Yes we did, but people kept on coming into our branches outside of varrock",
						"And telling us our signs were wrong",
						"As if we didn't know what town we were in or something!");
				}
			}
		} else if (menu == 2 && config().WANT_BANK_PINS) {
			int bankPinMenu = multi(player, "Set a bank pin", "Change bank pin", "Delete bank pin");
			if (bankPinMenu == 0) {
				setbankpin(player);
			} else if (bankPinMenu == 1) {
				changebankpin(player);
			} else if (bankPinMenu == 2) {
				removebankpin(player);
			}
		} else if ((menu == 2 || menu == 3) && config().SPAWN_AUCTION_NPCS) {
			if(validatebankpin(player)) {
				player.getWorld().getMarket().addPlayerCollectItemsTask(player);
			}
		}
	}

	@Override
	public void onOpNpc(Player player, Npc n, String command) {
		if (inArray(n.getID(), BANKERS)) {
			Npc banker = player.getWorld().getNpc(n.getID(),
				player.getX() - 2, player.getX() + 2,
				player.getY() - 2, player.getY() + 2);
			if (banker == null) return;
			if (command.equalsIgnoreCase("Bank") && config().RIGHT_CLICK_BANK) {
				quickFeature(n, player, false);
			} else if (command.equalsIgnoreCase("Collect") && config().SPAWN_AUCTION_NPCS) {
				quickFeature(n, player, true);
			}
		}
	}

	@Override
	public boolean blockOpNpc(Player player, Npc n, String command) {
		if (inArray(n.getID(), BANKERS) && command.equalsIgnoreCase("Bank")) {
			return true;
		}
		if (inArray(n.getID(), BANKERS) && command.equalsIgnoreCase("Collect")) {
			return true;
		}
		return false;
	}

	private void quickFeature(Npc npc, Player player, boolean auction) {
		if (player.isIronMan(IronmanMode.Ultimate.id())) {
			player.message("As an Ultimate Iron Man, you cannot use the bank.");
			return;
		}

		if(validatebankpin(player)) {
			if (auction) {
				player.getWorld().getMarket().addPlayerCollectItemsTask(player);
			} else {
				player.setAccessingBank(true);
				ActionSender.showBank(player);
			}
		}
	}
}
