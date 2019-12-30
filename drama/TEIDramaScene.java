package de.uni_koeln.spinfo.tesla.component.reader.drama;

import de.uni_koeln.spinfo.tesla.roles.core.impl.hibernate.data.Token;

public class TEIDramaScene extends Token implements IScene {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1742914119466994125L;

	private int number;

	public void setNumber(int number) {
		this.number = number;
	}

	@Override
	public int getNumber() {
		return number;
	}
}