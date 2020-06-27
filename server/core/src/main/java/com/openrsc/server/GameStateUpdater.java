package com.openrsc.server;

import com.openrsc.server.constants.NpcId;
import com.openrsc.server.database.impl.mysql.queries.logging.PMLog;
import com.openrsc.server.model.GlobalMessage;
import com.openrsc.server.model.PlayerAppearance;
import com.openrsc.server.model.Point;
import com.openrsc.server.model.PrivateMessage;
import com.openrsc.server.model.entity.Entity;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.player.PlayerSettings;
import com.openrsc.server.model.entity.update.*;
import com.openrsc.server.net.PacketBuilder;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.util.rsc.DataConversions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class GameStateUpdater {
	/**
	 * The asynchronous logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger();

	private long lastWorldUpdateDuration = 0;
	private long lastProcessPlayersDuration = 0;
	private long lastProcessNpcsDuration = 0;
	private long lastProcessMessageQueuesDuration = 0;
	private long lastUpdateClientsDuration = 0;
	private long lastDoCleanupDuration = 0;
	private long lastExecuteWalkToActionsDuration = 0;

	private final Server server;
	public final Server getServer() {
		return server;
	}

	public GameStateUpdater(final Server server) {
		this.server = server;
	}

	public void load() {

	}

	public void unload() {
		lastWorldUpdateDuration = 0;
		lastProcessPlayersDuration = 0;
		lastProcessNpcsDuration = 0;
		lastProcessMessageQueuesDuration = 0;
		lastUpdateClientsDuration = 0;
		lastDoCleanupDuration = 0;
		lastExecuteWalkToActionsDuration = 0;
	}

	// private static final int PACKET_UPDATETIMEOUTS = 0;
	public void sendUpdatePackets(final Player player) {
		// TODO: Should be private
		try {
			updatePlayers(player);
			updatePlayerAppearances(player);
			updateNpcs(player);
			updateNpcAppearances(player);
			updateGameObjects(player);
			updateWallObjects(player);
			updateGroundItems(player);
			sendClearLocations(player);
			updateTimeouts(player);
		} catch (final Exception e) {
			LOGGER.catching(e);
			player.unregister(true, "Exception while updating player " + player.getUsername());
		}
	}

	/**
	 * Checks if the player has moved within the last X minutes
	 */
	protected void updateTimeouts(final Player player) {
		final long curTime = System.currentTimeMillis();
		final int timeoutLimit = getServer().getConfig().IDLE_TIMER; // 5 minute idle log out
		final int autoSave = getServer().getConfig().AUTO_SAVE; // 30 second autosave
		if (player.isRemoved() || player.getAttribute("dummyplayer", false)) {
			return;
		}
		if (curTime - player.getLastSaveTime() >= (autoSave) && player.loggedIn()) {
			player.timeIncrementActivity();
			player.save();
			player.setLastSaveTime(curTime);
		}
		if (curTime - player.getLastPing() >= 30000) {
			player.unregister(false, "Ping time-out");
		} else if (player.warnedToMove()) {
			if (curTime - player.getLastMoved() >= (timeoutLimit + 60000) && player.loggedIn() && !player.hasElevatedPriveledges()) {
				player.unregister(true, "Movement time-out");
			} else if (player.hasMoved()) {
				player.setWarnedToMove(false);
			}
		} else if (curTime - player.getLastMoved() >= timeoutLimit && !player.isMod()) {
			if (player.isSleeping()) {
				player.setSleeping(false);
				ActionSender.sendWakeUp(player, false, false);
			}
			player.message("@cya@You have been standing here for " + (timeoutLimit / 60000)
				+ " mins! Please move to a new area");
			player.setWarnedToMove(true);
		}
	}

	protected void updateNpcs(final Player playerToUpdate) {
		final com.openrsc.server.net.PacketBuilder packet = new com.openrsc.server.net.PacketBuilder();
		packet.setID(79);
		packet.startBitAccess();
		packet.writeBits(playerToUpdate.getLocalNpcs().size(), 8);
		for (final Iterator<Npc> it$ = playerToUpdate.getLocalNpcs().iterator(); it$.hasNext(); ) {
			Npc localNpc = it$.next();

			if (!playerToUpdate.withinRange(localNpc) || localNpc.isRemoved() || localNpc.isRespawning() || localNpc.isTeleporting() || localNpc.inCombat()) {
				it$.remove();
				packet.writeBits(1, 1);
				packet.writeBits(1, 1);
				packet.writeBits(3, 2);
			} else {
				if (localNpc.hasMoved()) {
					packet.writeBits(1, 1);
					packet.writeBits(0, 1);
					packet.writeBits(localNpc.getSprite(), 3);
				} else if (localNpc.spriteChanged()) {
					packet.writeBits(1, 1);
					packet.writeBits(1, 1);
					packet.writeBits(localNpc.getSprite(), 4);
				} else {
					packet.writeBits(0, 1);
				}
			}
		}
		for (final Npc newNPC : playerToUpdate.getViewArea().getNpcsInView()) {
			if (playerToUpdate.getLocalNpcs().contains(newNPC) || newNPC.equals(playerToUpdate) || newNPC.isRemoved() || newNPC.isRespawning()
				|| newNPC.getID() == NpcId.NED_BOAT.id() && !playerToUpdate.getCache().hasKey("ned_hired")
				|| !playerToUpdate.withinRange(newNPC, (getServer().getConfig().VIEW_DISTANCE * 8) - 1) || (newNPC.isTeleporting() && !newNPC.inCombat())) {
				continue;
			} else if (playerToUpdate.getLocalNpcs().size() >= 255) {
				break;
			}
			final byte[] offsets = DataConversions.getMobPositionOffsets(newNPC.getLocation(), playerToUpdate.getLocation());
			packet.writeBits(newNPC.getIndex(), 12);
			packet.writeBits(offsets[0], 6);
			packet.writeBits(offsets[1], 6);
			packet.writeBits(newNPC.getSprite(), 4);
			packet.writeBits(newNPC.getID(), 10);

			playerToUpdate.getLocalNpcs().add(newNPC);
		}
		packet.finishBitAccess();
		playerToUpdate.write(packet.toPacket());
	}

	protected void updatePlayers(final Player playerToUpdate) {
		final com.openrsc.server.net.PacketBuilder positionBuilder = new com.openrsc.server.net.PacketBuilder();
		positionBuilder.setID(191);
		positionBuilder.startBitAccess();
		positionBuilder.writeBits(playerToUpdate.getX(), 11);
		positionBuilder.writeBits(playerToUpdate.getY(), 13);
		positionBuilder.writeBits(playerToUpdate.getSprite(), 4);
		positionBuilder.writeBits(playerToUpdate.getLocalPlayers().size(), 8);

		if (playerToUpdate.loggedIn()) {
			for (final Iterator<Player> it$ = playerToUpdate.getLocalPlayers().iterator(); it$.hasNext(); ) {
				final Player otherPlayer = it$.next();

				if (!playerToUpdate.withinRange(otherPlayer) || !otherPlayer.loggedIn() || otherPlayer.isRemoved()
					|| otherPlayer.isTeleporting() || otherPlayer.isInvisibleTo(playerToUpdate)
					|| otherPlayer.inCombat() || otherPlayer.hasMoved()) {
					positionBuilder.writeBits(1, 1); //Needs Update
					positionBuilder.writeBits(1, 1); //Update Type
					positionBuilder.writeBits(3, 2); //???
					it$.remove();
					playerToUpdate.getKnownPlayerAppearanceIDs().remove(otherPlayer.getUsernameHash());
				} else {
					if (!otherPlayer.hasMoved() && !otherPlayer.spriteChanged()) {
						positionBuilder.writeBits(0, 1); //Needs Update
					} else {
						// The player is actually going to be updated
						if (otherPlayer.hasMoved()) {
							positionBuilder.writeBits(1, 1); //Needs Update
							positionBuilder.writeBits(0, 1); //Update Type
							positionBuilder.writeBits(otherPlayer.getSprite(), 3);
						} else if (otherPlayer.spriteChanged()) {
							positionBuilder.writeBits(1, 1); //Needs Update
							positionBuilder.writeBits(1, 1); //Update Type
							positionBuilder.writeBits(otherPlayer.getSprite(), 4);
						}
					}
				}
			}

			for (final Player otherPlayer : playerToUpdate.getViewArea().getPlayersInView()) {
				if (playerToUpdate.getLocalPlayers().contains(otherPlayer) || otherPlayer.equals(playerToUpdate)
					|| !otherPlayer.withinRange(playerToUpdate) || !otherPlayer.loggedIn()
					|| otherPlayer.isRemoved() || otherPlayer.isInvisibleTo(playerToUpdate)
					|| (otherPlayer.isTeleporting() && !otherPlayer.inCombat())) {
					continue;
				}
				final byte[] offsets = DataConversions.getMobPositionOffsets(otherPlayer.getLocation(),
					playerToUpdate.getLocation());
				positionBuilder.writeBits(otherPlayer.getIndex(), 11);
				positionBuilder.writeBits(offsets[0], 6);
				positionBuilder.writeBits(offsets[1], 6);
				positionBuilder.writeBits(otherPlayer.getSprite(), 4);
				playerToUpdate.getLocalPlayers().add(otherPlayer);
				if (playerToUpdate.getLocalPlayers().size() >= 255) {
					break;
				}
			}
		}
		positionBuilder.finishBitAccess();
		playerToUpdate.write(positionBuilder.toPacket());
	}

	public void updateNpcAppearances(final Player player) {
		final ConcurrentLinkedQueue<Damage> npcsNeedingHitsUpdate = new ConcurrentLinkedQueue<>();
		final ConcurrentLinkedQueue<ChatMessage> npcMessagesNeedingDisplayed = new ConcurrentLinkedQueue<>();
		final ConcurrentLinkedQueue<Projectile> npcProjectilesNeedingDisplayed = new ConcurrentLinkedQueue<>();
		final ConcurrentLinkedQueue<Skull> npcSkullsNeedingDisplayed = new ConcurrentLinkedQueue<>();
		final ConcurrentLinkedQueue<Wield> npcWieldsNeedingDisplayed = new ConcurrentLinkedQueue<>();
		final ConcurrentLinkedQueue<BubbleNpc> npcBubblesNeedingDisplayed = new ConcurrentLinkedQueue<>();

		for (final Npc npc : player.getLocalNpcs()) {
			final UpdateFlags updateFlags = npc.getUpdateFlags();
			if (updateFlags.hasChatMessage()) {
				ChatMessage chatMessage = updateFlags.getChatMessage();
				npcMessagesNeedingDisplayed.add(chatMessage);
			}
			if (updateFlags.hasSkulled()) {
				Skull skull = updateFlags.getSkull().get();
				npcSkullsNeedingDisplayed.add(skull);
			}
			if (updateFlags.changedWield()) {
				Wield wield = updateFlags.getWield().get();
				npcWieldsNeedingDisplayed.add(wield);
			}
			if (updateFlags.changedWield2()) {
				Wield wield2 = updateFlags.getWield2().get();
				npcWieldsNeedingDisplayed.add(wield2);
			}
			if (updateFlags.hasTakenDamage()) {
				Damage damage = updateFlags.getDamage().get();
				npcsNeedingHitsUpdate.add(damage);
			}
			if (updateFlags.hasFiredProjectile()) {
				Projectile projectileFired = updateFlags.getProjectile().get();
				npcProjectilesNeedingDisplayed.add(projectileFired);
			}
			if (updateFlags.hasBubbleNpc()) {
					BubbleNpc bubble = updateFlags.getActionBubbleNpc().get();
					npcBubblesNeedingDisplayed.add(bubble);
			}
		}
		final int updateSize = npcMessagesNeedingDisplayed.size() + npcsNeedingHitsUpdate.size()
			+ npcProjectilesNeedingDisplayed.size() + npcSkullsNeedingDisplayed.size() + npcWieldsNeedingDisplayed.size() + npcBubblesNeedingDisplayed.size();
		if (updateSize > 0) {
			final PacketBuilder npcAppearancePacket = new PacketBuilder();
			npcAppearancePacket.setID(104);
			npcAppearancePacket.writeShort(updateSize);

			ChatMessage chatMessage;
			while ((chatMessage = npcMessagesNeedingDisplayed.poll()) != null) {
				npcAppearancePacket.writeShort(chatMessage.getSender().getIndex());
				npcAppearancePacket.writeByte((byte) 1);
				npcAppearancePacket.writeShort(chatMessage.getRecipient() == null ? -1 : chatMessage.getRecipient().getIndex());
				npcAppearancePacket.writeString(chatMessage.getMessageString());
			}
			Damage npcNeedingHitsUpdate;
			while ((npcNeedingHitsUpdate = npcsNeedingHitsUpdate.poll()) != null) {
				npcAppearancePacket.writeShort(npcNeedingHitsUpdate.getIndex());
				npcAppearancePacket.writeByte((byte) 2);
				npcAppearancePacket.writeByte((byte) npcNeedingHitsUpdate.getDamage());
				npcAppearancePacket.writeByte((byte) npcNeedingHitsUpdate.getCurHits());
				npcAppearancePacket.writeByte((byte) npcNeedingHitsUpdate.getMaxHits());
			}
			Projectile projectile;
			while ((projectile = npcProjectilesNeedingDisplayed.poll()) != null) {
				Entity victim = projectile.getVictim();
				if (victim.isNpc()) {
					npcAppearancePacket.writeShort(projectile.getCaster().getIndex());
					npcAppearancePacket.writeByte((byte) 3);
					npcAppearancePacket.writeShort(projectile.getType());
					npcAppearancePacket.writeShort(((Npc) victim).getIndex());
				} else if (victim.isPlayer()) {
					npcAppearancePacket.writeShort(projectile.getCaster().getIndex());
					npcAppearancePacket.writeByte((byte) 4);
					npcAppearancePacket.writeShort(projectile.getType());
					npcAppearancePacket.writeShort(((Player) victim).getIndex());
				}
			}
			Skull npcNeedingSkullUpdate;
			while ((npcNeedingSkullUpdate = npcSkullsNeedingDisplayed.poll()) != null) {
				npcAppearancePacket.writeShort(npcNeedingSkullUpdate.getIndex());
				npcAppearancePacket.writeByte((byte) 5);
				npcAppearancePacket.writeByte((byte) npcNeedingSkullUpdate.getSkull());
			}
			Wield npcNeedingWieldUpdate;
			while ((npcNeedingWieldUpdate = npcWieldsNeedingDisplayed.poll()) != null) {
				npcAppearancePacket.writeShort(npcNeedingWieldUpdate.getIndex());
				npcAppearancePacket.writeByte((byte) 6);
				npcAppearancePacket.writeByte((byte) npcNeedingWieldUpdate.getWield());
				npcAppearancePacket.writeByte((byte) npcNeedingWieldUpdate.getWield2());
			}
			BubbleNpc npcNeedingBubbleUpdate;
			while ((npcNeedingBubbleUpdate = npcBubblesNeedingDisplayed.poll()) != null) {
				npcAppearancePacket.writeShort(npcNeedingBubbleUpdate.getOwner().getIndex());
				npcAppearancePacket.writeByte((byte) 7);
				npcAppearancePacket.writeShort(npcNeedingBubbleUpdate.getID());
			}
			player.write(npcAppearancePacket.toPacket());
		}
	}

	/**
	 * Handles the appearance updating for @param player
	 *
	 * @param player
	 */
	public void updatePlayerAppearances(final Player player) {
		final ArrayDeque<Bubble> bubblesNeedingDisplayed = new ArrayDeque<>();
		final ArrayDeque<ChatMessage> chatMessagesNeedingDisplayed = new ArrayDeque<>();
		final ArrayDeque<Projectile> projectilesNeedingDisplayed = new ArrayDeque<>();
		final ArrayDeque<Damage> playersNeedingDamageUpdate = new ArrayDeque<>();
		final ArrayDeque<HpUpdate> playersNeedingHpUpdate = new ArrayDeque<HpUpdate>();
		final ArrayDeque<Player> playersNeedingAppearanceUpdate = new ArrayDeque<>();

		if (player.getUpdateFlags().hasBubble()) {
			Bubble bubble = player.getUpdateFlags().getActionBubble().get();
			bubblesNeedingDisplayed.add(bubble);
		}
		if (player.getUpdateFlags().hasFiredProjectile()) {
			Projectile projectileFired = player.getUpdateFlags().getProjectile().get();
			projectilesNeedingDisplayed.add(projectileFired);
		}
		if (player.getUpdateFlags().hasChatMessage()) {
			ChatMessage chatMessage = player.getUpdateFlags().getChatMessage();
			chatMessagesNeedingDisplayed.add(chatMessage);
		}
		if (player.getUpdateFlags().hasTakenDamage()) {
			Damage damage = player.getUpdateFlags().getDamage().get();
			playersNeedingDamageUpdate.add(damage);
		}
		if (player.getUpdateFlags().hasTakenHpUpdate()) {
			HpUpdate hpUpdate = player.getUpdateFlags().getHpUpdate().get();
			playersNeedingHpUpdate.add(hpUpdate);
		}
		if (player.getUpdateFlags().hasAppearanceChanged()) {
			playersNeedingAppearanceUpdate.add(player);
		}
		for (final Player otherPlayer : player.getLocalPlayers()) {
			final UpdateFlags updateFlags = otherPlayer.getUpdateFlags();

			if(otherPlayer.getUsername().trim().equalsIgnoreCase("kenix") && player.getUsername().trim().equalsIgnoreCase("kenix")) {
				LOGGER.info("UF: " + updateFlags + ", isTeleporting: " + otherPlayer.isTeleporting() + ", Override: " + player.requiresAppearanceUpdateForPeek(otherPlayer));
			}

			if (updateFlags.hasBubble()) {
				final Bubble bubble = updateFlags.getActionBubble().get();
				bubblesNeedingDisplayed.add(bubble);
			}
			if (updateFlags.hasFiredProjectile()) {
				Projectile projectileFired = updateFlags.getProjectile().get();
				projectilesNeedingDisplayed.add(projectileFired);
			}
			if (updateFlags.hasChatMessage() && !player.getSettings().getPrivacySetting(PlayerSettings.PRIVACY_BLOCK_CHAT_MESSAGES)) {
				ChatMessage chatMessage = updateFlags.getChatMessage();
				chatMessagesNeedingDisplayed.add(chatMessage);
			}
			if (updateFlags.hasTakenDamage()) {
				Damage damage = updateFlags.getDamage().get();
				playersNeedingDamageUpdate.add(damage);
			}
			if (updateFlags.hasTakenHpUpdate()) {
				HpUpdate hpUpdate = updateFlags.getHpUpdate().get();
				playersNeedingHpUpdate.add(hpUpdate);
			}
			if (player.requiresAppearanceUpdateFor(otherPlayer)) {
				playersNeedingAppearanceUpdate.add(otherPlayer);
			}
		}
		issuePlayerAppearanceUpdatePacket(player, bubblesNeedingDisplayed, chatMessagesNeedingDisplayed,
			projectilesNeedingDisplayed, playersNeedingDamageUpdate, playersNeedingHpUpdate, playersNeedingAppearanceUpdate);
	}

	private void issuePlayerAppearanceUpdatePacket(final Player player, final Queue<Bubble> bubblesNeedingDisplayed,
												   final Queue<ChatMessage> chatMessagesNeedingDisplayed, final Queue<Projectile> projectilesNeedingDisplayed,
												   final Queue<Damage> playersNeedingDamageUpdate,final Queue<HpUpdate> playersNeedingHpUpdate,
												   final Queue<Player> playersNeedingAppearanceUpdate) {
		if (player.loggedIn()) {
			final int updateSize = bubblesNeedingDisplayed.size() + chatMessagesNeedingDisplayed.size()
				+ playersNeedingDamageUpdate.size() + projectilesNeedingDisplayed.size()
				+ playersNeedingAppearanceUpdate.size() + playersNeedingHpUpdate.size();

			if (updateSize > 0) {
				final PacketBuilder appearancePacket = new PacketBuilder();
				appearancePacket.setID(234);
				appearancePacket.writeShort(updateSize);
				Bubble b;
				while ((b = bubblesNeedingDisplayed.poll()) != null) {
					appearancePacket.writeShort(b.getOwner().getIndex());
					appearancePacket.writeByte((byte) 0);
					appearancePacket.writeShort(b.getID());
				}
				ChatMessage cm;
				while ((cm = chatMessagesNeedingDisplayed.poll()) != null) {
					Player sender = (Player) cm.getSender();
					boolean tutorialPlayer = sender.getLocation().onTutorialIsland() && !sender.hasElevatedPriveledges();
					boolean muted = sender.isMuted();

					int chatType = cm.getRecipient() == null ? (tutorialPlayer || muted ? 7 : 1)
						: cm.getRecipient() instanceof Player ? (tutorialPlayer || muted ? 7 : 6) : 6;
					appearancePacket.writeShort(cm.getSender().getIndex());
					appearancePacket.writeByte(chatType);

					if (chatType == 1 || chatType == 7) {
						if (cm.getSender() != null && cm.getSender() instanceof Player)
							appearancePacket.writeInt(sender.getIcon());
					}

					if (chatType == 7) {
						appearancePacket.writeByte(sender.isMuted() ? 1 : 0);
						appearancePacket.writeByte(sender.getLocation().onTutorialIsland() ? 1 : 0);
					}

					if (chatType != 7 || player.isAdmin()) {
						appearancePacket.writeString(cm.getMessageString());
					} else {
						appearancePacket.writeString("");
					}
				}
				Damage playerNeedingHitsUpdate;
				while ((playerNeedingHitsUpdate = playersNeedingDamageUpdate.poll()) != null) {
					appearancePacket.writeShort(playerNeedingHitsUpdate.getIndex());
					appearancePacket.writeByte((byte) 2);
					appearancePacket.writeByte((byte) playerNeedingHitsUpdate.getDamage());
					appearancePacket.writeByte((byte) playerNeedingHitsUpdate.getCurHits());
					appearancePacket.writeByte((byte) playerNeedingHitsUpdate.getMaxHits());
				}
				Projectile projectile;
				while ((projectile = projectilesNeedingDisplayed.poll()) != null) {
					Entity victim = projectile.getVictim();
					if (victim.isNpc()) {
						appearancePacket.writeShort(projectile.getCaster().getIndex());
						appearancePacket.writeByte((byte) 3);
						appearancePacket.writeShort(projectile.getType());
						appearancePacket.writeShort(((Npc) victim).getIndex());
					} else if (victim.isPlayer()) {
						appearancePacket.writeShort(projectile.getCaster().getIndex());
						appearancePacket.writeByte((byte) 4);
						appearancePacket.writeShort(projectile.getType());
						appearancePacket.writeShort(((Player) victim).getIndex());
					}
				}
				Player playerNeedingAppearanceUpdate;
				while ((playerNeedingAppearanceUpdate = playersNeedingAppearanceUpdate.poll()) != null) {
					PlayerAppearance appearance = playerNeedingAppearanceUpdate.getSettings().getAppearance();

					appearancePacket.writeShort((short) playerNeedingAppearanceUpdate.getIndex());
					appearancePacket.writeByte((byte) 5);
					//appearancePacket.writeShort(0);
					appearancePacket.writeString(playerNeedingAppearanceUpdate.getUsername());
					//appearancePacket.writeString(playerNeedingAppearanceUpdate.getUsername());

					appearancePacket.writeByte((byte) playerNeedingAppearanceUpdate.getWornItems().length);
					for (int i : playerNeedingAppearanceUpdate.getWornItems()) {
						appearancePacket.writeShort(i);
					}
					appearancePacket.writeByte(appearance.getHairColour());
					appearancePacket.writeByte(appearance.getTopColour());
					appearancePacket.writeByte(appearance.getTrouserColour());
					appearancePacket.writeByte(appearance.getSkinColour());
					appearancePacket.writeByte((byte) playerNeedingAppearanceUpdate.getCombatLevel());
					appearancePacket.writeByte((byte) (playerNeedingAppearanceUpdate.getSkullType()));

					if (playerNeedingAppearanceUpdate.getClan() != null) {
						appearancePacket.writeByte(1);
						appearancePacket.writeString(playerNeedingAppearanceUpdate.getClan().getClanTag());
					} else {
						appearancePacket.writeByte(0);
					}

					appearancePacket.writeByte(playerNeedingAppearanceUpdate.stateIsInvisible() ? 1 : 0);
					appearancePacket.writeByte(playerNeedingAppearanceUpdate.stateIsInvulnerable() ? 1 : 0);
					appearancePacket.writeByte(playerNeedingAppearanceUpdate.getGroupID());
					appearancePacket.writeInt(playerNeedingAppearanceUpdate.getIcon());
				}
				HpUpdate playerNeedingHpUpdate;
				while ((playerNeedingHpUpdate = playersNeedingHpUpdate.poll()) != null) {
					appearancePacket.writeShort(playerNeedingHpUpdate.getIndex());
					appearancePacket.writeByte((byte) 9);
					appearancePacket.writeByte((byte) playerNeedingHpUpdate.getCurHits());
					appearancePacket.writeByte((byte) playerNeedingHpUpdate.getMaxHits());
				}

				player.write(appearancePacket.toPacket());
			}
		}
	}

	protected void updateGameObjects(final Player playerToUpdate) {
		boolean changed = false;
		final PacketBuilder packet = new PacketBuilder();
		packet.setID(48);
		// TODO: This is not handled correctly.
		//       According to RSC+ replays, the server never tells the client to unload objects until
		//       a region is unloaded. It then instructs the client to only unload the region.
		for (final Iterator<GameObject> it$ = playerToUpdate.getLocalGameObjects().iterator(); it$.hasNext(); ) {
			final GameObject o = it$.next();
			if (!playerToUpdate.withinGridRange(o) || o.isRemoved() || o.isInvisibleTo(playerToUpdate)) {
				final int offsetX = o.getX() - playerToUpdate.getX();
				final int offsetY = o.getY() - playerToUpdate.getY();
				//If the object is close enough we can use regular way to remove:
				if (offsetX > -128 && offsetY > -128 && offsetX < 128 && offsetY < 128) {
					packet.writeShort(60000);
					packet.writeByte(offsetX);
					packet.writeByte(offsetY);
					packet.writeByte(o.getDirection());
					it$.remove();
					changed = true;
				} else {
					//If it's not close enough we need to use the region clean packet
					playerToUpdate.getLocationsToClear().add(o.getLocation());
					it$.remove();
					changed = true;
				}
			}
		}

		for (final GameObject newObject : playerToUpdate.getViewArea().getGameObjectsInView()) {
			if (!playerToUpdate.withinGridRange(newObject) || newObject.isRemoved()
				|| newObject.isInvisibleTo(playerToUpdate) || newObject.getType() != 0
				|| playerToUpdate.getLocalGameObjects().contains(newObject)) {
				continue;
			}
			packet.writeShort(newObject.getID());
			final int offsetX = newObject.getX() - playerToUpdate.getX();
			final int offsetY = newObject.getY() - playerToUpdate.getY();
			packet.writeByte(offsetX);
			packet.writeByte(offsetY);
			packet.writeByte(newObject.getDirection());
			playerToUpdate.getLocalGameObjects().add(newObject);
			changed = true;
		}
		if (changed)
			playerToUpdate.write(packet.toPacket());
	}

	protected void updateGroundItems(final Player playerToUpdate) {
		boolean changed = false;
		final PacketBuilder packet = new PacketBuilder();
		packet.setID(99);
		for (final Iterator<GroundItem> it$ = playerToUpdate.getLocalGroundItems().iterator(); it$.hasNext(); ) {
			final GroundItem groundItem = it$.next();
			final int offsetX = (groundItem.getX() - playerToUpdate.getX());
			final int offsetY = (groundItem.getY() - playerToUpdate.getY());

			if (!playerToUpdate.withinGridRange(groundItem)) {
				if (offsetX > -128 && offsetY > -128 && offsetX < 128 && offsetY < 128) {
					packet.writeByte(255);
					packet.writeByte(offsetX);
					packet.writeByte(offsetY);
					if (getServer().getConfig().WANT_BANK_NOTES)
						packet.writeByte(groundItem.getNoted() ? 1 : 0);
				} else {
					playerToUpdate.getLocationsToClear().add(groundItem.getLocation());
				}
				it$.remove();
				changed = true;
			} else if (groundItem.isRemoved() || groundItem.isInvisibleTo(playerToUpdate)) {
				packet.writeShort(groundItem.getID() + 32768);
				packet.writeByte(offsetX);
				packet.writeByte(offsetY);
				if (getServer().getConfig().WANT_BANK_NOTES)
					packet.writeByte(groundItem.getNoted() ? 1 : 0);
				//System.out.println("Removing " + groundItem + " with isRemoved() remove: " + offsetX + ", " + offsetY);
				it$.remove();
				changed = true;
			}
		}

		for (final GroundItem groundItem : playerToUpdate.getViewArea().getItemsInView()) {
			if (!playerToUpdate.withinGridRange(groundItem) || groundItem.isRemoved()
				|| groundItem.isInvisibleTo(playerToUpdate)
				|| playerToUpdate.getLocalGroundItems().contains(groundItem)) {
				continue;
			}
			packet.writeShort(groundItem.getID());
			final int offsetX = groundItem.getX() - playerToUpdate.getX();
			final int offsetY = groundItem.getY() - playerToUpdate.getY();
			packet.writeByte(offsetX);
			packet.writeByte(offsetY);
			if (getServer().getConfig().WANT_BANK_NOTES) {
				packet.writeByte(groundItem.getNoted() ? 1 : 0);
			}
			playerToUpdate.getLocalGroundItems().add(groundItem);
			changed = true;
		}
		if (changed) {
			playerToUpdate.write(packet.toPacket());
		}
	}

	protected void updateWallObjects(final Player playerToUpdate) {
		boolean changed = false;
		final PacketBuilder packet = new PacketBuilder();
		packet.setID(91);

		for (final Iterator<GameObject> it$ = playerToUpdate.getLocalWallObjects().iterator(); it$.hasNext(); ) {
			final GameObject o = it$.next();
			if (!playerToUpdate.withinGridRange(o) || (o.isRemoved() || o.isInvisibleTo(playerToUpdate))) {
				final int offsetX = o.getX() - playerToUpdate.getX();
				final int offsetY = o.getY() - playerToUpdate.getY();
				if (offsetX > -128 && offsetY > -128 && offsetX < 128 && offsetY < 128) {
					packet.writeShort(60000);
					packet.writeByte(offsetX);
					packet.writeByte(offsetY);
					packet.writeByte(o.getDirection());
					it$.remove();
					changed = true;
				} else {
					playerToUpdate.getLocationsToClear().add(o.getLocation());
					it$.remove();
					changed = true;
				}
			}
		}
		for (final GameObject newObject : playerToUpdate.getViewArea().getGameObjectsInView()) {
			if (!playerToUpdate.withinGridRange(newObject) || newObject.isRemoved()
				|| newObject.isInvisibleTo(playerToUpdate) || newObject.getType() != 1
				|| playerToUpdate.getLocalWallObjects().contains(newObject)) {
				continue;
			}

			final int offsetX = newObject.getX() - playerToUpdate.getX();
			final int offsetY = newObject.getY() - playerToUpdate.getY();
			packet.writeShort(newObject.getID());
			packet.writeByte(offsetX);
			packet.writeByte(offsetY);
			packet.writeByte(newObject.getDirection());
			playerToUpdate.getLocalWallObjects().add(newObject);
			changed = true;
		}
		if (changed) {
			playerToUpdate.write(packet.toPacket());
		}
	}

	protected void sendClearLocations(final Player player) {
		if (player.getLocationsToClear().size() > 0) {
			final PacketBuilder packetBuilder = new PacketBuilder(211);
			for (final Point point : player.getLocationsToClear()) {
				final int offsetX = point.getX() - player.getX();
				final int offsetY = point.getY() - player.getY();
				packetBuilder.writeShort(offsetX);
				packetBuilder.writeShort(offsetY);
			}
			player.getLocationsToClear().clear();
			player.write(packetBuilder.toPacket());
		}
	}

	public long doUpdates() {
		final long gameStateStart = System.currentTimeMillis();
		lastWorldUpdateDuration = updateWorld();
		lastProcessPlayersDuration = processPlayers();
		lastProcessNpcsDuration = processNpcs();
		lastProcessMessageQueuesDuration = processMessageQueues();
		lastUpdateClientsDuration = updateClients();
		lastDoCleanupDuration = doCleanup();
		lastExecuteWalkToActionsDuration = executeWalkToActions();
		final long gameStateEnd = System.currentTimeMillis();

		return gameStateEnd - gameStateStart;
	}

	protected final long updateWorld() {
		final long updateWorldStart = System.currentTimeMillis();
		getServer().getWorld().run();
		final long updateWorldEnd = System.currentTimeMillis();
		return updateWorldEnd - updateWorldStart;
	}

	protected final long updateClients() {
		final long updateClientsStart	= System.currentTimeMillis();
		for (final Player player : getServer().getWorld().getPlayers()) {
			sendUpdatePackets(player);
			player.process();
		}
		final long updateClientsEnd		= System.currentTimeMillis();
		return updateClientsEnd - updateClientsStart;
	}

	protected final long doCleanup() {// it can do the teleport at this time.
		final long doCleanupStart	= System.currentTimeMillis();

		/*
		 * Reset the update related flags and unregister npcs flagged as
		 * unregistering
		 */
		for (final Npc npc : getServer().getWorld().getNpcs()) {
			npc.setHasMoved(false);
			npc.resetSpriteChanged();
			npc.getUpdateFlags().reset();
			npc.setTeleporting(false);
		}

		/*
		 * Reset the update related flags and unregister players that are
		 * flagged as unregistered
		 */
		for (final Player player : getServer().getWorld().getPlayers()) {
			player.setTeleporting(false);
			player.resetSpriteChanged();
			player.getUpdateFlags().reset();
			player.setHasMoved(false);
		}

		final long doCleanupEnd	= System.currentTimeMillis();

		return doCleanupEnd - doCleanupStart;
	}

	protected final long executeWalkToActions() {
		final long executeWalkToActionsStart	= System.currentTimeMillis();
		for (final Player player : getServer().getWorld().getPlayers()) {
			if (player.getWalkToAction() != null) {
				if (player.getWalkToAction().shouldExecute()) {
					player.getWalkToAction().execute();
				}
			}
		}
		final long executeWalkToActionsEnd	= System.currentTimeMillis();
		return executeWalkToActionsEnd - executeWalkToActionsStart;
	}

	protected final long processNpcs() {
		final long processNpcsStart	= System.currentTimeMillis();
		for (final Npc n : getServer().getWorld().getNpcs()) {
			try {
				if (n.isUnregistering()) {
					getServer().getWorld().unregisterNpc(n);
					continue;
				}

				// Only do the walking tick here if the NPC's walking tick matches the game tick
				if(!getServer().getConfig().WANT_CUSTOM_WALK_SPEED) {
					n.updatePosition();
				}
			} catch (final Exception e) {
				LOGGER.error("Error while updating " + n + " at position " + n.getLocation() + " loc: " + n.getLoc());
				LOGGER.catching(e);
			}
		}
		final long processNpcsEnd = System.currentTimeMillis();
		return processNpcsEnd - processNpcsStart;
	}

	/**
	 * Updates the messages queues for each player
	 */
	protected final long processMessageQueues() {
		final long processMessageQueuesStart = System.currentTimeMillis();
		for (final Player player : getServer().getWorld().getPlayers()) {
			final PrivateMessage pm = player.getNextPrivateMessage();
			if (pm != null) {
				Player affectedPlayer = getServer().getWorld().getPlayer(pm.getFriend());
				if (affectedPlayer != null) {
					if ((affectedPlayer.getSocial().isFriendsWith(player.getUsernameHash()) || !affectedPlayer.getSettings()
						.getPrivacySetting(PlayerSettings.PRIVACY_BLOCK_PRIVATE_MESSAGES))
						&& !affectedPlayer.getSocial().isIgnoring(player.getUsernameHash()) || player.isMod()) {
						ActionSender.sendPrivateMessageSent(player, affectedPlayer.getUsernameHash(), pm.getMessage(), false);
						ActionSender.sendPrivateMessageReceived(affectedPlayer, player, pm.getMessage(), false);
					}

					player.getWorld().getServer().getGameLogger().addQuery(new PMLog(player.getWorld(), player.getUsername(), pm.getMessage(),
						DataConversions.hashToUsername(pm.getFriend())));
				}
			}
		}
		GlobalMessage gm ;
		while((gm = getServer().getWorld().getNextGlobalMessage()) != null) {
			for (final Player player : getServer().getWorld().getPlayers()) {
				if (player == gm.getPlayer()) {
					ActionSender.sendPrivateMessageSent(gm.getPlayer(), -1L, gm.getMessage(), true);
				}
				else if (!player.getSettings().getPrivacySetting(PlayerSettings.PRIVACY_BLOCK_PRIVATE_MESSAGES)
						&& !player.getSocial().isIgnoring(gm.getPlayer().getUsernameHash()) || gm.getPlayer().isMod()) {
					ActionSender.sendPrivateMessageReceived(player, gm.getPlayer(), gm.getMessage(), true);
				}
			}
		}
		for (final Player player : getServer().getWorld().getPlayers()) {
			if (player.requiresOfferUpdate()) {
				ActionSender.sendTradeItems(player);
				player.setRequiresOfferUpdate(false);
			}
		}
		final long processMessageQueuesEnd	= System.currentTimeMillis();
		return processMessageQueuesEnd - processMessageQueuesStart;
	}

	/**
	 * Update the position of players, and check if who (and what) they are
	 * aware of needs updated
	 */
	protected final long processPlayers() {
		final long processPlayersStart	= System.currentTimeMillis();
		for (final Player player : getServer().getWorld().getPlayers()) {
			// Checking login because we don't want to unregister more than once
			if (player.isUnregistering() && player.isLoggedIn()) {
				getServer().getWorld().unregisterPlayer(player);
				continue;
			}

			// Only do the walking tick here if the Players' walking tick matches the game tick
			if(!getServer().getConfig().WANT_CUSTOM_WALK_SPEED) {
				player.updatePosition();
			}

			if (player.getUpdateFlags().hasAppearanceChanged()) {
				player.incAppearanceID();
			}
		}
		final long processPlayersEnd	= System.currentTimeMillis();
		return processPlayersEnd - processPlayersStart;
	}

	public long getLastWorldUpdateDuration() {
		return lastWorldUpdateDuration;
	}

	public long getLastProcessPlayersDuration() {
		return lastProcessPlayersDuration;
	}

	public long getLastProcessNpcsDuration() {
		return lastProcessNpcsDuration;
	}

	public long getLastProcessMessageQueuesDuration() {
		return lastProcessMessageQueuesDuration;
	}

	public long getLastUpdateClientsDuration() {
		return lastUpdateClientsDuration;
	}

	public long getLastDoCleanupDuration() {
		return lastDoCleanupDuration;
	}

	public long getLastExecuteWalkToActionsDuration() {
		return lastExecuteWalkToActionsDuration;
	}
}
