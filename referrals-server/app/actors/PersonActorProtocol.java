package actors;

import java.util.ArrayList;
import java.util.List;

import model.Neighbor;

/**
 * @author srivassumit
 *
 */
public class PersonActorProtocol {

	/**
	 * Query message for Person Actor - to query the actor
	 * 
	 * @author srivassumit
	 *
	 */
	public static class Query {
		public final double[] query;

		public Query() {
			this.query = new double[0];
		}

		public Query(double[] query) {
			this.query = query.clone();
		}
	}

	/**
	 * DumpState message for Person Actor - to dump the state of the actor
	 * 
	 * @author srivassumit
	 *
	 */
	public static class DumpState {
		public DumpState() {
		}
	}

	/**
	 * Referral message for Person Actor - returned by PersonActor as a referral
	 * 
	 * @author srivassumit
	 *
	 */
	public static class Referral {
		public final String referral;
		public final double[] query;

		public Referral() {
			this.referral = "";
			this.query = new double[0];
		}

		public Referral(String referral, double[] query) {
			this.referral = referral;
			this.query = query;
		}
	}

	/**
	 * Answer message for Person Actor - returned by PersonActor as an answer
	 * 
	 * @author srivassumit
	 *
	 */
	public static class Answer {
		public final double[] answer;
		public final double[] query;
		public final List<Neighbor> referralChain;

		public Answer() {
			this.answer = new double[0];
			this.query = new double[0];
			this.referralChain = new ArrayList<Neighbor>();
		}

		public Answer(double[] answer, double[] query, List<Neighbor> referralChain) {
			this.answer = answer;
			this.query = query;
			this.referralChain = referralChain;
		}
	}

	/**
	 * Refusal message for Person Actor - returned by PersonActor as a refusal
	 * 
	 * @author srivassumit
	 *
	 */
	public static class Refusal {
		public Refusal() {
		}
	}

	/**
	 * GetNeeds message for Person Actor - Upon receiving this message, the person
	 * actor returns its needs array.
	 * 
	 * @author srivassumit
	 *
	 */
	public static class GetNeeds {
		public GetNeeds() {
		}
	}

}
