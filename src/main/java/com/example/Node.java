package com.example;

import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Kill;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import scala.concurrent.Future;

import java.time.Duration;
import java.util.*;

// #NodeLifeCycleer
public class Node extends AbstractActor {

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
	public Set<Integer> requestSet = new HashSet<>();
	public Random random = new Random();
	int min = 5, max = 7;
	int connectionTimeout = 5000;

	public interface NodeLifeCycle{}

	public static class ReceiveAllNodeReferences implements NodeLifeCycle {
		private Map<Integer, ActorRef> nodeMap;
		public 	ReceiveAllNodeReferences(Map<Integer, ActorRef> nodeMap){
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
		public ArrayList<Integer> gossipPeers;
		public CommandAddToTest(ArrayList<Integer> gossipPeers){
			this.gossipPeers = gossipPeers;
		}
	}

	public static class BeginSimulate implements NodeLifeCycle{}

	public static class TerminateSimulation implements NodeLifeCycle{}

	public static class ConnectionRequest implements NodeLifeCycle{
		// public int connectionRequestFrom;
		public ArrayList<Integer> gossipPeers;
		Integer source;
		ConnectionRequest(ArrayList<Integer> gossipPeers, Integer replyTo){
			this.gossipPeers = gossipPeers;
			this.source = replyTo;
		}
	}

	public static class ConnectionResponse implements NodeLifeCycle{
		public ArrayList<Integer> gossipPeers;
		Integer source;
		ConnectionResponse(ArrayList<Integer> gossipPeers, Integer replyTo){
			this.gossipPeers = gossipPeers;
			this.source = replyTo;
		}
	}


	// public static Behavior<NodeLifeCycle> create(int nodeId, Boolean isAttacker, Boolean online, Integer timeToLive, 
	// 									Set<Integer> triedPeers, Set<Integer> testedPeers) {
	// 	return Behaviors.setup(context -> new Node(context, nodeId, isAttacker, online, timeToLive, triedPeers, testedPeers));
	// }

	private Node(int nodeId, Boolean isAttacker, int attackerRange, int victimNodeRange, Boolean online, Integer timeToLive, 
					Set<Integer> triedPeers, Set<Integer> testedPeers) {
		// super(context);
		this.nodeId = nodeId;
		this.isAttacker = isAttacker;
		this.attackerRange = attackerRange;
		this.victimNodeRange = victimNodeRange;
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
			.match(ConnectionRequest.class, this::uponConnectionRequest)
			.match(TerminateSimulation.class, this::onTerminate).build();

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
		
		int triedPeerCount = this.random.nextInt(nodeMap.size()/10) + 2;
		for(int i =0; i < triedPeerCount; i++){
			int randomNode = this.random.nextInt(nodeMap.size());
			if(randomNode != nodeId)
				this.triedPeers.add(randomNode);
		}
		if(nodeId < attackerRange)
		for(int i = attackerRange; i < victimNodeRange; i++) testedPeers.add(i);
		logger.info("currentnode: {}, triedpeers{},", nodeId, triedPeers);
		gossipNodeToTriedPeers();
	}

	private void gossipNodeToTriedPeers(){
		Node.CommandAddToTried command = new Node.CommandAddToTried(nodeId);
		for(int eachNode:this.triedPeers){
			nodeMap.get(eachNode).tell(command, getSelf());
		}
		gossipPeersToTest();
	}

	private void gossipPeersToTest(){
		// Random randomPeer = new Random();
		
		// ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
		int gossipPeerCount = this.random.nextInt(max-min)+min;
		// if(nodeId < attackerRange){
		// 	for(int eachNode : this.triedPeers){
		// 		for(int i=0; i<gossipPeerCount; i++){
		// 			gossipPeers.add(this.random.nextInt(attackerRange));
		// 		}
		// 	}
		// }
		// else{
			for(int eachNode:this.triedPeers){
				ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
				// gossipPeers.clear();
				for(int i=0; i<gossipPeerCount; i++){
					gossipPeers.add(this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size()-1)).findFirst().orElse(null));
				}
			
				Node.CommandAddToTest command = new Node.CommandAddToTest(gossipPeers);
				nodeMap.get(eachNode).tell(command, getSelf());
			}
		// }
	}
	private void addPeersToTest(CommandAddToTest command){
		Iterator<Integer> it = command.gossipPeers.iterator();
		while (it.hasNext()) {
			Integer eachNode = it.next();
			if(eachNode != this.nodeId && !this.triedPeers.contains(eachNode))
				this.testedPeers.add(eachNode);
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
		// return this;
	}

	public void onTerminate(TerminateSimulation command){
		// logger.info("Termination simulate for " + nodeId);
		// getSelf().t
		// getContext().getSystem().terminate();
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
				gossipPeers.add(this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size()-1)).findFirst().orElse(null));
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

		try {
            Thread.sleep(500+ random.nextInt(1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

		logger.info("asking connection by " + nodeId + "to" + randomPeer);
        f.onComplete(new OnComplete<Object>(){
            public void onComplete(Throwable t, Object result){
				if(result != null){
					ConnectionResponse response = (ConnectionResponse)result;
					if(response.gossipPeers != null && !response.gossipPeers.isEmpty()){
						for(int eachNode:command.gossipPeers) {
							if(eachNode!= nodeId && !triedPeers.contains(eachNode))
								testedPeers.add(eachNode);
						}
						triedPeers.remove(triedPeers.stream().skip(random.nextInt(triedPeers.size()-1)).findFirst().orElse(null));
						triedPeers.add(command.source);
						testedPeers.remove(command.source);
						requestSet.clear();
						logger.info("connection made between " + nodeId + " and " + response.source);
					}
					else{
						requestSet.add(randomPeer);
						logger.info("connection rejected between " + nodeId + " and " + response.source);
					}
				}
			}
        }, getContext().getSystem().dispatcher());
		logger.info("currentnode: {}, triedpeers{}, testedPeers: {}", nodeId, triedPeers, testedPeers);
		
	}

	public void uponConnectionRequest(ConnectionRequest command){
		logger.info("got connection request by " + command.source + " to " + nodeId);
		if(nodeId < attackerRange && command.source >= attackerRange && command.source < victimNodeRange){
			// if(requestSet.contains(command.source) || {
				logger.info("attacker {} accepted victim node connection", nodeId);
				for(int eachNode:command.gossipPeers) {
					if(eachNode!= this.nodeId && !this.triedPeers.contains(eachNode))
						this.testedPeers.add(eachNode);
				}
				// this.triedPeers.remove(this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size()-1)).findFirst().orElse(null));
				// this.triedPeers.add(command.source);
				// if(testedPeers.contains(command.source))
				// 	this.testedPeers.remove(command.source);
				requestSet.clear();
				
				
				int gossipPeerCount = this.random.nextInt(max-min)+min;
				ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
				for(int i=0; i<gossipPeerCount; i++){
					gossipPeers.add(this.random.nextInt(attackerRange));
				}
				ConnectionResponse response = new ConnectionResponse(gossipPeers, this.nodeId);
				getSender().tell(response, getSelf());
			// }
			// else{
			// 	if(!this.testedPeers.contains(command.source) && !this.triedPeers.contains(command.source))
			// 		this.testedPeers.add(command.source);
			// 	ConnectionResponse response = new ConnectionResponse(null, nodeId);
			// 	getSender().tell(response, getSelf());
			// 	logger.info("connection rejected by attacker {} from {}", nodeId, command.source);
			// }
		}
		else{
			if(requestSet.contains(command.source)){
				for(int eachNode:command.gossipPeers) {
					if(eachNode!= this.nodeId && !this.triedPeers.contains(eachNode))
						this.testedPeers.add(eachNode);
				}
				int peer_remove = this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size()-1)).findFirst().orElse(null);
				if(nodeId > attackerRange && nodeId < victimNodeRange && peer_remove < attackerRange)
					while(peer_remove < attackerRange) peer_remove = this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size()-1)).findFirst().orElse(null);
				this.triedPeers.remove(peer_remove);
				this.triedPeers.add(command.source);
				this.testedPeers.remove(command.source);
				requestSet.clear();

				
				int gossipPeerCount = this.random.nextInt(max-min)+min;
				ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
				for(int i=0; i<gossipPeerCount; i++){
					//take random elements from the tried table
					gossipPeers.add(this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size()-1)).findFirst().orElse(null));
				}

				ConnectionResponse response = new ConnectionResponse(gossipPeers, this.nodeId);
				getSender().tell(response, getSelf());
				logger.info("connection accepted from " + command.source + " by " + nodeId);
			}
			else{
				//repond no , //Create class behaviour answerconnectionrequest -> //update request map //restart simulation
				if(!this.testedPeers.contains(command.source) && !this.triedPeers.contains(command.source))
					this.testedPeers.add(command.source);
				ConnectionResponse response = new ConnectionResponse(null, nodeId);
				getSender().tell(response, getSelf());
				logger.info("connection rejected from " + command.source + " by " + nodeId);
			}
		}
		// return this;
	}

	

}


