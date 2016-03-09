package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import util.Tuple;

public class DistributedSetCoverRouter extends ActiveRouter {
	static int warmupTime = 0;
	static boolean warmUpEnded = false;

	static int messageTtl = 600;
	static private ArrayList<DistributedSetCoverRouter> routerList = new ArrayList<>();

	static private int cooltime = 300;

	private ArrayList<DistributedSetCoverRouter> receivedList = new ArrayList<>();
	private HashMap<DistributedSetCoverRouter, Integer> deltaList = new HashMap<>();
	private HashMap<DTNHost, Integer> meetCount = new HashMap<>();

	private boolean isInitialized = false;
	private int currentDelay = cooltime;
	private int messageSentCount = 0;
	private boolean isCenter = false;

	static void warmUpEnd() {
		ArrayList<DistributedSetCoverRouter> removeList = new ArrayList<>();

		for (DistributedSetCoverRouter router : routerList) {
			if (router.getHost() == null) {
				removeList.add(router);
				continue;
			}

			router.clearMessages();

			// TODO
			router.isCenter = Math.random() <= 0.1;

			if (router.isCenter == true) {
				router.createMessage("the_single_message");
			}
		}

		routerList.removeAll(removeList);
	}

	static private boolean isWarmUp() {
		return warmupTime > SimClock.getTime();
	}

	public DistributedSetCoverRouter(Settings s) {
		super(s);

		initSelf();
	}

	protected DistributedSetCoverRouter(ActiveRouter r) {
		super(r);

		initSelf();
	}

	private void initSelf() {
		routerList.add(this);

		warmupTime = new Settings(report.Report.REPORT_NS).getInt(report.Report.WARMUP_S);
	}

	private void initialize() {
		for (DistributedSetCoverRouter other : routerList) {
			if (this == other) {
				continue;
			}

			meetCount.put(other.getHost(), 1 + (int) (Math.random() * 100));
		}
	}

	private void createMessage() {
		createMessage(getHost().getAddress() + "-" + messageSentCount);
	}

	private void createMessage(String id) {
		Message m = new Message(getHost(), null, id, 0);
		m.setResponseSize(0);
		createNewMessage(m);
	}

	private void trySelfAlgorithm() {
		List<Tuple<Message, Connection>> messages = new ArrayList<Tuple<Message, Connection>>();

		Collection<Message> msgCollection = getMessageCollection();
		List<Connection> connections = new ArrayList<>(getConnections());

		if (connections.size() == 0) {
			return;
		}

		int randIndex = (int) (Math.random() * connections.size());
		Connection otherCon = connections.get(randIndex);

		List<Message> randMsgList = new ArrayList<>();

		for (Message msg : msgCollection) {
			DTNHost other = otherCon.getOtherNode(getHost());
			if (msg.getId().contains(other.getAddress() + "-") == true) {
				continue;
			}

			if (msg.getHops().get(msg.getHopCount()) == other) {
				continue;
			}

			randMsgList.add(msg);
		}

		if (randMsgList.size() == 0) {
			return;
		}

		Message randMsg = randMsgList.get((int) (Math.random() * randMsgList.size()));
		messages.add(new Tuple<>(randMsg, otherCon));

		Tuple<Message, Connection> ret = tryMessagesForConnected(messages);
		if (ret != null) {
			removeFromMessages(randMsg.getId());
		}
	}

	@Override
	protected int checkReceiving(Message m, DTNHost from) {
		int recvCheck = super.checkReceiving(m, from);

		if (recvCheck == RCV_OK) {
			if (isWarmUp() == true) {
				if (msgTtl <= 0) {

				} else {
					msgTtl -= meetCount.get(from);
				}
			} else {
				/*
				 * don't accept a message that has already traversed this node
				 */
				if (m.getHops().contains(getHost())) {
					recvCheck = DENIED_OLD;
				}
			}
		}

		return recvCheck;
	}

	@Override
	public void update() {
		if (isInitialized == false) {
			isInitialized = true;
			initialize();
		}

		if (warmUpEnded == false && isWarmUp() == false) {
			warmUpEnded = true;
			warmUpEnd();
		}

		// if (isTransferring() || !canStartTransfer()) {
		// return;
		// }

		if (exchangeDeliverableMessages() != null) {
			return;
		}

		if (isWarmUp() == true) {
			currentDelay -= SimClock.getIntTime();
			if (currentDelay <= 0) {
				currentDelay += cooltime;

				createMessage();
			}

			trySelfAlgorithm();
		} else {
			tryAllMessagesToAllConnections();
		}
	}

	@Override
	public MessageRouter replicate() {
		// TODO Auto-generated method stub
		return new DistributedSetCoverRouter(this);
	}

}