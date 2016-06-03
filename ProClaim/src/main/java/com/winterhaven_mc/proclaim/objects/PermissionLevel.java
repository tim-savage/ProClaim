package com.winterhaven_mc.proclaim.objects;

@SuppressWarnings("SimplifiableIfStatement")
public enum PermissionLevel {
	
	/* Permission Values:
	 * NONE = 0;				// 00000000 0x00
	 * UNDEF1 = 1;				// 00000001 0x01
	 * ACCESS = 2;				// 00000010 0x02
	 * UNDEF2 = 4;				// 00000100 0x04
	 * CONTAINER = 8;			// 00001000 0x08
	 * UNDEF3 = 16;				// 00010000 0x10
	 * BUILD = 32;				// 00100000 0x20
	 * GRANT = 64;				// 01000000 0x40
	 * 
	 * UNDEF1_GRANT = 65;		// 01000001 0x41
	 * ACCESS_GRANT = 66;		// 01000010 0x42
	 * UNDEF2_GRANT = 68;		// 01000100 0x44
	 * COUNTAINER_GRANT = 72;	// 01001000 0x48
	 * UNDEF3_GRANT = 80;		// 01010000 0x50
	 * BUILD_GRANT = 96;		// 01100000 0x60
	 */
	
	/* Permission Masks:
	 * NONE = 0;				// 00000000 0x00
	 * UNDEF1 = 1;				// 00000001 0x01
	 * ACCESS = 3;				// 00000011 0x03
	 * UNDEF2 = 7;				// 00000111 0x07
	 * CONTAINER = 15;			// 00001111 0x0F
	 * UNDEF3 = 31;				// 00011111 0x1F
	 * BUILD = 63;				// 00111111 0x3F
	 * GRANT = 64;				// 01000000 0x40
	 * 
	 * UNDEF1_GRANT = 65;		// 01000001 0x41
	 * ACCESS_GRANT = 67;		// 01000011 0x43
	 * UNDEF2_GRANT = 71;		// 01000111 0x47
	 * CONTAINER_GRANT = 79;	// 01001111 0x4F
	 * UNDEF3_GRANT = 95;		// 01011111 0x5F
	 * BUILD_GRANT = 127;		// 01111111 0x7F
	 */

// Undefined values are for possible future permission levels
	
	NONE((byte) 0, (byte) 0),
//	UNDEF1((byte) 1, (byte) 1),
	ACCESS((byte) 2, (byte) 3),
//	UNDEF2((byte) 4, (byte) 7),
	CONTAINER((byte) 8, (byte) 15),
//	UNDEF3((byte) 16, (byte) 31),
	BUILD((byte) 32, (byte) 63),
	GRANT((byte) 64, (byte) 64),
//	GRANT_UNDEF1((byte) 65, (byte) 65),
	ACCESS_GRANT((byte) 66, (byte) 67),
//	GRANT_UNDEF2((byte) 68, (byte) 71),
	CONTAINER_GRANT((byte) 72, (byte) 79),
//	GRANT_UNDEF3((byte) 80, (byte) 95);
	BUILD_GRANT((byte) 96, (byte) 127);
	
	private final byte value;
	private final byte mask;
	
	/**
	 * Class constructor
	 * @param value
	 */
	private PermissionLevel(final byte value, final byte mask) {
		this.value = value;
		this.mask = mask;
	}
	
	/**
	 * Get the mask value of a PermissionLevel
	 * @return
	 */
	public final byte getMask() {
		return mask;
	}

	/**
	 * Get the byte value of a PermissionLevel
	 * @return
	 */
	public final byte toByte() {
		return this.value;
	}

	
	/**
	 * Get a corresponding PermissionLevel from a byte value
	 * @param value
	 * @return ClaimPermission enum
	 */
	public final static PermissionLevel fromByte(final byte value) {

		PermissionLevel returnValue = PermissionLevel.NONE;
		
		for (PermissionLevel p : PermissionLevel.values()) {
			if (p.toByte() == value) {
				returnValue = p;
			}
		}
		return returnValue;
	}
	

	/**
	 * Test if this permission level gives permission for passed permission level<br>
	 * Example usage:<br>
	 *  boolean iCanHazAccess = myPerm.allows(PermissionLevel.ACCESS);
	 * @param p
	 * @return boolean
	 */
	public final boolean allows(final PermissionLevel p) {
		
		// if passed permission level is null, return false
		if (p == null) {
			return false;
		}
		
		return (this.getMask() & p.toByte()) == p.toByte();
	}
	
	
	/**
	 * Test if a permission level can grant for passed permission level<br>
	 * Example usage:<br>
	 *  boolean iCanGiveAccess = myPerm.canGrant(PermissionLevel.ACCESS)<br>
	 * @param p
	 * @return boolean
	 */
	public final boolean canGrant(final PermissionLevel p) {

		// if passed permission level is null, return false
		if (p == null) {
			return false;
		}

		return ((this.getMask() & GRANT.toByte()) == GRANT.toByte()
				&& (this.getMask() & p.toByte()) == p.toByte());

	}

	/**
	 * Add a permission level to an existing permission level
	 * @param p
	 * @return new permission level
	 */
	public final PermissionLevel add(final PermissionLevel p) {
		
		if (p == null) {
			return null;
		}
		
		byte result = this.toByte();
		return fromByte(result |= p.getMask());
	}
	
	/** Add grant permission to a permission level<br>
	 * Example usage:<br>
	 *  PermissionLevel myPerm = PermissionLevel.ACCESS;<br>
	 *  myPerm = myPerm.addGrant();
	 */
	public final PermissionLevel addGrant() {
		
		byte result = this.toByte();
		return fromByte(result |= GRANT.mask);
	}
	
	
	/** Static Add grant permission to a passed permission level<br>
	 * Example usage:<br>
	 *  PermissionLevel myPerm = PermissionLevel.ACCESS;<br>
	 *  myPerm = PermissionLevel.addGrant(myPerm);
	 */
	// static version takes a permission level as parameter
	public static PermissionLevel addGrant(final PermissionLevel p) {
		byte result = p.value;
		return fromByte(result |= GRANT.mask);
	}
	
}
