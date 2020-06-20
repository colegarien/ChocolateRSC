package com.openrsc.server.plugins.npcs;

import com.openrsc.server.constants.IronmanMode;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.external.CerterDef;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.TalkNpcTrigger;
import com.openrsc.server.util.rsc.DataConversions;

import static com.openrsc.server.plugins.Functions.*;

public class Certer implements TalkNpcTrigger {

	//official certers
	int[] certers = new int[]{NpcId.GILES.id(), NpcId.MILES.id(), NpcId.NILES.id(), NpcId.JINNO.id(), NpcId.WATTO.id(),
			NpcId.OWEN.id(), NpcId.CHUCK.id(), NpcId.ORVEN.id(), NpcId.PADIK.id(), NpcId.SETH.id()};
	//forester is custom certer if wc guild enabled
	//sidney smith is in its own file

	@Override
	public void onTalkNpc(Player player, final Npc n) {

		// Forester (Log certer; custom)
		if ((n.getID() == NpcId.FORESTER.id())
			&& !config().WANT_WOODCUTTING_GUILD) {
			return;
		}

		final CerterDef certerDef = player.getWorld().getServer().getEntityHandler().getCerterDef(n.getID());
		if (certerDef == null) {
			return;
		}

		beginCertExchange(certerDef, player, n);
	}

	private void beginCertExchange(CerterDef certerDef, Player player, Npc n) {
		npcsay(player, n, "Welcome to my " + certerDef.getType()
			+ " exchange stall");

		String ending = (n.getID() == NpcId.MILES.id() || n.getID() == NpcId.CHUCK.id() || n.getID() == NpcId.WATTO.id() ? "s" : "");

		// First Certer Menu
		int firstType = firstMenu(certerDef, ending, player, n);
		switch(firstType) {
			case 0:
				say(player, n, "I have some certificates to trade in");
				break;
			case 1:
				say(player, n, "I have some " + certerDef.getType() + ending + " to trade in");
				break;
			//case 2 handled separately
		}

		int secondType = -1;

		//informational only
		if (firstType != 2) {
			// Second Certer Menu
			secondType = secondMenu(certerDef, ending, player, n, firstType);
		}

		// Final Certer Menu
		switch (firstType) {
			case 0: //cert to item
				decertMenu(certerDef, ending, player, n, secondType);
				break;
			case 1: //item to cert
				certMenu(certerDef, ending, player, n, secondType);
				break;
			case 2: //informational
				infMenu(certerDef, ending, player, n);
				break;
		}
	}

	private int firstMenu(CerterDef certerDef, String ending, Player player, Npc n) {
		return multi(player, n, false, "I have some certificates to trade in",
				"I have some " + certerDef.getType() + ending + " to trade in",
				"What is a " + certerDef.getType() + " exchange stall?");
	}

	private int secondMenu(CerterDef certerDef, String ending, Player player, Npc n, int option) {
		if (option == -1)
			return -1;

		final String[] names = certerDef.getCertNames();
		switch(option) {
			case 0:
				player.message("what sort of certificate do you wish to trade in?");
				return multi(player, n, false, names);
			case 1:
				player.message("what sort of " + certerDef.getType() + ending + " do you wish to trade in?");
				return multi(player, n, false, names);
			default:
				return -1;
		}
	}

	private void decertMenu(CerterDef certerDef, String ending, Player player, Npc n, int index) {
		final String[] names = certerDef.getCertNames();
		player.message("How many certificates do you wish to trade in?");
		int certAmount;
		if (config().WANT_CERTER_BANK_EXCHANGE) {
			certAmount = multi(player, n, false, "One", "two", "Three", "four",
				"five", "All to bank");
		} else {
			certAmount = multi(player, n, false, "One", "two", "Three", "four", "five");
		}
		if (certAmount < 0)
			return;
		int certID = certerDef.getCertID(index);
		if (certID < 0) {
			return;
		}
		int itemID = certerDef.getItemID(index);
		if (certAmount == 5) {
			if (player.isIronMan(IronmanMode.Ultimate.id())) {
				player.message("As an Ultimate Iron Man. you cannot use certer bank exchange.");
				return;
			}
			certAmount = player.getCarriedItems().getInventory().countId(certID);
			if (certAmount <= 0) {
				player.message("You don't have any " + names[index]
					+ " certificates");
				return;
			}
			Item bankItem = new Item(itemID, certAmount * 5);
			if (player.getCarriedItems().remove(new Item(certID, certAmount)) > -1) {
				player.message("You exchange the certificates, "
					+ bankItem.getAmount() + " "
					+ bankItem.getDef(player.getWorld()).getName()
					+ " is added to your bank");
				player.getBank().add(bankItem, false);
			}
		} else {
			certAmount += 1;
			int itemAmount = certAmount * 5;
			if (player.getCarriedItems().getInventory().countId(certID) < certAmount) {
				player.message("You don't have that many certificates");
				return;
			}
			if (player.getCarriedItems().remove(new Item(certID, certAmount)) > -1) {
				player.message("You exchange your certificates for "
					+ certerDef.getType() + ending);
				for (int x = 0; x < itemAmount; x++) {
					player.getCarriedItems().getInventory().add(new Item(itemID, 1));
				}
			}
		}
	}

	private void certMenu(CerterDef certerDef, String ending, Player player, Npc n, int index) {
		final String[] names = certerDef.getCertNames();
		player.message("How many " + certerDef.getType() + ending
			+ " do you wish to trade in?");
		int certAmount;
		if (config().WANT_CERTER_BANK_EXCHANGE) {
			certAmount = multi(player, n, false, "five", "ten", "Fifteen", "Twenty", "Twentyfive",
					"All from bank");
		} else {
			certAmount = multi(player, n, false, "five", "ten", "Fifteen", "Twenty", "Twentyfive");
		}
		if (certAmount < 0)
			return;
		int certID = certerDef.getCertID(index);
		if (certID < 0) {
			return;
		}
		int itemID = certerDef.getItemID(index);
		if (certAmount == 5) {
			if (player.isIronMan(IronmanMode.Ultimate.id())) {
				player.message("As an Ultimate Iron Man. you cannot use certer bank exchange.");
				return;
			}
			certAmount = (int) (player.getBank().countId(itemID) / 5);
			int itemAmount = certAmount * 5;
			if (itemAmount <= 0) {
				player.message("You don't have any " + names[index] + " to cert");
				return;
			}

			if (!player.getBank().remove(new Item(itemID, itemAmount), false)) {
				player.message("You exchange the " + certerDef.getType() + ", "
					+ itemAmount + " "
					+ player.getWorld().getServer().getEntityHandler().getItemDef(itemID).getName()
					+ " is taken from your bank");
				player.getCarriedItems().getInventory().add(new Item(certID, certAmount));
			}
		} else {
			certAmount += 1;
			int itemAmount = certAmount * 5;
			if (player.getCarriedItems().getInventory().countId(itemID) < itemAmount) {
				player.message("You don't have that " + (ending.equals("") ? "much" : "many")
						+ " " + certerDef.getType() + ending);
				return;
			}
			player.message("You exchange your " + certerDef.getType() + ending
				+ " for certificates");
			for (int x = 0; x < itemAmount; x++) {
				player.getCarriedItems().remove(new Item(itemID));
			}
			player.getCarriedItems().getInventory().add(new Item(certID, certAmount));
		}
	}

	private void infMenu(CerterDef certerDef, String ending, Player player, Npc n) {
		String item;
		switch(certerDef.getType()) {
			case "ore":
				item = "ores";
				break;
			case "bar":
				item = "bars";
				break;
			case "fish":
				item = "fish";
				break;
			case "log":
				item = "logs";
				break;
			default:
				item = certerDef.getType();
				break;
		}
		say(player, n, "What is a " + certerDef.getType() + " exchange store?");
		npcsay(player, n, "You may exchange your " + item + " here",
				"For certificates which are light and easy to carry",
				"You can carry many of these certificates at once unlike " + item,
				"5 " + item + " will give you one certificate",
				"You may also redeem these certificates here for " + item + " again",
				"The advantage of doing this is",
				"You can trade large amounts of " + item + " with other players quickly and safely");
	}

	@Override
	public boolean blockTalkNpc(Player player, Npc n) {
		return (DataConversions.inArray(certers, n.getID())) || (n.getID() == NpcId.FORESTER.id() && player.getConfig().WANT_WOODCUTTING_GUILD);
	}
}
