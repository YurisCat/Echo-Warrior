package com.yuriscat.echowarrior.item;

public enum EchoTrait {
	BAD_TEMPER(0, "坏脾气"),
	LAZY(1, "慵懒"),
	COURAGE(2, "勇气"),
	SKINNY(3, "瘦削"),
	STURDY(4, "壮硕");

	private final int bit;
	private final String displayName;

	EchoTrait(int bit, String displayName) {
		this.bit = bit;
		this.displayName = displayName;
	}

	public int mask() {
		return 1 << this.bit;
	}

	public String displayName() {
		return this.displayName;
	}
}
