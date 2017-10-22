package actors;

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

		public Referral(String referral) {
			this.referral = referral;
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

		public Answer(double[] answer) {
			this.answer = answer;
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

	public static class Refusal {
		public Refusal() {
		}
	}

}
