/*
 * package edu.NUDT.pdl.Nina.KNN;
 * 
 * import java.util.ArrayList; import java.util.HashMap; import
 * java.util.HashSet; import java.util.Iterator; import java.util.List; import
 * java.util.Map; import java.util.Set; import java.util.Vector; import
 * java.util.Map.Entry;
 * 
 * import edu.NUDT.pdl.Nina.Ninaloader; import
 * edu.NUDT.pdl.Nina.Clustering.HSH_Manager; import
 * edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.DoubleComp; import
 * edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.NodesPairComp; import
 * edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.ResponseObjCommCB; import
 * edu.NUDT.pdl.Nina.StableNC.lib.RemoteState; import
 * edu.NUDT.pdl.Nina.StableNC.nc.StableManager; import
 * edu.NUDT.pdl.Nina.util.bloom.Apache.Filter; import edu.harvard.syrah.prp.Log;
 * import edu.harvard.syrah.prp.SortedList; import
 * edu.harvard.syrah.pyxida.ping.PingManager; import
 * edu.harvard.syrah.sbon.async.Barrier; import
 * edu.harvard.syrah.sbon.async.CBResult; import
 * edu.harvard.syrah.sbon.async.Config; import edu.harvard.syrah.sbon.async.EL;
 * import edu.harvard.syrah.sbon.async.CallbacksIF.CB0; import
 * edu.harvard.syrah.sbon.async.CallbacksIF.CB1; import
 * edu.harvard.syrah.sbon.async.CallbacksIF.CB2; import
 * edu.harvard.syrah.sbon.async.comm.AddressFactory; import
 * edu.harvard.syrah.sbon.async.comm.AddressIF; import
 * edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF; import
 * edu.harvard.syrah.sbon.async.comm.obj.ObjCommRRCB;
 * 
 * public class SubSetSearchManager {
 * 
 * 
 * private static Log log=new Log(SubSetSearchManager.class); // for CB
 * Map<AddressIF, Vector<CBcached>> cachedCB = new HashMap<AddressIF,
 * Vector<CBcached>>( 10);
 * 
 * Map<AddressIF, Vector<record>> registeredNearestNodes = new
 * HashMap<AddressIF, Vector<record>>( 10);
 * 
 * int MultipleRestart = 3;
 * 
 * public final ObjCommIF comm; public final PingManager pingManager; public
 * final StableManager ncManager;
 * 
 * public double betaRatio =
 * Double.parseDouble(Config.getConfigProps().getProperty("betaRatio", "0.5"));
 * 
 *//**
	 * constructor
	 * 
	 * @param _comm
	 * @param pingManager
	 * @param ncManager
	 */
/*
 * public SubSetSearchManager(ObjCommIF _comm, PingManager _pingManager,
 * StableManager _ncManager) { comm=_comm; pingManager=_pingManager;
 * ncManager=_ncManager;
 * 
 * }
 * 
 * 
 * public void init() {
 * 
 * comm.registerMessageCB(SubSetClosestRequestMsg.class, new
 * SubSetClosestReqHandler());
 * comm.registerMessageCB(CompleteSubsetSearchRequestMsg.class, new
 * SubSetCompleteNNReqHandler() );
 * comm.registerMessageCB(SubSetTargetLocatedRequestMsg.class, new
 * SubSetTargetLockReqHandler() );
 * comm.registerMessageCB(RepeatedSubSetRequestMsg.class, new
 * SubSetRepeatedNNReqHandler() );
 * 
 * //target locator
 * 
 * //complete
 * 
 * }
 * 
 *//**
	 * query constrained KNN search
	 * 
	 * @param target
	 * @param candidates
	 * @param k
	 * @param cbDone
	 */
/*
 * public void queryKNearestNeighbors(final AddressIF target, List<AddressIF>
 * candidates, final int k, final CB1<List<NodesPair>> cbDone) { // TODO
 * Auto-generated method stub
 * 
 * 
 * 
 * int branches = Math.min(MultipleRestart,
 * MClient.instance.g_rings.primarySize); final Set<AddressIF> randNodes = new
 * HashSet<AddressIF>(10);
 * 
 * // rings MClient.instance.g_rings.getRandomNodes(randNodes, branches); if
 * (randNodes.contains(target)) { log .debug(
 * "$: we remove the target, then we start the KNN search process! ");
 * randNodes.remove(target); }
 * 
 * int realNodeNo = randNodes.size();
 * 
 * log.info("!!!!!!!!!!!!!!!!!!!!!!!\nTotal branches: " + realNodeNo +
 * "\n!!!!!!!!!!!!!!!!!!!!!!!");
 * 
 * final long timeStamp = System.currentTimeMillis();
 * 
 * if (!randNodes.isEmpty()) { final Barrier barrierUpdate = new Barrier(true);
 * // generate the KNN's seed final int seed = Ninaloader.random.nextInt();
 * 
 * //save the CB //======================================== long timer =
 * System.currentTimeMillis(); if (!cachedCB.containsKey(Long.valueOf(seed))) {
 * // Vector<CBcached> cached = new Vector<CBcached>( // 5); CBcached cb=new
 * CBcached(cbDone, barrierUpdate, seed,timer); cachedCB.put(Long.valueOf(seed),
 * cb); }
 * 
 * //========================================= Iterator<AddressIF> ier =
 * randNodes.iterator(); for (int index = 0; index < realNodeNo; index++) {
 * 
 * AddressIF dest = ier.next(); // test dest if (dest.equals(target)) {
 * continue; }
 * 
 * // version number final int version = Ninaloader.random.nextInt(); // added K
 * neighbors Set<NodesPair> NearestNeighborIndex = new HashSet<NodesPair>(10);
 * // save the target into cache, to avoid target being select as // candidate
 * KNN node // Note: NearestNeighborIndex-1=k means there are k nodes
 * 
 * NearestNeighborIndex.add(new NodesPair(AddressFactory .create(target),null,
 * 0));
 * 
 * // record forbidden nodes Filter filter =null;
 * 
 * if(candidates==null||candidates.isEmpty()){ filter=null; }else{ filter=
 * SubSetManager.getEmptyFilter(); SubSetManager subset=new
 * SubSetManager(filter); subset.constructSubsets4KNN(candidates); } Filter
 * forbiddenFilter = SubSetManager.getEmptyFilter(); SubSetManager subset=new
 * SubSetManager(forbiddenFilter); //add the target
 * subset.insertPeers2Subset(target);
 * 
 * 
 * //log.info("Bloom filter: " + filter.getEntryCount()); //
 * ===================================== // filter //
 * ===================================== SubSetClosestRequestMsg msg = new
 * SubSetClosestRequestMsg(Ninaloader.me, Ninaloader.me, target, k,
 * NearestNeighborIndex, version, seed, null, filter, forbiddenFilter);
 * 
 * // first find the farthest node FarthestRequestMsg FMsg = new
 * FarthestRequestMsg(Ninaloader.me, target, Ninaloader.me, msg);
 * 
 * // send to a random neighbor in Set upNeighbors // select a node in the ring
 * 
 * // log.info("###############\n$: send out" + index // + "th KNN query of " +
 * Ninaloader.me + " to Neighbor " // + dest + "\n###############");
 * 
 * barrierUpdate.fork(); comm.sendRequestMessage(FMsg, dest, new
 * ObjCommRRCB<FarthestResponseMsg>() {
 * 
 * @Override protected void cb(CBResult result, final FarthestResponseMsg
 * responseMsg, AddressIF node, Long ts) { switch (result.state) { case OK: { //
 * log.info("K Nearest Request received by new query node: "); //
 * cdDone.call(result); // save the callbacks // add into a list
 * 
 * break; } case ERROR: case TIMEOUT: { cbDone.call(result, null); // the
 * returned // map is null break; } } // ---------------------- } //
 * -------------------------- }); } randNodes.clear(); //long interval=2*1000;
 * EL.get().registerTimerCB(barrierUpdate, new CB0() {
 * 
 * @Override
 * 
 * @SuppressWarnings("unchecked") protected void cb(CBResult result) { String
 * errorString; // merge the results
 * 
 * // if it does not appear, add //long curTime = System.currentTimeMillis(); //
 * double time=(curTime-)/1000; // log.info(
 * "\n@@:\n All KNN branches are reported after "+time+" seconds\n\n");
 * 
 * CBcached np = cachedCB.get(Long.valueOf(seed));
 * 
 * 
 * if (np != null) {
 * 
 * long curTimer=System.currentTimeMillis(); long time=curTimer-np.timeStamp;
 * ElapsedKNNSearchTime.add(time);
 * 
 * List<NodesPair> nps = new ArrayList<NodesPair>(5); List<AddressIF> tmpKNN =
 * new ArrayList<AddressIF>(5);
 * 
 * // List rtts=new ArrayList(5); Map<AddressIF, RemoteState<AddressIF>> maps =
 * new HashMap<AddressIF, RemoteState<AddressIF>>( 5);
 * 
 * 
 * Set<NodesPair> nodePairs = np.nps; // iterate, we use the latency as the hash
 * key if (nodePairs != null) { Iterator<NodesPair> ierNp = nodePairs
 * .iterator(); while (ierNp.hasNext()) { NodesPair cachedNP = ierNp.next();
 * 
 * // skip myself if (cachedNP.startNode .equals(target)) { continue; } if
 * (cachedNP.startNode.equals(target)) { continue; }
 * 
 * if (cachedNP.rtt < 0) { continue; }
 * 
 * if (!maps.containsKey(cachedNP.startNode)) { RemoteState<AddressIF> cached =
 * new RemoteState<AddressIF>( cachedNP.startNode);
 * cached.addSample(cachedNP.rtt); maps.put(cachedNP.startNode, cached);
 * 
 * } else { // averaged RemoteState<AddressIF> cached = maps
 * .get(cachedNP.startNode); cached.addSample(cachedNP.rtt);
 * maps.put(cachedNP.startNode, cached); } } } //}
 * 
 * SortedList<Double> sortedL = new SortedList<Double>( new DoubleComp());
 * HashMap<Double, AddressIF> unSorted = new HashMap<Double, AddressIF>( 2);
 * 
 * Iterator<Entry<AddressIF, RemoteState<AddressIF>>> ierMap = maps
 * .entrySet().iterator(); while (ierMap.hasNext()) { Entry<AddressIF,
 * RemoteState<AddressIF>> tmp = ierMap .next();
 * unSorted.put(Double.valueOf(tmp.getValue() .getSample()), tmp.getKey());
 * sortedL.add(Double.valueOf(tmp.getValue() .getSample())); }
 * 
 * if (sortedL.size() > 0) { // yes the results
 * 
 * Iterator<Double> ierRTT = sortedL.iterator();
 * 
 * while (ierRTT.hasNext()) { Double item = ierRTT.next();
 * 
 * AddressIF tmpNode = unSorted.get(item); // target if (tmpNode.equals(target))
 * { continue; }
 * 
 * // full if (tmpKNN.size() == k) { break; } else {
 * 
 * // add if (tmpKNN.contains(tmpNode)) { continue; } else {
 * tmpKNN.add(tmpNode); NodesPair nodePair = new NodesPair(tmpNode, null,
 * item.doubleValue()); nodePair.elapsedTime=time; nps.add(nodePair); } } }
 * 
 * // log.debug("KNN Number: " + nps.size());
 * 
 * cbDone.call(CBResult.OK(), nps);
 * 
 * // remove the callbacks cachedCB.remove(Long.valueOf(seed));
 * 
 * // cachedCB.get(target).clear(); // cachedCB.remove(target); //remove the
 * callback // registered for arg1.target nps.clear(); sortedL.clear();
 * unSorted.clear(); maps.clear(); tmpKNN.clear();
 * 
 * } else { log.debug("No RTT measurements!"); randNodes.clear();
 * cbDone.call(CBResult.ERROR(), null); } } else { log.debug("NO cachedCB!");
 * randNodes.clear(); cbDone.call(CBResult.ERROR(), null); }
 * 
 * } });
 * 
 * }// index else { randNodes.clear(); cbDone.call(CBResult.ERROR(), null); } }
 * 
 * 
 * 
 * public void receiveClosestQuery(final SubSetClosestRequestMsg req, final
 * StringBuffer errorBuffer) {
 * 
 * log.info("~~~~~~~~~~~~~~\n$: Receive KNN search from: " + req.from + " to " +
 * req.target + " Root: " + req.root+ "\n~~~~~~~~~~~~~~");
 * 
 * if (req != null) {
 * 
 * 
 * //=========================================================== //last hop node
 * AddressIF from =null;
 * if(req.getHopRecords()!=null&&!req.getHopRecords().isEmpty()){
 * from=req.getHopRecords().get(req.getHopRecords().size()-1); }
 * 
 * 
 * // not cached yet, and this node is not the request node // if the root, if
 * (Ninaloader.me.equals(req.getRoot())) { from = null; } final AddressIF
 * LastHopNode = from; log.info("$: message is from: "+LastHopNode );
 * //===========================================================
 * 
 * // repeated KNN request, since I have been included // TODO: need to be
 * changed to a list of vectors
 * 
 * SubSetManager subset=new SubSetManager(req.getForbiddenFilter()); // found,
 * repeated KNN if (subset.checkNode(Ninaloader.me) ) {
 * 
 * //double[] rtt=new double[1]; //ncManager.isCached(req.target, rtt, -1);
 * //NodesPair np=new NodesPair(Ninaloader.me,req.target,rtt[0]);
 * 
 * //if (!req.getNearestNeighborIndex().contains(np)) { // log.info(
 * "FALSE assert in the forbidden filter!, we do not found a match!");
 * 
 * // not null, repeated query msg // I have registered the address for target
 * req.target, // since I am a KNN of req.target // not null, not myself
 * System.err.println("+++++++++++++++++++\n Repeated KNN from " + req.getFrom()
 * + "\n++++++++++++++++++++");
 * 
 * if (req.getFrom()!=null&&!req.getFrom().equals(Ninaloader.me)) { // not in
 * 
 * 
 * //in, but not saved
 * 
 * 
 * RepeatedSubSetRequestMsg rmsg = new RepeatedSubSetRequestMsg(
 * req.getOriginFrom(), Ninaloader.me, req .getTarget(), req.getK(), req
 * .getNearestNeighborIndex(), req .getVersion(), req.getSeed(), req
 * .getRoot(),req.getFilter(),req.getForbiddenFilter());
 * 
 * int len=req.getHopRecords().size(); //remove last element in hops if(len>0){
 * req.getHopRecords().remove(len-1); }
 * rmsg.HopRecords.addAll(req.getHopRecords());
 * //====================================================
 * comm.sendRequestMessage(rmsg, req.getFrom(), new
 * ObjCommRRCB<RepeatedSubSetResponseMsg>() {
 * 
 * @Override protected void cb( CBResult result, final RepeatedSubSetResponseMsg
 * responseMsg, AddressIF node, Long ts) { switch (result.state) { case OK: {
 * log .debug("repeated request acked "); // cdDone.call(result); break; } case
 * ERROR: case TIMEOUT: { // cdDone.call(result); break; } } //
 * ---------------------- } // -------------------------- }); return; } else {
 * // yes, myself, backtracking CompleteSubsetSearchRequestMsg msg3 =
 * CompleteSubsetSearchRequestMsg .makeCopy(req); msg3.from = Ninaloader.me;
 * returnKNN2Origin(msg3);
 * 
 * double []lat=new double[1]; ncManager.isCached(req.target, lat, -1);
 * backtracking(req,lat[0], errorBuffer); return;
 * 
 * } }
 * 
 * 
 * 
 * 
 * // not the target itself, not repeated KNN node, here is the main // logic
 * 
 * // if the nearest nodes reach requirements, return if
 * (req.getNearestNeighborIndex() != null &&
 * (req.getNearestNeighborIndex().size() - 1) >= req.getK()) { log.debug(
 * " $: completed! "); CompleteSubsetSearchRequestMsg msg3 =
 * CompleteSubsetSearchRequestMsg.makeCopy(req); msg3.from = Ninaloader.me;
 * this.returnKNN2Origin(msg3); return; }
 * 
 * // else continue
 * 
 * 
 * // TODO: if last hop fails
 * 
 * // log.info("$: last hop: " + LastHopNode); // to confirm whether I am a new
 * nearest node for target
 * 
 * // send the target a gossip msg // LOWTODO could bias which nodes are sent
 * based on his coord final long sendStamp = System.nanoTime();
 * 
 * ncManager.doPing(req.getTarget(), new CB2<Double, Long>() {
 * 
 * @Override protected void cb(CBResult result, Double latency, Long timer) { //
 * TODO Auto-generated method stub final double[] rtt = { (System.nanoTime() -
 * sendStamp) / 1000000d }; switch (result.state) { case OK: {
 * 
 * if (latency.doubleValue() >= 0) { rtt[0] = latency.doubleValue(); } // //
 * TODO: smoothed RTT // save the latency measurements
 * 
 * double[] latencyNC=new double[1]; if(ncManager.isCached(req.getTarget(),
 * latencyNC,rtt[0])){ rtt[0]=latencyNC[0]; } //use the offset to select nodes
 * final double lat =rtt[0]+AbstractNNSearchManager.offsetLatency;
 * 
 * // request nodes to probe to target nodes final Vector<AddressIF> ringMembers
 * = new Vector<AddressIF>( 10);
 * 
 * log.info("@@@@@@@@@@@@@@@@@@@@@@@@\n Node: " + Ninaloader.me + " is: " + lat
 * + " from Node: " + req.getTarget() + "\n@@@@@@@@@@@@@@@@@@@@@@@@"); betaRatio
 * = MClient.instance.g_rings.fillVector(req .getTarget(), lat, lat, betaRatio,
 * ringMembers,AbstractNNSearchManager.offsetLatency);
 * 
 * synchronized(ringMembers){ // add perturbation Set<AddressIF> randomSeeds =
 * new HashSet<AddressIF>( defaultNodes); randomSeeds.addAll(ringMembers);
 * MClient.instance.g_rings.getRandomNodes(randomSeeds, defaultNodes);
 * ringMembers.clear(); ringMembers.addAll(randomSeeds); randomSeeds.clear();
 * 
 * // not target from rings if (ringMembers.contains(req.getTarget())) {
 * ringMembers.remove(req.getTarget()); }
 * 
 * // log.info("The size of returned members: " // + ringMembers.size());
 * 
 * SubSetManager subset=new SubSetManager(req.getForbiddenFilter()); //
 * postprocess the ring members Iterator<AddressIF> ierRing =
 * ringMembers.iterator(); while (ierRing.hasNext()) { AddressIF tmpNode =
 * ierRing.next(); if (subset.checkNode(tmpNode)) { // found forbidden nodes
 * ierRing.remove(); } }
 * 
 * 
 * // also remove the backtracking node Iterator<AddressIF> ierBack =
 * req.getHopRecords() .iterator(); while (ierBack.hasNext()) {
 * 
 * AddressIF tmp = ierBack.next(); if (ringMembers.contains(tmp)) { log.debug(
 * "Remove forbidden backtracking node: "+ tmp); ringMembers.remove(tmp); } }
 * 
 * }
 * 
 * // ---------------------------------
 * 
 * 
 * // boolean for current nodes boolean isNearest = false; boolean another =
 * false; // returned ID is null // Note: if the ringMembers contain the target
 * // node, current node may not be the nearest // nodes if (ringMembers == null
 * || (ringMembers.size() == 0)) {
 * 
 * // the originNode is the only one node // no nearer nodes, so return me //
 * log.info(Ninaloader.me // + " is the closest to: " // + req.target // +
 * " from g_rings.fillVector @: " // + Ninaloader.me + " Target: " // +
 * req.target);
 * 
 * // TODO: add the clustering based 1-step // search, the ring must be balanced
 * 
 * 
 * // backtrack // find last hop // TODO: assume that K<=N // AddressIF
 * lastHop=null;
 * 
 * // I am the lastHop // TODO change the beta according to // current rings,
 * 9-24 // remove condition: // ||req.K<=req.NearestNeighborIndex.size()-1, //
 * 9-24 if (LastHopNode == null || LastHopNode.equals(Ninaloader.me)) {
 * log.info("Finish! ");
 * 
 * SubSetManager subset = new SubSetManager(req.getFilter()); if
 * (req.getFilter()==null||subset .checkNode(Ninaloader.me)) {
 * 
 * req.getNearestNeighborIndex().add( new NodesPair(Ninaloader.me,
 * req.getTarget(), lat-AbstractNNSearchManager.offsetLatency,LastHopNode));
 * 
 * 
 * } subset=new SubSetManager(req.getForbiddenFilter());
 * subset.insertPeers2Subset(Ninaloader.me);
 * 
 * CompleteSubsetSearchRequestMsg msg3 =
 * CompleteSubsetSearchRequestMsg.makeCopy(req); msg3.from = Ninaloader.me;
 * returnKNN2Origin(msg3); return;
 * 
 * 
 * } else {
 * 
 * // log.info(" find a new last hop!!!");
 * 
 * backtracking(req, lat, errorBuffer); }
 * 
 * 
 * } else {
 * 
 * // final double refRTT=lat; // TODO: ask a set of nodes to ping a set of //
 * nodes
 * 
 * TargetProbes(ringMembers, req.getTarget(), new CB1<String>() {
 * 
 * @Override protected void cb(CBResult ncResult, String errorString) { switch
 * (ncResult.state) { case OK: { // log.debug(
 * "$: Target Probes are issued for KNN"); } case ERROR: case TIMEOUT: { break;
 * } } } }, new CB2<Set<NodesPair>, String>() {
 * 
 * @Override protected void cb(CBResult ncResult, Set<NodesPair> nps2, String
 * errorString) {
 * 
 * switch (ncResult.state) { case OK: { ringMembers.clear(); final
 * Set<NodesPair> nps1=new HashSet<NodesPair>(1); nps1.addAll(nps2);
 * 
 * //+++++++++++++++++++++++++++++++++++++ // use the rule to select next hop
 * nodes AddressIF target=req.target; Set<NodesPair> closestNodes=new
 * HashSet<NodesPair>(1); SubSetManager subset=new
 * SubSetManager(req.getForbiddenFilter());
 * 
 * SubSetManager subsetFilter=new SubSetManager(req.getFilter()); //select based
 * rules AddressIF
 * returnID=ClosestNodeRule(target,nps1,closestNodes,subset,LastHopNode);
 * 
 * //record new nearest nodes if(closestNodes!=null&&!closestNodes.isEmpty()){
 * Iterator<NodesPair> ierNode = closestNodes.iterator();
 * 
 * while(ierNode.hasNext()){ NodesPair tmp = ierNode.next(); AddressIF
 * from=tmp.startNode; //add, only when it is closer than me, and does not
 * reside on the backtrack path
 * 
 * if(!subset.checkNode(from)){ subset.insertPeers2Subset(from); //checked in
 * candidates if(req.getFilter()==null||subsetFilter.checkNode(from)){
 * req.getNearestNeighborIndex().add(tmp); } }
 * 
 * } }
 * 
 * 
 * //+++++++++++++++++++++++++++++++++++++ if(returnID==null){ //found nearest,
 * we backtrack // I am the lastHop // remove 9-24 //remove last hop
 * 
 * backtracking(req, lat,errorBuffer);
 * 
 * }else { // returnID is not // Ninaloader.me // final SubSetClosestRequestMsg
 * msg = new SubSetClosestRequestMsg( req.getOriginFrom(), Ninaloader.me,
 * req.getTarget(), req.getK(), req.getNearestNeighborIndex(), req.getVersion(),
 * req.getSeed(), req.getRoot(), req.getFilter(), req.getForbiddenFilter());
 * 
 * //current hop msg.getHopRecords().addAll(req.getHopRecords());
 * msg.addCurrentHop(Ninaloader.me);
 * 
 * 
 * //me if(returnID.equals(Ninaloader.me)){ receiveClosestQuery(msg,
 * errorBuffer); }else{
 * 
 * comm.sendRequestMessage( msg, returnID, new
 * ObjCommRRCB<SubSetClosestResponseMsg>() {
 * 
 * @Override protected void cb( CBResult result, final SubSetClosestResponseMsg
 * responseMsg, AddressIF node, Long ts) { switch (result.state) { case OK: {
 * log .debug("Request Acked by new query node: "); // cdDone.call(result);
 * break; } case ERROR: case TIMEOUT: { // cdDone.call(result);
 * findNewClosestNxtNode( sortedL, msg, LastHopNode, lat); Iterator ier2 =
 * nps1.iterator(); SortedList<NodesPair> sortedL = new SortedList<NodesPair>(
 * new NodesPairComp()); while (ier2.hasNext()) {
 * 
 * NodesPair tmp = (NodesPair) ier2.next(); // invalid // measurements if
 * (tmp.rtt < 0) { continue; } // the same with // target if
 * (tmp.startNode.equals(req.getTarget())) { continue; } // if tmp.lat < //
 * current // latency from // me
 * 
 * //10-18- add the beta constriant, to reduce the elapsed time // if < beta,
 * return current node if (tmp.rtt <= lat) { sortedL.add(tmp); } }
 * findNewClosestNxtNode(sortedL,msg,lat,errorBuffer); break; } } //
 * ---------------------- } // --------------------------
 * 
 * });//} } break; } case ERROR: case TIMEOUT: { System.err .println(
 * "KNN Search: Target probes to " + req .getTarget() + " FAILED!"); // the //
 * returned // map // is // null // we move backward backtracking(req,
 * lat,errorBuffer); break; } }
 * 
 * }// }
 * 
 * ); }// end of target probe
 * 
 * // cdDone.call(result); break; }
 * 
 * case TIMEOUT: case ERROR: {
 * 
 * String error = "RTT request to " + req.getTarget().toString(false) +
 * " failed:" + result.toString(); log.warn(error); if (errorBuffer.length() !=
 * 0) { errorBuffer.append(","); } errorBuffer.append(error); //
 * cdDone.call(result);
 * 
 * // the target can not be reached, we finish the KNN // search process
 * CompleteSubsetSearchRequestMsg msg3 =CompleteSubsetSearchRequestMsg
 * .makeCopy(req); msg3.from = Ninaloader.me; // CompleteNNRequestMsg //
 * msg3=CompleteNNRequestMsg.makeCopy(req); returnKNN2Origin(msg3);
 * 
 * break; }
 * 
 * }
 * 
 * }
 * 
 * });
 * 
 * } }
 * 
 * public void returnKNN2Origin(CompleteSubsetSearchRequestMsg msg3) {
 * 
 * // I should not work, since I have been a KNNer if
 * (!Ninaloader.me.equals(msg3.OriginFrom)) {
 * 
 * log .debug(
 * "!!!!!!!!!!!!!!!!!!!!!!\nComplete @ TargetLockReqHandler\n!!!!!!!!!!!!!!!!!!!!!!"
 * ); // TODO: after the normal search process, we add the extra search // steps
 * here
 * 
 * // query completed, send msg to query nodes // send the result
 * comm.sendRequestMessage(msg3, msg3.OriginFrom, new
 * ObjCommRRCB<CompleteSubSubsetSearchResponseMsg>() {
 * 
 * @Override protected void cb(CBResult result, final
 * CompleteSubSubsetSearchResponseMsg responseMsg, AddressIF node, Long ts) {
 * switch (result.state) { case OK: {
 * 
 * // acked by original query node // cdDone.call(result); break; } case ERROR:
 * case TIMEOUT: { // cdDone.call(result); break; } } } });
 * 
 * } else { // yes I am the originFrom this.postProcessKNN(msg3); }
 * 
 * }
 * 
 *//**
	 * I am the orginFrom node, which send the KNN query
	 */
/*
 * void postProcessKNN(CompleteSubsetSearchRequestMsg arg1) {
 * 
 * 
 * final Set<NodesPair> NNs = arg1.getNearestNeighborIndex(); // callback
 * invoked
 * 
 * 
 * CBcached curCB = cachedCB.get(Long.valueOf(arg1.seed)); if (curCB != null) {
 * 
 * // match the seed, if it does not match, it means it is // another KNN search
 * log.info("\n@@:\n KNN branch is reported from " + arg1.from);
 * curCB.nps.addAll(NNs); // done, wake up the barrier
 * //curCB.wake.setNumForks(1); curCB.wake.join(); }
 * 
 * // notify nodes that cache the query records to withdraw
 * 
 * }
 * 
 * 
 * 
 *//**
	 * backtracking
	 * 
	 * @param req
	 * @param LastHopNode
	 */
/*
 * private void backtracking(final SubSetClosestRequestMsg req, final double
 * lat,final StringBuffer errorBuffer ) {
 * 
 * 
 * //remove last hop
 * 
 * AddressIF LastHopNode1=null; int hopLen=req.getHopRecords().size();
 * if(hopLen>0){ //the last hop LastHopNode1=req.getHopRecords().get(hopLen-1);
 * req.getHopRecords().remove(hopLen-1); }
 * 
 * final AddressIF LastHopNode=LastHopNode1;
 * 
 * //==================================================== //not included
 * SubSetManager subset=new SubSetManager(req.getForbiddenFilter());
 * if(!subset.checkNode(Ninaloader.me)){
 * 
 * SubSetManager subsetFilter = new SubSetManager(req.getFilter()); //
 * candidates if (req.getFilter()==null||subsetFilter.checkNode(Ninaloader.me))
 * { NodesPair tmp=new NodesPair(Ninaloader.me, null, lat,LastHopNode);
 * req.getNearestNeighborIndex().add(tmp); }
 * 
 * subset.insertPeers2Subset(Ninaloader.me); // OK send originFrom a message
 * log.info(Ninaloader.me + " is the closest to: " + req.getTarget() +
 * " from TargetProbes @: " + Ninaloader.me + " Target: " + req.getTarget());
 * 
 * } // ------------------------------------- // due to "final" constriant //
 * register an item to registeredNearestNodes, stop answering the //
 * corresponding request
 * 
 * log.info("\n----------------------\n KNN hops: " + req.getHopRecords().size()
 * + "\n----------------------\n");
 * 
 * //forward back
 * 
 * if(LastHopNode!=null&&!LastHopNode.equals(Ninaloader.me)){
 * 
 * final SubSetTargetLocatedRequestMsg msg = new SubSetTargetLocatedRequestMsg(
 * Ninaloader.me, req.getOriginFrom(), req.getTarget(), req .getK(),
 * req.getNearestNeighborIndex(), req .getVersion(), req.getSeed(),
 * req.getRoot(),req.getFilter(),req.getForbiddenFilter());
 * 
 * msg.HopRecords.addAll(req.getHopRecords());
 * 
 * // different nodes from target comm.sendRequestMessage(msg, LastHopNode, new
 * ObjCommRRCB<SubSetTargetLocatedResponseMsg>() {
 * 
 * @Override protected void cb(CBResult result, final
 * SubSetTargetLocatedResponseMsg responseMsg, AddressIF node, Long ts) {
 * 
 * switch (result.state) { case OK: { // acked by original query node //
 * cdDone.call(result); break; } case ERROR: case TIMEOUT: { //
 * cdDone.call(result); findAliveAncestor(msg, LastHopNode); break; } } } });
 * 
 * }else{ //myself, if it is the sebacktracking, we finish CompleteNNRequestMsg
 * msg3 = CompleteNNRequestMsg.makeCopy(req); msg3.from = Ninaloader.me; // send
 * the result returnKNN2Origin(msg3); return;
 * 
 * } }
 * 
 * 
 * 
 * 
 * private void findAliveAncestor(final SubSetTargetLocatedRequestMsg msg,
 * AddressIF lastHopNode) { // TODO Auto-generated method stub final
 * List<AddressIF> ancestors = msg.HopRecords;
 * 
 * boolean ContactAsker = false;
 * 
 * if (ancestors.size() > 0) {
 * 
 * System.out .println("\n==================\n The total No. of ancestors: " +
 * ancestors.size() + "\n==================\n");
 * 
 * final int[] bitVector = new int[ancestors.size()]; for (int i = 0; i <
 * ancestors.size(); i++) { bitVector[i] = 0; } AddressIF ClosestAncestor =
 * null;
 * 
 * Set<AddressIF> tmpList = new HashSet<AddressIF>(1);
 * tmpList.addAll(ancestors);
 * 
 * collectRTTs(tmpList, new CB2<Set<NodesPair>, String>() {
 * 
 * @Override protected void cb(CBResult ncResult, Set<NodesPair> nps, String
 * errorString) { // send data request message to the core node
 * 
 * // TODO Auto-generated method stub int rank = Integer.MAX_VALUE;
 * 
 * if (nps != null && nps.size() > 0) { System.out .println(
 * "\n==================\n Alive No. of ancestors: " + nps.size() +
 * "\n==================\n"); // find the nodes that are closest to the root
 * Iterator<NodesPair> ier = nps.iterator(); while (ier.hasNext()) { NodesPair
 * tmp = ier.next(); int index = ancestors.indexOf(tmp.endNode);
 * 
 * // failed measurement if (tmp.rtt < 0) { index = -1; }
 * 
 * if (index < 0) { continue; } else { // found the element, and it is smaller
 * than // rank, i.e., it is closer to the target bitVector[index] = 1; } }
 * 
 * } else { // all nodes fail, so there are no alive nodes rank = -1; }
 * 
 * // iterate the bitVector // =============================== int start = -1;
 * boolean first = false; int ZeroCounter = 0; for (int i = 0; i <
 * ancestors.size(); i++) { // first failed node if (!first && (bitVector[i] ==
 * 0)) { start = i; first = true; } if (bitVector[i] == 0) { ZeroCounter++; } }
 * 
 * // all empty, or the root is empty if (ZeroCounter == ancestors.size() ||
 * start == 0) { // we do not have any node to report, no backtracking // nodes
 * !
 * 
 * CompleteSubsetSearchRequestMsg msg3 = CompleteSubsetSearchRequestMsg
 * .makeCopy(msg); msg3.from = Ninaloader.me; returnKNN2Origin(msg3); return;
 * 
 * } else {
 * 
 * if (start == -1) { // it means no ancestors are dead! start =
 * ancestors.size() - 1; } else { // we found an ancestor node // remove
 * unnecessary ancestors final List<AddressIF> missing = new
 * ArrayList<AddressIF>( 1); missing.addAll(ancestors.subList(start, ancestors
 * .size()));
 * 
 * ancestors.removeAll(missing); } int newLen = ancestors.size(); final
 * AddressIF LastHopNode = AddressFactory .create(ancestors.get(newLen - 1));
 * ancestors.remove(newLen - 1);
 * 
 * // different nodes from target comm.sendRequestMessage(msg, LastHopNode, new
 * ObjCommRRCB<SubSetTargetLocatedResponseMsg>() {
 * 
 * @Override protected void cb( CBResult result, final
 * SubSetTargetLocatedResponseMsg responseMsg, AddressIF node, Long ts) {
 * 
 * switch (result.state) { case OK: { // acked by original query node //
 * cdDone.call(result); System.out .println("We find new lastHop node: " +
 * LastHopNode);
 * 
 * break; } case ERROR: case TIMEOUT: { // cdDone.call(result);
 * findAliveAncestor(msg, LastHopNode); break; } } } }); }
 * 
 * // =============================== }
 * 
 * });
 * 
 * tmpList.clear();
 * 
 * } else { // we do not have any node to report, no backtracking nodes !
 * ContactAsker = true; // contact the origin node if (ContactAsker) {
 * CompleteSubsetSearchRequestMsg msg3 =
 * CompleteSubsetSearchRequestMsg.makeCopy(msg); msg3.from = Ninaloader.me;
 * returnKNN2Origin(msg3); return; } }
 * 
 * }
 * 
 *//**
	 * If next-hop node fails, we find new candidate from the sortedList
	 * 
	 * @param sortedL
	 * @param msg
	 * @param lastHopNode
	 */
/*
 * private void findNewClosestNxtNode(final SortedList<NodesPair> sortedL, final
 * SubSetClosestRequestMsg msg, final double lat,final StringBuffer errorBuffer)
 * {
 * 
 * // TODO Auto-generated method stub if (sortedL != null && sortedL.size() > 0)
 * {
 * 
 * final List<AddressIF> candidates = new ArrayList<AddressIF>(1);
 * Iterator<NodesPair> ier = sortedL.iterator(); while (ier.hasNext()) {
 * candidates.add(ier.next().startNode); }
 * 
 * final int[] bitVector = new int[candidates.size()]; for (int i = 0; i <
 * candidates.size(); i++) { bitVector[i] = 0; }
 * 
 * Set<AddressIF> tmpList = new HashSet<AddressIF>(1);
 * tmpList.addAll(candidates);
 * 
 * collectRTTs(tmpList, new CB2<Set<NodesPair>, String>() {
 * 
 * @Override protected void cb(CBResult ncResult, Set<NodesPair> nps, String
 * errorString) {
 * 
 * if (nps != null && nps.size() > 0) { System.out .println(
 * "\n==================\n Alive No. of next-hop node: " + nps.size() +
 * "\n==================\n"); // find the nodes that are closest to the root
 * Iterator<NodesPair> ier = nps.iterator(); while (ier.hasNext()) { NodesPair
 * tmp = ier.next(); int index = candidates.indexOf(tmp.endNode);
 * 
 * // failed measurement if (tmp.rtt < 0) { index = -1; } if (index < 0) {
 * continue; } else { // found the element, and it is smaller than // rank,
 * i.e., it is closer to the target bitVector[index] = 1; } }
 * 
 * } else { // all nodes fail, so there are no alive nodes }
 * 
 * // iterate the bitVector // =============================== int start = -1;
 * boolean first = false; int ZeroCounter = 0; for (int i = 0; i <
 * candidates.size(); i++) { // first alive node if (!first && (bitVector[i] ==
 * 1)) { start = i; first = true; } if (bitVector[i] == 0) { ZeroCounter++; } }
 * 
 * // all empty if (ZeroCounter == candidates.size() || start == -1) { // we do
 * not have any node to report, we backtrack !
 * 
 * backtracking(msg, lat,errorBuffer);
 * 
 * return;
 * 
 * } else {
 * 
 * // send to new node AddressIF returnID = candidates.get(start);
 * comm.sendRequestMessage(msg, returnID, new
 * ObjCommRRCB<SubSetClosestResponseMsg>() {
 * 
 * @Override protected void cb( CBResult result, final SubSetClosestResponseMsg
 * responseMsg, AddressIF node, Long ts) { switch (result.state) { case OK: {
 * log .debug("Request Acked by new query node: "); // cdDone.call(result);
 * break; } case ERROR: case TIMEOUT: { // cdDone.call(result);
 * findNewClosestNxtNode(sortedL, msg,lat,errorBuffer); break; } } //
 * ---------------------- } // --------------------------
 * 
 * });
 * 
 * }
 * 
 * } });
 * 
 * } else { // we do not have next-hop node, so we finish the searching process,
 * // I am the nearest node, and backtrack backtracking(msg, lat, errorBuffer);
 * } }
 * 
 * 
 * 
 * //====================================================
 * 
 * class SubSetClosestReqHandler extends
 * ResponseObjCommCB<SubSetClosestRequestMsg> {
 * 
 * @Override protected void cb(CBResult arg0, SubSetClosestRequestMsg arg1,
 * AddressIF arg2, Long arg3, CB1<Boolean> arg4) { // TODO Auto-generated method
 * stub final AddressIF answerNod = arg1.getFrom(); final AddressIF original =
 * arg1.getOriginFrom(); final AddressIF to = arg1.getTarget(); //
 * System.out.println("kNN Request from: "+answerNod);
 * 
 * final long msgID = arg1.getMsgId();
 * 
 * SubSetClosestResponseMsg msg2 = new SubSetClosestResponseMsg(original,
 * Ninaloader.me);
 * 
 * final StringBuffer errorBuffer = new StringBuffer();
 * 
 * receiveClosestQuery(arg1, errorBuffer); //
 * 
 * receiveClosestQuery(original, to,errorBuffer, new CB0(){ protected void
 * cb(CBResult result) {
 * 
 * switch (result.state) { case OK: { // Initialise the external APIs
 * 
 * break; } default: { String error = "fail to find closest"; log.warn(error);
 * break; } } } });
 * 
 * sendResponseMessage("Closest", answerNod, msg2, msgID, null, arg4);
 * 
 * }
 * 
 * }
 * 
 * 
 * 
 * 
 * // ---------------------------------------------- public class
 * SubSetRepeatedNNReqHandler extends
 * ResponseObjCommCB<RepeatedSubSetRequestMsg> {
 * 
 * @Override protected void cb(CBResult arg0, RepeatedSubSetRequestMsg arg1,
 * AddressIF arg2, Long arg3, CB1<Boolean> arg4) { // Auto-generated method stub
 * final AddressIF answerNod = arg1.from; log.info("repeated from: " +
 * answerNod); final long msgID = arg1.getMsgId();
 * 
 * RepeatedSubSetResponseMsg msg2 = new RepeatedSubSetResponseMsg(
 * Ninaloader.me); sendResponseMessage("repeated", answerNod, msg2, msgID, null,
 * arg4);
 * 
 * // backtracking process // backtracking process // from=lastHop int
 * len=arg1.HopRecords.size(); AddressIF from=null; if(len>0){
 * from=arg1.HopRecords.get(len-1); // arg1.HopRecords.remove(len-1); }
 * 
 * 
 * null; Vector<backtracking> vList = cachedBackTrack.get(arg1.target); if
 * (vList != null) { Iterator<backtracking> ier = vList.iterator(); if (ier !=
 * null) { while (ier.hasNext()) { backtracking tmp = ier.next(); if
 * ((arg1.OriginFrom.equals(tmp.OriginFrom)) && (arg1.target.equals(tmp.target))
 * && (arg1.version == tmp.version) && (arg1.seed == tmp.seed)) { from =
 * tmp.lastHop; break; } } } }
 * 
 * SubSetClosestRequestMsg msg1 = new SubSetClosestRequestMsg(arg1.OriginFrom,
 * from, arg1.target, arg1.K, arg1.NearestNeighborIndex, arg1.version,
 * arg1.seed, arg1.root, arg1.filter, arg1.ForbiddenFilter); //hop if
 * (!arg1.HopRecords.isEmpty()) { msg1.getHopRecords().addAll(arg1.HopRecords);
 * } final StringBuffer errorBuffer = new StringBuffer();
 * 
 * 
 * receiveClosestQuery(msg1, errorBuffer);
 * 
 * }
 * 
 * }
 *//**
	 * answer the callback of intermediate nodes
	 * 
	 * @author ericfu
	 * 
	 */
/*
 * class SubSetTargetLockReqHandler extends
 * ResponseObjCommCB<SubSetTargetLocatedRequestMsg> {
 * 
 * @Override protected void cb(CBResult arg0, SubSetTargetLocatedRequestMsg
 * arg1, AddressIF arg2, Long arg3, CB1<Boolean> arg4) { // final AddressIF
 * answerNod = arg1.from; // original from //Set<NodesPair> NNs =
 * arg1.NearestNeighborIndex;
 * 
 * // backtracking process // from=lastHop int len=arg1.HopRecords.size();
 * AddressIF from=null; if(len>0){ from=arg1.HopRecords.get(len-1); //
 * arg1.HopRecords.remove(len-1); }
 * 
 * 
 * 
 * // reach the K limit, or there is no backtrack node
 * 
 * // TODO: error // from maybe null SubSetClosestRequestMsg msg1 = new
 * SubSetClosestRequestMsg(arg1.OriginFrom, from, arg1.target, arg1.K,
 * arg1.getNearestNeighborIndex(), arg1.version, arg1.seed, arg1.root,
 * arg1.filter, arg1.ForbiddenFilter);
 * 
 * msg1.getHopRecords().addAll(arg1.HopRecords);
 * 
 * 
 * final StringBuffer errorBuffer = new StringBuffer();
 * 
 * receiveClosestQuery(msg1, errorBuffer);
 * 
 * 
 * final long msgID = arg1.getMsgId();
 * 
 * SubSetTargetLocatedResponseMsg msg22 = new SubSetTargetLocatedResponseMsg(
 * Ninaloader.me); sendResponseMessage("TargetLock", answerNod, msg22, msgID,
 * null, arg4);
 * 
 * }
 * 
 * }
 * 
 * 
 *//**
	 * completed the query process
	 * 
	 * @author ericfu
	 * 
	 */
/*
 * class SubSetCompleteNNReqHandler extends
 * ResponseObjCommCB<CompleteSubsetSearchRequestMsg> {
 * 
 * @SuppressWarnings("unchecked")
 * 
 * @Override protected void cb(CBResult arg0, CompleteSubsetSearchRequestMsg
 * arg1, AddressIF arg2, Long arg3, CB1<Boolean> arg4) {
 * 
 * // final AddressIF answerNod=arg1.from; //original from
 * 
 * // acked
 * 
 * // ------------------------------------------
 * CompleteSubSubsetSearchResponseMsg msg22 = new
 * CompleteSubSubsetSearchResponseMsg( Ninaloader.me); msg22.setResponse(true);
 * msg22.setMsgId(arg1.getMsgId()); //
 * ------------------------------------------ sendResponseMessage("CompleteNN",
 * arg1.from, msg22, arg1.getMsgId(), null, arg4); // postProcessing
 * postProcessKNN(arg1);
 * 
 * }
 * 
 * }
 * 
 * class SubSetWithdrawReqHandler extends ResponseObjCommCB<WithdrawRequestMsg>
 * {
 * 
 * @Override protected void cb(CBResult arg0, WithdrawRequestMsg arg1, AddressIF
 * arg2, Long arg3, CB1<Boolean> arg4) { // TODO Auto-generated method stub //
 * remove records
 * 
 * synchronized (registeredNearestNodes) {
 * 
 * Vector<record> RegisteredNNVec = registeredNearestNodes .get(arg1.target); if
 * (RegisteredNNVec != null && RegisteredNNVec.size() > 0) { Iterator<record>
 * ier = RegisteredNNVec.iterator(); while (ier.hasNext()) { record tmp =
 * ier.next(); if (tmp.OriginFrom.equals(arg1.OriginFrom) && tmp.version ==
 * arg1.version && tmp.seed == arg1.seed) { ier.remove(); break; } } } }
 * 
 * synchronized (cachedBackTrack) { backtracking tmp = null;
 * Vector<backtracking> vList = cachedBackTrack.get(arg1.target); if (vList !=
 * null) { Iterator<backtracking> ier = vList.iterator(); while (ier.hasNext())
 * { tmp = ier.next(); if (tmp.target.equals(arg1.target) &&
 * tmp.OriginFrom.equals(arg1.OriginFrom) && (tmp.version == arg1.version) &&
 * (tmp.seed == arg1.seed)) {
 * 
 * // cachedBackTrack.get(arg1.target).remove(tmp); ier.remove(); break; } } }
 * 
 * } // ------------------------------------- WithdrawResponseMsg msg33 = new
 * WithdrawResponseMsg(Ninaloader.me); sendResponseMessage("Withdraw",
 * arg1.from, msg33, arg1.getMsgId(), null, arg4);
 * 
 * }
 * 
 * }
 * 
 * class FarthestReqHandler extends ResponseObjCommCB<FarthestRequestMsg> {
 * 
 * @Override protected void cb(CBResult arg0, FarthestRequestMsg arg1, AddressIF
 * arg2, Long arg3, CB1<Boolean> arg4) { // TODO Auto-generated method stub //
 * final AddressIF remNod = arg1.target; final AddressIF answerNod = arg1.from;
 * final long msgID = arg1.getMsgId(); final StringBuffer errorBuffer = new
 * StringBuffer(); receiveFathestReq(arg1, errorBuffer);
 * sendResponseMessage("Farthest", answerNod, new FarthestResponseMsg(
 * Ninaloader.me), msgID, null, arg4);
 * 
 * } }
 * 
 *//**
	 * receive
	 * 
	 * @author ericfu
	 * 
	 */
/*
 * public class QueryKNNReqHandler extends ResponseObjCommCB<QueryKNNRequestMsg>
 * {
 * 
 * @Override protected void cb(CBResult result, QueryKNNRequestMsg arg1,
 * AddressIF arg2, Long arg3, CB1<Boolean> arg4) { // TODO Auto-generated method
 * stub
 * 
 * final AddressIF nonRingNode = arg1.from; final AddressIF target =
 * arg1.target; final int version = arg1.version;
 * 
 * QueryKNNResponseMsg msg = new QueryKNNResponseMsg(Ninaloader.me);
 * sendResponseMessage("QueryKNNRequest", nonRingNode, msg, arg1 .getMsgId(),
 * null, arg4);
 * 
 * queryKNearestNeighbors(arg1.target, arg1.K, new CB1<List<NodesPair>>() {
 * 
 * @Override protected void cb(CBResult result, List<NodesPair> knns) { // TODO
 * Auto-generated method stub
 * 
 * // send KNN results to query nodes FinKNNRequestMsg msg = new
 * FinKNNRequestMsg( Ninaloader.me, target, knns, version);
 * comm.sendRequestMessage(msg, nonRingNode, new
 * ObjCommRRCB<FinKNNResponseMsg>() {
 * 
 * @Override protected void cb(CBResult result, FinKNNResponseMsg finKNN,
 * AddressIF arg2, Long arg3) { // TODO Auto-generated method stub switch
 * (result.state) { case OK: { log .info(
 * "Successfully finish KNN queries from non-ring nodes"); break; } case
 * TIMEOUT: case ERROR: { // error log .info("FAILED to send out KNN query! " +
 * result .toString()); break; } }
 * 
 * }
 * 
 * }); }
 * 
 * });
 * 
 * 
 * 
 * }
 * 
 * }
 * 
 * public class FinKNNRequestMsgHandler extends
 * ResponseObjCommCB<FinKNNRequestMsg> {
 * 
 * @Override protected void cb(CBResult result, FinKNNRequestMsg arg1, AddressIF
 * arg2, Long arg3, CB1<Boolean> arg4) {
 * 
 * 
 * // TODO Auto-generated method stub FinKNNResponseMsg msg2 = new
 * FinKNNResponseMsg(Ninaloader.me); sendResponseMessage("FinKNNRequest",
 * arg1.from, msg2, arg1 .getMsgId(), null, arg4);
 * 
 * 
 * switch (result.state) { case OK: { AddressIF target = arg1.target; int
 * version = arg1.version; if (pendingKNNs4NonRings.containsKey(target)) {
 * 
 * Vector<cachedKNNs4NonRingNodes> tmpVec = pendingKNNs4NonRings .get(target);
 * Iterator<cachedKNNs4NonRingNodes> ier = tmpVec.iterator(); while
 * (ier.hasNext()) { cachedKNNs4NonRingNodes rec = ier.next();
 * 
 * if (rec.version == version) { // found // log.info(
 * "FOUND the pending KNN callbacks!");
 * 
 * rec.cbDone.call(CBResult.OK(), arg1.NearestNeighborIndex); ier.remove();
 * break; } }
 * 
 * }
 * 
 * log.info("Successfully finish KNN queries!"); break; } case TIMEOUT: case
 * ERROR: { // error log.info("FAILED to receive KNN query! " +
 * result.toString()); break; }
 * 
 * }
 * 
 * 
 * }
 * 
 * }
 * 
 * //====================================================
 * 
 * 
 *//**
	 * test if the addr is already recorded as KNNs
	 * 
	 * @param req
	 * @return
	 */
/*
 * public boolean QueryHit(ClosestRequestMsg req, AddressIF addr) { if (addr ==
 * null || req == null) { return true; } SubSetManager subset = new
 * SubSetManager(req.getForbiddenFilter()); if (subset.checkNode(addr)) { return
 * true; } else { return false; }
 * 
 * }
 * 
 * // ===================================================================== //
 * TODO: sort nodes //
 * =====================================================================
 * 
 *//**
	 * query K nearest nodes for a target,
	 *//*
	 * @Override public void queryKNearestNeighbors(AddressIF target, int k,
	 * CB1<List<NodesPair>> cbDone) { // TODO Auto-generated method stub
	 * 
	 * }
	 * 
	 * @Override public void sortNodesByDistances(Vector<AddressIF> addrs, int
	 * N, CB1<Set<NodesPair>> cbDone) { // TODO Auto-generated method stub
	 * 
	 * }
	 * 
	 * @Override public void queryKNN(String peeringNeighbor, String target, int
	 * K, CB1<List<NodesPair>> cbDone) { // TODO Auto-generated method stub
	 * 
	 * }
	 * 
	 * 
	 * 
	 * @Override public void receiveClosestQuery(ClosestRequestMsg req,
	 * StringBuffer errorBuffer) { // TODO Auto-generated method stub
	 * 
	 * }
	 * 
	 * }
	 */