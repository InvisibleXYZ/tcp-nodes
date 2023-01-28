Description of the protocol:
The TCP protocol was used both for communicating with client and between nodes.

There are two types of massages:
1. Communication between nodes only (CONNECT <address>:<port>, TERMINATED <address>:<port>)
2. Communication with a client and nodes(set-value <key>:<value>, get-max, etc)
There is no difference in way of sending, receiving. This difference was made only to
demonstrate user what is happening.

Starting:
When node is starting, it stores it`s own port, key and value, starts ServerSocket and
thread to work with multiple clients. Also it stores <address>:<port> numbers of the nodes
it intends to connect and send CONNECT <address>:<port> massages to the nodes (where
<address> and <port> is address and port of the sender).

Storing connections:
Connections are storing using Map<String, Status>, where
String - address in format of <address>:<port>
Status - CONNECTED or TERMINATED, enum

Command buffer:
If node received a massage, which it probabaly would need to send further (operation messages),
it stores information about it inside the Map<String, String> with limited size, where
String - massage
String - result of operation or ALDONE if operation in work

Handeling massages:
1. CONNECT <address>:<port>: when node is receiving this massage, it means that some node wants to
							 to connect. The node save the <address>:<port> information for future
							 communication;
							 
2. set-value <key>:<value>:  if this node`s key is equal to <key>, it changes value to <value> and sends OK.
							 If not, it sends this message to one of the stored (connected)
							 nodes, if it`s not TERMINATED.
							 Waiting for node to anwer, if ALDONE, skip this node and send
							 to next one, else return anwer.
							 If there is noone to send and key is not equal to <key>, send ERROR.
							 This massage is handled using Depth First Search Algorithm;
							 
3. get-value <key>:			 if this node`s key is equal to <key>, it sends value.
							 If not, it sends this message to one of the stored (connected)
							 nodes, if it`s not TERMINATED.
							 Waiting for node to anwer, if ALDONE, skip this node and send
							 to next one, else return anwer.
							 If there is noone to send and key is not equal to <key>, send ERROR.
							 This massage is handled using Depth First Search Algorithm.
							 
4. find-key <key>:			 if this node`s key is equal to <key>, it sends it`s address in form <address>:<port>.
							 If not, it sends this message to one of the stored (connected)
							 nodes, if it`s not TERMINATED.
							 Waiting for node to anwer, if ALDONE, skip this node and send
							 to next one, else return anwer.
							 If there is noone to send and key is not equal to <key>, send ERROR.
							 This massage is handled using Depth First Search Algorithm.
							 
5. get-max:					 The node sends this message to one of the stored (connected)
							 nodes, if it`s not TERMINATED.
							 Waiting for node to anwer, if ALDONE, skip this node and send
							 to next one, else compare value of the answer with the value of this node
							 and find the max.
							 If there is nomore connections to send, returns <key>:<value> of
							 neighbor nodes with max value or itself if has max value was value of the node;
							 This massage is handled using Depth First Search Algorithm.
							 
5. get-min:					 The node sends this message to one of the stored (connected)
							 nodes, if it`s not TERMINATED.
							 Waiting for node to anwer, if ALDONE, skip this node and send
							 to next one, else compare value of the answer with the value of this node
							 and find the min.
							 If there is nomore connections to send, returns <key>:<value> of
							 neighbor nodes with min value or itself if has min value was value of the node;
							 This massage is handled using Depth First Search Algorithm.

6. new-record <key>:<value>: set new key and value of this node. Send OK message

7. terminate:				 sending TERMINATED <address>:<port> massage to all not TERMINATED
							 neighbors, where <address>:<port> is a address and port numbers of
							 node which sending. Returning OK massage and exits the program.

8. TERMINATED <address>:<port>: changes the value of <address> to TERMINATED in stored connections.

Depth First Search Algorithm:
This algorithm was used, because of it`s controlability comparing, for example, to Breadth First Search.
Basicly, algorithm sends message to next node and waits for the answer and next node may send to it`s next,
making something like a line waiting for answer. Sometimes a line may change before return to the first node,
in order to find all the nodes.

How to start the program:
1. Compile the source files:
Write the command to the command prompt in this folder:
-javac DatabaseNode.java

2. Run node(s):
Write the command to the command prompt in this folder:
java DatabaseNode -tcpport <TCP port number> -record <key>:<value> [ -connect <address>:<port> ]
where:
-tcpport <TCP port number>: port number on which this node will operate (OBLIGATORY FIELD),
							<TCP port number> - integer number;
-record <key>:<value>: pair of data, will be used for different operations on the node (OBLIGATORY FIELD),
						<key> and <value> - integer numbers;
-connect <address>:<port>: address of another node to connect with (NON-OBLIGATORY FIELD, CAN BE WRITTEN MULTIPLE TIMES),
						   <address> - IP address('localhost' if another node is run on the same machine), <port> - integer number;

Better to run in seperate command prompt (PowerShell) windows, because each node has it`s own output during the operations;

3. Connect client to operate on the created network;

Implemented:
1. TCP protocol
2. Communication
3. Massage handling
4. Every operation required

Not working:
Everything that was required is working