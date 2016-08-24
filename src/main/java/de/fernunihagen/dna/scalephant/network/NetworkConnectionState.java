package de.fernunihagen.dna.scalephant.network;

public enum NetworkConnectionState {

	/**
	 * The states of a network connection
	 */
	
	/**
	 * The connection is establishing
	 */
	NETWORK_CONNECTION_OPENING,
	
	/**
	 * The connection is open
	 */
	NETWORK_CONNECTION_OPEN,
	
	/**
	 * The connection is closing
	 */
	NETWORK_CONNECTION_CLOSING,
	
	/**
	 * The connection is closed
	 */
	NETWORK_CONNECTION_CLOSED,
	
	/**
	 * The connection is closed with errors
	 */
	NETWORK_CONNECTION_CLOSED_WITH_ERRORS;	
}