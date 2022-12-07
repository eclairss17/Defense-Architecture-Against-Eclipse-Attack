package com.example;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.slf4j.Slf4jLogger;

import java.util.Random;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NetworkMain extends AbstractActor {

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    public class StartSimulate{
        public StartSimulate() {}
    }
    
    public int numberOfNodes;
    public int attackerRange;
    public int victimRange;
    private Map<Integer, ActorRef> allNodes = new HashMap<>();
    Random attackerRandom = new Random();
    Random ttlRandom = new Random();

    // private NetworkMain(ActorContext<ReceivePeerInformation> context, Integer numberOfNodes) {
    private NetworkMain(Integer numberOfNodes, int percentageAttacker, int percentageVictim) {
        // super(context);
        //#create-actors
        this.numberOfNodes = numberOfNodes;
        this.attackerRange = numberOfNodes * percentageAttacker / 100;
        this.victimRange = this.attackerRange + numberOfNodes * percentageVictim / 100;
        Boolean isAttacker = false;
        Integer timeToLive;
        Set<Integer> triedPeers;
        Set<Integer> testedPeers;
        for(int i = 0; i < numberOfNodes; i++){
            isAttacker = i < attackerRange;
            timeToLive = ttlRandom.nextInt(10000) + 10000;
            triedPeers = new HashSet<>();
            testedPeers = new HashSet<>();
            // allNodes.put(i, context.spawn(Node.create(i, isAttacker, true, timeToLive,triedPeers, testedPeers), "peer"+i));
            allNodes.put(i, getContext().actorOf(Props.create(Node.class, i, isAttacker, this.attackerRange, this.victimRange, true, timeToLive,triedPeers, testedPeers)));
        }
        for(int i =0; i < numberOfNodes; i++){
            Node.ReceiveAllNodeReferences command = new Node.ReceiveAllNodeReferences(allNodes);
            allNodes.get(i).tell(command, getSelf());
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("initialized all nodes");
        logger.info(this.allNodes.toString());

        onStartSimulate(new StartSimulate());
    }

    // public static Behavior<ReceivePeerInformation> create(Integer numberOfNodes) {
    //     return Behaviors.setup(context -> new NetworkMain(context, numberOfNodes));
    // }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(StartSimulate.class, this::onStartSimulate).build();
    }

    private void onStartSimulate(StartSimulate info) {
        for(int i=0; i < this.numberOfNodes; i++){
            logger.info("start simulate for {}!", i);
            Node.BeginSimulate command = new Node.BeginSimulate();
            allNodes.get(i).tell(command, getSelf());
        }
    }
}
