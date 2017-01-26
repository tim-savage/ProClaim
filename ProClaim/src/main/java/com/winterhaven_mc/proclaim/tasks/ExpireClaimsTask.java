package com.winterhaven_mc.proclaim.tasks;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.scheduler.BukkitRunnable;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.storage.Claim;
import com.winterhaven_mc.proclaim.storage.PlayerState;

public final class ExpireClaimsTask extends BukkitRunnable {

	// reference to main class
    private final PluginMain plugin;

    /**
     * Class constructor
     * @param plugin reference to plugin main class
     */
    ExpireClaimsTask(PluginMain plugin) {
    	
    	// set reference to main class
    	this.plugin = plugin;
    }
    
    
	@Override
	public void run() {

		// if claim-auto-expire is configured false, do nothing and return
		if (!plugin.getConfig().getBoolean("claim-auto-expire")) {
			return;
		}
		
		// get configured expire days
		final Long expireDays = plugin.getConfig().getLong("claim-expire-days");

		// if expireDays is configured zero or negative, do not expire claims
		if (expireDays <= 0) {
			return;
		}
		
		// get Set of all claims
		final Set<Claim> claims = new HashSet<>(Claim.getAllClaims());

		// iterate over set of all claims
		for (Claim claim : claims) {
			
			// if claim is not admin claim and claim is not locked...
			if (!claim.isAdminClaim() && !claim.isLocked()) {

				// get claim owner player state
				final PlayerState owner = PlayerState.getPlayerState(claim.getOwnerUUID());

				// get claim expiration date
				final Instant claimExpireDate = owner.getLastLogin().plus(expireDays,ChronoUnit.DAYS);

				// if current time is after claim expiration date, expire claim
				if (Instant.now().isAfter(claimExpireDate)) {
				
					// expire claim
					claim.abandon();
				}
			}
		}
	}
	
}
