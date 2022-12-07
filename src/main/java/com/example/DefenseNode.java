package com.example;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Kill;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import scala.concurrent.Future;

import java.util.*;

// #NodeLifeCycleer
public class DefenseNode extends AbstractActor {

	org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

	public int nodeId;
	public Boolean isAttacker;
	public int attackerRange;
	public int victimNodeRange;
	public Boolean online;
	public Integer timeToLive;
	public Set<Integer> triedPeers;
	public Set<Integer> testedPeers;
	public Map<Integer, ActorRef> nodeMap = null;
	public Set<Integer> requestSet;
	public Map<Integer, TreeNode> masterMap;
	public Map<Integer, TreeNode> toplevelMap;
	public Random random = new Random();
	int min = 5, max = 7;
	int connectionTimeout = 5000;

	static class TreeNode{
		int nodeId;
		int count;
		Set<TreeNode> children;
		public TreeNode(int nodeId, int count, Set<TreeNode> children){
			this.nodeId = nodeId;
			this.count = count;
			this.children = children;
		}
	};

	public interface NodeLifeCycle{}

	public static class ReceiveAllNodeReferences implements NodeLifeCycle{
		private Map<Integer, ActorRef> nodeMap;
		public ReceiveAllNodeReferences(Map<Integer, ActorRef> nodeMap){
			this.nodeMap = nodeMap;
		}
	}
	public static class CommandAddToTried implements NodeLifeCycle {
		public int tellNodeToAddToTried;
		
		public CommandAddToTried(int tellNodeToAddToTried){
			this.tellNodeToAddToTried = tellNodeToAddToTried;
		}
	}

	public static class CommandAddToTest implements NodeLifeCycle {
		int peersFromNodeId;
		public ArrayList<Integer> gossipPeers;
		public CommandAddToTest(int peersFromNodeId, ArrayList<Integer> gossipPeers){
			this.peersFromNodeId = peersFromNodeId;
			this.gossipPeers = gossipPeers;
		}
	}

	public static class BeginSimulate implements NodeLifeCycle{}

	public static class ConnectionRequest implements NodeLifeCycle{
		public int connectionRequestFrom;
		public ArrayList<Integer> gossipPeers;
		ConnectionRequest(ArrayList<Integer> gossipPeers, Integer replyTo){
			this.connectionRequestFrom = replyTo;
			this.gossipPeers = gossipPeers;
		}
	}

	public static class ConnectionResponse implements NodeLifeCycle{
		public ArrayList<Integer> gossipPeers;
		Integer connectionRequestFrom;
		ConnectionResponse(ArrayList<Integer> gossipPeers, Integer replyTo){
			this.gossipPeers = gossipPeers;
			this.connectionRequestFrom = replyTo;
		}
	}

	// public static Behavior<NodeLifeCycle> create(int nodeId, Boolean isAttacker, Boolean online, Integer timeToLive, 
	// 									Set<Integer> triedPeers, Set<Integer> testedPeers) {
	// 	return Behaviors.setup(context -> new DefenseNode(context, nodeId, isAttacker, online, timeToLive, triedPeers, testedPeers));
	// }

	private DefenseNode(int nodeId, Boolean isAttacker, int attackerRange, int victimNodeRange, Boolean online, Integer timeToLive, 
					Set<Integer> triedPeers, Set<Integer> testedPeers) {
		// super(context);
		this.nodeId = nodeId;
		this.isAttacker = isAttacker;
		this.online = online;
		this.timeToLive = timeToLive;
		this.triedPeers = triedPeers;
		this.testedPeers = testedPeers;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(ReceiveAllNodeReferences.class, this::startReceiveAllNodeReferences)
			.match(CommandAddToTried.class, this::addNodeToTried)
			.match(CommandAddToTest.class, this::addPeersToTest)
			.match(BeginSimulate.class, this::simulate)
			.match(ConnectionRequest.class, this::uponConnectionRequest).build();

	}

	private void startReceiveAllNodeReferences(ReceiveAllNodeReferences command) {
		// getContext().getLog().info("Hello {}!", command.nodeMap);
		this.nodeMap = command.nodeMap;
		fillNodeTriedTable();
		// logger.info(this.nodeMap);
		// return this;
	}
	private void addNodeToTried(CommandAddToTried command){
		if(!this.triedPeers.contains(command.tellNodeToAddToTried))
			this.triedPeers.add(command.tellNodeToAddToTried);
		// return this;
	}

	private void fillNodeTriedTable(){
		// Random triedPeersCount = new Random();
		int triedPeerCount = this.random.nextInt(10);
		for(int i =0; i < triedPeerCount; i++){
			int randomNode = this.random.nextInt(nodeMap.size());
			if(randomNode != nodeId){
				this.triedPeers.add(nodeId);
				TreeNode newNode = new TreeNode(nodeId,0,new HashSet<TreeNode>());
				masterMap.put(nodeId, newNode);
				toplevelMap.put(nodeId, newNode);
			//create tree map master{node id, count of active children, set of children}
			//create TreeNode of tried peer and add to top level treenode map
			}
		}
		if(nodeId < attackerRange)
		for(int i = attackerRange; i < victimNodeRange; i++) testedPeers.add(i);
		logger.info("currentnode: {}, triedpeers{},", nodeId, triedPeers);
		gossipNodeToTriedPeers();
	}

	private void gossipNodeToTriedPeers(){
		DefenseNode.CommandAddToTried command = new DefenseNode.CommandAddToTried(nodeId);
		for(int eachNode:this.triedPeers){
			nodeMap.get(eachNode).tell(command, getSelf());
		}
		gossipPeersToTest();
	}
	private void gossipPeersToTest(){
		// Random randomPeer = new Random();
		int min = 4, max = 7;
		// ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
		for(int eachNode:this.triedPeers){
			int gossipPeerCount = this.random.nextInt(max-min)+min;
			ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
			// gossipPeers.clear();
			for(int i=0; i<gossipPeerCount; i++){
				gossipPeers.add(this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size())).findFirst().orElse(null));
			}
		
			DefenseNode.CommandAddToTest command = new DefenseNode.CommandAddToTest(this.nodeId, gossipPeers);
			nodeMap.get(eachNode).tell(command, getSelf());
		}
	}
	private void addPeersToTest(CommandAddToTest command){
		Iterator<Integer> it = command.gossipPeers.iterator();
		while (it.hasNext()) {
			Integer eachNode = it.next();
			if(eachNode!= this.nodeId && !this.triedPeers.contains(eachNode) && !masterMap.containsKey(eachNode) )//check if the child already exists in master treenode map
				this.testedPeers.add(eachNode);
				TreeNode newNode = new TreeNode(eachNode,0,new HashSet<TreeNode>());
				masterMap.put(eachNode, newNode);
				masterMap.get(command.peersFromNodeId).children.add(newNode);
				//create treenode for this node, add this to the children set of the one sending the command
		}
		logger.info("currentnode: {}, triedpeers{}, testedPeers: {}", nodeId, triedPeers, testedPeers);
		// return this;
	}

	public void simulate(BeginSimulate command){
		logger.info("Begin simulate for " + nodeId);
		if(eclipsed()) {
			logger.info("Node {} was eclipsed", nodeId);
			// getContext().getSystem().terminate();
			for(Map.Entry<Integer, ActorRef> entry : nodeMap.entrySet()){
				logger.info("Terminate simulation for " + entry.getKey());
				if(entry.getKey() != nodeId) entry.getValue().tell(Kill.getInstance(), getSelf());
			}
			getContext().getSystem().terminate();
			getSelf().tell(Kill.getInstance(), getSelf());
		}
		conectionRequest();
		// if(!(nodeId < attackerRange && triedPeers.contains(victimNode)))
			getSelf().tell(new BeginSimulate(), getSelf());
	}

	private boolean eclipsed(){
		if(nodeId < attackerRange) return false;
		int count = 0;
		for (Integer peer : triedPeers) {
			if(peer < attackerRange){
				count++;
			}
		}
		
		logger.info("count of attacker nodes in victim {} of {}: " + count, nodeId, triedPeers);
		return (count * 100) > (triedPeers.size() * 60) && (nodeId >= attackerRange && nodeId < victimNodeRange);
	}

	private void conectionRequest(){

		int gossipPeerCount = this.random.nextInt(max-min)+min;
		ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
		int randomPeer;
		if(nodeId < attackerRange){
			for(int i=0; i<gossipPeerCount; i++){
				gossipPeers.add(this.random.nextInt(attackerRange));
			}
			// randomPeer = this.victimNode;
		}
		else{
			for(int i=0; i<gossipPeerCount; i++){
				//take random elements from the tried table
				gossipPeers.add(this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size())).findFirst().orElse(null));
			}
		}
		int findPeer = 0;
		if(testedPeers.size() > 1)
			findPeer = this.testedPeers.stream().skip(this.random.nextInt(this.testedPeers.size()-1)).findFirst().orElse(null);
		else{
			findPeer = this.random.nextInt(nodeMap.size());
			while(triedPeers.contains(findPeer)) findPeer = this.random.nextInt(nodeMap.size());
			testedPeers.add(findPeer);
		}
		randomPeer = findPeer;

		ConnectionRequest command = new ConnectionRequest(gossipPeers, this.nodeId);
		
		Future<Object> f = Patterns.ask(nodeMap.get(randomPeer), command, connectionTimeout);

		logger.info("asking...");

		try {
            Thread.sleep(500+random.nextInt(1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        f.onComplete(new OnComplete<Object>(){
            public void onComplete(Throwable t, Object result){
				ConnectionResponse response = (ConnectionResponse)result;
				if(response.gossipPeers != null && !response.gossipPeers.isEmpty()){
					for(int eachNode:command.gossipPeers) {
						if(eachNode!= nodeId && !triedPeers.contains(eachNode))//check if the child already exists in master treenode map
							testedPeers.add(eachNode);
							TreeNode newNode = new TreeNode(eachNode,0,new HashSet<TreeNode>());
							masterMap.put(eachNode, newNode);
							masterMap.get(response.connectionRequestFrom).children.add(newNode);
							//create treenode for this node, add this to the children set of the one sending the command
					}
					triedPeers.remove(triedPeers.stream().skip(random.nextInt(triedPeers.size()-1)).findFirst().orElse(null));
					triedPeers.add(command.connectionRequestFrom);
					testedPeers.remove(command.connectionRequestFrom);
					requestSet.clear();
					logger.info("connection made between " + nodeId + " and " + response.connectionRequestFrom);
				}
				else{
					requestSet.add(randomPeer);
					logger.info("connection Rejected between " + nodeId + "and" + response.connectionRequestFrom);
				}
            }
        }, getContext().getSystem().dispatcher());
		logger.info("currentnode: {}, triedpeers{}, testedPeers: {}", nodeId, triedPeers, testedPeers);
	}

	public void uponConnectionRequest(ConnectionRequest command){

		logger.info("got connection request by " + command.connectionRequestFrom + " to " + nodeId);
		if(nodeId < attackerRange && command.connectionRequestFrom >= attackerRange && command.connectionRequestFrom < victimNodeRange){
			// if(requestSet.contains(command.source) || {
				logger.info("attacker {} accepted victim node connection", nodeId);
				for(int eachNode:command.gossipPeers) {
					if(eachNode!= this.nodeId && !this.triedPeers.contains(eachNode))
						this.testedPeers.add(eachNode);
					TreeNode newNode = new TreeNode(eachNode,0,new HashSet<TreeNode>());
					masterMap.put(eachNode, newNode);
					masterMap.get(command.connectionRequestFrom).children.add(newNode);
					//create treenode for this node, add this to the children set of the one sending the command
				}
			// this.triedPeers.add(command.connectionRequestFrom);
			// this.testedPeers.remove(command.connectionRequestFrom);
			requestSet.clear();

			int gossipPeerCount = this.random.nextInt(max-min)+min;
			ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
			for(int i=0; i<gossipPeerCount; i++){
				gossipPeers.add(this.random.nextInt(attackerRange));
			}
			ConnectionResponse response = new ConnectionResponse(gossipPeers, this.nodeId);
			getSender().tell(response, getSelf());
		}
		else{
			if(requestSet.contains(command.connectionRequestFrom)){
				for(int eachNode:command.gossipPeers) {
					if(eachNode!= this.nodeId && !this.triedPeers.contains(eachNode))
						this.testedPeers.add(eachNode);
				}
				int peer_remove = this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size()-1)).findFirst().orElse(null);
				if(nodeId > attackerRange && nodeId < victimNodeRange && peer_remove < attackerRange)
					while(peer_remove < attackerRange) peer_remove = this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size()-1)).findFirst().orElse(null);
				this.triedPeers.remove(peer_remove);
				this.triedPeers.add(command.connectionRequestFrom);
				this.testedPeers.remove(command.connectionRequestFrom);
				requestSet.clear();

				
				int gossipPeerCount = this.random.nextInt(max-min)+min;
				ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
				for(int i=0; i<gossipPeerCount; i++){
					//take random elements from the tried table
					gossipPeers.add(this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size()-1)).findFirst().orElse(null));
				}

				ConnectionResponse response = new ConnectionResponse(gossipPeers, this.nodeId);
				getSender().tell(response, getSelf());
				logger.info("connection accepted from " + command.connectionRequestFrom + " by " + nodeId);
			}
			else{
				//repond no , //Create class behaviour answerconnectionrequest -> //update request map //restart simulation
				if(!this.testedPeers.contains(command.connectionRequestFrom) && !this.triedPeers.contains(command.connectionRequestFrom))
					this.testedPeers.add(command.connectionRequestFrom);
				ConnectionResponse response = new ConnectionResponse(null, nodeId);
				getSender().tell(response, getSelf());
				logger.info("connection rejected from " + command.connectionRequestFrom + " by " + nodeId);
			}
		}

		// return this;
	}


}


