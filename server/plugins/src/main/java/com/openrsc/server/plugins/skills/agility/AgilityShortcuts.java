package com.openrsc.server.plugins.skills.agility;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.Skills;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.UseLocTrigger;
import com.openrsc.server.plugins.triggers.OpLocTrigger;
import com.openrsc.server.util.rsc.Formulae;

import static com.openrsc.server.plugins.Functions.*;

public class AgilityShortcuts implements OpLocTrigger,
	UseLocTrigger {

	private static final int SHORTCUT_FALADOR_HANDHOLD = 693;
	private static final int SHORTCUT_BRIMHAVEN_SWING = 694;
	private static final int SHORTCUT_BRIMHAVEN_BACK_SWING = 695;
	private static final int SHORTCUT_EDGE_DUNGEON_SWING = 684;
	private static final int SHORTCUT_EDGE_DUNGEON_BACK_SWING = 685;
	private static final int SHORTCUT_WEST_COALTRUCKS_LOG = 681;
	private static final int SHORTCUT_EAST_COALTRUCKS_LOG = 680;
	private static final int SHILO_VILLAGE_ROCKS_TO_BRIDGE = 710;
	private static final int SHILO_VILLAGE_BRIDGE_BLOCKADE_JUMP = 691;
	private static final int SHORTCUT_YANILLE_AGILITY_ROPESWING = 628;
	private static final int SHORTCUT_YANILLE_AGILITY_ROPESWING_BACK = 627;
	private static final int SHORTCUT_YANILLE_AGILITY_LEDGE = 614;
	private static final int SHORTCUT_YANILLE_AGILITY_LEDGE_BACK = 615;
	private static final int SHORTCUT_YANILLE_PILE_OF_RUBBLE = 636;
	private static final int SHORTCUT_YANILLE_PILE_OF_RUBBLE_UP = 633;
	private static final int SHORTCUT_YANILLE_PIPE = 656;
	private static final int SHORTCUT_YANILLE_PIPE_BACK = 657;
	private static final int GREW_ISLAND_ROPE_ATTACH = 662;
	private static final int GREW_ISLAND_ROPE_ATTACHED = 663;
	private static final int GREW_ISLAND_SWING_BACK = 664;
	private static final int EAST_KARAMJA_LOG = 692;
	private static final int EAST_KARAMJA_STONES = 701;
	private static final int YANILLE_CLIMBING_ROCKS = 1029;
	private static final int YANILLE_WATCHTOWER_HANDHOLDS = 658;
	private static final int TAVERLY_PIPE = 1236;
	private static final int TAVERLY_PIPE_RETURN = 1237;
	private static final int ENTRANA_RUBBLE = 1286;

	@Override
	public boolean blockOpLoc(Player player, GameObject obj, String command) {
		return inArray(obj.getID(), SHORTCUT_YANILLE_PIPE,
			SHORTCUT_YANILLE_PIPE_BACK,
			SHORTCUT_YANILLE_PILE_OF_RUBBLE,
			SHORTCUT_YANILLE_PILE_OF_RUBBLE_UP,
			SHORTCUT_YANILLE_AGILITY_LEDGE,
			SHORTCUT_YANILLE_AGILITY_LEDGE_BACK, SHORTCUT_FALADOR_HANDHOLD,
			SHORTCUT_BRIMHAVEN_SWING, SHORTCUT_BRIMHAVEN_BACK_SWING,
			SHORTCUT_EDGE_DUNGEON_SWING, SHORTCUT_EDGE_DUNGEON_BACK_SWING,
			SHORTCUT_WEST_COALTRUCKS_LOG, SHORTCUT_EAST_COALTRUCKS_LOG,
			SHORTCUT_YANILLE_AGILITY_ROPESWING,
			SHORTCUT_YANILLE_AGILITY_ROPESWING_BACK,
			GREW_ISLAND_ROPE_ATTACHED,
			GREW_ISLAND_SWING_BACK,
			EAST_KARAMJA_LOG,
			EAST_KARAMJA_STONES,
			YANILLE_CLIMBING_ROCKS,
			YANILLE_WATCHTOWER_HANDHOLDS,
			SHILO_VILLAGE_ROCKS_TO_BRIDGE,
			SHILO_VILLAGE_BRIDGE_BLOCKADE_JUMP,
			TAVERLY_PIPE,
			TAVERLY_PIPE_RETURN,
			ENTRANA_RUBBLE);
	}

	@Override
	public void onOpLoc(Player player, GameObject obj, String command) {
		switch (obj.getID()) {
			case SHILO_VILLAGE_BRIDGE_BLOCKADE_JUMP:
				if (getCurrentLevel(player, Skills.AGILITY) < 32) {
					player.message("You need an agility level of 32 to climb the rocks");
					return;
				}
				mes("The bridge beyond this fence looks very unsafe.");
				mes("However, you could try to negotiate it if you're feeling very agile.");
				player.message("Would you like to try?");
				int jumpMenu = multi(player,
					"No thanks! It looks far too dangerous!",
					"Yes, I'm totally brave and quite agile!");
				if (jumpMenu == 0) {
					mes("You decide that common sense is the better part of valour.",
						"And stop yourself from being hurled to what must be an ");
					player.message("inevitable death.");
				} else if (jumpMenu == 1) {
					mes("You prepare to negotiate the bridge fence...");
					mes("You run and jump...");
					if (succeed(player, 32)) {
						player.message("...and land perfectly on the other side!");
						if (player.getX() >= 460) { // back
							player.teleport(458, 828);
						} else {
							player.teleport(460, 828);
						}
					} else {
						player.message("...slip and fall incompetently into the river below!");
						player.teleport(458, 832);
						say(player, null, "* Ahhhhhhhhhh! *");
						player.damage((getCurrentLevel(player, Skills.HITS) / 10));
						delay(config().GAME_TICK);
						player.teleport(458, 836);
						player.damage((getCurrentLevel(player, Skills.HITS) / 10));
						delay(config().GAME_TICK * 2);
						say(player, null, "* Gulp! *");
						delay(1500);
						player.teleport(459, 841);
						say(player, null, "* Gulp! *");
						delay(config().GAME_TICK * 2);
						player.message("You just manage to drag your pitiful frame onto the river bank.");
						say(player, null, "* Gasp! *");
						player.damage((getCurrentLevel(player, Skills.HITS) / 10));
						delay(config().GAME_TICK * 2);
						player.message("Though you nearly drowned in the river!");
					}
				}
				break;
			case SHILO_VILLAGE_ROCKS_TO_BRIDGE:
				if (getCurrentLevel(player, Skills.AGILITY) < 32) {
					player.message("You need an agility level of 32 to climb the rocks");
					return;
				}
				mes("These rocks look quite dangerous to climb.",
					"But you may be able to scale them.");
				player.message("Would you like to try?");
				int menu = multi(player,
					"Yes, I can easily climb this!",
					"Nope, I'm sure I'll probably fall!");
				if (menu == 0) {
					if (succeed(player, 32)) {
						mes("You manage to climb the rocks succesfully and pick");
						if (obj.getX() == 450) {
							player.message("a route though the trecherous embankment to the top.");
							player.teleport(452, 829);
						} else {
							player.message("a route though the trecherous embankment to the bottom.");
							player.teleport(449, 828);
						}
					} else {
						player.teleport(450, 828);
						mes("You fall and hurt yourself.");
						player.damage((getCurrentLevel(player, Skills.HITS) / 10));
						delay(config().GAME_TICK);
						player.teleport(449, 828);
					}
				} else if (menu == 1) {
					player.message("You decide not to climb the rocks.");
				}
				break;
			case SHORTCUT_FALADOR_HANDHOLD:
				if (getCurrentLevel(player, Skills.AGILITY) < 5) {
					player.message("You need an agility level of 5 to climb the wall");
					return;
				}
				player.message("You climb over the wall");
				teleport(player, 338, 555);
				player.incExp(Skills.AGILITY, 50, true);
				break;
			case SHORTCUT_BRIMHAVEN_SWING:
				if (getCurrentLevel(player, Skills.AGILITY) < 10) {
					player.message("You need an agility level of 10 to attempt to swing on this vine");
					return;
				}
				player.message("You grab the vine and try and swing across");
				delay(config().GAME_TICK * 2);
				teleport(player, 511, 669);
				player.message("You skillfully swing across the stream");
				say(player, null, "Aaaaahahah");
				player.incExp(Skills.AGILITY, 20, true);
				break;
			case SHORTCUT_BRIMHAVEN_BACK_SWING:
				if (getCurrentLevel(player, Skills.AGILITY) < 10) {
					player.message("You need an agility level of 10 to attempt to swing on this vine");
					return;
				}
				player.message("You grab the vine and try and swing across");
				delay(config().GAME_TICK * 2);
				teleport(player, 508, 668);
				player.message("You skillfully swing across the stream");
				say(player, null, "Aaaaahahah");
				player.incExp(Skills.AGILITY, 20, true);
				break;
			case SHORTCUT_EDGE_DUNGEON_SWING:
				if (getCurrentLevel(player, Skills.AGILITY) < 15) {
					player.message("You need an agility level of 15 to attempt to swing on this rope");
					return;
				}
				delay(config().GAME_TICK * 2);
				teleport(player, 207, 3221);
				player.message("You skillfully swing across the hole");
				player.incExp(Skills.AGILITY, 40, true);
				break;
			case SHORTCUT_EDGE_DUNGEON_BACK_SWING:
				if (getCurrentLevel(player, Skills.AGILITY) < 15) {
					player.message("You need an agility level of 15 to attempt to swing on this rope");
					return;
				}
				delay(config().GAME_TICK * 2);
				teleport(player, 206, 3225);
				player.message("You skillfully swing across the hole");
				player.incExp(Skills.AGILITY, 40, true);
				break;
			case SHORTCUT_WEST_COALTRUCKS_LOG:
				if (getCurrentLevel(player, Skills.AGILITY) < 20) {
					player.message("You need an agility level of 20 to attempt balancing along this log");
					return;
				}
				player.message("You stand on the slippery log");
				for (int x = 595; x >= 592; x--) {
					teleport(player, x, 458);
					delay(config().GAME_TICK);
				}
				player.message("and you walk across");
				player.incExp(Skills.AGILITY, 34, true);
				break;
			case SHORTCUT_EAST_COALTRUCKS_LOG:
				if (getCurrentLevel(player, Skills.AGILITY) < 20) {
					player.message("You need an agility level of 20 to attempt balancing along this log");
					return;
				}
				player.message("You stand on the slippery log");
				for (int x = 595; x <= 598; x++) {
					teleport(player, x, 458);
					delay(config().GAME_TICK);
				}
				player.message("and you walk across");
				player.incExp(Skills.AGILITY, 34, true);
				break;
			// CONTINUE SHORTCUTS.
			case SHORTCUT_YANILLE_AGILITY_ROPESWING:
				if (getCurrentLevel(player, Skills.AGILITY) < 57) {
					player.message("You need an agility level of 57 to attempt to swing on this rope");
					return;
				}
				if (config().WANT_FATIGUE) {
					if (player.getFatigue() >= player.MAX_FATIGUE) {
						player.message("You are too tired to swing on the rope");
						return;
					}
				}
				player.message("You grab the rope and try and swing across");
				if (!succeed(player, 57, 77)) {
					mes("You miss the opposite side and fall to the level below");
					teleport(player, 596, 3534);
					return;
				}
				delay(2200);
				teleport(player, 596, 3581);
				player.message("You skillfully swing across the hole");
				player.incExp(Skills.AGILITY, 110, true);
				break;
			case SHORTCUT_YANILLE_AGILITY_ROPESWING_BACK:
				if (getCurrentLevel(player, Skills.AGILITY) < 57) {
					player.message("You need an agility level of 57 to attempt to swing on this rope");
					return;
				}
				if (config().WANT_FATIGUE) {
					if (player.getFatigue() >= player.MAX_FATIGUE) {
						player.message("You are too tired to swing on the rope");
						return;
					}
				}
				player.message("You grab the rope and try and swing across");
				if (!succeed(player, 57, 77)) {
					mes("You miss the opposite side and fall to the level below");
					teleport(player, 598, 3536);
					return;
				}
				delay(2200);
				teleport(player, 598, 3585);
				player.message("You skillfully swing across the hole");
				player.incExp(Skills.AGILITY, 110, true);
				break;

			case SHORTCUT_YANILLE_AGILITY_LEDGE:
				if (getCurrentLevel(player, Skills.AGILITY) < 40) {
					player.message("You need an agility level of 40 to attempt balancing along this log");
					return;
				}
				if (config().WANT_FATIGUE) {
					if (player.getFatigue() >= player.MAX_FATIGUE) {
						player.message("You are too tired to balance on the ledge");
						return;
					}
				}
				player.message("You put your foot on the ledge and try to edge across");
				delay(2200);
				if (!succeed(player, 40, 65)) {
					mes("you lose your footing and fall to the level below");
					teleport(player, 603, 3520);
					return;
				}
				teleport(player, 601, 3563);
				player.message("You skillfully balance across the hole");
				player.incExp(Skills.AGILITY, 90, true);
				break;
			case SHORTCUT_YANILLE_AGILITY_LEDGE_BACK:
				if (getCurrentLevel(player, Skills.AGILITY) < 40) {
					player.message("You need an agility level of 40 to attempt balancing along this log");
					return;
				}
				if (config().WANT_FATIGUE) {
					if (player.getFatigue() >= player.MAX_FATIGUE) {
						player.message("You are too tired to balance on the ledge");
						return;
					}
				}
				player.message("You put your foot on the ledge and try to edge across");
				delay(2200);
				if (!succeed(player, 40, 65)) {
					mes("you lose your footing and fall to the level below");
					teleport(player, 603, 3520);
					return;
				}
				teleport(player, 601, 3557);
				player.message("You skillfully balance across the hole");
				player.incExp(Skills.AGILITY, 90, true);
				break;

			case SHORTCUT_YANILLE_PILE_OF_RUBBLE:
				if (getCurrentLevel(player, Skills.AGILITY) < 67) {
					player.message("You need an agility level of 67 to attempt to climb down the rubble");
					return;
				}
				teleport(player, 580, 3525);
				player.message("You climb down the pile of rubble");
				break;
			case SHORTCUT_YANILLE_PILE_OF_RUBBLE_UP:
				if (getCurrentLevel(player, Skills.AGILITY) < 67) {
					player.message("You need an agility level of 67 to attempt to climb up the rubble");
					return;
				}
				if (config().WANT_FATIGUE) {
					if (player.getFatigue() >= player.MAX_FATIGUE) {
						player.message("You are too tired to climb up the rubble");
						return;
					}
				}
				teleport(player, 582, 3573);
				player.message("You climb up the pile of rubble");
				player.incExp(Skills.AGILITY, 54, true);
				break;

			case SHORTCUT_YANILLE_PIPE:
				if (getCurrentLevel(player, Skills.AGILITY) < 49) {
					player.message("You need an agility level of 49 to attempt to squeeze through the pipe");
					return;
				}
				if (config().WANT_FATIGUE) {
					if (player.getFatigue() >= player.MAX_FATIGUE) {
						player.message("You are too tired to squeeze through the pipe");
						return;
					}
				}
				player.message("You squeeze through the pipe");
				delay(2200);
				teleport(player, 608, 3568);
				player.incExp(Skills.AGILITY, 30, true);
				break;
			case SHORTCUT_YANILLE_PIPE_BACK:
				if (getCurrentLevel(player, Skills.AGILITY) < 49) {
					player.message("You need an agility level of 49 to attempt to squeeze through the pipe");
					return;
				}
				if (config().WANT_FATIGUE) {
					if (player.getFatigue() >= player.MAX_FATIGUE) {
						player.message("You are too tired to squeeze through the pipe");
						return;
					}
				}
				player.message("You squeeze through the pipe");
				delay(2200);
				teleport(player, 605, 3568);
				player.incExp(Skills.AGILITY, 30, true);
				break;
			case GREW_ISLAND_ROPE_ATTACHED:
				if (getCurrentLevel(player, Skills.AGILITY) < 30) {
					player.message("You need an agility level of 30 to attempt to swing across the stream");
					return;
				}
				player.message("You grab the rope and try and swing across");
				delay(2200);
				teleport(player, 664, 755);
				player.message("You skillfully swing across the stream");
				player.incExp(Skills.AGILITY, 50, true);
				break;
			case GREW_ISLAND_SWING_BACK:
				player.message("You grab the rope and try and swing across");
				delay(2200);
				teleport(player, 666, 755);
				player.message("You skillfully swing across the stream");
				player.incExp(Skills.AGILITY, 50, true);
				break;
			case EAST_KARAMJA_LOG:
				if (getCurrentLevel(player, Skills.AGILITY) < 32) {
					player.message("You need an agility level of 32 to attempt balancing along this log");
					return;
				}
				player.message("You attempt to walk over the the slippery log..");
				delay(config().GAME_TICK * 3);
				if (!succeed(player, 32)) {
					teleport(player, 368, 781);
					delay(config().GAME_TICK);
					player.message("@red@You fall into the stream!");
					player.message("You lose some health");
					teleport(player, 370, 776);
					player.damage(1);
					return;
				}
				if (player.getX() <= 367) {
					teleport(player, 368, 781);
					delay(config().GAME_TICK);
					teleport(player, 370, 781);
				} else {
					teleport(player, 368, 781);
					delay(config().GAME_TICK);
					teleport(player, 366, 781);
				}
				player.message("...and make it without any problems!");
				player.incExp(Skills.AGILITY, 10, true);
				break;
			case EAST_KARAMJA_STONES:
				if (getCurrentLevel(player, Skills.AGILITY) < 32) {
					player.message("You need an agility level of 32 to step on these stones");
					return;
				}
				if (config().WANT_FATIGUE) {
					if (player.getFatigue() >= player.MAX_FATIGUE) {
						player.message("You are too fatigued to continue.");
						return;
					}
				}
				player.message("You jump onto the rock");
				if (player.getY() <= 805) {
					teleport(player, 347, 806);
					delay(config().GAME_TICK);
					if (!succeed(player, 32)) {
						delay(900);
						teleport(player, 341, 809);
						player.message("@red@!!! You Fall !!!");
						mes("You get washed up on the other side of the river...",
							"After being nearly half drowned");
						player.damage((int) (player.getSkills().getLevel(Skills.HITS) / 4) + 2);
						return;
					}
					teleport(player, 346, 808);
				} else {
					teleport(player, 346, 807);
					delay(config().GAME_TICK);
					if (!succeed(player, 32)) {
						delay(900);
						teleport(player, 341, 805);
						player.message("@red@!!! You Fall !!!");
						mes("You get washed up on the other side of the river...",
							"After being nearly half drowned");
						player.damage((int) (player.getSkills().getLevel(Skills.HITS) / 4) + 2);
						return;
					}
					teleport(player, 347, 805);
				}
				player.message("And cross the water without problems.");
				player.incExp(Skills.AGILITY, 10, true);
				break;
			case YANILLE_CLIMBING_ROCKS:
				if (config().WANT_FATIGUE) {
					if (player.getFatigue() >= player.MAX_FATIGUE) {
						player.message("You are too tired to climb up the wall");
						return;
					}
				}
				if (getCurrentLevel(player, Skills.AGILITY) < 15) {
					player.message("You need an agility level of 15 to climb the wall");
					return;
				}
				player.message("You climb over the wall");
				teleport(player, 624, 741);
				player.incExp(Skills.AGILITY, 40, true);
				break;
			case YANILLE_WATCHTOWER_HANDHOLDS:
				if (config().WANT_FATIGUE) {
					if (player.getFatigue() >= player.MAX_FATIGUE) {
						player.message("You are too tired to climb up the wall");
						return;
					}
				}
				if (getCurrentLevel(player, Skills.AGILITY) < 18) {
					player.message("You need an agility level of 18 to climb the wall");
					return;
				}
				player.message("You climb up the wall");
				player.teleport(637, 1680);
				player.message("And climb in through the window");
				player.incExp(Skills.AGILITY, 50, true);
				break;

			case TAVERLY_PIPE_RETURN:
			if (getCurrentLevel(player, Skills.AGILITY) < 70) {
				player.message("You need an agility level of 70 to attempt to squeeze through the pipe");
				return;
			}
			if (config().WANT_FATIGUE) {
				if (player.getFatigue() >= player.MAX_FATIGUE) {
					player.message("You are too tired to squeeze through the pipe");
					return;
				}
			}
			player.message("You squeeze through the pipe");
			teleport(player, 372, 3352);
			player.incExp(Skills.AGILITY, 30, true);
			break;

			case TAVERLY_PIPE:
			if (getCurrentLevel(player, Skills.AGILITY) < 70) {
				player.message("You need an agility level of 70 to attempt to squeeze through the pipe");
				return;
			}
			if (config().WANT_FATIGUE) {
				if (player.getFatigue() >= player.MAX_FATIGUE) {
					player.message("You are too tired to squeeze through the pipe");
					return;
				}
			}
			player.message("You squeeze through the pipe");
			teleport(player, 375, 3352);
			player.incExp(Skills.AGILITY, 30, true);
			break;
			case ENTRANA_RUBBLE:
				if (getCurrentLevel(player, Skills.AGILITY) < 55) {
					player.message("You need an agility level of 55 to climb the rubble");
					return;
				}
				if (config().WANT_FATIGUE) {
					if (player.getFatigue() >= player.MAX_FATIGUE) {
						player.message("You are too tired to climb the rubble");
						return;
					}
				}
				delay(config().GAME_TICK);
				if (player.getLocation().getY() < 550) {
					teleport(player, 434, 551);
					player.incExp(Skills.AGILITY, 15, true);
				} else {
					teleport(player, 434, 549);
					player.incExp(Skills.AGILITY, 15, true);
				}
				break;
		}
	}

	boolean succeed(Player player, int req) {
		return Formulae.calcProductionSuccessful(req, getCurrentLevel(player, Skills.AGILITY), false, req + 30);
	}

	boolean succeed(Player player, int req, int lvlStopFail) {
		return Formulae.calcProductionSuccessful(req, getCurrentLevel(player, Skills.AGILITY), true, lvlStopFail);
	}

	@Override
	public boolean blockUseLoc(Player player, GameObject obj, Item item) {
		return obj.getID() == GREW_ISLAND_ROPE_ATTACH && item.getCatalogId() == ItemId.ROPE.id();
	}

	@Override
	public void onUseLoc(Player player, GameObject obj, Item item) {
		if (obj.getID() == GREW_ISLAND_ROPE_ATTACH && item.getCatalogId() == ItemId.ROPE.id()) {
			player.message("you tie the rope to the tree");
			player.getCarriedItems().remove(new Item(ItemId.ROPE.id()));
			player.getWorld().replaceGameObject(obj,
				new GameObject(player.getWorld(), obj.getLocation(), 663, obj.getDirection(), obj
					.getType()));
			player.getWorld().delayedSpawnObject(obj.getLoc(), 60000);
		}
	}

	// HERRING SPAWN I CHEST ROOM SINISTER CHEST = 362, 614, 3564
}
