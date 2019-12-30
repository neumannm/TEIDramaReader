package de.uni_koeln.spinfo.tesla.component.reader.drama;

import de.uni_koeln.spinfo.tesla.roles.core.impl.hibernate.data.Token;

public class TEIDramaAct extends Token implements IAct {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1882514072524835484L;

	private int number;

	public void setNumber(int number) {
		this.number = number;
	}

	@Override
	public int getNumber() {
		return number;
	}
}