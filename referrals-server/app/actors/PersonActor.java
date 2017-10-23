package actors;

import java.util.List;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.node.ObjectNode;

import actors.PersonActorProtocol.Answer;
import actors.PersonActorProtocol.DumpState;
import actors.PersonActorProtocol.GetNeeds;
import actors.PersonActorProtocol.Query;
import actors.PersonActorProtocol.Referral;
import actors.PersonActorProtocol.Refusal;
import akka.actor.AbstractActor;
import akka.actor.AbstractActor.Receive;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import controllers.HomeController;
import model.Neighbor;
import play.libs.Json;
import plugin.Drools;
import referral_helper.Utils;
import scala.concurrent.Await;
import scala.concurrent.Future;
import utils.Strings;

/**
 * @author srivassumit
 *
 */
public class PersonActor extends AbstractActor {

	/**
	 * 
	 * Logging help Refer:
	 * <a>https://doc.akka.io/docs/akka/current/java/logging.html</a>
	 */
	final Drools drools;

	public String name;
	public double[] expertise;
	public double[] needs;
	public List<Neighbor> neighbors;
	public List<Neighbor> acquaintances;

	private Stack<Neighbor> referralChain;

	public PersonActor(String name, double[] expertise, double needs[], List<Neighbor> neighbors, Drools drools) {
		this.name = name;
		this.expertise = expertise;
		this.needs = needs;
		this.neighbors = neighbors;
		this.acquaintances = neighbors;
		this.drools = drools;
		this.referralChain = new Stack<Neighbor>();
	}

	public static Props getProps(String name, double[] expertise, double needs[], List<Neighbor> neighbors,
			Drools drools) {
		return Props.create(PersonActor.class, () -> new PersonActor(name, expertise, needs, neighbors, drools));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(Query.class, query -> {
			Strings.doLog(drools, name, Strings.RECV, query);
			// Do Query Message processing
			play.Logger.info("Processing Query ");
			if (Utils.isExpertiseMatch(expertise, query.query)) {
				// If incoming query matches own expertise then generate an Answer from the
				// expertise and send it to the querying actor.
				play.Logger.info("Actor " + name + "'s expertise: " + Strings.arrayToString(expertise)
						+ " matched with query: " + Strings.arrayToString(query.query) + ". Returning Answer.");
				double[] answer = Utils.genAnswer(expertise, query.query);
				Strings.doLog(drools, name, Strings.SEND, new Answer());
				sender().tell(new Answer(answer, query.query, referralChain), self());
			} else {
				Neighbor bestNeighbor = getBestMatchingNeighbor(query.query);
				if (bestNeighbor != null) {
					// query matched soc. or exp. of neighbor. => provide referral
					referralChain.add(bestNeighbor);
					if (isDefault()) {
						// default actor would call the referrals.
						ActorRef referredActor = HomeController.actorNameMap.get(bestNeighbor.getName());
						try {
							final Timeout timeout = new Timeout(2, TimeUnit.SECONDS);
							final Future<Object> future = Patterns.ask(referredActor, new Query(query.query), timeout);
							final Object response = Await.result(future, timeout.duration());// can be answer/refusal
							sender().tell(response, self());
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						// other actors would return the referrals
						Strings.doLog(drools, name, Strings.SEND, new Referral());
						sender().tell(new Referral(bestNeighbor.getName(), query.query), self());
					}
				} else {
					// if best neighbor also not found, reply a refusal.
					Strings.doLog(drools, name, Strings.SEND, new Refusal());
					sender().tell(new Refusal(), self());
				}
			}
		}).match(DumpState.class, dumpState -> {
			Strings.doLog(drools, name, Strings.RECV, dumpState);
			ObjectNode jsonResponse = Strings.getJson(this);
			if (jsonResponse == null) {
				jsonResponse = Json.newObject();
				jsonResponse.put("status", "error");
				jsonResponse.put("message", "Error while creating State Dump for Person: " + name);
			}
			sender().tell(jsonResponse, self());
		}).match(Referral.class, referral -> {
			Strings.doLog(drools, name, Strings.RECV, referral);
			if (isDefault()) {
				ActorRef referredActor = HomeController.actorNameMap.get(referral.referral);
				try {
					final Timeout timeout = new Timeout(2, TimeUnit.SECONDS);
					final Future<Object> future = Patterns.ask(referredActor, new Query(referral.query), timeout);
					final Object response = Await.result(future, timeout.duration());// can be answer/refusal
					sender().tell(response, self());
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				Strings.doLog(drools, name, Strings.SEND, new Referral());
				sender().tell(referral, self());
			}
		}).match(Answer.class, answer -> {
			Strings.doLog(drools, name, Strings.RECV, answer);
			updateKnowledge(answer);
			Strings.doLog(drools, name, Strings.SEND, new Answer());
			sender().tell(answer, self());
		}).match(Refusal.class, refusal -> {
			Strings.doLog(drools, name, Strings.RECV, refusal);
			Strings.doLog(drools, name, Strings.SEND, new Refusal());
			sender().tell(refusal, self());
		}).match(GetNeeds.class, getNeeds -> {
			Strings.doLog(drools, name, Strings.RECV, getNeeds);
			sender().tell(needs, self());
		}).build();
	}

	private void updateKnowledge(Answer answer) {
		if (referralChain != null && referralChain.size() > 0) {
			// add all actors from refChain to acquaintance, which are not already there
			for (Neighbor neighbor : referralChain) {
				if (!acquaintances.contains(neighbor)) {
					acquaintances.add(neighbor);
				}
			}
			// update expertise of actor at end of the referral chain.
			Neighbor lastNeighbor = referralChain.pop();
			Utils.updateExpertise(answer.query, answer.answer, lastNeighbor.getExpertise());
			for (Neighbor acquaintance : acquaintances) {
				if (acquaintance.getName().equals(lastNeighbor.getName())) {
					acquaintance = lastNeighbor;
				}
			}
			// update sociability of all non-terminal actors in referral chain.
			if (referralChain.size() > 0) {// if there are and actors left in referral chain
				for (Neighbor neighbor : referralChain) {
					if (!neighbor.getName().equals(lastNeighbor.getName()) && !neighbor.getName().equals(name)) {
						int distanceToTail = referralChain.size() - referralChain.indexOf(neighbor) - 1;
						Utils.updateSociability(answer.query, answer.answer, distanceToTail, neighbor.getSociability());
						for (Neighbor acquaintance : acquaintances) {
							if (acquaintance.getName().equals(neighbor.getName())) {
								acquaintance = neighbor;
							}
						}
					}
				}
			}

			// choose new neighbors
			int maxNeighbors = Utils.getMaxNumOfNeighbors();
			double[] defaultQuery = { 1.0, 1.0, 1.0, 1.0 };
			SortedMap<Double, Neighbor> sortedAcquaintances = new TreeMap<Double, Neighbor>();
			for (Neighbor acquaintance : acquaintances) {
				// calculate fitness score
				double fitnessScore = Strings.weightedSum(Utils.getWeightOfSociability(), defaultQuery,
						acquaintance.getExpertise(), acquaintance.getSociability());
				sortedAcquaintances.put(fitnessScore, acquaintance);
			}
			neighbors.clear();
			for (int i=0;i<maxNeighbors;i++) {
				neighbors.add(sortedAcquaintances.get(sortedAcquaintances.lastKey()));
			}
		}
	}

	private Neighbor getBestMatchingNeighbor(double[] query) {
		SortedMap<Double, Neighbor> matchedNeighbors = new TreeMap<Double, Neighbor>();
		for (Neighbor neighbor : neighbors) {
			if (Utils.isExpertiseMatch(neighbor.getExpertise(), query)
					|| Utils.isExpertiseMatch(neighbor.getSociability(), query)) {
				double fitnessScore = Strings.weightedSum(Utils.getWeightOfSociability(), query,
						neighbor.getExpertise(), neighbor.getSociability());
				matchedNeighbors.put(fitnessScore, neighbor);
			}
		}
		return matchedNeighbors.size() == 0 ? null : matchedNeighbors.get(matchedNeighbors.lastKey());
	}

	private boolean isDefault() {
		return Strings.DEFAULT.equalsIgnoreCase(name);
	}

}
