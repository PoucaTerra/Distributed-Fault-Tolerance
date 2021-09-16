/**
 * Message from the leader to the followers to send snapshot
 * @author fc51033; fc51088; fc51101
 */
public class InstallSnapshotRequest {
	
	private int term;
	private int leaderId;
	private int lastIncludedIndex;
	private int lastIncludedTerm;	
	//offset
	//data
	private boolean done;
	
	/**
	 * Constructor for a snapshot send request
	 * @param term - termo of leader
	 * @param leaderId - leader id
	 * @param lastIncludedIndex - index of the last entry to be replaced
	 * @param lastIncludedTerm - term of lastIncludedIndex
	 * @param done - true if last chunk
	 */
	public InstallSnapshotRequest(int term, int leaderId, int lastIncludedIndex, int lastIncludedTerm, boolean done) {
		this.term = term;
		this.leaderId = leaderId;
		this.lastIncludedIndex = lastIncludedIndex;
		this.lastIncludedTerm = lastIncludedTerm;
		this.done = done;
	}

	/**
	 * @return the term
	 */
	public int getTerm() {
		return term;
	}

	/**
	 * @return the leaderId
	 */
	public int getLeaderId() {
		return leaderId;
	}

	/**
	 * @return the lastIncludedIndex
	 */
	public int getLastIncludedIndex() {
		return lastIncludedIndex;
	}

	/**
	 * @return the lastIncludedTerm
	 */
	public int getLastIncludedTerm() {
		return lastIncludedTerm;
	}

	/**
	 * @return the done
	 */
	public boolean isDone() {
		return done;
	}
	
}
