package plus.crates.csgo;

import org.bukkit.plugin.java.JavaPlugin;
import plus.crates.CratesPlus;

public class CSGO extends JavaPlugin {

	public void onEnable() {
		CratesPlus.getOpenHandler().registerOpener(new CSGOOpener(this, "CSGO"));
	}

}
