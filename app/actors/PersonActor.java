package actors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
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
	 * If sourceActorName doesn't work in logback.xml, then try using mdc logging to
	 * put values in the log. E.g.:
	 * 
	 * <pre>
	 * Map<String, Object> mdc;
	 * mdc = new HashMap<String, Object>();
	 * mdc.put("requestId", 1234);
	 * mdc.put("visitorId", 5678);
	 * log.setMDC(mdc);
	 * log.info("Starting new request");
	 * 
	 * log.clearMDC();
	 * </pre>
	 * 
	 * In Logback.xml:
	 * 
	 * <pre>
	 * %-5level %logger{36} [req: %X{requestId}, visitor: %X{visitorId}] - %msg%n
	 * </pre>
	 * 
	 * Refer: <a>https://doc.akka.io/docs/akka/current/java/logging.html</a>
	 */
	final Drools drools;

	public String name;
	public double[] expertise;
	public double[] needs;
	public List<Neighbor> neighbors;
	public List<Neighbor> acquaintances;

	private List<Neighbor> callList;
	private List<Neighbor> referralChain;

	public PersonActor(String name, double[] expertise, double needs[], List<Neighbor> neighbors, Drools drools) {
		this.name = name;
		this.expertise = expertise;
		this.needs = needs;
		this.neighbors = neighbors;
		this.acquaintances = neighbors;
		this.drools = drools;
		this.callList = new ArrayList<Neighbor>();
		this.referralChain = new LinkedList<Neighbor>();
	}

	public static Props getProps(String name, double[] expertise, double needs[], List<Neighbor> neighbors,
			Drools drools) {
		return Props.create(PersonActor.class, () -> new PersonActor(name, expertise, needs, neighbors, drools));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(Query.class, query -> {
			drools.kieSession.insert(query);
			drools.kieSession.fireAllRules();
			Neighbor bestNeighbor = null;
			// Do Query Message processing
			if (Utils.isExpertiseMatch(expertise, query.query)) {
				// If incoming query matches own expertise then generate an Answer from the
				// expertise and send it to the querying actor.
				play.Logger.info("Actor " + name + "'s expertise: " + Strings.arrayToString(expertise)
						+ " matched with query: " + Strings.arrayToString(query.query) + ". Returning Answer.");
				double[] answer = Utils.genAnswer(expertise, query.query);
				sender().tell(new Answer(answer), self());
			} else if ((bestNeighbor = getBestMatchingNeighbor(query.query)) != null) {
				// else if the query matches the sociability or expertise of any neighbors,
				// provide a referral to those neighbors.
				sender().tell(new Referral(bestNeighbor.getName()), self());
			} else {
				// reply a refusal.
				sender().tell(new Refusal(), self());
			}
		}).match(DumpState.class, dumpState -> {
			drools.kieSession.insert(dumpState);
			ObjectNode jsonResponse = Strings.getJson(this);
			if (jsonResponse == null) {
				jsonResponse = Json.newObject();
				jsonResponse.put("status", "error");
				jsonResponse.put("message", "Error while creating State Dump for Person: " + name);
			}
			drools.kieSession.fireAllRules();
			sender().tell(jsonResponse, self());
		}).match(Referral.class, referral -> {
			drools.kieSession.insert(referral);

			// Process Referral message
			ActorRef referredActor = HomeController.actorNameMap.get(referral.referral);
			// Send the query to the referred actor.
			// Should probably check here that the same actor hasn't been asked before or
			// something?
			try {
				final Timeout timeout = new Timeout(2, TimeUnit.SECONDS);
				final Future<Object> future = Patterns.ask(referredActor, new GetNeeds(), timeout);
				final Object response = Await.result(future, timeout.duration());//can be referral/answer/refusal
				//will this response will also be received asynchronously as a Referral/Answer/Refusal message?
			} catch (Exception e) {
				e.printStackTrace();
			}

			drools.kieSession.fireAllRules();
			sender().tell("Success", self());
		}).match(Answer.class, answer -> {
			drools.kieSession.insert(answer);

			// Process Answer message

			drools.kieSession.fireAllRules();
			sender().tell("Success", self());
		}).match(GetNeeds.class, getNeeds -> {
			drools.kieSession.insert(getNeeds);
			drools.kieSession.fireAllRules();
			sender().tell(needs, self());
		}).build();
	}

	private void updateKnowledge() {
		// add all actors from referral chain, which are not already there in
		// acquaintance list.
		
		// update expertise of actor at end of the referral chain.
		
		// update sociability of all non-terminal actors in referral chain.
	}

	private Neighbor getBestMatchingNeighbor(double[] query) {
		SortedMap<Double, Neighbor> matchedNeighbors = new TreeMap<Double, Neighbor>();
		for (Neighbor n : neighbors) {
			if (Utils.isExpertiseMatch(n.getExpertise(), query) || Utils.isExpertiseMatch(n.getSociability(), query)) {
				double fitnessScore = Strings.weightedSum(Utils.getWeightOfSociability(), query, n.getExpertise(),
						n.getSociability());
				matchedNeighbors.put(fitnessScore, n);
			}
		}
		return matchedNeighbors.size() == 0 ? null : matchedNeighbors.get(matchedNeighbors.lastKey());
	}

	@Override
	public boolean equals(Object obj) {
		return this.name.equals(((PersonActor) obj).name);
	}

}
