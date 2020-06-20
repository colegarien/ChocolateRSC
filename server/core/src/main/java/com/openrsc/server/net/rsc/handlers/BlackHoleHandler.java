package com.openrsc.server.net.rsc.handlers;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.net.Packet;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.net.rsc.PacketHandler;

import java.util.Optional;

public class BlackHoleHandler implements PacketHandler {

	@Override
	public void handlePacket(Packet packet, Player player) throws Exception {
		if (player == null) {
			return;
		}
		if (player.getLocation().onBlackHole()) {
			if (player.isBusy()) {
				if (player.inCombat()) {
					player.message("You cannot do that whilst fighting!");
				}
				return;
			}
			player.teleport(311, 3348);
			player.message("you return to the dwarven mines");
			if (player.getCarriedItems().hasCatalogID(ItemId.DISK_OF_RETURNING.id(), Optional.of(false))) {
				player.getCarriedItems().remove(new Item(ItemId.DISK_OF_RETURNING.id()));
				player.message("consuming your disk of returning");
			}
			ActionSender.sendPlayerOnBlackHole(player);
		}
	}
}
