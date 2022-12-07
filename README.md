# Defense-Architecture-Against-Eclipse-Attack
Tracking Peer Connections - A Defense Architecture Against Eclipse Attack

### Built by:
- Ekleen kaur
- Venkata Shiva Reddy Manchala

## Motivation
Bitcoin is the most powerful cryptocurrency, yet it doesn’t guarantee the honesty of its peers. It is vulnerable to different kinds of attacks, so is the network really safe?
The network needs to be safeguarded from such adversaries.


## Abstract
In a peer-to-peer blockchain network, like bitcoin, the nodes in the network trust their peers to share the information of the longest blockchain and process transactions. But when an attacker who has control over a substantial number of IP addresses eclipses all connections to a victim bitcoin node, he can exploit the victim for attacks on bitcoin’s mining and consensus system. Upon inspecting the eclipse attack’s design, we present a novel attack mitigation architecture that prevents the attack before it could possibly happen. From the peer-node information gossiped on the blockchain network to make connections we use an N-ary tree to keep track of these connections. Assuming each node has an equal probability of being an attacker, the protocol deterministically thwarts the new connections to peers rooting back to the same parent. We prove the effectiveness of our protocol design as a powerful defense strategy against the eclipse attack by artificially simulating a small-scale version of blockchain network and analyzing its close resemblance to the real-world Bitcoin network. We launch an eclipse attack on this simulated network, where we anticipate and avert the attack by inhibiting connections to malicious nodes. Upon following our strategy, the attacker cannot make sufficient botnet connections to the victim necessary for the eclipse attack to succeed. 


### Technical details:
- Maven application
- Source language: Java
- uses Akka actor model for P2P network
- Here is the <a href="https://uflorida-my.sharepoint.com/:u:/g/personal/ekleenkaur_ufl_edu/EVgdGp5MM25BlQPuUIq82EUBGU79qg3l-5jeH0d_2KpxWw?e=7yVVac"> link </a> to docker image file of the implmentation

## Steps to execute the code:


#### Using doker image
Download and extract the docker image tar file from the link above and execute the image. Open bash and follow these instructions...

Change the directory:
```
cd /usr/src/app
```

Run the program by executing the command:
```
mvn compile exec:exec
```

<img width="1341" alt="Screen Shot 2022-12-07 at 10 24 44 AM" src="https://user-images.githubusercontent.com/49470184/206257307-5544060a-53c7-4b71-ae42-0131709e46e0.png">



The program prompts for the following inputs:
<img width="1243" alt="Screen Shot 2022-12-07 at 10 25 40 AM" src="https://user-images.githubusercontent.com/49470184/206260124-0ba73b08-f927-4264-9f7d-db1427664957.png">


The following is the execution screenshots of the program at start of simulation:

<img width="1334" alt="Screen Shot 2022-12-07 at 10 22 49 AM" src="https://user-images.githubusercontent.com/49470184/206260308-c29d20cc-2854-495d-bfd0-9b16ad8fb373.png">


End of simulation after a node is eclipsed:
<img width="1100" alt="Screen Shot 2022-12-07 at 10 19 14 AM" src="https://user-images.githubusercontent.com/49470184/206260427-4f4f5029-e1b3-465d-932c-bac206c40f0c.png">

The program runs indefinitely if no victim node gets eclipsed and needs to be explicitly terminated.


#### Logging configuration
The source code contains two configurations where we can choose to either print the logs to STDOUT or a log file.
