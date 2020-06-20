package com.openrsc.server.net.rsc.handlers;

import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.update.ChatMessage;
import com.openrsc.server.model.snapshot.Chatlog;
import com.openrsc.server.net.Packet;
import com.openrsc.server.net.rsc.PacketHandler;
import com.openrsc.server.database.impl.mysql.queries.logging.ChatLog;
import com.openrsc.server.util.rsc.DataConversions;

public final class ChatHandler implements PacketHandler {

	public void handlePacket(Packet packet, Player sender) throws Exception {
		if (sender.isMuted()) {
			sender.message(sender.getConfig().MESSAGE_PREFIX + "You are muted " + (sender.getMuteExpires() == -1 ? "@red@permanently" : "for @cya@" + sender.getMinutesMuteLeft() + "@whi@ minutes."));
		}

		if (!sender.hasElevatedPriveledges() && sender.getLocation().onTutorialIsland()) {
			sender.message("Once you finish the tutorial, typing here sends messages to nearby players");
		}

		String message = DataConversions.upperCaseAllFirst(
			DataConversions.stripBadCharacters(
				DataConversions.getEncryptedString(packet, Short.MAX_VALUE)));

		ChatMessage chatMessage = new ChatMessage(sender, message);
		sender.getUpdateFlags().setChatMessage(chatMessage);

		// We do not want muted/tutorial chat to be logged
		if(sender.getLocation().onTutorialIsland() || sender.isMuted()) {
			return;
		}

		sender.getWorld().getServer().getGameLogger().addQuery(new ChatLog(sender.getWorld(), sender.getUsername(), chatMessage.getMessageString()));
		sender.getWorld().addEntryToSnapshots(new Chatlog(sender.getUsername(), chatMessage.getMessageString()));
	}
}
