package de.diddiz.MeasuringTape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;

class Session
{
	private static final HashMap<Player, Session> sessions = new HashMap<Player, Session>();

	public static Session getSession(Player player) {
		Session session = sessions.get(player);
		if (session == null) {
			session = new Session();
			sessions.put(player, session);
		}
		return session;
	}

	boolean MTEnabled;
	final List<Location> pos = new ArrayList<Location>(2);
	MeasuringMode mode;

	Session() {
		mode = MeasuringMode.DISTANCE;
		MTEnabled = Config.defaultEnabled;
		resetPos();
	}

	void resetPos() {
		pos.clear();
		pos.add(null);
		pos.add(null);
	}

	boolean isPos1Set() {
		return pos.get(0) != null;
	}

	boolean isPos2Set() {
		return pos.get(1) != null;
	}
}
