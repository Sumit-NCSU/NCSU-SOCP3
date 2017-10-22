package model;

import utils.Strings;

/**
 * @author srivassumit
 *
 */
public class InputGraphModel {

	private String name;
	private double[] expertise;
	private double[] needs;
	private Neighbor[] neighbors;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the expertise
	 */
	public double[] getExpertise() {
		return expertise;
	}

	/**
	 * @param expertise
	 *            the expertise to set
	 */
	public void setExpertise(double[] expertise) {
		this.expertise = expertise;
	}

	/**
	 * @return the needs
	 */
	public double[] getNeeds() {
		return needs;
	}

	/**
	 * @param needs
	 *            the needs to set
	 */
	public void setNeeds(double[] needs) {
		this.needs = needs;
	}

	/**
	 * @return the neighbors
	 */
	public Neighbor[] getNeighbors() {
		return neighbors;
	}

	/**
	 * @param neighbors
	 *            the neighbors to set
	 */
	public void setNeighbors(Neighbor[] neighbors) {
		this.neighbors = neighbors;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "InputGraphModel [name=" + name + ", expertise=" + Strings.arrayToString(expertise) + ", needs="
				+ Strings.arrayToString(needs) + ", neighbors=" + neighbors + "]";
	}

}
