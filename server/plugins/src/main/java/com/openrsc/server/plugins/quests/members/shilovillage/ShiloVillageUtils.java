package com.openrsc.server.plugins.quests.members.shilovillage;

import com.openrsc.server.constants.*;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.plugins.triggers.DropObjTrigger;
import com.openrsc.server.plugins.triggers.OpInvTrigger;
import com.openrsc.server.plugins.triggers.UseInvTrigger;
import com.openrsc.server.plugins.triggers.TakeObjTrigger;

import static com.openrsc.server.plugins.Functions.*;

public class ShiloVillageUtils implements DropObjTrigger, OpInvTrigger, UseInvTrigger, TakeObjTrigger {

	static void BUMPY_DIRT_HOLDER(Player player) {
		player.message("Do you want to try to crawl through the fissure?");
		if (player.getCache().hasKey("SV_DIG_ROPE")) {
			player.message("You see that a rope is attached nearby");
		}
		int menu = multi(player,
			"Yes, I'll give it a go!",
			"No thanks, it looks a bit dark!");
		if (menu == 0) {
			player.message("You start to contort your body...");
			mes("With some dificulty you manage to push your body",
					"through the small crack in the rock.");
			if (!player.getCache().hasKey("SV_DIG_ROPE")) {
				mes("As you squeeze out of the hole...");
				player.message("you realise that there is a huge drop underneath you");
				player.message("You begin falling....");
				player.teleport(380, 3692);
				delay(config().GAME_TICK);
				say(player, null, "Ahhhhh!");
				player.damage(1);
				player.message("Your body is battered as you hit the cavern walls.");
				say(player, null, "Ooooff!");
				player.damage(1);
				delay(config().GAME_TICK);
				player.teleport(352, 3650);
				player.damage((int) (getCurrentLevel(player, Skills.HITS) * 0.2 + 10));
				mes("You hit the floor and it knocks the wind out of you!");
				say(player, null, "Ugghhhh!!");
			}
			else {
				mes("You squeeze through the fissure in the granite",
						"And once through, you cleverly use the rope to slowly lower",
						"yourself to the floor.");
				say(player, null, "Yay!");
				player.teleport(352, 3650);
			}
			player.incExp(Skills.AGILITY, 30, true);
			if(player.getQuestStage(Quests.SHILO_VILLAGE) == 2) {
				player.updateQuestStage(Quests.SHILO_VILLAGE, 3);
			}
		} else if (menu == 1) {
			player.message("You think better of attempting to squeeze your body into the fissure.");
			say(player, null, "It looked very dangerous, and dark...",
				"scarey!");
		}
	}

	public static boolean succeed(Player player, int req) {
		int level_difference = getCurrentLevel(player, Skills.AGILITY) - req;
		int percent = random(1, 100);

		if (level_difference < 0)
			return true;
		if (level_difference >= 15)
			level_difference = 80;
		if (level_difference >= 20)
			level_difference = 90;
		else
			level_difference = 30 + level_difference;

		return percent <= level_difference;
	}

	// Zadimus: 589
	private void dropZadimusCorpse(Player player) {
		mes("You feel an uneartly compunction to bury this corpse!");
		if (player.getLocation().inBounds(445, 749, 449, 753)) {
			mes("You hear an unearthly moaning sound as you see",
				"an apparition materialises right in front of you.");
			Npc zadimus = addnpc(player.getWorld(), NpcId.ZADIMUS.id(), player.getX(), player.getY(), 60000);
			delay(config().GAME_TICK);
			if (zadimus != null) {
				npcsay(player, zadimus, "You have released me from my torture, and now I shall aid you");
				delay(config().GAME_TICK);
				npcsay(player, zadimus, "You seek to dispell the one who tortured and killed me");
				delay(config().GAME_TICK);
				npcsay(player, zadimus, "Remember this...");
				delay(config().GAME_TICK);
				npcsay(player, zadimus, "'I am the key, but only kin may approach her.'");
				mes("The apparition disapears into the ground where you buried the corpse.");
				zadimus.remove();
				mes("You see the ground in front of you shake ",
					"as a shard of bone forces its way to the surface.");
				player.message("You take the bone shard and place it in your inventory.");
				player.getCarriedItems().remove(new Item(ItemId.ZADIMUS_CORPSE.id()));
				player.getCarriedItems().getInventory().add(new Item(ItemId.BONE_SHARD.id()));
				if (player.getQuestStage(Quests.SHILO_VILLAGE) == 3) {
					player.setQuestStage(Quests.SHILO_VILLAGE, 4);
				}
			}
		} else {
			mes("You hear a ghostly wailing sound coming from the corpse",
				"and a whispering voice says,");
			player.message("'@yel@Zadimus: Let me rest in a sacred place and assist you I will'");
		}
	}

	@Override
	public boolean blockDropObj(Player player, Integer invIndex, Item item, Boolean fromInventory) {
		return inArray(item.getCatalogId(), ItemId.STONE_PLAQUE.id(), ItemId.CRUMPLED_SCROLL.id(), ItemId.TATTERED_SCROLL.id(), ItemId.ZADIMUS_CORPSE.id(),
				ItemId.BONE_KEY.id(), ItemId.BONE_BEADS.id(), ItemId.BONE_SHARD.id(), ItemId.LOCATING_CRYSTAL.id(), ItemId.BERVIRIUS_TOMB_NOTES.id(),
				ItemId.SWORD_POMMEL.id(), ItemId.RASHILIYA_CORPSE.id(), ItemId.BEADS_OF_THE_DEAD.id());
	}

	//533
	@Override
	public void onDropObj(Player player, Integer invIndex, Item item, Boolean fromInventory) {
		if (item.getCatalogId() == ItemId.RASHILIYA_CORPSE.id()) {
			mes("The remains of Rashiliyia look quite delicate.",
				"You sense that a spirit needs to be put to rest.");
			player.message("Are you sure that you want to drop the remains ?");
			int menu = multi(player,
				"Yes, I am sure.",
				"No, I'll keep hold of the remains.");
			if (menu == 0) {
				if (player.getCache().hasKey("dolmen_zombie")) {
					player.getCache().remove("dolmen_zombie");
				}
				if (player.getCache().hasKey("dolmen_skeleton")) {
					player.getCache().remove("dolmen_skeleton");
				}
				if (player.getCache().hasKey("dolmen_ghost")) {
					player.getCache().remove("dolmen_ghost");
				}
				player.getCarriedItems().remove(new Item(ItemId.RASHILIYA_CORPSE.id()));
				mes("You drop Rashiliyias remains on the ground.",
					"The bones turn to dust and forms into the shape of a human figure.");
				Npc rash = addnpc(player.getWorld(), NpcId.RASHILIYIA.id(), player.getX(), player.getY(), 30000);
				mes("The figure turns to you and you hear a cackling, croaky voice on the air.");
				if (rash != null) {
					npcsay(player, rash, "Many thanks for releasing me!",
						"Please excuse me, I must attend to my plans!");
					rash.remove();
				}
				player.message("The figure turns and soars away quickly disapearing into the distance.");
			} else if (menu == 1) {
				player.message("You decide to keep hold of Rashiliyias remains.");
			}
		}
		else if (item.getCatalogId() == ItemId.BEADS_OF_THE_DEAD.id()) {
			mes("Are you sure you want to drop the Beads of the Dead?");
			player.message("It looks very rare and unique.");
			int menu = multi(player,
				"Yes, I'm sure.",
				"Nope, I've had second thoughts.");
			if (menu == 0) {
				mes("As the necklace hits the floor, it disintigrates",
					"into a puff of white powder.");
				player.message("and you start to wonder if it ever really existed?");
				player.getCarriedItems().remove(new Item(ItemId.BEADS_OF_THE_DEAD.id()));
			} else if (menu == 1) {
				player.message("You decide not to drop the Beads of the Dead.");
			}
		}
		else if (item.getCatalogId() == ItemId.BONE_BEADS.id()) {
			mes("As the beads hit the floor, they disintegrate into");
			player.message("puffs of white powder.");
			player.getCarriedItems().remove(new Item(ItemId.BONE_BEADS.id()));
		}
		else if (item.getCatalogId() == ItemId.BERVIRIUS_TOMB_NOTES.id()) {
			player.message("As you drop the delicate scrolls onto the floor, they");
			player.message("disintegrate immediately.");
			if (!player.getCache().hasKey("dropped_writing")) {
				player.getCache().store("dropped_writing", true);
			}
			player.getCarriedItems().remove(new Item(ItemId.BERVIRIUS_TOMB_NOTES.id()));
		}
		else if (item.getCatalogId() == ItemId.LOCATING_CRYSTAL.id()) {
			player.message("Are you sure you want to drop this crystal?");
			player.message("It looks very delicate and it may break.");
			int menu = multi(player,
				"Yes, I am sure.",
				"No, I've reconsidered, I'll keep it!");
			if (menu == 0) {
				mes("As you drop the cystal, it hits a rock and explodes.");
				player.message("You are lascerated by shards of glass.");
				player.damage(10);
				player.getCarriedItems().remove(new Item(ItemId.LOCATING_CRYSTAL.id()));
			} else if (menu == 1) {
				player.message("You decide to keep the Locating Crystal ");
				player.message("tucked into your inventory safe and sound.");
			}
		}
		else if (item.getCatalogId() == ItemId.SWORD_POMMEL.id()) {
			mes("You drop the sword pommel on the floor.");
			player.message("It turns to dust as soon as it hits the ground.");
			player.getCarriedItems().remove(new Item(ItemId.SWORD_POMMEL.id()));
		}
		else if (item.getCatalogId() == ItemId.BONE_KEY.id()) {
			player.message("This looks quite valuable.");
			player.message("As you go to throw the item away");
			player.message("Zadimus' words come to you again.");
			player.message("@yel@'I am the key, but only kin may approach her'");
		}
		else if (item.getCatalogId() == ItemId.BONE_SHARD.id()) {
			player.message("You cannot bring yourself to drop this item.");
			player.message("You remember the words that Zadimus said when he appeared");
			player.message("in front of you.");
			player.message("@yel@'I am the key, but only kin may approach her.");
		}
		else if (item.getCatalogId() == ItemId.CRUMPLED_SCROLL.id()) {
			player.message("This looks quite important, are you sure you want to drop it?");
			int menu = multi(player,
				"Yes, I'm sure.",
				"No, I think I'll keep it.");
			if (menu == 0) {
				player.message("As you drop the item, it gets carried off by the wind.");
				player.message("never to be seen again.");
				player.getCarriedItems().remove(new Item(ItemId.CRUMPLED_SCROLL.id()));
			} else if (menu == 1) {
				player.message("You decide against throwing the item away.");
			}
		}
		else if (item.getCatalogId() == ItemId.TATTERED_SCROLL.id()) {
			player.message("This looks quite important, are you sure you want to drop it?");
			int menu = multi(player,
				"Yes, I'm sure.",
				"No, I think I'll keep it.");
			if (menu == 0) {
				player.message("You decide to throw the item away.");
				player.message("As you drop the item, it falls down a narrow crevice.");
				player.message("never to be seen again.");
				player.getCarriedItems().remove(new Item(ItemId.TATTERED_SCROLL.id()));
			} else if (menu == 1) {
				player.message("You decide against throwing the item away.");
			}
		}
		else if (item.getCatalogId() == ItemId.ZADIMUS_CORPSE.id()) {
			dropZadimusCorpse(player);
		}
		else if (item.getCatalogId() == ItemId.STONE_PLAQUE.id()) {
			player.message("This looks quite important, are you sure you want to drop it?");
			int menu = multi(player,
				"Yes, I'm sure.",
				"No, I think I'll keep it.");
			if (menu == 0) {
				player.message("As you drop the item, it bounces into a stream.");
				player.message("never to be seen again.");
				player.getCarriedItems().remove(new Item(ItemId.STONE_PLAQUE.id()));
			} else if (menu == 1) {
				player.message("You decide against throwing the item away.");
			}
		}
	}

	@Override
	public boolean blockOpInv(Player player, Integer invIndex, Item item, String command) {
		return inArray(item.getCatalogId(), ItemId.ZADIMUS_CORPSE.id(), ItemId.CRUMPLED_SCROLL.id(), ItemId.TATTERED_SCROLL.id(), ItemId.STONE_PLAQUE.id(),
				ItemId.BONE_SHARD.id(), ItemId.BERVIRIUS_TOMB_NOTES.id(), ItemId.LOCATING_CRYSTAL.id(), ItemId.BONE_KEY.id(), ItemId.RASHILIYA_CORPSE.id());
	}

	@Override
	public void onOpInv(Player player, Integer invIndex, Item item, String command) { // bury corpse
		if (item.getCatalogId() == ItemId.RASHILIYA_CORPSE.id()) {
			player.message("Nothing interesting happens");
		}
		else if (item.getCatalogId() == ItemId.BONE_KEY.id()) { // bone key
			player.message("The key is intricately carved out of bone.");
		}
		else if (item.getCatalogId() == ItemId.LOCATING_CRYSTAL.id()) { // activate crystal
			mes("You feel the crystal trying to draw upon your spiritual energy.");
			player.message("Do you want to let it.");
			int menu = multi(player,
				"Yes, that seems fine.",
				"No, it sounds a bit dangerous.");
			if (menu == 0) {
				if (getCurrentLevel(player, Skills.PRAYER) < 10) {
					player.message("You have no spiritual energy that the crystal can draw from.");
					delay(config().GAME_TICK * 2);
					player.message("You need to have at least 10 prayer points for it to work.");
					return;
				}
				int objectX = 351;
				//TODO: check ranges
				if (objectX - player.getX() <= 5 && objectX - player.getX() >= -5) {
					player.message("The crystal blazes brilliantly.");
					player.getSkills().subtractLevel(Skills.PRAYER, 1);
				} else if (objectX - player.getX() <= 7 && objectX - player.getX() >= -7) {
					player.message("@yel@The crystal is very bright.");
					player.getSkills().subtractLevel(Skills.PRAYER, 1);
				}else if (objectX - player.getX() <= 10 && objectX - player.getX() >= -10) {
					player.message("@red@The crystal glows brightly");
					player.getSkills().subtractLevel(Skills.PRAYER, 1);
				} else if (objectX - player.getX() <= 20 && objectX - player.getX() >= -20) {
					player.message("The crystal glows feintly");
					player.getSkills().subtractLevel(Skills.PRAYER, 1);
				} else {
					player.message("Nothing seems different about the Crystal.");
					player.getSkills().subtractLevel(Skills.PRAYER, 2);
				}
			} else if (menu == 1) {
				player.message("You decide not to allow the crystal to draw spiritual energy from your body.");
			}
		}
		else if (item.getCatalogId() == ItemId.BERVIRIUS_TOMB_NOTES.id()) { // read tomb notes
			player.message("This scroll is a collection of writings..");
			delay(config().GAME_TICK * 2);
			player.message("Some of them are just scraps of papyrus with what looks like random scribblings.");
			delay(config().GAME_TICK * 2);
			player.message("Which would you like to read?");
			delay(config().GAME_TICK * 2);
			int menu = multi(player,
				"Tattered Yellow papyrus",
				"Decayed White papyrus",
				"Crusty Orange papyrus");
			if (menu >= 0 && !player.getCache().hasKey("read_tomb_notes")) {
				player.getCache().store("read_tomb_notes", true);
			}
			if (menu == 0) {
				ActionSender.sendBox(player, "...and rest like your mother who is silent in the peace of her "
					+ "tomb far to the North of Ah Za Rhoon. Near the sea, and under "
					+ "the hills deep in the underground to watch all of nature from the "
					+ "darkness of her final resting place.", false);
			} else if (menu == 1) {
				ActionSender.sendBox(player, "...Rashiliyia did so love objects of beauty. Her tomb was "
					+ "adnorned with crystals that glowed brightly when near to each other.", false);
			} else if (menu == 2) {
				ActionSender.sendBox(player, "...the sphere is activated when power of a spiritual nature is "
					+ "expended upon it, this can be very draining on the body...", false);
			}
		}
		else if (item.getCatalogId() == ItemId.BONE_SHARD.id()) {
			player.message("The words of Zadimus come back to you.");
			player.message("@yel@'I am the key, but only kin may approach her.'");
		}
		else if (item.getCatalogId() == ItemId.ZADIMUS_CORPSE.id()) {
			dropZadimusCorpse(player);
		}
		else if (item.getCatalogId() == ItemId.CRUMPLED_SCROLL.id()) {
			mes("This looks like part of a scroll about Rashiliyia",
				"Would you like to read it?");
			int menu = multi(player,
				"Yes please!",
				"No thanks.");
			if (menu == 0) {
				ActionSender.sendBox(player, "Rashiliyia's rage went unchecked.% %"
					+ "She killed without mercy for revenge of her sons life.% %"
					+ "Like a spectre through the night she entered houses and one "
					+ "by one quietly strangled life from the occupants.% %"
					+ "It is said that only a handful survived, protected by necklace wards to keep the Witch Queen at bay.", true);
			} else if (menu == 1) {
				player.message("You decide to leave the scroll well alone.");
			}
		}
		else if (item.getCatalogId() == ItemId.TATTERED_SCROLL.id()) {
			mes("This looks like part of a scroll about someone called Berverius..");
			player.message("Would you like to read it?");
			int menu = multi(player,
				"Yes please.",
				"No thanks.");
			if (menu == 0) {
				ActionSender.sendBox(player, "Bervirius, song of King Danthalas, was killed in battle.% %"
					+ "His devout Mother Rashiliyia was so heartbroken that she% swore fealty to Zamorak "
					+ "if he would return her son to her.% %"
					+ "Bervirius returned as an undead creature and terrorized the "
					+ "King and Queen. Many guards died fighting the Undead "
					+ "Berverious, eventually the undead Bervirius was set on fire and "
					+ "soon only the bones remained.% %"
					+ "His remains were taken far to the South, and then towards the "
					+ "setting sun to a tomb that is surrounded by and level with the "
					+ "sea. The only remedy for containing the spirits of witches and undead.", true);
			} else if (menu == 1) {
				player.message("You decide not to open the scroll but instead put it carefully back into your inventory.");
			}
		}
		else if (item.getCatalogId() == ItemId.STONE_PLAQUE.id()) {
			mes("The markings are very intricate. It's a very strange language.",
					"The meaning of it evades you though.");
		}
	}

	@Override
	public boolean blockUseInv(Player player, Integer invIndex, Item item1, Item item2) {
		//chisel and pommel sword / bone beads and wire / chisel and bone shard
		return compareItemsIds(item1, item2, ItemId.CHISEL.id(), ItemId.SWORD_POMMEL.id())
				|| compareItemsIds(item1, item2, ItemId.BONE_BEADS.id(), ItemId.BRONZE_WIRE.id())
				|| compareItemsIds(item1, item2, ItemId.CHISEL.id(), ItemId.BONE_SHARD.id());
	}

	@Override
	public void onUseInv(Player player, Integer invIndex, Item item1, Item item2) {
		if (compareItemsIds(item1, item2, ItemId.BONE_BEADS.id(), ItemId.BRONZE_WIRE.id())) {
			if (getCurrentLevel(player, Skills.CRAFTING) < 20) {
				player.message("You need a level of 20 Crafting to craft this.");
				return;
			}
			mes("You successfully craft the beads and Bronze Wire ");
			player.message("into a necklace which you name, 'Beads of the dead'");
			player.getCarriedItems().remove(new Item(ItemId.BRONZE_WIRE.id()));
			player.getCarriedItems().remove(new Item(ItemId.BONE_BEADS.id()));
			player.getCarriedItems().getInventory().add(new Item(ItemId.BEADS_OF_THE_DEAD.id()));
		}
		else if (compareItemsIds(item1, item2, ItemId.CHISEL.id(), ItemId.BONE_SHARD.id())) {
			if (player.getQuestStage(Quests.SHILO_VILLAGE) == -1) {
				player.message("You're not quite sure what to make with this.");
				return;
			}
			if (player.getCache().hasKey("can_chisel_bone")) {
				if (getCurrentLevel(player, Skills.CRAFTING) < 20) {
					player.message("You need a level of 20 Crafting to craft this.");
					return;
				}
				mes("Remembering Zadimus' words and the strange bone lock,",
					"you start to craft the bone.");
				player.message("You succesfully make a key out of the bone shard.");
				player.getCarriedItems().remove(new Item(ItemId.BONE_SHARD.id()));
				player.getCarriedItems().getInventory().add(new Item(ItemId.BONE_KEY.id()));
				player.incExp(Skills.CRAFTING, 35, true);
			} else {
				mes("You're not quite sure what to make with this.");
				player.message("Perhaps it will come to you as you discover more about Rashiliyia?");
			}
		}
		else if (compareItemsIds(item1, item2, ItemId.CHISEL.id(), ItemId.SWORD_POMMEL.id())) {
			if (getCurrentLevel(player, Skills.CRAFTING) < 20) {
				player.message("You need a level of 20 Crafting to craft this.");
				return;
			}
			mes("You prepare the ivory pommel and the chisel to start crafting...",
				"You successfully craft some of the ivory into beads.");
			player.message("They may look good as part of a necklace.");
			player.incExp(Skills.CRAFTING, 35, true);
			player.getCarriedItems().remove(new Item(ItemId.SWORD_POMMEL.id()));
			player.getCarriedItems().getInventory().add(new Item(ItemId.BONE_BEADS.id()));
		}
	}

	@Override
	public boolean blockTakeObj(Player player, GroundItem i) {
		return i.getID() == ItemId.COINS.id() && i.getX() == 358 && i.getY() == 3626;
	}

	@Override
	public void onTakeObj(Player player, GroundItem i) {
		if (i.getID() == ItemId.COINS.id() && i.getX() == 358 && i.getY() == 3626) {
			if (player.getCache().hasKey("coins_shilo_cave")) {
				i.remove();
				give(player, ItemId.COINS.id(), 10);
				player.message("The coins turn to dust in your hand...");
			} else {
				mes("As soon as you touch the coins...",
					"You hear the grinding sound of bones");
				player.message("against stone as you see skeletons and ");
				delay(config().GAME_TICK * 2);
				player.message("Zombies rising up out of the ground.");
				addnpc(player.getWorld(), 40, player.getX() - 1, player.getY() + 1, 60000);
				addnpc(player.getWorld(), 40, player.getX() - 1, player.getY() - 1, 60000);

				addnpc(player.getWorld(), 542, player.getX() + 2, player.getY() + 1, 60000);
				addnpc(player.getWorld(), 542, player.getX() + 1, player.getY() - 1, 60000);
				player.message("The coins turn to dust in your hands.");
				player.getCache().store("coins_shilo_cave", true);
			}
		}
	}
}
