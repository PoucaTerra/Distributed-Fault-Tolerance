/**
 * Answer to InstallSnapshotRequest
 * @author fc51033; fc51088; fc51101
 */
public class InstallSnapshotResponse {
	
	private int term;
	
	/**
	 * Constructor for the answer
	 * @param term - for the leader to update himself
	 */
	public InstallSnapshotResponse(int term) {
		this.term = term;
	}

	/**
	 * @return the term
	 */
	public int getTerm() {
		return term;
	}
	
	
}
