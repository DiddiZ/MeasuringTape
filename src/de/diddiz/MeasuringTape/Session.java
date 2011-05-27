package de.diddiz.MeasuringTape;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;

class Session
{
	public Boolean MTEnabled;
	public List<Location> pos;
	public MeasuringMode mode;
	public long lastTape;

	public Session(boolean enabled) {
		lastTape = 0;
		mode = MeasuringMode.DISTANCE;
		MTEnabled = enabled;
		ResetPos();
	}

	public void ResetPos() {
		pos = new ArrayList<Location>();
		pos.add(null);
		pos.add(null);
	}

	public boolean isPos1Set() {
		return pos.get(0) != null;
	}

	public boolean isPos2Set() {
		return pos.get(1) != null;
	}
}
