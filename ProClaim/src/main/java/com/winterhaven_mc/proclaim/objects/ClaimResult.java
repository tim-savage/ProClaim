package com.winterhaven_mc.proclaim.objects;

import com.winterhaven_mc.proclaim.storage.Claim;

import java.util.HashSet;

public final class ClaimResult {

	private boolean success = true;
	private Claim resultClaim;
	private final HashSet<Claim> overlapClaims = new HashSet<>();
	
	
	public final boolean isSuccess() {
		return success;
	}

	public final void setSuccess(final boolean success) {
		this.success = success;
	}

	final Claim getResultClaim() {
		return resultClaim;
	}

	public final void setResultClaim(final Claim resultClaim) {
		this.resultClaim = resultClaim;
	}
	
	public final void addOverlapClaim(final Claim overlapClaim) {
		this.overlapClaims.add(overlapClaim);
	}
	
	final HashSet<Claim> getOverlapClaims() {
		return this.overlapClaims;
	}
	
}
