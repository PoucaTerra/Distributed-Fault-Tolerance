import java.io.Serializable;

/**
 * Response to a request vote
 * @author fc51033; fc51088; fc51101
 */
public class RequestVoteResponse implements Serializable{

	private static final long serialVersionUID = -2803918966539244715L;
	
	private int term;
	private boolean voteGranted;
	
	/**
	 * Response to request vote
	 * @param term - term of voter
	 * @param voteGranted - true if voted in the server that emmited the request; false otherwise
	 */
	public RequestVoteResponse(int term, boolean voteGranted) {
		this.term = term;
		this.voteGranted = voteGranted;
	}

	/**
	 * @return the term
	 */
	public int getTerm() {
		return term;
	}

	/**
	 * @return the voteGranted
	 */
	public boolean isVoteGranted() {
		return voteGranted;
	}
}
