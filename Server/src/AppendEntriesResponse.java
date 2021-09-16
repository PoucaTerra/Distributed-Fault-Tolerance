
import java.io.Serializable;

/**
 * Response from a follower to a append entry rpc
 * @author fc51033; fc51088; fc51101
 *
 */
public class AppendEntriesResponse implements Serializable{
	
	private static final long serialVersionUID = -5213610688633103833L;
	private int term;
	private boolean success;
	
	/**
	 * Response to an append entry request/heartbeat
	 * @param term - term of the sender
	 * @param success - if the append was accepted
	 */
	public AppendEntriesResponse(int term, boolean success) {
		this.term = term;
		this.success = success;
	}

	/**
	 * @return the term
	 */
	public int getTerm() {
		return term;
	}

	/**
	 * @return the success
	 */
	public boolean isSuccess() {
		return success;
	}
}
