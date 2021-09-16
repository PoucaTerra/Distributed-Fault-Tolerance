import java.io.Serializable;
import java.util.List;

/**
 * Object the leader sends to a follower in heartbeats/append entries rpc
 * @author fc51033; fc51088; fc51101
 */
public class AppendEntries implements Serializable{

	private static final long serialVersionUID = -5294586410022351512L;

	private int term;
	private int leaderID;
	private int prevLogIndex;
	private int prevLogTerm;
	private List<LogEntry> entry;
	private int leaderCommit;
	private boolean isHeartbeat;

	/**
	 * HEARTBEAT
	 * @param term - term of the sender
	 * @param leaderID - id of the sender aka leader
	 */
	public AppendEntries(int term, int leaderID, int leaderCommit) {
		this.isHeartbeat = true;
		this.term = term;
		this.leaderID = leaderID;
		this.leaderCommit = leaderCommit;
	}


	/**
	 * Append Entry Request to Follower
	 * @param term - term of leader
	 * @param leaderID - id of leader
	 * @param prevLogIndex - index of the lprevious log entry
	 * @param prevLogTerm - term of previous log entry
	 * @param entry - List of entries to send
	 * @param leaderCommitIndex - leaders commit index
	 */
	public AppendEntries(int term, int leaderID, int prevLogIndex, int prevLogTerm, 
			List<LogEntry> entry, int leaderCommitIndex) {

		this.term = term;
		this.leaderID = leaderID;
		this.prevLogIndex = prevLogIndex;
		this.prevLogTerm = prevLogTerm;
		this.entry = entry;
		this.leaderCommit = leaderCommitIndex;
		this.isHeartbeat = false;
	}

	/**
	 * @return the term
	 */
	public int getTerm() {
		return term;
	}

	/**
	 * @return the leaderID
	 */
	public int getLeaderID() {
		return leaderID;
	}

	/**
	 * @return the prevLogIndex
	 */
	public int getPrevLogIndex() {
		return prevLogIndex;
	}

	/**
	 * @return the prevLogTerm
	 */
	public int getPrevLogTerm() {
		return prevLogTerm;
	}

	/**
	 * @return the entry
	 */
	public List<LogEntry> getEntry() {
		return entry;
	}

	/**
	 * @return the leaderCommitIndex
	 */
	public int getLeaderCommitIndex() {
		return leaderCommit;
	}

	/**
	 * @return the isHeartbeat
	 */
	public boolean isHeartbeat() {
		return isHeartbeat;
	}

}
