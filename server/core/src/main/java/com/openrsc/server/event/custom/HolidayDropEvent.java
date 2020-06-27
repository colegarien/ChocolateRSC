package com.openrsc.server.event.custom;

import com.openrsc.server.model.Point;
import com.openrsc.server.model.ViewArea;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.world.World;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.MessageType;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

public class HolidayDropEvent extends HourlyEvent  {
	private ArrayList<Integer> items;
	private String eventMessage;
	private Player executor;

	public HolidayDropEvent(final World world, final int lifeTime, final Player executor, final ArrayList<Integer> items) {
		this(world, lifeTime, 0, executor, items, (String)null);
	}

	public HolidayDropEvent(final World world, final int lifeTime, final int minute, final Player executor, final ArrayList<Integer> items) {
		this(world, lifeTime, minute, executor, items, (String)null);
	}

	private HolidayDropEvent(final World world, final int lifeTime, final int minute, final Player executor, final ArrayList<Integer> items, final String eventMessage) {
		super(world, lifeTime, minute,"Holiday Drop Event");
		this.executor = executor;
		this.items = items;
		this.eventMessage = eventMessage;
	}

	public void action() {
		int totalItemsDropped = 0;

		// TODO: This should not require a passed in player.
		ViewArea view = getExecutor().getViewArea();

		for (int y = 96; y < 870; ) { // Highest Y is 867 currently.
			for (int x = 1; x < 770; ) { // Highest X is 766 currently.

				// Check for item dropped right beside this
				if (view.getGroundItem(Point.location(x, y - 1)) == null &&
					view.getGroundItem(Point.location(x - 1, y - 1)) == null &&
					view.getGroundItem(Point.location(x + 1, y - 1)) == null) {

					boolean containsObject = view.getGameObject(Point.location(x, y)) != null;
					int traversal = getWorld().getTile(x, y).traversalMask;
					boolean isBlocking = (
						(traversal & 16) != 0 || // diagonal wall \
							(traversal & 32) != 0 || // diagonal wall /
							(traversal & 64) != 0    // water or black,  etc.
					);
					if (!containsObject && !isBlocking) { // Nothing in the way.
						getWorld().registerItem(new GroundItem(executor.getWorld(), getItems().get(DataConversions.random(0, getItems().size() - 1)), x, y, 1, (Player) null));
						totalItemsDropped++;
					}
				}
				x += DataConversions.random(20, 27); // How much space between each along X axis
			}
			y += DataConversions.random(1, 2);
		}

		for (Player p : getWorld().getPlayers()) {
			if(!p.isAdmin()) {
				continue;
			}

			p.playerServerMessage(MessageType.QUEST, getWorld().getServer().getConfig().MESSAGE_PREFIX + "Dropped " + totalItemsDropped + " of item IDs: " + StringUtils.join(getItems(), ", "));
		}

		if(getEventMessage() != null) {
			for (Player p : getWorld().getPlayers())
				ActionSender.sendMessage(p, null, 0, MessageType.QUEST, getEventMessage(), 0);
		}
	}

	public ArrayList<Integer> getItems() {
		return items;
	}

	private String getEventMessage() {
		return eventMessage;
	}

	private Player getExecutor() {
		return executor;
	}
}
