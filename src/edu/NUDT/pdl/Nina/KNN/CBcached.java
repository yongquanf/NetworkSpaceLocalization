package edu.NUDT.pdl.Nina.KNN;

import java.util.HashSet;
import java.util.Set;

import edu.harvard.syrah.sbon.async.Barrier;

public class CBcached {
	public int version; // to denote the ith KNN search
	public Object cb;
	public Set<NodesPair> nps;
	public final Barrier wake;
	public final long seed;
	public long timeStamp;

	public int ExpectedNumber = -1;

	public int totalSendingMessage = 0;

	public int direction = KNNManager.ClosestSearch;

	public void setDirection(int _direction) {
		direction = _direction;
	}

	public int getDirection() {
		return direction;
	}

	public CBcached(int _version, Object _cb, Barrier _wake, int _seed, long _timeStamp) {
		version = _version;
		cb = _cb;
		wake = _wake;
		seed = _seed;
		nps = new HashSet<NodesPair>(5);
		timeStamp = _timeStamp;
	}

	public CBcached(Object _cb, Barrier _wake, long _seed, long _timeStamp) {
		version = -1;
		cb = _cb;
		wake = _wake;
		seed = _seed;
		nps = new HashSet<NodesPair>(5);
		timeStamp = _timeStamp;
	}
}