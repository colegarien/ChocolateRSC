package com.openrsc.server.plugins.itemactions;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.UsePlayerTrigger;

import static com.openrsc.server.plugins.Functions.*;

public class ArravInvUseOnPlayer implements UsePlayerTrigger {

	@Override
	public boolean blockUsePlayer(Player player, Player otherPlayer, Item item) {
		if (item.getCatalogId() == ItemId.BROKEN_SHIELD_ARRAV_1.id() || item.getCatalogId() == ItemId.BROKEN_SHIELD_ARRAV_2.id()) {
			return true;
		}
		if (item.getCatalogId() == ItemId.PHOENIX_GANG_WEAPON_KEY.id()) {
			return true;
		}
		if (item.getCatalogId() == ItemId.CERTIFICATE.id()) {
			return true;
		}
		if (item.getCatalogId() == ItemId.CANDLESTICK.id()) {
			return true;
		}
		if (item.getCatalogId() == ItemId.MISCELLANEOUS_KEY.id()) {
			return true;
		}
		return false;
	}

	@Override
	public void onUsePlayer(Player player, Player otherPlayer, Item item) {
		if (item.getCatalogId() == ItemId.MISCELLANEOUS_KEY.id()
			|| item.getCatalogId() == ItemId.CANDLESTICK.id()
			|| item.getCatalogId() == ItemId.CERTIFICATE.id()
			|| item.getCatalogId() == ItemId.BROKEN_SHIELD_ARRAV_1.id()
			|| item.getCatalogId() == ItemId.BROKEN_SHIELD_ARRAV_2.id()
			|| item.getCatalogId() == ItemId.PHOENIX_GANG_WEAPON_KEY.id()) {
			if (otherPlayer.getCarriedItems().getInventory().full()) {
				player.message("Other player doesn't have enough inventory space to receive the object");
				return;
			}
			player.resetPath();
			otherPlayer.resetPath();
			player.getCarriedItems().remove(new Item(item.getCatalogId()));
			give(otherPlayer, item.getCatalogId(), 1);
			mes(0, "You give the " + item.getDef(player.getWorld()).getName() + " to " + otherPlayer.getUsername());
			otherPlayer.message(player.getUsername() + " has given you a " + item.getDef(player.getWorld()).getName());
		}
	}
}
