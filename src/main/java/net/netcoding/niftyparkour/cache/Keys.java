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

	public static boolean isKey(Keys key, String compare) {
		return key == valueOf(compare);
	}

	public static boolean hasKey(String key) {
		return valueOf(key) != null;
	}

	@Override
	public String toString() {
		return this.key;
	}

}