package com.openrsc.server.database.impl.mysql;

import com.openrsc.server.Server;
import com.openrsc.server.content.achievement.Achievement;
import com.openrsc.server.content.achievement.AchievementReward;
import com.openrsc.server.content.achievement.AchievementTask;
import com.openrsc.server.database.GameDatabase;
import com.openrsc.server.database.GameDatabaseException;
import com.openrsc.server.database.struct.*;
import com.openrsc.server.external.GameObjectLoc;
import com.openrsc.server.external.ItemLoc;
import com.openrsc.server.external.NPCLoc;
import com.openrsc.server.model.container.BankPreset;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.container.ItemStatus;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.util.rsc.DataConversions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

public class MySqlGameDatabase extends GameDatabase {

	private final MySqlGameDatabaseConnection connection;
	private final MySqlQueries queries;
	private final Set<Integer> itemIDList;

	public MySqlGameDatabase(final Server server) {
		super(server);
		connection = new MySqlGameDatabaseConnection(getServer());
		queries = new MySqlQueries(getServer());
		itemIDList = Collections.synchronizedSortedSet(new TreeSet<Integer>());
	}

	public void openInternal() {
		getConnection().open();
	}

	public void closeInternal() {
		getConnection().close();
	}

	protected void startTransaction() throws GameDatabaseException {
		try {
			getConnection().executeQuery("START TRANSACTION");
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	protected void commitTransaction() throws GameDatabaseException {
		try {
			getConnection().executeQuery("COMMIT");
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	protected void rollbackTransaction() throws GameDatabaseException {
		try {
			getConnection().executeQuery("ROLLBACK");
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	public void initializeOnlinePlayers() throws GameDatabaseException {
		executeUpdateQuery(getQueries().initializeOnlineUsers);
	}

	protected void executeBatchUpdateQuery(String query, Object[][] records) throws GameDatabaseException {
		try {
			final PreparedStatement statement = getConnection().prepareStatement(query);
			for (Object[] parameters : records) {
				// TODO this is copy+pasted from CreateStatement!
				Object[] unpackedParameters = unpackParameters(parameters);

				int parameterIndex = 1;
				for (Object parameter : unpackedParameters) {
					if (parameter instanceof Integer) {
						statement.setInt(parameterIndex, (Integer) parameter);
					} else if (parameter instanceof Long) {
						statement.setLong(parameterIndex, (Long) parameter);
					} else if (parameter instanceof Boolean) {
						statement.setBoolean(parameterIndex, (Boolean) parameter);
					} else if (parameter instanceof String) {
						statement.setString(parameterIndex, (String) parameter);
					} else if (parameter instanceof Byte[]) {
						statement.setBlob(parameterIndex, new javax.sql.rowset.serial.SerialBlob((byte[]) parameter));
					} else {
						throw new GameDatabaseException(this, "Unknown Parameter(" + parameterIndex + ") Type \"" + parameter.getClass().getName() + "\"");
					}

					parameterIndex++;
				}

				statement.addBatch();
			}
			try {
				statement.executeBatch();
			} finally {
				statement.close();
			}

		} catch (SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	protected int executeUpdateQuery(String query, Object... parameters) throws GameDatabaseException {
		try {
			PreparedStatement statement = createStatement(query, parameters);

			int lastInsertId = -1;
			try {
				statement.executeUpdate();

				final ResultSet resultSet = statement.getGeneratedKeys();
				try {
					if (resultSet.next()) {
						lastInsertId = resultSet.getInt(1);
					}
				} finally {
					resultSet.close();
				}
			} finally {
				statement.close();
			}

			return lastInsertId;
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	private PreparedStatement createStatement(String query, Object... parameters) throws GameDatabaseException, SQLException {
		final PreparedStatement statement = getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		Object[] unpackedParameters = unpackParameters(parameters);

		int parameterIndex = 1;
		for (Object parameter : unpackedParameters) {
			if (parameter instanceof Integer) {
				statement.setInt(parameterIndex, (Integer) parameter);
			} else if (parameter instanceof Long) {
				statement.setLong(parameterIndex, (Long) parameter);
			} else if (parameter instanceof Boolean) {
				statement.setBoolean(parameterIndex, (Boolean) parameter);
			} else if (parameter instanceof String) {
				statement.setString(parameterIndex, (String) parameter);
			} else if (parameter instanceof byte[]) {
				statement.setBlob(parameterIndex, new javax.sql.rowset.serial.SerialBlob((byte[]) parameter));
			} else {
				throw new GameDatabaseException(this, "Unknown Parameter(" + parameterIndex + ") Type \"" + parameter.getClass().getName() + "\"");
			}

			parameterIndex++;
		}

		return statement;
	}

	private Object[] unpackParameters(Object[] parameters) {
		boolean hasPackedParameter = Arrays.stream(parameters).anyMatch(p -> p instanceof Object[]);
		if (!hasPackedParameter) {
			return parameters;
		}

		ArrayList<Object> unpackedParameters = new ArrayList<Object>();
		for (Object parameter : parameters) {
			if (parameter instanceof Object[] && !(parameter instanceof Byte[])) {
				unpackedParameters.addAll(Arrays.asList(unpackParameters((Object[]) parameter)));
			} else {
				unpackedParameters.add(parameter);
			}
		}

		return unpackedParameters.toArray();
	}

	@Override
	protected boolean queryPlayerExists(int playerId) throws GameDatabaseException {
		try {
			return hasNextFromInt(getQueries().playerExists, playerId);
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected boolean queryPlayerExists(String username) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().userToId, username);
			final ResultSet result = statement.executeQuery();
			try {
				boolean playerExists = result.isBeforeFirst();
				return playerExists;
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected int queryPlayerIdFromUsername(String username) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().userToId, username);
			final ResultSet result = statement.executeQuery();
			try {
				if (result.next()) {
					return result.getInt("id");
				}
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
		return -1;
	}

	@Override
	protected String queryUsernameFromPlayerId(int playerId) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().idToUser, playerId);
			final ResultSet result = statement.executeQuery();
			try {
				if (result.next()) {
					return result.getString("username");
				}
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
		return null;
	}

	@Override
	protected String queryBanPlayer(String userNameToBan, Player bannedBy, long bannedForMinutes) throws GameDatabaseException {
		final String replyMessage;

		if (bannedForMinutes == -1) {
			executeUpdateQuery(getQueries().banPlayer, bannedForMinutes, userNameToBan);
			replyMessage = userNameToBan + " has been banned permanently";
		} else if (bannedForMinutes == 0) {
			executeUpdateQuery(getQueries().unbanPlayer, userNameToBan);
			replyMessage = userNameToBan + " has been unbanned.";
		} else {
			executeUpdateQuery(getQueries().banPlayer, (System.currentTimeMillis() + (bannedForMinutes * 60000)), userNameToBan);
			replyMessage = userNameToBan + " has been banned for " + bannedForMinutes + " minutes";
		}

		return replyMessage;
	}

	@Override
	protected NpcLocation[] queryNpcLocations() throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().npcLocs);
			final ResultSet result = statement.executeQuery();

			final ArrayList<NpcLocation> npcLocs = new ArrayList<>();
			try {
				while (result.next()) {
					NpcLocation npcLocation = new NpcLocation();
					npcLocation.id = result.getInt("id");
					npcLocation.startX = result.getInt("startX");
					npcLocation.minX = result.getInt("minX");
					npcLocation.maxX = result.getInt("maxX");
					npcLocation.startY = result.getInt("startY");
					npcLocation.minY = result.getInt("minY");
					npcLocation.maxY = result.getInt("maxY");

					npcLocs.add(npcLocation);
				}
				return npcLocs.toArray(new NpcLocation[npcLocs.size()]);
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected SceneryObject[] queryObjects() throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().objects);
			final ResultSet result = statement.executeQuery();

			final ArrayList<SceneryObject> objects = new ArrayList<>();
			try {
				while (result.next()) {
					SceneryObject object = new SceneryObject();
					object.x = result.getInt("x");
					object.y = result.getInt("y");
					object.id = result.getInt("id");
					object.direction = result.getInt("direction");
					object.type = result.getInt("type");

					objects.add(object);
				}
				return objects.toArray(new SceneryObject[objects.size()]);
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected FloorItem[] queryGroundItems() throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().groundItems);
			final ResultSet result = statement.executeQuery();

			final ArrayList<FloorItem> groundItems = new ArrayList<>();
			try {
				while (result.next()) {
					FloorItem groundItem = new FloorItem();
					groundItem.id = result.getInt("id");
					groundItem.x = result.getInt("x");
					groundItem.y = result.getInt("y");
					groundItem.amount = result.getInt("amount");
					groundItem.respawn = result.getInt("respawn");

					groundItems.add(groundItem);
				}
				return groundItems.toArray(new FloorItem[groundItems.size()]);
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected Integer[] queryInUseItemIds() throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().inUseItemIds);
			final ResultSet result = statement.executeQuery();

			final ArrayList<Integer> inUseItemIds = new ArrayList<>();
			while (result.next()) {
				inUseItemIds.add(result.getInt("itemID"));
			}
			return inUseItemIds.toArray(new Integer[inUseItemIds.size()]);
		} catch (SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected void queryAddDropLog(ItemDrop drop) throws GameDatabaseException {
		executeUpdateQuery(getQueries().dropLogInsert, drop.itemId, drop.playerId, drop.amount, drop.npcId);
	}

	@Override
	protected PlayerLoginData queryPlayerLoginData(String username) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().playerLoginData, username);
			final ResultSet playerSet = statement.executeQuery();

			final PlayerLoginData loginData = new PlayerLoginData();

			if (!playerSet.first()) {
				return null;
			}

			try {
				loginData.id = playerSet.getInt("id");
				loginData.groupId = playerSet.getInt("group_id");
				loginData.password = playerSet.getString("pass");
				loginData.salt = playerSet.getString("salt");
				loginData.banned = playerSet.getLong("banned");
			} finally {
				playerSet.close();
				statement.close();
			}

			return loginData;
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected void queryCreatePlayer(String username, String email, String password, long creationDate, String ip) throws GameDatabaseException {
		executeUpdateQuery(getQueries().createPlayer, username, email, password, System.currentTimeMillis() / 1000, ip);
	}

	@Override
	protected boolean queryRecentlyRegistered(String ipAddress) throws GameDatabaseException {
		try {
			PreparedStatement statement = createStatement(getQueries().recentlyRegistered, ipAddress, (System.currentTimeMillis() / 1000) - 60);
			ResultSet result = statement.executeQuery();

			try {
				if (result.next()) {
					return true;
				}
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
		return false;
	}

	@Override
	protected void queryInitializeStats(int playerId) throws GameDatabaseException {
		executeUpdateQuery(getQueries().initStats, playerId);
	}

	@Override
	protected void queryInitializeExp(int playerId) throws GameDatabaseException {
		executeUpdateQuery(getQueries().initExp, playerId);
	}

	@Override
	protected PlayerData queryLoadPlayerData(Player player) throws GameDatabaseException {
		try {
			PlayerData playerData = new PlayerData();

			final PreparedStatement statement = createStatement(getQueries().playerData, player.getUsername());
			final ResultSet result = statement.executeQuery();

			try {
				if (!result.next()) {
					result.close();
					return null;
				}
				playerData.playerId = result.getInt("id");
				playerData.groupId = result.getInt("group_id");
				playerData.combatStyle = (byte) result.getInt("combatstyle");
				playerData.combatLevel = result.getInt("combat");
				playerData.totalLevel = result.getInt("skill_total");
				playerData.loginDate = result.getLong("login_date");
				playerData.loginIp = result.getString("login_ip");
				playerData.xLocation = result.getInt("x");
				playerData.yLocation = result.getInt("y");

				playerData.fatigue = result.getInt("fatigue");
				playerData.kills = result.getInt("kills");
				playerData.deaths = result.getInt("deaths");
				playerData.npcKills = result.getInt("npc_kills");
				if (server.getConfig().SPAWN_IRON_MAN_NPCS) {
					playerData.ironMan = result.getInt("iron_man");
					playerData.ironManRestriction = result.getInt("iron_man_restriction");
					playerData.hcIronManDeath = result.getInt("hc_ironman_death");
				}
				playerData.questPoints = result.getShort("quest_points");

				playerData.blockChat = result.getInt("block_chat") == 1;
				playerData.blockPrivate = result.getInt("block_private") == 1;
				playerData.blockTrade = result.getInt("block_trade") == 1;
				playerData.blockDuel = result.getInt("block_duel") == 1;

				playerData.cameraAuto = result.getInt("cameraauto") == 1;
				playerData.oneMouse = result.getInt("onemouse") == 1;
				playerData.soundOff = result.getInt("soundoff") == 1;

				playerData.bankSize = result.getInt("bank_size");
				playerData.muteExpires = result.getLong("muted");

				playerData.hairColour = result.getInt("haircolour");
				playerData.topColour = result.getInt("topcolour");
				playerData.trouserColour = result.getInt("trousercolour");
				playerData.skinColour = result.getInt("skincolour");
				playerData.headSprite = result.getInt("headsprite");
				playerData.bodySprite = result.getInt("bodysprite");

				playerData.male = result.getInt("male") == 1;
			} finally {
				result.close();
				statement.close();
			}
			return playerData;
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected PlayerInventory[] queryLoadPlayerInvItems(Player player) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().playerInvItems, player.getDatabaseID());
			final ResultSet result = statement.executeQuery();
			final ArrayList<PlayerInventory> list = new ArrayList<>();

			try {
				while (result.next()) {
					PlayerInventory invItem = new PlayerInventory();
					invItem.itemId = result.getInt("itemId");
					invItem.slot = result.getInt("slot");
					invItem.item = new Item(result.getInt("catalogId"));
					invItem.item.getItemStatus().setAmount(result.getInt("amount"));
					invItem.item.getItemStatus().setNoted(result.getInt("noted") == 1);
					invItem.item.getItemStatus().setWielded(result.getInt("wielded") == 1);
					invItem.item.getItemStatus().setDurability(result.getInt("durability"));
					list.add(invItem);
				}
			} finally {
				result.close();
				statement.close();
			}

			return list.toArray(new PlayerInventory[list.size()]);
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected PlayerEquipped[] queryLoadPlayerEquipped(Player player) throws GameDatabaseException {
		try {
			final ArrayList<PlayerEquipped> list = new ArrayList<>();

			if (getServer().getConfig().WANT_EQUIPMENT_TAB) {
				final PreparedStatement statement = createStatement(getQueries().playerEquipped, player.getDatabaseID());
				final ResultSet result = statement.executeQuery();

				try {
					while (result.next()) {
						final PlayerEquipped equipped = new PlayerEquipped();
						equipped.itemId = result.getInt("itemId");
						ItemStatus itemStatus = new ItemStatus();
						itemStatus.setCatalogId(result.getInt("catalogId"));
						itemStatus.setAmount(result.getInt("amount"));
						itemStatus.setNoted(result.getInt("noted") == 1);
						itemStatus.setWielded(result.getInt("wielded") == 1);
						itemStatus.setDurability(result.getInt("durability"));
						equipped.itemStatus = itemStatus;

						list.add(equipped);
					}
				} finally {
					result.close();
					statement.close();
				}
			}

			return list.toArray(new PlayerEquipped[list.size()]);
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected PlayerBank[] queryLoadPlayerBankItems(Player player) throws GameDatabaseException {
		try {
			final ArrayList<PlayerBank> list = new ArrayList<>();

			final PreparedStatement statement = createStatement(getQueries().playerBankItems, player.getDatabaseID());
			final ResultSet result = statement.executeQuery();

			try {
				while (result.next()) {
					final PlayerBank bankItem = new PlayerBank();
					bankItem.itemId = result.getInt("itemId");
					ItemStatus itemStatus = new ItemStatus();
					itemStatus.setCatalogId(result.getInt("catalogId"));
					itemStatus.setAmount(result.getInt("amount"));
					itemStatus.setNoted(result.getInt("noted") == 1);
					itemStatus.setWielded(result.getInt("wielded") == 1);
					itemStatus.setDurability(result.getInt("durability"));
					bankItem.itemStatus = itemStatus;

					list.add(bankItem);
				}
			} finally {
				result.close();
				statement.close();
			}

			return list.toArray(new PlayerBank[list.size()]);
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected PlayerBankPreset[] queryLoadPlayerBankPresets(Player player) throws GameDatabaseException {
		try {
			ArrayList<PlayerBankPreset> list = new ArrayList<>();

			if (getServer().getConfig().WANT_BANK_PRESETS) {
				final PreparedStatement statement = createStatement(getQueries().playerBankPresets, player.getDatabaseID());
				final ResultSet result = statement.executeQuery();

				try {
					while (result.next()) {
						final PlayerBankPreset bankPreset = new PlayerBankPreset();
						bankPreset.slot = result.getInt("slot");

						InputStream readBlob = result.getBlob("inventory").getBinaryStream();
						ByteArrayOutputStream buffer = new ByteArrayOutputStream();
						int nRead;

						byte[] data = new byte[1024];
						while ((nRead = readBlob.read(data, 0, data.length)) != -1) {
							buffer.write(data, 0, nRead);
						}
						buffer.flush();
						readBlob.close();
						bankPreset.inventory = buffer.toByteArray();

						readBlob = result.getBlob("equipment").getBinaryStream();
						buffer = new ByteArrayOutputStream();

						data = new byte[1024];
						while ((nRead = readBlob.read(data, 0, data.length)) != -1) {
							buffer.write(data, 0, nRead);
						}
						buffer.flush();
						readBlob.close();
						bankPreset.equipment = buffer.toByteArray();

						list.add(bankPreset);
					}
				} finally {
					result.close();
					statement.close();
				}
			}

			return list.toArray(new PlayerBankPreset[list.size()]);
		} catch (final SQLException | IOException ex) {
			// We want to trigger a rollback so sending out the GameDatabaseException
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected PlayerFriend[] queryLoadPlayerFriends(Player player) throws GameDatabaseException {
		try {
			final ArrayList<PlayerFriend> list = new ArrayList<>();
			final PreparedStatement statement = createStatement(getQueries().playerFriends, player.getDatabaseID());

			final List<Long> friends = longListFromResultSet(statement.executeQuery(), "friend");

			try {
				for (int i = 0; i < friends.size(); i++) {
					final PlayerFriend friend = new PlayerFriend();
					friend.playerHash = friends.get(i);

					list.add(friend);
				}
			} finally {
				statement.close();
			}

			return list.toArray(new PlayerFriend[list.size()]);
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected PlayerIgnore[] queryLoadPlayerIgnored(Player player) throws GameDatabaseException {
		try {
			final ArrayList<PlayerIgnore> list = new ArrayList<>();
			final PreparedStatement statement = createStatement(getQueries().playerIgnored, player.getDatabaseID());

			final List<Long> friends = longListFromResultSet(statement.executeQuery(), "ignore");

			try {
				for (int i = 0; i < friends.size(); i++) {
					final PlayerIgnore ignore = new PlayerIgnore();
					ignore.playerHash = friends.get(i);

					list.add(ignore);
				}
			} finally {
				statement.close();
			}
			return list.toArray(new PlayerIgnore[list.size()]);
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected PlayerQuest[] queryLoadPlayerQuests(Player player) throws GameDatabaseException {
		try {
			ArrayList<PlayerQuest> list = new ArrayList<>();

			final PreparedStatement statement = createStatement(getQueries().playerQuests, player.getDatabaseID());
			final ResultSet result = statement.executeQuery();

			try {
				while (result.next()) {
					final PlayerQuest quest = new PlayerQuest();
					quest.questId = result.getInt("id");
					quest.stage = result.getInt("stage");

					list.add(quest);
				}
			} finally {
				result.close();
				statement.close();
			}

			return list.toArray(new PlayerQuest[list.size()]);
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected PlayerAchievement[] queryLoadPlayerAchievements(Player player) throws GameDatabaseException {
		try {
			final ArrayList<PlayerAchievement> list = new ArrayList<>();

			final PreparedStatement statement = createStatement(getQueries().playerAchievements, player.getDatabaseID());
			final ResultSet result = statement.executeQuery();

			try {
				while (result.next()) {
					final PlayerAchievement achievement = new PlayerAchievement();
					achievement.achievementId = result.getInt("id");
					achievement.status = result.getInt("status");

					list.add(achievement);
				}
			} finally {
				result.close();
				statement.close();
			}

			return list.toArray(new PlayerAchievement[list.size()]);
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected PlayerCache[] queryLoadPlayerCache(Player player) throws GameDatabaseException {
		try {
			final ArrayList<PlayerCache> list = new ArrayList<>();

			final PreparedStatement statement = createStatement(getQueries().playerCache, player.getDatabaseID());
			final ResultSet result = statement.executeQuery();

			try {
				while (result.next()) {
					final PlayerCache cache = new PlayerCache();
					cache.key = result.getString("key");
					cache.type = result.getInt("type");
					cache.value = result.getString("value");

					list.add(cache);
				}
			} finally {
				result.close();
				statement.close();
			}

			return list.toArray(new PlayerCache[list.size()]);
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected PlayerNpcKills[] queryLoadPlayerNpcKills(Player player) throws GameDatabaseException {
		try {
			final ArrayList<PlayerNpcKills> list = new ArrayList<>();

			final PreparedStatement statement = createStatement(getQueries().npcKillSelectAll, player.getDatabaseID());
			final ResultSet result = statement.executeQuery();

			try {
				while (result.next()) {
					final PlayerNpcKills kills = new PlayerNpcKills();
					kills.npcId = result.getInt("npcID");
					kills.killCount = result.getInt("killCount");

					list.add(kills);
				}
			} finally {
				result.close();
				statement.close();
			}

			return list.toArray(new PlayerNpcKills[list.size()]);
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected PlayerSkills[] queryLoadPlayerSkills(Player player) throws GameDatabaseException {
		try {
			int skillLevels[] = fetchLevels(player.getDatabaseID());
			PlayerSkills[] playerSkills = new PlayerSkills[skillLevels.length];
			for (int i = 0; i < playerSkills.length; i++) {
				playerSkills[i] = new PlayerSkills();
				playerSkills[i].skillId = i;
				playerSkills[i].skillCurLevel = skillLevels[i];
			}
			return playerSkills;
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected PlayerExperience[] queryLoadPlayerExperience(final int playerId) throws GameDatabaseException {
		try {
			int experience[] = fetchExperience(playerId);
			PlayerExperience[] playerExperiences = new PlayerExperience[experience.length];
			for (int i = 0; i < playerExperiences.length; i++) {
				playerExperiences[i] = new PlayerExperience();
				playerExperiences[i].skillId = i;
				playerExperiences[i].experience = experience[i];
			}
			return playerExperiences;
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected String queryPreviousPassword(int playerId) throws GameDatabaseException {
		String returnVal = "";
		try {
			final PreparedStatement statement = createStatement(getQueries().previousPassword, playerId);
			final ResultSet result = statement.executeQuery();
			try {
				if (!result.next()) {
					result.close();
				}
				returnVal = result.getString("previous_pass");
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}

		return returnVal;
	}

	@Override
	protected LinkedList<Achievement> queryLoadAchievements() throws GameDatabaseException {
		LinkedList<Achievement> loadedAchievements = new LinkedList<Achievement>();
		try {
			PreparedStatement fetchAchievement = createStatement(getQueries().achievements);
			ResultSet result = fetchAchievement.executeQuery();
			try {
				while (result.next()) {
					ArrayList<AchievementReward> rewards = queryLoadAchievementRewards(result.getInt("id"));
					ArrayList<AchievementTask> tasks = queryLoadAchievementTasks(result.getInt("id"));

					Achievement achievement = new Achievement(tasks, rewards, result.getInt("id"),
						result.getString("name"), result.getString("description"), result.getString("extra"));
					loadedAchievements.add(achievement);
				}
			} finally {
				fetchAchievement.close();
				result.close();
			}
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}

		return loadedAchievements;
	}

	@Override
	protected ArrayList<AchievementReward> queryLoadAchievementRewards(int achievementId) throws GameDatabaseException {
		ArrayList<AchievementReward> rewards = new ArrayList<AchievementReward>();

		try {
			PreparedStatement fetchRewards = createStatement(getQueries().rewards, achievementId);
			ResultSet rewardResult = fetchRewards.executeQuery();
			try {
				while (rewardResult.next()) {
					Achievement.TaskReward rewardType = Achievement.TaskReward.valueOf(Achievement.TaskReward.class, rewardResult.getString("reward_type"));
					rewards.add(new AchievementReward(rewardType, rewardResult.getInt("item_id"), rewardResult.getInt("amount"),
						rewardResult.getInt("guaranteed") == 1 ? true : false));
				}
			} finally {
				fetchRewards.close();
				rewardResult.close();
			}
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}

		return rewards;
	}

	protected ArrayList<AchievementTask> queryLoadAchievementTasks(int achievementId) throws GameDatabaseException {
		ArrayList<AchievementTask> tasks = new ArrayList<AchievementTask>();

		try {
			PreparedStatement fetchTasks = createStatement(getQueries().tasks, achievementId);
			ResultSet taskResult = fetchTasks.executeQuery();
			try {
				while (taskResult.next()) {
					Achievement.TaskType type = Achievement.TaskType.valueOf(Achievement.TaskType.class, taskResult.getString("type"));
					tasks.add(new AchievementTask(type, taskResult.getInt("do_id"), taskResult.getInt("do_amount")));
				}
			} finally {
				fetchTasks.close();
				taskResult.close();
			}
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}

		return tasks;
	}

	@Override
	protected PlayerRecoveryQuestions queryPlayerRecoveryData(int playerId, String tableName) throws GameDatabaseException {
		try {
			PlayerRecoveryQuestions recoveryQuestions = new PlayerRecoveryQuestions();
			final PreparedStatement statement = createStatement(getQueries().playerRecoveryInfo.replace(getQueries().TABLENAME_PLACEHOLDER, tableName), playerId);
			final ResultSet resultSet = statement.executeQuery();

			try {
				if (resultSet.next()) {
					recoveryQuestions.username = resultSet.getString("username");
					recoveryQuestions.question1 = resultSet.getString("question1");
					recoveryQuestions.question2 = resultSet.getString("question2");
					recoveryQuestions.question3 = resultSet.getString("question3");
					recoveryQuestions.question4 = resultSet.getString("question4");
					recoveryQuestions.question5 = resultSet.getString("question5");
					for (int i = 0; i < 5; i++) {
						recoveryQuestions.answers[i] = resultSet.getString("answer" + (i + 1));
					}
					recoveryQuestions.dateSet = resultSet.getInt("date_set");
					recoveryQuestions.ipSet = resultSet.getString("ip_set");
					if (tableName.equals("player_recovery")) {
						recoveryQuestions.previousPass = resultSet.getString("previous_pass");
						recoveryQuestions.earlierPass = resultSet.getString("earlier_pass");
					}

					return recoveryQuestions;
				}
				return null;
			} finally {
				statement.close();
				resultSet.close();
			}
		} catch (SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected void queryInsertPlayerRecoveryData(int playerId, PlayerRecoveryQuestions recoveryQuestions, String tableName) throws GameDatabaseException {
		executeUpdateQuery(getQueries().newPlayerRecoveryInfo.replace(getQueries().TABLENAME_PLACEHOLDER, tableName),
			playerId,
			recoveryQuestions.username,
			recoveryQuestions.question1,
			recoveryQuestions.question2,
			recoveryQuestions.question3,
			recoveryQuestions.question4,
			recoveryQuestions.question5,
			recoveryQuestions.answers,
			recoveryQuestions.dateSet,
			recoveryQuestions.ipSet);
	}

	@Override
	protected int queryInsertRecoveryAttempt(int playerId, String username, long time, String ip) throws GameDatabaseException {
		return executeUpdateQuery(getQueries().playerRecoveryAttempt, playerId, username, time, ip);
	}

	@Override
	protected void queryCancelRecoveryChange(int playerId) throws GameDatabaseException {
		executeUpdateQuery(getQueries().cancelRecoveryChangeRequest, playerId);
	}

	@Override
	protected PlayerContactDetails queryContactDetails(int playerId) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().contactDetails, playerId);
			final ResultSet result = statement.executeQuery();
			final PlayerContactDetails contactDetails = new PlayerContactDetails();
			try {
				if (result.next()) {
					contactDetails.id = playerId;
					contactDetails.username = result.getString("username");
					contactDetails.fullName = result.getString("fullname");
					contactDetails.zipCode = result.getString("zipCode");
					contactDetails.country = result.getString("country");
					contactDetails.email = result.getString("email");
					contactDetails.dateModified = result.getInt("date_modified");
					contactDetails.ip = result.getString("ip");

					return contactDetails;
				}
				return null;
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected void queryInsertContactDetails(int playerId, PlayerContactDetails contactDetails) throws GameDatabaseException {
		executeUpdateQuery(getQueries().newContactDetails, playerId, contactDetails.username, contactDetails.fullName, contactDetails.zipCode, contactDetails.country, contactDetails.email, contactDetails.dateModified, contactDetails.ip);
	}

	@Override
	protected void queryUpdateContactDetails(int playerId, PlayerContactDetails contactDetails) throws GameDatabaseException {
		executeUpdateQuery(getQueries().updateContactDetails, contactDetails.fullName, contactDetails.zipCode, contactDetails.country, contactDetails.email, contactDetails.dateModified, contactDetails.ip, playerId);
	}

	@Override
	protected ClanDef[] queryClans() throws GameDatabaseException {
		try {
			final ArrayList<ClanDef> clans = new ArrayList<>();

			final PreparedStatement statement = createStatement(getQueries().clans);
			final ResultSet resultSet = statement.executeQuery();

			try {
				while (resultSet.next()) {
					ClanDef clan = new ClanDef();
					clan.id = resultSet.getInt("id");
					clan.name = resultSet.getString("name");
					clan.tag = resultSet.getString("tag");
					clan.kick_setting = resultSet.getInt("kick_setting");
					clan.invite_setting = resultSet.getInt("invite_setting");
					clan.allow_search_join = resultSet.getInt("allow_search_join");
					clan.clan_points = resultSet.getInt("clan_points");

					clans.add(clan);
				}
				return clans.toArray(new ClanDef[clans.size()]);
			} finally {
				statement.close();
				resultSet.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected ClanMember[] queryClanMembers(int clanId) throws GameDatabaseException {
		try {
			final ArrayList<ClanMember> clanMembers = new ArrayList<>();

			final PreparedStatement preparedStatement = createStatement(getQueries().clanMembers, clanId);
			final ResultSet resultSet = preparedStatement.executeQuery();

			try {
				while (resultSet.next()) {
					ClanMember clanMember = new ClanMember();
					clanMember.username = resultSet.getString("username");
					clanMember.rank = resultSet.getInt("rank");
					clanMember.kills = resultSet.getInt("kills");
					clanMember.deaths = resultSet.getInt("deaths");

					clanMembers.add(clanMember);
				}
				return clanMembers.toArray(new ClanMember[clanMembers.size()]);
			} finally {
				preparedStatement.close();
				resultSet.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected int queryNewClan(String name, String tag, String leader) throws GameDatabaseException {
		return executeUpdateQuery(getQueries().newClan, name, tag, leader);
	}

	@Override
	protected void querySaveClanMembers(final int clanId, final ClanMember[] clanMembers) throws GameDatabaseException {
		ArrayList<Object[]> records = new ArrayList<Object[]>();
		for (ClanMember clanMember : clanMembers) {
			records.add(new Object[]{
				clanId,
				clanMember.username,
				clanMember.rank,
				clanMember.kills,
				clanMember.deaths
			});
		}

		executeBatchUpdateQuery(getQueries().saveClanMember, records.toArray(new Object[][]{}));
	}

	@Override
	protected void queryDeleteClan(int clanId) throws GameDatabaseException {
		executeUpdateQuery(getQueries().deleteClan, clanId);
	}

	@Override
	protected void queryDeleteClanMembers(int clanId) throws GameDatabaseException {
		executeUpdateQuery(getQueries().deleteClanMembers, clanId);
	}

	@Override
	protected void queryUpdateClan(ClanDef clan) throws GameDatabaseException {
		executeUpdateQuery(getQueries().updateClan, clan.name, clan.tag, clan.leader, clan.kick_setting, clan.invite_setting, clan.allow_search_join, clan.clan_points, clan.id);
	}

	@Override
	protected void queryUpdateClanMember(ClanMember clanMember) throws GameDatabaseException {
		executeUpdateQuery(getQueries().updateClanMember, clanMember.rank, clanMember.username);
	}

	@Override
	protected void queryExpiredAuction(ExpiredAuction[] expiredAuctions) throws GameDatabaseException {
		ArrayList<Object[]> records = new ArrayList<Object[]>();
		for (ExpiredAuction expiredAuction : expiredAuctions) {
			records.add(new Object[]{
				expiredAuction.item_id,
				expiredAuction.item_amount,
				expiredAuction.time,
				expiredAuction.playerID,
				expiredAuction.explanation
			});
		}

		executeBatchUpdateQuery(getQueries().expiredAuction, records.toArray(new Object[][]{}));
	}

	@Override
	protected ExpiredAuction[] queryCollectibleItems(int playerId) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().collectibleItems, playerId);
			final ResultSet result = statement.executeQuery();

			final ArrayList<ExpiredAuction> expiredAuctions = new ArrayList<>();
			try {
				while (result.next()) {
					ExpiredAuction item = new ExpiredAuction();
					item.claim_id = result.getInt("claim_id");
					item.item_id = result.getInt("item_id");
					item.item_amount = result.getInt("item_amount");
					item.playerID = result.getInt("playerID");
					item.explanation = result.getString("explanation");

					expiredAuctions.add(item);
				}
				return expiredAuctions.toArray(new ExpiredAuction[expiredAuctions.size()]);
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected void queryCollectItems(ExpiredAuction[] claimedItems) throws GameDatabaseException {
		ArrayList<Object[]> records = new ArrayList<Object[]>();
		for (ExpiredAuction item : claimedItems) {
			records.add(new Object[]{
				item.claim_time,
				item.claim_id
			});
		}

		executeBatchUpdateQuery(getQueries().collectItem, records.toArray(new Object[][]{}));
	}

	@Override
	protected void queryNewAuction(AuctionItem auctionItem) throws GameDatabaseException {
		executeUpdateQuery(getQueries().newAuction, auctionItem.itemID, auctionItem.amount, auctionItem.amount_left, auctionItem.price, auctionItem.seller, auctionItem.seller_username, auctionItem.buyer_info, auctionItem.time);
	}

	@Override
	protected void queryCancelAuction(final int auctionId) throws GameDatabaseException {
		executeUpdateQuery(getQueries().cancelAuction, auctionId);
	}

	@Override
	protected int queryAuctionCount() throws GameDatabaseException {
		try {
			PreparedStatement statement = createStatement(getQueries().auctionCount);
			ResultSet result = statement.executeQuery();
			try {
				if (result.next()) {
					int auctionCount = result.getInt("auction_count");
					return auctionCount;
				}
			} finally {
				statement.close();
				result.close();
			}
			return 0;
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected int queryPlayerAuctionCount(int playerId) throws GameDatabaseException {
		try {
			PreparedStatement statement = createStatement(getQueries().playerAuctionCount, playerId);
			ResultSet result = statement.executeQuery();
			try {
				if (result.next()) {
					int auctionCount = result.getInt("my_slots");
					return auctionCount;
				}
			} finally {
				statement.close();
				result.close();
			}
			return 0;
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected AuctionItem queryAuctionItem(int auctionId) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().auctionItem, auctionId);
			final ResultSet result = statement.executeQuery();

			final AuctionItem auctionItem = new AuctionItem();
			try {
				if (result.next()) {
					auctionItem.auctionID = result.getInt("auctionID");
					auctionItem.itemID = result.getInt("itemID");
					auctionItem.amount = result.getInt("amount");
					auctionItem.amount_left = result.getInt("amount_left");
					auctionItem.price = result.getInt("price");
					auctionItem.seller = result.getInt("seller");
					auctionItem.seller_username = result.getString("seller_username");
					auctionItem.buyer_info = result.getString("buyer_info");
					auctionItem.time = result.getLong("time");

					return auctionItem;
				}
			} finally {
				result.close();
				statement.close();
			}
			return null;
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected AuctionItem[] queryAuctionItems() throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().auctionItems);
			final ResultSet result = statement.executeQuery();

			final ArrayList<AuctionItem> auctionItems = new ArrayList<>();
			try {
				while (result.next()) {
					AuctionItem auctionItem = new AuctionItem();
					auctionItem.auctionID = result.getInt("auctionID");
					auctionItem.itemID = result.getInt("itemID");
					auctionItem.amount = result.getInt("amount");
					auctionItem.amount_left = result.getInt("amount_left");
					auctionItem.price = result.getInt("price");
					auctionItem.seller = result.getInt("seller");
					auctionItem.seller_username = result.getString("seller_username");
					auctionItem.buyer_info = result.getString("buyer_info");
					auctionItem.time = result.getLong("time");

					auctionItems.add(auctionItem);
				}
				return auctionItems.toArray(new AuctionItem[auctionItems.size()]);
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected void querySetSoldOut(final AuctionItem auctionItem) throws GameDatabaseException {
		executeUpdateQuery(getQueries().auctionSellOut, auctionItem.amount_left, auctionItem.sold_out, auctionItem.buyer_info, auctionItem.auctionID);
	}

	@Override
	protected void queryUpdateAuction(final AuctionItem auctionItem) throws GameDatabaseException {
		executeUpdateQuery(getQueries().updateAuction, auctionItem.amount_left, auctionItem.price, auctionItem.buyer_info, auctionItem.auctionID);
	}

	@Override
	protected void querySavePlayerData(int playerId, PlayerData playerData) throws GameDatabaseException {
		ArrayList<Object> parameterList = new ArrayList<Object>();
		parameterList.addAll(Arrays.asList(
			playerData.combatLevel,
			playerData.totalLevel,
			playerData.xLocation,
			playerData.yLocation,
			playerData.fatigue,
			playerData.kills,
			playerData.deaths,
			playerData.npcKills
		));
		if (getServer().getConfig().SPAWN_IRON_MAN_NPCS) {
			parameterList.addAll(Arrays.asList(
				playerData.ironMan,
				playerData.ironManRestriction,
				playerData.hcIronManDeath
			));
		}
		parameterList.addAll(Arrays.asList(
			playerData.questPoints,
			playerData.hairColour,
			playerData.topColour,
			playerData.trouserColour,
			playerData.skinColour,
			playerData.headSprite,
			playerData.bodySprite,
			playerData.male ? 1 : 0,
			playerData.combatStyle,
			playerData.muteExpires,
			playerData.bankSize,
			playerData.groupId,
			playerData.blockChat ? 1 : 0,
			playerData.blockPrivate ? 1 : 0,
			playerData.blockTrade ? 1 : 0,
			playerData.blockDuel ? 1 : 0,
			playerData.cameraAuto ? 1 : 0,
			playerData.oneMouse ? 1 : 0,
			playerData.soundOff ? 1 : 0,
			playerId
		));

		executeUpdateQuery(getQueries().save_UpdateBasicInfo, parameterList.toArray());
	}

	@Override
	protected void querySavePassword(int playerId, String newPassword) throws GameDatabaseException {
		executeUpdateQuery(getQueries().save_Password, newPassword, playerId);
	}

	@Override
	protected void querySavePreviousPasswords(int playerId, String newLastPass, String newEarlierPass) throws GameDatabaseException {
		executeUpdateQuery(getQueries().save_PreviousPasswords, newLastPass, newEarlierPass, playerId);
	}

	@Override
	protected void querySaveLastRecoveryTryId(int playerId, int lastRecoveryTryId) throws GameDatabaseException {
		executeUpdateQuery(getQueries().playerLastRecoveryTryId, lastRecoveryTryId, playerId);
	}

	@Override
	protected void querySavePlayerInventory(int playerId, PlayerInventory[] inventory) throws GameDatabaseException {
		executeUpdateQuery(getQueries().save_DeleteInv, playerId);

		ArrayList<Object[]> records1 = new ArrayList<Object[]>();
		ArrayList<Object[]> records2 = new ArrayList<Object[]>();
		for (PlayerInventory item : inventory) {
			records1.add(new Object[]{
				playerId,
				item.itemId,
				item.slot
			});

			records2.add(new Object[]{
				item.itemId,
				item.catalogID,
				item.amount,
				item.noted ? 1 : 0,
				item.wielded ? 1 : 0,
				item.durability
			});
		}

		executeBatchUpdateQuery(getQueries().save_InventoryAdd, records1.toArray(new Object[][]{}));
		executeBatchUpdateQuery(getQueries().save_ItemCreate, records2.toArray(new Object[][]{}));
	}

	@Override
	protected void querySavePlayerEquipped(int playerId, PlayerEquipped[] equipment) throws GameDatabaseException {
		executeUpdateQuery(getQueries().save_DeleteEquip, playerId);

		ArrayList<Object[]> records1 = new ArrayList<Object[]>();
		ArrayList<Object[]> records2 = new ArrayList<Object[]>();
		for (PlayerEquipped item : equipment) {
			records1.add(new Object[]{
				playerId,
				item.itemId
			});

			records2.add(new Object[]{
				item.itemId,
				item.itemStatus.getCatalogId(),
				item.itemStatus.getAmount(),
				item.itemStatus.getNoted() ? 1 : 0,
				1,
				item.itemStatus.getDurability()
			});
		}

		executeBatchUpdateQuery(getQueries().save_EquipmentAdd, records1.toArray(new Object[][]{}));
		executeBatchUpdateQuery(getQueries().save_ItemCreate, records2.toArray(new Object[][]{}));
	}

	@Override
	protected void querySavePlayerBank(int playerId, PlayerBank[] bank) throws GameDatabaseException {
		executeUpdateQuery(getQueries().save_DeleteBank, playerId);
		if (bank.length > 0) {
			ArrayList<Object[]> records1 = new ArrayList<Object[]>();
			ArrayList<Object[]> records2 = new ArrayList<Object[]>();
			int slot = 0;
			for (PlayerBank item : bank) {
				records1.add(new Object[]{
					playerId,
					item.itemId,
					slot++
				});

				records2.add(new Object[]{
					item.itemId,
					item.itemStatus.getCatalogId(),
					item.itemStatus.getAmount(),
					item.itemStatus.getNoted() ? 1 : 0,
					0,
					item.itemStatus.getDurability()
				});
			}

			executeBatchUpdateQuery(getQueries().save_BankAdd, records1.toArray(new Object[][]{}));
			executeBatchUpdateQuery(getQueries().save_ItemCreate, records2.toArray(new Object[][]{}));
		}
	}

	@Override
	protected void querySavePlayerBankPresets(int playerId, PlayerBankPreset[] bankPreset) throws GameDatabaseException {
		if (getServer().getConfig().WANT_BANK_PRESETS) {

			ArrayList<Object[]> records1 = new ArrayList<Object[]>();
			ArrayList<Object[]> records2 = new ArrayList<Object[]>();
			for (int i = 0; i < BankPreset.PRESET_COUNT; ++i) {
				records1.add(new Object[]{
					playerId,
					i
				});
			}

			for (PlayerBankPreset playerBankPreset : bankPreset) {
				records2.add(new Object[]{
					playerId,
					playerBankPreset.slot,
					playerBankPreset.inventory,
					playerBankPreset.equipment
				});
			}

			executeBatchUpdateQuery(getQueries().save_BankPresetRemove, records1.toArray(new Object[][]{}));
			executeBatchUpdateQuery(getQueries().save_BankPresetAdd, records2.toArray(new Object[][]{}));
		}
	}

	@Override
	protected void querySavePlayerFriends(int playerId, PlayerFriend[] friends) throws GameDatabaseException {
		executeUpdateQuery(getQueries().save_DeleteFriends, playerId);

		ArrayList<Object[]> records = new ArrayList<Object[]>();
		for (final PlayerFriend friend : friends) {
			String username = DataConversions.hashToUsername(friend.playerHash);
			if (username.equalsIgnoreCase("invalid_name"))
				continue;

			records.add(new Object[]{
				playerId,
				friend.playerHash,
				DataConversions.hashToUsername(friend.playerHash)
			});
		}

		executeBatchUpdateQuery(getQueries().save_AddFriends, records.toArray(new Object[][]{}));
	}

	@Override
	protected void querySavePlayerIgnored(int playerId, PlayerIgnore[] ignoreList) throws GameDatabaseException {
		executeUpdateQuery(getQueries().save_DeleteIgnored, playerId);

		ArrayList<Object[]> records = new ArrayList<Object[]>();
		for (final PlayerIgnore ignored : ignoreList) {
			records.add(new Object[]{
				playerId,
				ignored.playerHash
			});
		}

		executeBatchUpdateQuery(getQueries().save_AddIgnored, records.toArray(new Object[][]{}));
	}

	@Override
	protected void querySavePlayerQuests(int playerId, PlayerQuest[] quests) throws GameDatabaseException {
		executeUpdateQuery(getQueries().save_DeleteQuests, playerId);

		ArrayList<Object[]> records = new ArrayList<Object[]>();
		for (final PlayerQuest quest : quests) {
			records.add(new Object[]{
				playerId,
				quest.questId,
				quest.stage
			});
		}

		executeBatchUpdateQuery(getQueries().save_AddQuest, records.toArray(new Object[][]{}));
	}

	@Override
	protected void querySavePlayerAchievements(int playerId, PlayerAchievement[] achievements) throws GameDatabaseException {

	}

	@Override
	protected void querySavePlayerCache(int playerId, PlayerCache[] cache) throws GameDatabaseException {
		executeUpdateQuery(getQueries().save_DeleteCache, playerId);

		ArrayList<Object[]> records = new ArrayList<Object[]>();
		for (final PlayerCache cacheKey : cache) {
			records.add(new Object[]{
				playerId,
				cacheKey.type,
				cacheKey.key,
				cacheKey.value
			});
		}

		executeBatchUpdateQuery(getQueries().save_AddCache, records.toArray(new Object[][]{}));
	}

	@Override
	protected void querySavePlayerNpcKills(int playerId, PlayerNpcKills[] kills) throws GameDatabaseException {
		try {
			final Map<Integer, Integer> uniqueIDMap = new HashMap<>();

			final PreparedStatement statement = createStatement(getQueries().npcKillSelectAll, playerId);
			final ResultSet result = statement.executeQuery();
			try {
				while (result.next()) {
					final int key = result.getInt("npcID");
					final int value = result.getInt("ID");
					uniqueIDMap.put(key, value);
				}
			} finally {
				statement.close();
			}

			ArrayList<Object[]> records1 = new ArrayList<Object[]>();
			ArrayList<Object[]> records2 = new ArrayList<Object[]>();
			for (PlayerNpcKills kill : kills) {
				if (!uniqueIDMap.containsKey(kill.npcId)) {
					records1.add(new Object[]{
						kill.killCount,
						kill.npcId,
						playerId
					});
				} else {
					records2.add(new Object[]{
						kill.killCount,
						uniqueIDMap.get(kill.npcId),
						kill.npcId,
						playerId
					});
				}
			}

			executeBatchUpdateQuery(getQueries().npcKillInsert, records1.toArray(new Object[][]{}));
			executeBatchUpdateQuery(getQueries().npcKillUpdate, records2.toArray(new Object[][]{}));
			result.close();
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected void querySavePlayerSkills(int playerId, PlayerSkills[] currSkillLevels) throws GameDatabaseException {
		ArrayList<Object> parameterList = new ArrayList<Object>();
		Arrays.sort(currSkillLevels, Comparator.comparingInt(s -> s.skillId));
		for (PlayerSkills skill : currSkillLevels) {
			parameterList.add(skill.skillCurLevel);
		}
		parameterList.add(playerId);

		executeUpdateQuery(getQueries().updateStats, parameterList.toArray());
	}

	@Override
	protected void querySavePlayerExperience(int playerId, PlayerExperience[] experience) throws GameDatabaseException {
		ArrayList<Object> parameterList = new ArrayList<Object>();
		Arrays.sort(experience, Comparator.comparingInt(s -> s.skillId));
		for (PlayerExperience exp : experience) {
			parameterList.add(exp.experience);
		}
		parameterList.add(playerId);

		executeUpdateQuery(getQueries().updateExperience, parameterList.toArray());
	}

	@Override
	protected int queryMaxItemID() throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().max_itemStatus);
			ResultSet result = statement.executeQuery();
			try {
				if (result.next()) {
					return result.getInt("itemID");
				}
			} finally {
				result.close();
				statement.close();
			}
			return 0;
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected int queryItemCreate(Item item) throws GameDatabaseException {
		return executeUpdateQuery(getQueries().save_ItemCreate, item.getCatalogId(), item.getItemStatus().getAmount(), item.getItemStatus().getNoted() ? 1 : 0, item.getItemStatus().isWielded() ? 1 : 0, item.getItemStatus().getDurability());
	}

	@Override
	protected void queryItemPurge(final Item item) throws GameDatabaseException {
		purgeItemID(item.getItemId());
		executeUpdateQuery(getQueries().save_ItemPurge, item.getItemId());
	}

	@Override
	protected void queryItemUpdate(final Item item) throws GameDatabaseException {
		if (item.getItemId() == Item.ITEM_ID_UNASSIGNED) {
			throw new GameDatabaseException(this, "An unassigned item attempted to be updated: " + item.getCatalogId());
		}

		executeUpdateQuery(getQueries().save_ItemUpdate, item.getAmount(), item.getNoted() ? 1 : 0, item.isWielded() ? 1 : 0, item.getItemStatus().getDurability(), item.getItemId());
	}

	@Override
	protected void queryInventoryAdd(final int playerId, final Item item, int slot) throws GameDatabaseException {
		synchronized (itemIDList) {
			int itemId = item.getItemId();
			if (itemId == Item.ITEM_ID_UNASSIGNED) {
				itemId = assignItemID(item);
			}

			executeUpdateQuery(getQueries().save_InventoryAdd, playerId, itemId, slot);
		}
	}

	@Override
	protected void queryInventoryRemove(final int playerId, final Item item) throws GameDatabaseException {
		synchronized (itemIDList) {
			itemPurge(item);
			executeUpdateQuery(getQueries().save_InventoryRemove, playerId, item.getItemId());
		}
	}

	@Override
	protected void queryEquipmentAdd(final int playerId, final Item item) throws GameDatabaseException {
		synchronized (itemIDList) {
			int itemId = item.getItemId();
			if (itemId == Item.ITEM_ID_UNASSIGNED) {
				itemId = assignItemID(item);
			}

			executeUpdateQuery(getQueries().save_EquipmentAdd, playerId, itemId);
		}
	}

	@Override
	protected void queryEquipmentRemove(final int playerId, final Item item) throws GameDatabaseException {
		synchronized (itemIDList) {
			itemPurge(item);
			executeUpdateQuery(getQueries().save_EquipmentRemove, playerId, item.getItemId());
		}
	}

	@Override
	protected void queryBankAdd(final int playerId, final Item item, int slot) throws GameDatabaseException {
		synchronized (itemIDList) {
			int itemId = item.getItemId();
			if (itemId == Item.ITEM_ID_UNASSIGNED) {
				itemId = assignItemID(item);
			}

			executeUpdateQuery(getQueries().save_BankAdd, playerId, itemId, slot);
		}
	}

	@Override
	protected void queryBankRemove(final int playerId, final Item item) throws GameDatabaseException {
		synchronized (itemIDList) {
			itemPurge(item);
			executeUpdateQuery(getQueries().save_BankRemove, playerId, item.getItemId());
		}
	}

	@Override
	protected int queryPlayerIdFromToken(String token) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().playerIdFromPairToken, token);
			final ResultSet result = statement.executeQuery();
			try {
				if (result.next()) {
					return result.getInt("playerID");
				}
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
		return -1;
	}

	@Override
	protected void queryPairPlayer(int playerId, long discordId) throws GameDatabaseException {
		executeUpdateQuery(getQueries().pairDiscord, playerId, 3, "discordID", discordId);
	}

	@Override
	protected void queryRemovePairToken(int playerId) throws GameDatabaseException {
		executeUpdateQuery(getQueries().deleteTokenFromCache, playerId);
	}

	@Override
	protected String queryWatchlist(long discordId) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().watchlist, discordId);
			final ResultSet result = statement.executeQuery();
			try {
				if (result.next()) {
					return result.getString("value");
				}
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
		return null;
	}

	@Override
	protected void queryUpdateWatchlist(long discordId, String watchlist) throws GameDatabaseException {
		executeUpdateQuery(getQueries().updateWatchlist, watchlist, discordId);
	}

	@Override
	protected void queryNewWatchlist(long discordId, String watchlist) throws GameDatabaseException {
		executeUpdateQuery(getQueries().save_AddCache, 0, 1, "watchlist_" + discordId, watchlist);
	}

	@Override
	protected void queryDeleteWatchlist(long discordId) throws GameDatabaseException {
		executeUpdateQuery(getQueries().deleteWatchlist, discordId);
	}

	@Override
	protected DiscordWatchlist[] queryWatchlists() throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().watchlists);
			final ResultSet result = statement.executeQuery();
			final ArrayList<DiscordWatchlist> watchlists = new ArrayList<>();
			try {
				while (result.next()) {
					DiscordWatchlist watchlist = new DiscordWatchlist();
					watchlist.discordId = Long.parseLong(result.getString("key").substring(10));
					watchlist.list = result.getString("value");
					watchlists.add(watchlist);
				}

				return watchlists.toArray(new DiscordWatchlist[watchlists.size()]);
			} finally {
				result.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected int queryPlayerIdFromDiscordId(long discordId) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().discordIdToPlayerId, discordId);
			final ResultSet results = statement.executeQuery();
			try {
				if (results.next()) {
					return results.getInt("playerID");
				}
			} finally {
				results.close();
				statement.close();
			}
		} catch (final SQLException ex) {
			throw new GameDatabaseException(this, ex.getMessage());
		}
		return 0;
	}

	@Override
	protected PlayerRecoveryQuestions[] queryPlayerRecoveryChanges(Player player) throws GameDatabaseException {
		try {
			final ArrayList<PlayerRecoveryQuestions> list = new ArrayList<>();
			final PreparedStatement statement = createStatement(getQueries().playerPendingRecovery, player.getDatabaseID());
			final ResultSet result = statement.executeQuery();
			try {
				while (result.next()) {
					PlayerRecoveryQuestions questions = new PlayerRecoveryQuestions();
					questions.dateSet = result.getLong("date_set");
					for (int i = 0; i < 5; i++) {
						questions.answers[i] = result.getString("answer" + (i + 1));
					}
					questions.ipSet = result.getString("ip_set");
					questions.question1 = result.getString("question1");
					questions.question2 = result.getString("question2");
					questions.question3 = result.getString("question3");
					questions.question4 = result.getString("question4");
					questions.question5 = result.getString("question5");
					questions.username = result.getString("username");
					list.add(questions);
				}
			} finally {
				result.close();
				statement.close();
			}
			return list.toArray(new PlayerRecoveryQuestions[list.size()]);
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected String queryPlayerLoginIp(String username) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().fetchLoginIp, username);
			final ResultSet result = statement.executeQuery();
			String ip = null;
			try {
				if (result.next())
					ip = result.getString("login_ip");
			} finally {
				result.close();
				statement.close();
			}
			return ip;
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected LinkedPlayer[] queryLinkedPlayers(String ip) throws GameDatabaseException {
		try {
			final PreparedStatement statement = createStatement(getQueries().fetchLinkedPlayers, ip);
			final ResultSet result = statement.executeQuery();

			final ArrayList<LinkedPlayer> list = new ArrayList<>();
			try {
				while (result.next()) {
					final int group = result.getInt("group_id");
					final String user = result.getString("username");

					final LinkedPlayer linkedPlayer = new LinkedPlayer();
					linkedPlayer.groupId = group;
					linkedPlayer.username = user;

					list.add(linkedPlayer);
				}
			} finally {
				result.close();
				statement.close();
			}

			return list.toArray(new LinkedPlayer[list.size()]);
		} catch (final SQLException ex) {
			// Convert SQLException to a general usage exception
			throw new GameDatabaseException(this, ex.getMessage());
		}
	}

	@Override
	protected void queryInsertNpcSpawn(NPCLoc loc) throws GameDatabaseException {
		executeUpdateQuery(getQueries().addNpcSpawn, loc.id, loc.startX, loc.minX, loc.maxX, loc.startY, loc.minY, loc.maxY);
	}

	@Override
	protected void queryDeleteNpcSpawn(NPCLoc loc) throws GameDatabaseException {
		executeUpdateQuery(getQueries().removeNpcSpawn, loc.id, loc.startX, loc.minX, loc.maxX, loc.startY, loc.minY, loc.maxY);
	}

	@Override
	protected void queryInsertObjectSpawn(GameObjectLoc loc) throws GameDatabaseException {
		executeUpdateQuery(getQueries().addObjectSpawn, loc.getX(), loc.getY(), loc.getId(), loc.getDirection(), loc.getType());
	}

	@Override
	protected void queryDeleteObjectSpawn(GameObjectLoc loc) throws GameDatabaseException {
		executeUpdateQuery(getQueries().removeObjectSpawn, loc.getX(), loc.getY(), loc.getId(), loc.getDirection(), loc.getType());
	}

	@Override
	protected void queryDeleteItemSpawn(ItemLoc loc) throws GameDatabaseException {
		executeUpdateQuery(getQueries().removeItemSpawn, loc.getId(), loc.getX(), loc.getY());
	}

	@Override
	protected void queryInsertItemSpawn(ItemLoc loc) throws GameDatabaseException {
		executeUpdateQuery(getQueries().addItemSpawn, loc.getId(), loc.getX(), loc.getY(), loc.getAmount(), loc.getRespawnTime());
	}

	private int[] fetchLevels(int playerID) throws SQLException, GameDatabaseException {
		final PreparedStatement statement = createStatement(getQueries().playerCurExp, playerID);
		final ResultSet result = statement.executeQuery();

		try {
			result.next();

			int[] data = new int[getServer().getConstants().getSkills().getSkillsCount()];
			for (int i = 0; i < data.length; i++) {
				data[i] = result.getInt(getServer().getConstants().getSkills().getSkillName(i));
			}
			return data;
		} finally {
			result.close();
			statement.close();
		}
	}

	private int[] fetchExperience(int playerID) throws SQLException, GameDatabaseException {
		final int[] data = new int[getServer().getConstants().getSkills().getSkillsCount()];
		final PreparedStatement statement = createStatement(getQueries().playerExp, playerID);
		final ResultSet result = statement.executeQuery();
		try {
			if (result.next()) {
				for (int i = 0; i < data.length; i++) {
					data[i] = result.getInt(getServer().getConstants().getSkills().getSkillName(i));
				}
			}
		} finally {
			result.close();
			statement.close();
		}

		return data;
	}

	private PreparedStatement statementFromString(String query, String... longA) throws SQLException {
		PreparedStatement prepared = null;
		prepared = getConnection().prepareStatement(query);

		for (int i = 1; i <= longA.length; i++) {
			prepared.setString(i, longA[i - 1]);
		}

		return prepared;
	}

	private List<Long> longListFromResultSet(ResultSet result, String param) throws SQLException {
		List<Long> list = new ArrayList<Long>();
		try {
			while (result.next()) {
				list.add(result.getLong(param));
			}
		} finally {
			result.close();
		}

		return list;
	}

	private PreparedStatement statementFromInteger(String statement, int... longA) throws SQLException {
		PreparedStatement prepared = null;
		prepared = getConnection().prepareStatement(statement);

		for (int i = 1; i <= longA.length; i++) {
			prepared.setInt(i, longA[i - 1]);
		}

		return prepared;
	}

	private boolean hasNextFromInt(String statement, int... intA) throws SQLException {
		PreparedStatement prepared = null;
		ResultSet result = null;
		prepared = getConnection().prepareStatement(statement);

		for (int i = 1; i <= intA.length; i++) {
			prepared.setInt(i, intA[i - 1]);
		}

		result = prepared.executeQuery();

		Boolean retVal = false;
		try {
			retVal = Objects.requireNonNull(result).next();
		} catch (final Exception e) {
			return false;
		} finally {
			prepared.close();
			result.close();
		}
		return retVal;
	}

	public boolean isConnected() {
		return getConnection().isConnected();
	}

	protected MySqlGameDatabaseConnection getConnection() {
		return connection;
	}

	private MySqlQueries getQueries() {
		return queries;
	}

	public Set<Integer> getItemIDList() {
		return this.itemIDList;
	}

	public int addItemToPlayer(Item item) {
		try {
			int itemId = item.getItemId();
			if (itemId == Item.ITEM_ID_UNASSIGNED) {
				return assignItemID(item);
			}
			return itemId;
		} catch (GameDatabaseException e) {
			System.out.println(e);
		}
		return Item.ITEM_ID_UNASSIGNED;
	}

	public void removeItemFromPlayer(Item item) {
		try {
			itemPurge(item);
		} catch (GameDatabaseException e) {
			System.out.println(e);
		}
	}

	public int assignItemID(Item item) throws GameDatabaseException {
		synchronized (itemIDList) {
			int itemId = itemCreate(item);
			item.setItemId(this, itemId);
			itemIDList.add(itemId);
			return itemId;
		}
	}

	private void purgeItemID(int itemID) {
		synchronized (itemIDList) {
			Iterator<Integer> iterator = itemIDList.iterator();
			while (iterator.hasNext()) {
				Integer listID = iterator.next();
				if (listID == itemID) {
					iterator.remove();
					return;
				}
			}
		}
	}
}

