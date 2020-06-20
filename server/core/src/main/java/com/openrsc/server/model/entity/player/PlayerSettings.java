package com.openrsc.server.model.entity.player;

import com.openrsc.server.model.PlayerAppearance;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.model.entity.npc.Npc;

import java.util.HashMap;

public class PlayerSettings {

	public static final int PRIVACY_BLOCK_CHAT_MESSAGES = 0,
		PRIVACY_BLOCK_PRIVATE_MESSAGES = 1,
		PRIVACY_BLOCK_TRADE_REQUESTS = 2,
		PRIVACY_BLOCK_DUEL_REQUESTS = 3;

	public static final int GAME_SETTING_AUTO_CAMERA = 0,
		GAME_SETTING_MOUSE_BUTTONS = 1,
		GAME_SETTING_SOUND_EFFECTS = 2;

	private HashMap<Long, Long> attackedBy = new HashMap<Long, Long>();
	private HashMap<Integer, Long> attackedBy2 = new HashMap<Integer, Long>();

	private boolean[] privacySettings = new boolean[4];
	private boolean[] gameSettings = new boolean[3];

	private PlayerAppearance appearance;

	private Player player;

	PlayerSettings(Player player) {
		this.player = player;
	}


	public void setPrivacySetting(int i, boolean b) {
		if (i == 1) {
			if (privacySettings[1] && !b) {
				for (Player pl : player.getWorld().getPlayers()) {
					if (!player.getSocial().isFriendsWith(pl.getUsernameHash())
						&& pl.getSocial().isFriendsWith(player.getUsernameHash())
						&& pl.getIndex() != player.getIndex()) {
						ActionSender.sendFriendUpdate(pl, player.getUsernameHash()
						);
					}
				}
			} else if (!privacySettings[1] && b) {
				for (Player pl : player.getWorld().getPlayers()) {
					if (!player.getSocial().isFriendsWith(pl.getUsernameHash())
						&& pl.getSocial().isFriendsWith(player.getUsernameHash())
						&& pl.getIndex() != player.getIndex()) {
						ActionSender.sendFriendUpdate(pl, player.getUsernameHash()
						);
					}
				}
			}
		}
		privacySettings[i] = b;
	}

	public boolean getPrivacySetting(int i) {
		return privacySettings[i];
	}

	public boolean[] getPrivacySettings() {
		return privacySettings;
	}

	public void setPrivacySettings(boolean[] privacySettings) {
		this.privacySettings = privacySettings;
	}

	public boolean getGameSetting(int i) {
		return gameSettings[i];
	}

	public boolean[] getGameSettings() {
		return gameSettings;
	}

	public void setGameSettings(boolean[] gameSettings) {
		this.gameSettings = gameSettings;
	}

	public void setGameSetting(int i, boolean b) {
		gameSettings[i] = b;
	}

	public PlayerAppearance getAppearance() {
		return appearance;
	}

	public void setAppearance(PlayerAppearance pa) {
		this.appearance = pa;
	}

	void addAttackedBy(Player player) {
		attackedBy.put(player.getUsernameHash(), System.currentTimeMillis());
	}
	public void addAttackedBy(Npc n) {
		attackedBy2.put(n.getID(), System.currentTimeMillis());
	}

	HashMap<Long, Long> getAttackedBy() {
		return attackedBy;
	}
	HashMap<Integer, Long> getAttackedBy2() {
		return attackedBy2;
	}

	public long lastAttackedBy(Player player) {
		Long time = attackedBy.get(player.getUsernameHash());
		if (time != null) {
			return time;
		}
		return 0;
	}
	long lastAttackedBy(Npc n) {
		Long time = attackedBy2.get(n.getID());
		if (time != null) {
			return time;
		}
		return 0;
	}
}
