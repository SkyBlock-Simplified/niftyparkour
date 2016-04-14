package net.netcoding.niftyparkour.cache;

public enum Keys {

	CHECKPOINT("checkpoint2"),
	MENU("menu2"),
	SPAWN("spawn2"),
	WARP("warp2");

	private final String key;

	Keys(String key) {
		this.key = key;
	}

	public static Keys getKey(String key) {
		for (Keys real : values()) {
			if (real.toString().equals(key))
				return real;
		}

		return null;
	}

	public static boolean hasKey(String key) {
		return getKey(key) != null;
	}

	public static boolean isKey(Keys key, String compare) {
		return key == getKey(compare);
	}

	@Override
	public String toString() {
		return this.key;
	}

}