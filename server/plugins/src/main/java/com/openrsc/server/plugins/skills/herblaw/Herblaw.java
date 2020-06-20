package com.openrsc.server.plugins.skills.herblaw;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.Quests;
import com.openrsc.server.constants.Skills;
import com.openrsc.server.external.ItemHerbDef;
import com.openrsc.server.external.ItemHerbSecond;
import com.openrsc.server.external.ItemUnIdentHerbDef;
import com.openrsc.server.model.container.CarriedItems;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.OpInvTrigger;
import com.openrsc.server.plugins.triggers.UseInvTrigger;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.MessageType;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.openrsc.server.plugins.Functions.*;

public class Herblaw implements OpInvTrigger, UseInvTrigger {

	private static int[] unidentifiedHerbs = {
		ItemId.UNIDENTIFIED_GUAM_LEAF.id(),
		ItemId.UNIDENTIFIED_MARRENTILL.id(),
		ItemId.UNIDENTIFIED_TARROMIN.id(),
		ItemId.UNIDENTIFIED_HARRALANDER.id(),
		ItemId.UNIDENTIFIED_RANARR_WEED.id(),
		ItemId.UNIDENTIFIED_IRIT_LEAF.id(),
		ItemId.UNIDENTIFIED_AVANTOE.id(),
		ItemId.UNIDENTIFIED_KWUARM.id(),
		ItemId.UNIDENTIFIED_CADANTINE.id(),
		ItemId.UNIDENTIFIED_DWARF_WEED.id(),
		ItemId.UNIDENTIFIED_TORSTOL.id(),
		ItemId.UNIDENTIFIED_SNAKE_WEED.id(),
		ItemId.UNIDENTIFIED_ARDRIGAL.id(),
		ItemId.UNIDENTIFIED_SITO_FOIL.id(),
		ItemId.UNIDENTIFIED_VOLENCIA_MOSS.id(),
		ItemId.UNIDENTIFIED_ROGUES_PURSE.id()
	};

	@Override
	public void onOpInv(Player player, Integer invIndex, final Item item, String command) {
		if (DataConversions.inArray(unidentifiedHerbs, item.getCatalogId()) && command.equalsIgnoreCase("Identify")) {
			handleHerbIdentify(item, player);
		}
	}

	public boolean blockOpInv(Player player, Integer invIndex, final Item item, String command) {
		return DataConversions.inArray(unidentifiedHerbs, item.getCatalogId()) && command.equalsIgnoreCase("Identify");
	}

	private void handleHerbIdentify(final Item herb, Player player) {
		if (!config().MEMBER_WORLD) {
			player.sendMemberErrorMessage();
			return;
		}
		ItemUnIdentHerbDef herbDef = herb.getUnIdentHerbDef(player.getWorld());
		if (herbDef == null) {
			return;
		}
		if (player.getSkills().getLevel(Skills.HERBLAW) < herbDef.getLevelRequired()) {
			player.playerServerMessage(MessageType.QUEST, "You cannot identify this herb");
			player.playerServerMessage(MessageType.QUEST, "you need a higher herblaw level");
			return;
		}
		if (player.getQuestStage(Quests.DRUIDIC_RITUAL) != -1) {
			player.message("You need to complete Druidic ritual quest first");
			return;
		}

		int repeat = 1;
		if (config().BATCH_PROGRESSION) {
			repeat = player.getCarriedItems().getInventory().countId(herb.getCatalogId());
		}

		startbatch(repeat);
		batchIdentify(player, herb, herbDef);
	}

	private void batchIdentify(Player player, Item herb, ItemUnIdentHerbDef herbDef) {
		if (player.getSkills().getLevel(Skills.HERBLAW) < herbDef.getLevelRequired()) {
			player.playerServerMessage(MessageType.QUEST, "You cannot identify this herb");
			player.playerServerMessage(MessageType.QUEST, "you need a higher herblaw level");
			return;
		}
		if (player.getQuestStage(Quests.DRUIDIC_RITUAL) != -1) {
			player.message("You need to complete Druidic ritual quest first");
			return;
		}
		if (config().WANT_FATIGUE) {
			if (config().STOP_SKILLING_FATIGUED >= 2
				&& player.getFatigue() >= player.MAX_FATIGUE) {
				player.message("You are too tired to identify this herb");
				return;
			}
		}
		Item newItem = new Item(herbDef.getNewId());
		Item herbToRemove = player.getCarriedItems().getInventory().get(
			player.getCarriedItems().getInventory().getLastIndexById(herb.getCatalogId(), Optional.of(false)));
		if (herbToRemove == null) return;
		player.getCarriedItems().remove(herbToRemove);
		player.getCarriedItems().getInventory().add(newItem);
		player.playerServerMessage(MessageType.QUEST, "This herb is " + newItem.getDef(player.getWorld()).getName());
		player.incExp(Skills.HERBLAW, herbDef.getExp(), true);
		delay(config().GAME_TICK * 2);

		// Repeat
		updatebatch();
		if (!ifinterrupted() && !ifbatchcompleted()) {
			batchIdentify(player, herb, herbDef);
		}
	}

	@Override
	public void onUseInv(Player player, Integer invIndex, Item item, Item usedWith) {
		ItemHerbSecond secondDef = null;
		int itemID = item.getCatalogId();
		int usedWithID = usedWith.getCatalogId();
		CarriedItems carriedItems = player.getCarriedItems();

		// Add secondary ingredient
		if ((secondDef = player.getWorld().getServer().getEntityHandler().getItemHerbSecond(itemID, usedWithID)) != null) {
			doHerbSecond(player, item, usedWith, secondDef, false);
		} else if ((secondDef = player.getWorld().getServer().getEntityHandler().getItemHerbSecond(usedWithID, itemID)) != null) {
			doHerbSecond(player, usedWith, item, secondDef, true);

		// Grind ingredient
		} else if (itemID == ItemId.PESTLE_AND_MORTAR.id()) {
			doGrind(player, item, usedWith);
		} else if (usedWithID == ItemId.PESTLE_AND_MORTAR.id()) {
			doGrind(player, usedWith, item);

		// Add herb to vial
		} else if (itemID == ItemId.VIAL.id()) {
			doHerblaw(player, item, usedWith);
		} else if (usedWithID == ItemId.VIAL.id()) {
			doHerblaw(player, usedWith, item);

		// Ogre potion (Watchtower quest)
		} else if (itemID == ItemId.UNFINISHED_OGRE_POTION.id() && usedWithID == ItemId.GROUND_BAT_BONES.id()) {
			makeLiquid(player, usedWith, item, true);
		} else if (itemID == ItemId.GROUND_BAT_BONES.id() && usedWithID == ItemId.UNFINISHED_OGRE_POTION.id()) {
			makeLiquid(player, item, usedWith, false);
		} else if (itemID == ItemId.UNFINISHED_POTION.id() && (usedWithID == ItemId.GROUND_BAT_BONES.id() || usedWithID == ItemId.GUAM_LEAF.id())) {
			makeLiquid(player, item, usedWith, false);
		} else if (usedWithID == ItemId.UNFINISHED_POTION.id() && (itemID == ItemId.GROUND_BAT_BONES.id() || itemID == ItemId.GUAM_LEAF.id())) {
			makeLiquid(player, usedWith, item, true);

		// Explosive compound (Digsite quest)
		} else if (usedWithID == ItemId.NITROGLYCERIN.id() && itemID == ItemId.AMMONIUM_NITRATE.id()
				|| usedWithID == ItemId.AMMONIUM_NITRATE.id() && itemID == ItemId.NITROGLYCERIN.id()) {
			if (player.getSkills().getLevel(Skills.HERBLAW) < 10) {
				player.playerServerMessage(MessageType.QUEST, "You need to have a herblaw level of 10 or over to mix this liquid");
				return;
			}
			if (player.getQuestStage(Quests.DRUIDIC_RITUAL) != -1) {
				player.message("You need to complete Druidic ritual quest first");
				return;
			}
			player.incExp(Skills.HERBLAW, 20, true);
			player.playerServerMessage(MessageType.QUEST, "You mix the nitrate powder into the liquid");
			player.message("It has produced a foul mixture");
			thinkbubble(new Item(ItemId.AMMONIUM_NITRATE.id()));
			carriedItems.remove(new Item(ItemId.AMMONIUM_NITRATE.id()));
			carriedItems.remove(new Item(ItemId.NITROGLYCERIN.id()));
			carriedItems.getInventory().add(new Item(ItemId.MIXED_CHEMICALS_1.id()));
		} else if (usedWithID == ItemId.GROUND_CHARCOAL.id() && itemID == ItemId.MIXED_CHEMICALS_1.id()
				|| usedWithID == ItemId.MIXED_CHEMICALS_1.id() && itemID == ItemId.GROUND_CHARCOAL.id()) {
			if (player.getSkills().getLevel(Skills.HERBLAW) < 10) {
				player.playerServerMessage(MessageType.QUEST, "You need to have a herblaw level of 10 or over to mix this liquid");
				return;
			}
			if (player.getQuestStage(Quests.DRUIDIC_RITUAL) != -1) {
				player.message("You need to complete Druidic ritual quest first");
				return;
			}
			player.incExp(Skills.HERBLAW, 25, true);
			player.playerServerMessage(MessageType.QUEST, "You mix the charcoal into the liquid");
			player.message("It has produced an even fouler mixture");
			thinkbubble(new Item(ItemId.GROUND_CHARCOAL.id()));
			carriedItems.remove(new Item(ItemId.GROUND_CHARCOAL.id()));
			carriedItems.remove(new Item(ItemId.MIXED_CHEMICALS_1.id()));
			carriedItems.getInventory().add(new Item(ItemId.MIXED_CHEMICALS_2.id()));
		} else if (usedWithID == ItemId.ARCENIA_ROOT.id() && itemID == ItemId.MIXED_CHEMICALS_2.id()
				|| usedWithID == ItemId.MIXED_CHEMICALS_2.id() && itemID == ItemId.ARCENIA_ROOT.id()) {
			if (player.getSkills().getLevel(Skills.HERBLAW) < 10) {
				player.playerServerMessage(MessageType.QUEST, "You need to have a herblaw level of 10 or over to mix this liquid");
				return;
			}
			if (player.getQuestStage(Quests.DRUIDIC_RITUAL) != -1) {
				player.message("You need to complete Druidic ritual quest first");
				return;
			}
			player.incExp(Skills.HERBLAW, 30, true);
			player.message("You mix the root into the mixture");
			player.message("You produce a potentially explosive compound...");
			thinkbubble(new Item(ItemId.ARCENIA_ROOT.id()));
			carriedItems.remove(new Item(ItemId.ARCENIA_ROOT.id()));
			carriedItems.remove(new Item(ItemId.MIXED_CHEMICALS_2.id()));
			carriedItems.getInventory().add(new Item(ItemId.EXPLOSIVE_COMPOUND.id()));
			say(player, null, "Excellent this looks just right");

		// Blamish oil (Heroes quest)
		} else if (usedWithID == ItemId.UNFINISHED_HARRALANDER_POTION.id() && itemID == ItemId.BLAMISH_SNAIL_SLIME.id()
				|| usedWithID == ItemId.BLAMISH_SNAIL_SLIME.id() && itemID == ItemId.UNFINISHED_HARRALANDER_POTION.id()) {
			if (player.getSkills().getLevel(Skills.HERBLAW) < 25) {
				player.playerServerMessage(MessageType.QUEST, "You need a herblaw level of 25 to make this potion");
				return;
			}
			if (player.getQuestStage(Quests.DRUIDIC_RITUAL) != -1) {
				player.message("You need to complete Druidic ritual quest first");
				return;
			}
			player.incExp(Skills.HERBLAW, 320, true);
			player.message("You mix the slime into your potion");
			carriedItems.remove(new Item(ItemId.UNFINISHED_HARRALANDER_POTION.id()));
			carriedItems.remove(new Item(ItemId.BLAMISH_SNAIL_SLIME.id()));
			carriedItems.getInventory().add(new Item(ItemId.BLAMISH_OIL.id()));

		// Snakes weed potion (Legends quest)
		} else if (usedWithID == ItemId.SNAKES_WEED_SOLUTION.id() && itemID == ItemId.ARDRIGAL.id()
				|| usedWithID == ItemId.ARDRIGAL.id() && itemID == ItemId.SNAKES_WEED_SOLUTION.id()) {
			if (player.getSkills().getLevel(Skills.HERBLAW) < 45) {
				player.playerServerMessage(MessageType.QUEST, "You need to have a herblaw level of 45 or over to mix this potion");
				return;
			}
			if (player.getQuestStage(Quests.DRUIDIC_RITUAL) != -1) {
				player.message("You need to complete Druidic ritual quest first");
				return;
			}
			//player needs to have learned secret from gujuo
			if (player.getQuestStage(Quests.LEGENDS_QUEST) >= 0 && player.getQuestStage(Quests.LEGENDS_QUEST) < 7) {
				player.message("You're not quite sure what effect this will have.");
				player.message("You decide against experimenting.");
				return;
			}
			player.message("You add the Ardrigal to the Snakesweed Solution.");
			player.message("The mixture seems to bubble slightly with a strange effervescence...");
			carriedItems.remove(new Item(ItemId.ARDRIGAL.id()));
			carriedItems.remove(new Item(ItemId.SNAKES_WEED_SOLUTION.id()));
			carriedItems.getInventory().add(new Item(ItemId.GUJUO_POTION.id()));
		} else if (usedWithID == ItemId.ARDRIGAL_SOLUTION.id() && itemID == ItemId.SNAKE_WEED.id()
				|| usedWithID == ItemId.SNAKE_WEED.id() && itemID == ItemId.ARDRIGAL_SOLUTION.id()) {
			if (player.getSkills().getLevel(Skills.HERBLAW) < 45) {
				player.playerServerMessage(MessageType.QUEST, "You need to have a herblaw level of 45 or over to mix this potion");
				return;
			}
			if (player.getQuestStage(Quests.DRUIDIC_RITUAL) != -1) {
				player.message("You need to complete Druidic ritual quest first");
				return;
			}
			//player needs to have learned secret from gujuo
			if (player.getQuestStage(Quests.LEGENDS_QUEST) >= 0 && player.getQuestStage(Quests.LEGENDS_QUEST) < 7) {
				player.message("You're not quite sure what effect this will have.");
				player.message("You decide against experimenting.");
				return;
			}
			player.message("You add the Snake Weed to the Ardrigal solution.");
			player.message("The mixture seems to bubble slightly with a strange effervescence...");
			carriedItems.remove(new Item(ItemId.SNAKE_WEED.id()));
			carriedItems.remove(new Item(ItemId.ARDRIGAL_SOLUTION.id()));
			carriedItems.getInventory().add(new Item(ItemId.GUJUO_POTION.id()));
		}
	}

	public boolean blockUseInv(Player player, Integer invIndex, Item item, Item usedWith) {
		int itemID = item.getCatalogId();
		int usedWithID = usedWith.getCatalogId();
		if ((player.getWorld().getServer().getEntityHandler().getItemHerbSecond(itemID, usedWithID)) != null
			|| (player.getWorld().getServer().getEntityHandler().getItemHerbSecond(usedWithID, itemID)) != null) {
			return true;
		} else if (itemID == ItemId.PESTLE_AND_MORTAR.id() || usedWithID == ItemId.PESTLE_AND_MORTAR.id()) {
			return true;
		} else if (itemID == ItemId.VIAL.id() || usedWithID == ItemId.VIAL.id()) {
			return true;
		} else if (itemID == ItemId.UNFINISHED_OGRE_POTION.id() && usedWithID == ItemId.GROUND_BAT_BONES.id()
			|| itemID == ItemId.GROUND_BAT_BONES.id() && usedWithID == ItemId.UNFINISHED_OGRE_POTION.id()) {
			return true;
		} else if (itemID == ItemId.UNFINISHED_POTION.id() && (usedWithID == ItemId.GROUND_BAT_BONES.id() || usedWithID == ItemId.GUAM_LEAF.id())
			|| usedWithID == ItemId.UNFINISHED_POTION.id() && (itemID == ItemId.GROUND_BAT_BONES.id() || itemID == ItemId.GUAM_LEAF.id())) {
			return true;
		} else if (usedWithID == ItemId.NITROGLYCERIN.id() && itemID == ItemId.AMMONIUM_NITRATE.id()
				|| usedWithID == ItemId.AMMONIUM_NITRATE.id() && itemID == ItemId.NITROGLYCERIN.id()) {
			return true;
		} else if (usedWithID == ItemId.GROUND_CHARCOAL.id() && itemID == ItemId.MIXED_CHEMICALS_1.id()
				|| usedWithID == ItemId.MIXED_CHEMICALS_1.id() && itemID == ItemId.GROUND_CHARCOAL.id()) {
			return true;
		} else if (usedWithID == ItemId.ARCENIA_ROOT.id() && itemID == ItemId.MIXED_CHEMICALS_2.id()
				|| usedWithID == ItemId.MIXED_CHEMICALS_2.id() && itemID == ItemId.ARCENIA_ROOT.id()) {
			return true;
		} else if (usedWithID == ItemId.UNFINISHED_HARRALANDER_POTION.id() && itemID == ItemId.BLAMISH_SNAIL_SLIME.id()
				|| usedWithID == ItemId.BLAMISH_SNAIL_SLIME.id() && itemID == ItemId.UNFINISHED_HARRALANDER_POTION.id()) {
			return true;
		} else if (usedWithID == ItemId.SNAKES_WEED_SOLUTION.id() && itemID == ItemId.ARDRIGAL.id()
				|| usedWithID == ItemId.ARDRIGAL.id() && itemID == ItemId.SNAKES_WEED_SOLUTION.id()) {
			return true;
		} else if (usedWithID == ItemId.ARDRIGAL_SOLUTION.id() && itemID == ItemId.SNAKE_WEED.id()
				|| usedWithID == ItemId.SNAKE_WEED.id() && itemID == ItemId.ARDRIGAL_SOLUTION.id()) {
			return true;
		}
		return false;
	}

	private void doHerblaw(Player player, final Item vial, final Item herb) {
		int vialID = vial.getCatalogId();
		int herbID = herb.getCatalogId();
		CarriedItems carriedItems = player.getCarriedItems();
		if (!config().MEMBER_WORLD) {
			player.sendMemberErrorMessage();
			return;
		}
		if (vialID == ItemId.VIAL.id() && herbID == ItemId.GROUND_BAT_BONES.id()) {
			player.message("You mix the ground bones into the water");
			player.message("Fizz!!!");
			say(player, null, "Oh dear, the mixture has evaporated!",
				"It's useless...");
			carriedItems.remove(new Item(vialID));
			carriedItems.remove(new Item(herbID));
			carriedItems.getInventory().add(new Item(ItemId.EMPTY_VIAL.id()));
			return;
		}
		if (vialID == ItemId.VIAL.id() && herbID == ItemId.JANGERBERRIES.id()) {
			player.message("You mix the berries into the water");
			carriedItems.remove(new Item(vialID));
			carriedItems.remove(new Item(herbID));
			carriedItems.getInventory().add(new Item(ItemId.UNFINISHED_POTION.id()));
			return;
		}
		if (vialID == ItemId.VIAL.id() && herbID == ItemId.ARDRIGAL.id()) {
			player.message("You put the ardrigal herb into the watervial.");
			player.message("You make a solution of Ardrigal.");
			carriedItems.remove(new Item(vialID));
			carriedItems.remove(new Item(herbID));
			carriedItems.getInventory().add(new Item(ItemId.ARDRIGAL_SOLUTION.id()));
			return;
		}
		if (vialID == ItemId.VIAL.id() && herbID == ItemId.SNAKE_WEED.id()) {
			player.message("You put the Snake Weed herb into the watervial.");
			player.message("You make a solution of Snake Weed.");
			carriedItems.remove(new Item(vialID));
			carriedItems.remove(new Item(herbID));
			carriedItems.getInventory().add(new Item(ItemId.SNAKES_WEED_SOLUTION.id()));
			return;
		}
		final ItemHerbDef herbDef = player.getWorld().getServer().getEntityHandler().getItemHerbDef(herbID);
		if (herbDef == null) {
			return;
		}
		int repeat = 1;
		if (config().BATCH_PROGRESSION) {
			repeat = Math.min(player.getCarriedItems().getInventory().countId(vialID, Optional.of(false)),
				player.getCarriedItems().getInventory().countId(herbID, Optional.of(false)));
		}
		startbatch(repeat);
		batchPotionMaking(player, herb, herbDef, vial);
	}

	private void batchPotionMaking(Player player, Item herb, ItemHerbDef herbDef, Item vial) {
		CarriedItems ci = player.getCarriedItems();
		if (player.getSkills().getLevel(Skills.HERBLAW) < herbDef.getReqLevel()) {
			player.playerServerMessage(MessageType.QUEST, "you need level " + herbDef.getReqLevel()
				+ " herblaw to make this potion");
			return;
		}
		if (player.getQuestStage(Quests.DRUIDIC_RITUAL) != -1) {
			player.message("You need to complete Druidic ritual quest first");
			return;
		}

		herb = player.getCarriedItems().getInventory().get(
			player.getCarriedItems().getInventory().getLastIndexById(herb.getCatalogId(), Optional.of(false))
		);
		vial = player.getCarriedItems().getInventory().get(
			player.getCarriedItems().getInventory().getLastIndexById(vial.getCatalogId(), Optional.of(false))
		);
		if (vial == null || herb == null) return;

		ci.remove(vial);
		ci.remove(herb);
		player.playSound("mix");
		player.playerServerMessage(MessageType.QUEST,
			"You put the " + herb.getDef(player.getWorld()).getName() + " into the vial of water");
		ci.getInventory().add(new Item(herbDef.getPotionId()));
		delay(config().GAME_TICK * 2);

		// Repeat
		updatebatch();
		if (!ifinterrupted() && !ifbatchcompleted()) {
			batchPotionMaking(player, herb, herbDef, vial);
		}
	}

	private void doHerbSecond(Player player, final Item second,
								 final Item unfinished, final ItemHerbSecond def, final boolean isSwapped) {
		int secondID = second.getCatalogId();
		int unfinishedID = unfinished.getCatalogId();
		if (!config().MEMBER_WORLD) {
			player.sendMemberErrorMessage();
			return;
		}
		if (unfinishedID != def.getUnfinishedID()) {
			return;
		}
		final AtomicReference<Item> bubbleItem = new AtomicReference<Item>();
		bubbleItem.set(null);

		// Shaman potion constraint
		if (secondID == ItemId.JANGERBERRIES.id() && unfinishedID == ItemId.UNFINISHED_GUAM_POTION.id() &&
			(player.getQuestStage(Quests.WATCHTOWER) >= 0 && player.getQuestStage(Quests.WATCHTOWER) < 6)) {
			say(player, null, "Hmmm...perhaps I shouldn't try and mix these items together",
				"It might have unpredictable results...");
			return;

		} else if (secondID == ItemId.JANGERBERRIES.id() && unfinishedID == ItemId.UNFINISHED_GUAM_POTION.id()) {
			if (!isSwapped) {
				bubbleItem.set(unfinished);
			} else {
				bubbleItem.set(second);
			}
		}

		int repeat = 1;
		if (config().BATCH_PROGRESSION) {
			repeat = Math.min(player.getCarriedItems().getInventory().countId(secondID),
				player.getCarriedItems().getInventory().countId(unfinishedID));
		}

		startbatch(repeat);
		batchPotionSecondary(player, unfinished, second, def, bubbleItem);
	}

	private void batchPotionSecondary(Player player, Item unfinished, Item second, ItemHerbSecond def, AtomicReference<Item> bubbleItem) {
		if (player.getSkills().getLevel(Skills.HERBLAW) < def.getReqLevel()) {
			player.playerServerMessage(MessageType.QUEST, "You need a herblaw level of "
				+ def.getReqLevel() + " to make this potion");
			return;
		}
		if (player.getQuestStage(Quests.DRUIDIC_RITUAL) != -1) {
			player.message("You need to complete Druidic ritual quest first");
			return;
		}
		if (config().WANT_FATIGUE) {
			if (config().STOP_SKILLING_FATIGUED >= 2
				&& player.getFatigue() >= player.MAX_FATIGUE) {
				player.message("You are too tired to make this potion");
				return;
			}
		}
		CarriedItems carriedItems = player.getCarriedItems();
		unfinished = carriedItems.getInventory().get(
			carriedItems.getInventory().getLastIndexById(unfinished.getCatalogId(), Optional.of(false))
		);
		second = carriedItems.getInventory().get(
			carriedItems.getInventory().getLastIndexById(second.getCatalogId(), Optional.of(false))
		);
		if (unfinished == null || second == null) return;

		if (bubbleItem.get() != null) {
			thinkbubble(bubbleItem.get());
		}
		player.playSound("mix");
		player.playerServerMessage(MessageType.QUEST, "You mix the " + second.getDef(player.getWorld()).getName()
			+ " into your potion");
		carriedItems.remove(second);
		carriedItems.remove(unfinished);
		carriedItems.getInventory().add(new Item(def.getPotionID(), 1));
		player.incExp(Skills.HERBLAW, def.getExp(), true);
		delay(config().GAME_TICK * 2);

		// Repeat
		updatebatch();
		if (!ifinterrupted() && !ifbatchcompleted()) {
			batchPotionSecondary(player, unfinished, second, def, bubbleItem);
		}
	}

	private boolean makeLiquid(Player player, final Item ingredient,
							   final Item unfinishedPot, final boolean isSwapped) {
		if (!config().MEMBER_WORLD) {
			player.sendMemberErrorMessage();
			return false;
		}

		int unfinishedPotID = unfinishedPot.getCatalogId();
		int ingredientID = ingredient.getCatalogId();
		CarriedItems carriedItems = player.getCarriedItems();
		if (unfinishedPotID == ItemId.UNFINISHED_POTION.id() && (ingredientID == ItemId.GROUND_BAT_BONES.id() || ingredientID == ItemId.GUAM_LEAF.id())
			|| ingredientID == ItemId.UNFINISHED_POTION.id() && (unfinishedPotID == ItemId.GROUND_BAT_BONES.id() || unfinishedPotID == ItemId.GUAM_LEAF.id())) {
			player.playerServerMessage(MessageType.QUEST, "You mix the liquid with the " + ingredient.getDef(player.getWorld()).getName().toLowerCase());
			player.message("Bang!!!");
			displayTeleportBubble(player, player.getX(), player.getY(), true);
			player.damage(8);
			say(player, null, "Ow!");
			player.playerServerMessage(MessageType.QUEST, "You mixed this ingredients incorrectly and the mixture exploded!");
			carriedItems.remove(new Item(unfinishedPotID));
			carriedItems.remove(new Item(ingredientID));
			carriedItems.getInventory().add(new Item(ItemId.EMPTY_VIAL.id(), 1));
			return false;
		}
		if (unfinishedPotID == ItemId.UNFINISHED_OGRE_POTION.id() && ingredientID == ItemId.GROUND_BAT_BONES.id()
			|| unfinishedPotID == ItemId.GROUND_BAT_BONES.id() && ingredientID == ItemId.UNFINISHED_OGRE_POTION.id()) {
			if (player.getSkills().getLevel(Skills.HERBLAW) < 14) {
				player.playerServerMessage(MessageType.QUEST,
					"You need to have a herblaw level of 14 or over to mix this liquid");
				return false;
			}
			if (player.getQuestStage(Quests.DRUIDIC_RITUAL) != -1) {
				player.message("You need to complete Druidic ritual quest first");
				return false;
			}
			if (player.getQuestStage(Quests.WATCHTOWER) >= 0 && player.getQuestStage(Quests.WATCHTOWER) < 6) {
				say(player, null, "Hmmm...perhaps I shouldn't try and mix these items together",
					"It might have unpredictable results...");
				return false;
			} else if (carriedItems.hasCatalogID(ingredientID)
				&& carriedItems.hasCatalogID(unfinishedPotID)) {
				if (!isSwapped) {
					thinkbubble(unfinishedPot);
				} else {
					thinkbubble(ingredient);
				}
				player.playerServerMessage(MessageType.QUEST,
					"You mix the " + ingredient.getDef(player.getWorld()).getName().toLowerCase() + " into the liquid");
				player.playerServerMessage(MessageType.QUEST, "You produce a strong potion");
				carriedItems.remove(new Item(ingredientID));
				carriedItems.remove(new Item(unfinishedPotID));
				carriedItems.getInventory().add(new Item(ItemId.OGRE_POTION.id()));
				//the other half has been done already
				player.incExp(Skills.HERBLAW, 100, true);
			}
		}
		return false;
	}

	private void doGrind(Player player, final Item mortar,
							final Item item) {
		if (!config().MEMBER_WORLD) {
			player.sendMemberErrorMessage();
			return;
		}
		int newID;
		switch (ItemId.getById(item.getCatalogId())) {
			case UNICORN_HORN:
				newID = ItemId.GROUND_UNICORN_HORN.id();
				break;
			case BLUE_DRAGON_SCALE:
				newID = ItemId.GROUND_BLUE_DRAGON_SCALE.id();
				break;
			/**
			 * Quest items.
			 */
			case BAT_BONES:
				newID = ItemId.GROUND_BAT_BONES.id();
				break;
			case A_LUMP_OF_CHARCOAL:
				newID = ItemId.GROUND_CHARCOAL.id();
				player.message("You grind the charcoal to a powder");
				break;
			case CHOCOLATE_BAR:
				newID = ItemId.CHOCOLATE_DUST.id();
				break;
			/**
			 * End of Herblaw Quest Items.
			 */
			default:
				return;
		}

		int repeat = 1;
		if (config().BATCH_PROGRESSION) {
			repeat = player.getCarriedItems().getInventory().countId(item.getCatalogId());
		}

		startbatch(repeat);
		batchGrind(player, item, newID);
	}

	private void batchGrind(Player player, Item item, int newID) {
		item = player.getCarriedItems().getInventory().get(
			player.getCarriedItems().getInventory().getLastIndexById(item.getCatalogId(), Optional.of(false))
		);
		if (item == null) return;
		player.getCarriedItems().remove(item);
		if (item.getCatalogId() != ItemId.A_LUMP_OF_CHARCOAL.id()) {
			player.playerServerMessage(MessageType.QUEST, "You grind the " + item.getDef(player.getWorld()).getName()
				+ " to dust");
		}
		if (item.getCatalogId() == ItemId.A_LUMP_OF_CHARCOAL.id() || item.getCatalogId() == ItemId.BAT_BONES.id()) {
			thinkbubble(new Item(ItemId.PESTLE_AND_MORTAR.id()));
		}
		player.getCarriedItems().getInventory().add(new Item(newID, 1));
		delay(config().GAME_TICK);

		// Repeat
		updatebatch();
		if (!ifinterrupted() && !ifbatchcompleted()) {
			delay(config().GAME_TICK);
			batchGrind(player, item, newID);
		}
	}
}
