package model;

import utils.Strings;

public class Neighbor {

	private String name;
	private double[] expertise;
	private double[] sociability;

	public Neighbor(String name, double[] expertise, double[] sociability) {
		this.name = name;
		this.expertise = expertise;
		if (sociability == null || sociability.length == 0) {
			// initialize the sociability to 0.5,0.5,0.5,0.5
			this.sociability = new double[] { 0.5, 0.5, 0.5, 0.5 };
		}
		this.sociability = sociability;
	}

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
	 * @return the sociability
	 */
	public double[] getSociability() {
		return sociability;
	}

	/**
	 * @param sociability
	 *            the sociability to set
	 */
	public void setSociability(double[] sociability) {
		this.sociability = sociability;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Neighbors [name=" + name + ", expertise=" + Strings.arrayToString(expertise) + ", sociability="
				+ Strings.arrayToString(sociability) + "]";
	}
}
