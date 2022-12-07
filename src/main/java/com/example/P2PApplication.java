package com.example;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.AbstractActor.Receive;

import java.util.Scanner;

public class P2PApplication {
	public static void main(String[] args) {

		org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(P2PApplication.class);

		Scanner input = new Scanner(System.in);
        System.out.println("Enter the number of nodes");
        int numberOfNodes = Integer.parseInt(input.nextLine());  

        System.out.println("Enter the percentage of attackers");
        int percentageAttackers = Integer.parseInt(input.nextLine());  

		System.out.println("Enter the percentage of victims");
        int percentageVictims = Integer.parseInt(input.nextLine());  

        final ActorSystem system = ActorSystem.create();
		ActorRef mainRef = system.actorOf(Props.create(NetworkMain.class, numberOfNodes, percentageAttackers, percentageVictims));

		/* Uncomment the code below to run the simulation with defense strategy */		
		// final ActorSystem<DefenseNetworkMain.ReceivePeerInformation> defenseNetworkMain =
		// 						 ActorSystem.create(DefenseNetworkMain.create(numberOfNodes), "DefenseSystem");


        logger.info(">>> Press ENTER to exit <<<");
		input.nextLine();
		input.close();
		system.terminate();
		// defenseNetworkMain.terminate();
    }

	// public class Main extends AbstractActor {

	// 	public class StartSimulate{
	// 		int numberOfNodes;
	// 		ActorRef networkMain;
	// 		public StartSimulate(int numberOfNodes, ActorRef networkMain) {
	// 			this.numberOfNodes = numberOfNodes;
	// 			this.networkMain = networkMain;
	// 		}
	// 	}
		
	// 	@Override
	// 	public Receive createReceive() {
	// 		return receiveBuilder().match(StartSimulate.class, this::onStartSimulate).build();
	// 	}

	// 	private void onStartSimulate(StartSimulate info) {
	// 		for(int i=0; i < info.numberOfNodes; i++){
	// 			Node.BeginSimulate command = new Node.BeginSimulate();
	// 			allNodes.get(i).tell(command, getSelf());
	// 		}
	// 	}

	// 	private static void simulate(ActorRef mainRef) {
	// 		mainRef.tell(new Node.BeginSimulate(), getSelf());
	// 	}

	// }
}
