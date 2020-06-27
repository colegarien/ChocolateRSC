package com.openrsc.server.plugins.skills.agility;

import com.openrsc.server.constants.Skills;
import com.openrsc.server.model.Point;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.OpLocTrigger;
import com.openrsc.server.util.rsc.Formulae;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.openrsc.server.plugins.Functions.*;

public class WildernessAgilityCourse implements OpLocTrigger {

	private static final int GATE = 703;
	private static final int SECOND_GATE = 704;
	private static final int WILD_PIPE = 705;
	private static final int WILD_ROPESWING = 706;
	private static final int STONE = 707;
	private static final int LEDGE = 708;
	private static final int VINE = 709;

	private static Set<Integer> obstacles = new HashSet<Integer>(Arrays.asList(WILD_PIPE, WILD_ROPESWING, STONE, LEDGE));
	private static Integer lastObstacle = VINE;

	@Override
	public boolean blockOpLoc(Player player, GameObject obj, String command) {
		return inArray(obj.getID(), GATE, SECOND_GATE, WILD_PIPE, WILD_ROPESWING, STONE, LEDGE, VINE);
	}

	@Override
	public void onOpLoc(Player player, GameObject obj, String command) {
		final int failRate = failRate();
		if (obj.getID() == GATE) {
			if (getCurrentLevel(player, Skills.AGILITY) < 52) {
				player.message("You need an agility level of 52 to attempt balancing along the ridge");
				return;
			}
			player.message("You go through the gate and try to edge over the ridge");
			delay(config().GAME_TICK * 2);
			teleport(player, 298, 130);
			delay(config().GAME_TICK * 2);
			if (failRate == 1) {
				mes("you lose your footing and fall into the wolf pit");
				teleport(player, 300, 129);
			} else if (failRate == 2) {
				mes("you lose your footing and fall into the wolf pit");
				teleport(player, 296, 129);
			} else {
				mes("You skillfully balance across the ridge");
				teleport(player, 298, 125);
				player.incExp(Skills.AGILITY, 50, true);
			}
			return;
		} else if (obj.getID() == SECOND_GATE) {
			player.message("You go through the gate and try to edge over the ridge");
			delay(config().GAME_TICK * 2);
			teleport(player, 298, 130);
			delay(config().GAME_TICK * 2);
			if (failRate == 1) {
				mes("you lose your footing and fall into the wolf pit");
				teleport(player, 300, 129);

			} else if (failRate == 2) {
				mes("you lose your footing and fall into the wolf pit");
				teleport(player, 296, 129);
			} else {
				mes("You skillfully balance across the ridge");
				teleport(player, 298, 134);
				player.incExp(Skills.AGILITY, 50, true);
			}
			return;
		}
		if (config().WANT_FATIGUE) {
			if (config().STOP_SKILLING_FATIGUED >= 1
				&& player.getFatigue() >= player.MAX_FATIGUE && !inArray(obj.getID(), VINE)) {
				player.message("you are too tired to train");
				return;
			}
		}
		boolean passObstacle = succeed(player);
		switch (obj.getID()) {
			case WILD_PIPE:
				player.message("You squeeze through the pipe");
				delay(config().GAME_TICK * 2);
				teleport(player, 294, 112);
				player.incExp(Skills.AGILITY, 50, true);
				AgilityUtils.completedObstacle(player, obj.getID(), obstacles, lastObstacle, 1500);
				return;
			case WILD_ROPESWING:
				player.message("You grab the rope and try and swing across");
				delay(config().GAME_TICK * 2);
				if (passObstacle) {
					mes("You skillfully swing across the hole");
					boundaryTeleport(player, Point.location(292, 108));
					player.incExp(Skills.AGILITY, 100, true);
					AgilityUtils.completedObstacle(player, obj.getID(), obstacles, lastObstacle, 1500);
					return;
				} else { // 13 damage on 85hp.
					// 11 damage on 73hp.
					//
					player.message("Your hands slip and you fall to the level below");
					delay(config().GAME_TICK * 2);
				}
				int damage = (int) Math.round((player.getSkills().getLevel(Skills.HITS)) * 0.15D);
				teleport(player, 293, 2942);
				player.message("You land painfully on the spikes");
				say(player, null, "ouch");
				player.damage(damage);
				return;
			case STONE:
				player.message("you stand on the stepping stones");
				delay(config().GAME_TICK * 2);
				if (passObstacle) {
					boundaryTeleport(player, Point.location(293, 105));
					delay(config().GAME_TICK);
				} else {
					player.message("Your lose your footing and land in the lava");
					teleport(player, 292, 104);
					int lavaDamage = (int) Math.round((player.getSkills().getLevel(Skills.HITS)) * 0.21D);
					player.damage(lavaDamage);
					return ;
				}
				boundaryTeleport(player, Point.location(294, 104));
				delay(config().GAME_TICK);
				boundaryTeleport(player, Point.location(295, 104));
				player.message("and walk across");
				delay(config().GAME_TICK);
				boundaryTeleport(player, Point.location(296, 105));
				delay(config().GAME_TICK);
				boundaryTeleport(player, Point.location(297, 106));
				player.incExp(Skills.AGILITY, 80, true);
				AgilityUtils.completedObstacle(player, obj.getID(), obstacles, lastObstacle, 1500);
				return;
			case LEDGE:
				player.message("you stand on the ledge");
				delay(config().GAME_TICK * 2);
				if (passObstacle) {
					boundaryTeleport(player, Point.location(296, 112));
					delay(config().GAME_TICK);
					player.message("and walk across");
					boundaryTeleport(player, Point.location(297, 112));
					delay(config().GAME_TICK);
					boundaryTeleport(player, Point.location(298, 112));
					delay(config().GAME_TICK);
					boundaryTeleport(player, Point.location(299, 111));
					delay(config().GAME_TICK);
					boundaryTeleport(player, Point.location(300, 111));
					delay(config().GAME_TICK);
					boundaryTeleport(player, Point.location(301, 111));
					player.incExp(Skills.AGILITY, 80, true);
					AgilityUtils.completedObstacle(player, obj.getID(), obstacles, lastObstacle, 1500);
				} else {
					player.message("you lose your footing and fall to the level below");
					delay(config().GAME_TICK * 2);
					int ledgeDamage = (int) Math.round((player.getSkills().getLevel(Skills.HITS)) * 0.25D);
					teleport(player, 298, 2945);
					player.message("You land painfully on the spikes");
					say(player, null, "ouch");
					player.damage(ledgeDamage);
				}
				return;
			case VINE:
				player.message("You climb up the cliff");
				delay(config().GAME_TICK * 2);
				boundaryTeleport(player, Point.location(305, 118));
				delay(config().GAME_TICK);
				boundaryTeleport(player, Point.location(304, 119));
				delay(config().GAME_TICK);
				boundaryTeleport(player, Point.location(304, 120));
				player.incExp(Skills.AGILITY, 80, true); // COMPLETION OF THE COURSE.
				AgilityUtils.completedObstacle(player, obj.getID(), obstacles, lastObstacle, 1500);
				return;
		}
	}

	private boolean succeed(Player player) {
		return Formulae.calcProductionSuccessful(52, getCurrentLevel(player, Skills.AGILITY), true, 102);
	}

	private int failRate() {
		return random(1, 5);
	}
}
