package routing;

import core.DTNHost;
import core.Message;
import core.Settings;

public class DistributedSetCoverRouter extends ActiveRouter {

	public DistributedSetCoverRouter(Settings s) {
		super(s);
	}

	protected DistributedSetCoverRouter(ActiveRouter r) {
		super(r);
	}

	@Override
	protected int checkReceiving(Message m, DTNHost from) {
		int recvCheck = super.checkReceiving(m, from);

		if (recvCheck == RCV_OK) {
			/* don't accept a message that has already traversed this node */
			if (m.getHops().contains(getHost())) {
				recvCheck = DENIED_OLD;
			}
		}

		return DENIED_UNSPECIFIED;
	}

	@Override
	public void update() {
		if (isTransferring() || !canStartTransfer()) {
			return;
		}

		if (exchangeDeliverableMessages() != null) {
			return;
		}

		tryAllMessagesToAllConnections();
	}

	@Override
	public MessageRouter replicate() {
		// TODO Auto-generated method stub
		return new DistributedSetCoverRouter(this);
	}

}