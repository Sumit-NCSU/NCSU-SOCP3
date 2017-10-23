package utils;

import org.slf4j.MDC;

import com.fasterxml.jackson.databind.node.ObjectNode;

import actors.PersonActor;
import play.libs.Json;
import plugin.Drools;

public class Strings {

	public static final String GENERATE = "generate";
	public static final String BLANK = "";
	public static final String SPACE = " ";
	public static final String SUCCESS = "success";
	public static final String ERROR = "error";
	public static final String MESSAGE = "message";
	public static final String STATUS = "status";
	public static final String SEND = "SEND";
	public static final String RECV = "RECV";
	public static final String DEFAULT = "default";

	public static ObjectNode getJson(PersonActor person) {
		try {
			ObjectNode result = Json.newObject();
			result.put("status", "success");
			if (person.neighbors == null || person.neighbors.isEmpty()) {
				result.set("neighbors", play.libs.Json.toJson(new double[0]));
			} else {
				result.set("neighbors", play.libs.Json.toJson(person.neighbors));
			}
			if (person.acquaintances == null || person.acquaintances.isEmpty()) {
				result.set("acquaintances", play.libs.Json.toJson(new double[0]));
			} else {
				result.set("acquaintances", play.libs.Json.toJson(person.acquaintances));
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			play.Logger.info("Error while creating state dump for person: " + person.name);
			return null;
		}
	}

	public static String arrayToString(double[] d) {
		StringBuilder sb = new StringBuilder();
		for (double i : d) {
			sb.append(", ").append(i);
		}
		return sb.toString().replaceFirst(", ", BLANK);
	}

	public static double weightedSum(double weight, double[] query, double[] expertise, double[] sociability) {
		return weight * innerProduct(query, sociability) + (1 - weight) * innerProduct(query, expertise);
	}

	public static double innerProduct(double[] d1, double[] d2) {
		double ipSum = 0;
		if (d1 != null && d2 != null && d1.length > 0 && d2.length > 0 && d1.length == d2.length) {
			for (int i = 0; i < d1.length; i++) {
				ipSum += d1[i] * d2[i];
			}
		}
		return ipSum;
	}

	public static void doLog(Drools drools, String actorName, String messageType, Object object) {
		MDC.put("sourceActorSystem", actorName);
		MDC.put("type", messageType);
		drools.kieSession.insert(object);
		drools.kieSession.fireAllRules();
		MDC.clear();
	}

}