/*
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
package edu.NUDT.pdl.Nina.StableNC.lib;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * A list of observers for the application-level coordinate.
 * 
 * 
 * 
 * @author Michael Parker, Jonathan Ledlie
 */

public class ObserverList {

	final protected List<ApplicationObserver> obs_list;

	protected ObserverList() {

		obs_list = new LinkedList<ApplicationObserver>();

	}

	/**
	 * 
	 * Returns the number of observers in the list.
	 * 
	 * 
	 * 
	 * @return the size of the observer list
	 */

	public int size() {

		return obs_list.size();

	}

	/**
	 * 
	 * Returns whether the observer list is empty, meaning its size equals
	 * 
	 * <code>0</code>.
	 * 
	 * 
	 * 
	 * @return <code>true</code> if the observer list is empty,
	 * 
	 *         <code>false</code> otherwise
	 */

	public boolean isEmpty() {

		return obs_list.isEmpty();

	}

	/**
	 * 
	 * Returns whether the list contains the given observer. If the parameter
	 * 
	 * <code>obj</code> is <code>null</code>, this method returns
	 * 
	 * <code>false</code>.
	 * 
	 * 
	 * 
	 * @param obj
	 * 
	 *            the observer to query for membership in the list
	 * 
	 * @return <code>true</code> if the list contains the observer,
	 * 
	 *         <code>false</code> otherwise
	 */

	public boolean contains(ApplicationObserver obj) {

		return (obj != null) ? obs_list.contains(obj) : false;

	}

	/**
	 * 
	 * Returns an iterator over the list of observers.
	 * 
	 * 
	 * 
	 * @return an iterator over the observer list
	 */

	public Iterator<ApplicationObserver> iterator() {

		return obs_list.iterator();

	}

	/**
	 * 
	 * Adds the given observer to the list of observers. If the parameter
	 * 
	 * <code>obj</code> is <code>null</code>, this method returns
	 * 
	 * <code>false</code> and the underlying list remains unchanged.
	 * 
	 * 
	 * 
	 * @param obj
	 * 
	 *            the observer to add to the list
	 * 
	 * @return <code>true</code> if the observer is added to the list,
	 * 
	 *         <code>false</code> otherwise
	 */

	public boolean add(ApplicationObserver obj) {

		return (obj != null) ? obs_list.add(obj) : false;

	}

	/**
	 * 
	 * Removes the given observer from the list of observers. If the parameter
	 * 
	 * <code>obj</code> is <code>null</code> or the list does not contain
	 * 
	 * the observer, this method returns <code>false</code> and the underlying
	 * 
	 * list remains unchanged.
	 * 
	 * 
	 * 
	 * @param obj
	 * 
	 *            the observer to remove from the list
	 * 
	 * @return <code>true</code> if the observer is removed from the list,
	 * 
	 *         <code>false</code> otherwise
	 */

	public boolean remove(ApplicationObserver obj) {

		return (obj != null) ? obs_list.remove(obj) : true;

	}

	/**
	 * 
	 * Removes all the observers from the list.
	 */

	public void clear() {

		obs_list.clear();

	}

}
