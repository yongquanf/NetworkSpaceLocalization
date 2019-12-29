package edu.NUDT.pdl.Nina.KNN;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import edu.NUDT.pdl.Nina.util.bloom.Apache.BloomFilterFactory;
import edu.NUDT.pdl.Nina.util.bloom.Apache.Filter;
import edu.NUDT.pdl.Nina.util.bloom.Apache.Key;
import edu.NUDT.pdl.Nina.util.bloom.Apache.Hash.Hash;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

public class SubSetManager implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3151280022392839895L;

	int filter_size = 16;

	private static final int nbHash = 8;
	private static final int vectorSize = 1024 * 4;
	private static final int hashType = Hash.MURMUR_HASH;
	private static final int nr = 2;
	private Filter filter;

	public SubSetManager() {
		filter = BloomFilterFactory.createDynamicBloomFilter(vectorSize, nbHash, hashType, nr);
	}

	public SubSetManager(Filter filter) {
		this.filter = filter;
	}

	public static Filter getEmptyFilter() {
		Filter filter1 = BloomFilterFactory.createBloomFilter(vectorSize, nbHash, hashType);
		return filter1;
	}

	/**
	 * check node
	 * 
	 * @param node
	 * @return
	 */
	public boolean checkNode(AddressIF node) {
		if (node == null) {
			return false;
		}
		if (filter.membershipTest(getKey(node))) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * insert a node into filter
	 * 
	 * @param node
	 */
	public void insertPeers2Subset(AddressIF node) {
		if (node == null) {
			return;
		}

		if (checkNode(node)) {
			return;
		} else {
			filter.add(getKey(node).makeCopy());
		}
	}

	/**
	 * add a list of nodes
	 * 
	 * @param candidates
	 */
	public void constructSubsets4KNN(List<AddressIF> candidates) {
		if (candidates == null || candidates.isEmpty()) {
			return;
		} else {
			Iterator<AddressIF> ier = candidates.iterator();
			while (ier.hasNext()) {
				AddressIF t = ier.next();
				insertPeers2Subset(t);
			}
		}
	}

	/**
	 * get key from the address, we directly use the address&port as the key
	 * 
	 * @param address
	 * @return
	 */
	public Key getKey(AddressIF address) {
		return new Key(address.getByteIPAddrPort());
	}

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

}
