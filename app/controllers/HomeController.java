package controllers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import actors.PersonActor;
import actors.PersonActorProtocol.DumpState;
import actors.PersonActorProtocol.GetNeeds;
import actors.PersonActorProtocol.Query;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.pattern.Patterns;
import akka.util.Timeout;
import model.Neighbor;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import plugin.Drools;
import referral_helper.QueryGenerator;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Await;
import scala.concurrent.Future;
import utils.Strings;

/**
 * This controller contains an action to handle HTTP requests to the
 * application's home page.
 */
@Singleton
public class HomeController extends Controller {

	final ActorSystem system;
	final Drools drools;
	public static Map<String, ActorRef> actorNameMap = new HashMap<String, ActorRef>();

	@Inject
	public HomeController(ActorSystem system, Drools drools) {
		this.system = system;
		this.drools = drools;
	}

	/**
	 * An action that renders an HTML page with a welcome message. The configuration
	 * in the <code>routes</code> file means that this method will be called when
	 * the application receives a <code>GET</code> request with a path of
	 * <code>/</code>.
	 */
	public Result index() {
		return ok(views.html.index.render());
	}

	/**
	 * This request results the server to load the input graph, which is represented
	 * as a JSON string in the body of the request. Sample input graph present in
	 * inputGraph.json
	 * 
	 * @param graph
	 * @return
	 */
	public Result loadGraph() {
		play.Logger.info("Load Graph called");
		try {
			JsonNode json = request().body().asJson();

			play.Logger.info("Input recieved: " + json);
			if (json != null) {
				for (JsonNode item : json) {
					actorNameMap.put(item.get("name").asText(), processItem(item));
				}
			} else {
				return ok(createErrorResponse("Error Input Graph Json is null! Please Check input."));
			}

			// Do 25 Queries per actor
			runTestQueries();

			return ok(createSuccessResponse(null, null));
		} catch (Exception e) {
			e.printStackTrace();
			return ok(createErrorResponse("Error while Loading the input Graph! Check application Logs."));
		}
	}

	/**
	 * The &lt;actor&gt; in the request represents the actor name, the xx represents
	 * a value between 0 and 1. When receiving this request, the corresponding actor
	 * will try to answer the specified query, either by itself or its neighbors. If
	 * no answer is found, the server replies an error message
	 * 
	 * @param actor
	 * @param value
	 * @return
	 */
	public Result queryActor(String actor, String value) {
		play.Logger.info("Query Actor called for actor: " + actor + ", the query is for: " + value);
		String result = "";
		return ok(result);
	}

	/**
	 * This request dumps the lists of neighbors and acquaintances of an actor.
	 * 
	 * @param actor
	 * 
	 * @return
	 */
	public CompletionStage<Result> dumpStates(String actor) {
		play.Logger.info("Dump States called for actor: " + actor);
		ActorRef actorRef = actorNameMap.get(actor);
		if (actorRef == null) {
			return CompletableFuture
					.completedFuture(ok(createErrorResponse("Actor with name: " + actor + " not found!")));
		} else {
			return FutureConverters.toJava(Patterns.ask(actorRef, new DumpState(), 1000))
					.thenApply(response -> ok((ObjectNode) response));
		}
	}

	/**
	 * This request is used to show all messages have been transferred between
	 * actors. The messages are logged using Drools
	 * 
	 * @return
	 */
	public Result messages() {
		play.Logger.info("Messages called");
		try {
			String content = Files.asCharSource(new File("logs/application.log"), Charsets.UTF_8).read();
			play.Logger.info("Log File read. Returning the logs to the user as a String.");
			return ok(content);
		} catch (IOException e) {
			e.printStackTrace();
			return ok(createErrorResponse("Error while reading the logs file!"));
		}
	}

	/**
	 * debug end point for resetting the graph.
	 * 
	 * @return
	 */
	public Result reset() {
		play.Logger.info("Reset Called");
		try {
			PoisonPill killMessage = PoisonPill.getInstance();
			for (Entry<String, ActorRef> entry : actorNameMap.entrySet()) {
				ActorRef actor = entry.getValue();
				actor.tell(killMessage, ActorRef.noSender());
			}
			actorNameMap.clear();
			drools.kieSession.insert(killMessage);
			drools.kieSession.fireAllRules();
			play.Logger.info("Reset Complete!");
			return ok(createSuccessResponse(null, null));
		} catch (Exception e) {
			return ok(createErrorResponse("Error unable to reset the graph."));
		}
	}

	/**
	 * This method processes an item from the Input Graph Json passed to the
	 * loadGraph() method.
	 * 
	 * @param item
	 *            the Json item from the JsonArray.
	 * @return the actorRef of the actor created from the given Json Item
	 */
	private ActorRef processItem(JsonNode item) {
		play.Logger.info("Processing Item: " + item);
		String name = item.get("name").asText();
		double[] expertise = new double[0];
		JsonNode expertiseArray = item.get("expertise");
		int i = 0;
		if (expertiseArray != null) {
			expertise = new double[4];
			for (JsonNode arrayValue : expertiseArray) {
				expertise[i++] = arrayValue.asDouble();
			}
		}
		double[] needs = new double[0];
		JsonNode needsArray = item.get("needs");
		i = 0;
		if (needsArray != null) {
			needs = new double[4];
			for (JsonNode arrayValue : needsArray) {
				needs[i++] = arrayValue.asDouble();
			}
		}
		List<Neighbor> neighbors = new ArrayList<Neighbor>();
		JsonNode neighborArray = item.get("neighbors");
		if (neighborArray != null) {
			for (JsonNode value : neighborArray) {
				String neighborName = value.get("name").asText();
				double[] neighborExpertise = new double[0];
				JsonNode neighborExpertiseArray = item.get("expertise");
				int j = 0;
				if (neighborExpertiseArray != null) {
					neighborExpertise = new double[4];
					for (JsonNode arrayValue : neighborExpertiseArray) {
						neighborExpertise[j++] = arrayValue.asDouble();
					}
				}
				double[] neighborSociability = new double[0];
				JsonNode neighborSociabilityArray = item.get("sociability");
				j = 0;
				if (neighborSociabilityArray != null) {
					neighborSociability = new double[4];
					for (JsonNode arrayValue : neighborSociabilityArray) {
						neighborSociability[j++] = arrayValue.asDouble();
					}
				}
				neighbors.add(new Neighbor(neighborName, neighborExpertise, neighborSociability));
			}
		}
		ActorRef actor = system.actorOf(PersonActor.getProps(name, expertise, needs, neighbors, drools));
		play.Logger.info("Created Actor: " + name);
		return actor;
	}

	/**
	 * This method is used to run test queries on the network that was just created
	 * using the loadGraph() method
	 */
	private void runTestQueries() {
		play.Logger.info("Running Test queries.");
		List<double[]> actorsNeeds = new LinkedList<double[]>();
		for (Map.Entry<String, ActorRef> entry : actorNameMap.entrySet()) {
			play.Logger.info("Finding needs for: " + entry.getKey());
			ActorRef actor = entry.getValue();
			try {
				final Timeout timeout = new Timeout(2, TimeUnit.SECONDS);
				final Future<Object> future = Patterns.ask(actor, new GetNeeds(), timeout);
				final double[] need = (double[]) Await.result(future, timeout.duration());
				sendQuery(entry, need);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method sends the query message to the corresponding actorRef stored in
	 * the Map.Entry which is passed as an argument to this function.
	 * 
	 * @param entry
	 *            the Map.Entry containing the Actor name and ActorRef.
	 * @param need
	 *            the need array of the actor.
	 */
	private void sendQuery(Entry<String, ActorRef> entry, double[] need) {
		QueryGenerator qg = QueryGenerator.getInstance();
		play.Logger.info("Generating Queries for: " + entry.getKey());
		for (int i = 0; i < 25; i++) {
			double[] query = qg.genQuery(entry.getKey(), need);
			play.Logger.info("Query#" + (i + 1) + ": " + Strings.arrayToString(query));
			FutureConverters.toJava(Patterns.ask(entry.getValue(), new Query(query), 1000))
					.thenApply(response -> (String) response);
		}
	}

	/**
	 * This method returns the following JSON response:
	 * 
	 * <pre>
	 * {
	 *   "status":"success",
	 *   "&lt;key&gt;":"&lt;message&gt;"
	 * </pre>
	 * 
	 * @param key
	 *            the key
	 * @param message
	 *            the message
	 * @return
	 */
	private ObjectNode createSuccessResponse(String key, Object message) {
		ObjectNode result = Json.newObject();
		result.put(Strings.STATUS, Strings.SUCCESS);
		if (key != null && message != null) {
			result.put(key, (String) message);
		}
		return result;
	}

	/**
	 * This method returns the following JSON response:
	 * 
	 * <pre>
	 * {
	 *   "status":"error",
	 *   "message":"&lt;message&gt;"
	 * </pre>
	 * 
	 * @param message
	 *            the message
	 * @return
	 */
	private ObjectNode createErrorResponse(String message) {
		ObjectNode result = Json.newObject();
		result.put(Strings.STATUS, Strings.ERROR);
		result.put(Strings.MESSAGE, message);
		return result;
	}

	/**
	 * @return the actorNameMap
	 */
	public Map<String, ActorRef> getActorNameMap() {
		return actorNameMap;
	}

}
