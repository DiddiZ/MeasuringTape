package de.diddiz.MeasuringTape;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;

class Session
{
	boolean MTEnabled;
	final List<Location> pos = new ArrayList<Location>(2);
	MeasuringMode mode;
	long lastTape;

	Session(boolean enabled) {
		lastTape = 0;
		mode = MeasuringMode.DISTANCE;
		MTEnabled = enabled;
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
