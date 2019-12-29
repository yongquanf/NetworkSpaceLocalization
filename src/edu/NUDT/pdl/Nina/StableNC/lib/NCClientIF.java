package edu.NUDT.pdl.Nina.StableNC.lib;

/*
 * Pyxida - a network coordinate library
 * 
 * Copyright 2008 Jonathan Ledlie and Peter Pietzuch
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Set;

public interface NCClientIF<T> {

	// for debugging simulations
	public abstract void setLocalID(T _local_addr);

	public abstract String toString();

	public abstract Hashtable<String, Double> getStatistics();

	public abstract void reset();

	/**
	 * Returns the dimension of the Euclidian space coordinates are embedded in.
	 * 
	 * @return the coordinate space dimension
	 */
	public abstract int getNumDimensions();

	/**
	 * Returns the system-level Vivaldi coordinates. These coordinates change
	 * more frequently than the application-level coordinates.
	 * 
	 * @return the system-level coordinates
	 */
	public abstract Coordinate getSystemCoords();

	/**
	 * Returns the system-level error, which denotes the accuracy of the
	 * system-level coordinates.
	 * 
	 * @return the system-level error
	 */
	public abstract double getSystemError();

	/**
	 * Returns the age of our coordinate Note that this does not require
	 * clock-synchronization because it is relative to our coordinate
	 * 
	 * @return relative age of our coordinate since we last updated it
	 */
	public abstract long getAge(long curr_time);

	/**
	 * Returns the list of observers, to which observers for the
	 * application-level coordinate can be added, removed, and so forth.
	 * 
	 * @return the list of observers for the application-level coordinate
	 */
	public abstract ObserverList getObserverList();

	/**
	 * Notifies this <code>VivaldiClient</code> object that a host that supports
	 * Vivaldi has joined the system. State associated with the new host is
	 * created. This method succeeds and returns <code>true</code> only if the
	 * host is not already registered with this <code>VivaldiClient</code>
	 * object.
	 * 
	 * @param addr
	 *            the address of the joining host
	 * @return <code>true</code> if <code>addr</code> is registered and its
	 *         associated state created, <code>false</code> otherwise
	 */
	public abstract boolean addHost(T addr);

	/**
	 * Notifies this <code>VivaldiClient</code> object that a host that supports
	 * Vivaldi and has the provided coordinates and error has joined the system.
	 * State associated with the new host is created. This method succeeds and
	 * returns <code>true</code> only if the host is not already registered with
	 * this <code>VivaldiClient</code> object.
	 * 
	 * @param addr
	 *            the address of the joining host
	 * @param _r_coord
	 *            the app-level coordinates of the remote host
	 * @param r_error
	 *            the system-level error of the remote host
	 * @param sample_rtt
	 *            the RTT sample to the remote host
	 * @param curr_time
	 *            the current time, in milliseconds
	 * @param can_update
	 *            <code>true</code> if this method can update a host already
	 *            present
	 * @return <code>true</code> if <code>addr</code> is registered and its
	 *         associated state created, <code>false</code> otherwise
	 */

	public abstract boolean addHost(T addr, Coordinate _r_coord,
			double r_error, long curr_time, boolean can_update);

	/**
	 * Notifies this <code>VivaldiClient</code> object that a host that supports
	 * Vivaldi has left the system. However, the state (i.e. short list of RTT
	 * values) is kept because it will be useful if and when the node returns
	 * into the system
	 * 
	 * @param addr
	 *            the address of the departing host
	 * @return <code>true</code> if <code>addr</code> was a known node
	 */

	public abstract boolean removeHost(T addr);

	/**
	 * Returns whether the given host has been registered with this
	 * <code>VivaldiClient</code> object.
	 * 
	 * @param addr
	 *            the address to query as registered
	 * @return <code>true</code> if registered, <code>false</code> otherwise
	 */
	public abstract boolean containsHost(T addr);

	/**
	 * Returns all hosts that support Vivaldi and have been registered with this
	 * <code>VivaldiClient</code> object. The returned set is backed by the true
	 * set of registered hosts, but cannot be modified.
	 * 
	 * @return the set of registered Vivaldi-supporting hosts
	 */
	public abstract Set<T> getHosts();

	/**
	 * This method is invoked when a new RTT sample is made to a host that
	 * supports Vivaldi. This method succeeds and returns <code>true</code> only
	 * if the host is already registered with this <code>VivaldiClient</code>
	 * object, and the RTT sample is valid.
	 * 
	 * @param addr
	 *            the address of the host
	 * @param _r_coord
	 *            the system-level coordinates of the remote host
	 * @param r_error
	 *            the system-level error of the remote host
	 * @param sample_rtt
	 *            the RTT sample to the remote host
	 * @param curr_time
	 *            the current time, in milliseconds
	 * @param can_add
	 *            <code>true</code> if this method can add a host not already
	 *            present
	 * @return <code>true</code> if <code>addr</code> is registered and the
	 *         sample is processed, <code>false</code> otherwise
	 */

	public abstract boolean processSample(T addr, Coordinate _r_coord,
			double r_error, double sample_rtt, long sample_age, long curr_time,
			boolean can_add);

	public abstract boolean hasAllNeighbors();

	public abstract boolean stabilized();

	public abstract String printNeighbors();

	public abstract T getNeighborToPing(long curr_time);

	public abstract void startUp(DataInputStream is) throws IOException;

	public abstract void shutDown(DataOutputStream os) throws IOException;

	// detect coordinate changes

	public abstract int significant_coordinates_change();

	public abstract int is_stable();

	public abstract void not_stable();

	public abstract void set_stable();

	public abstract int detect_bad_neighbor();

	public abstract void print_coord();

	public abstract void check_state();

	public abstract void update_neigherr();
}