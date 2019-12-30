package de.uni_koeln.spinfo.tesla.component.reader.drama;

import de.uni_koeln.spinfo.tesla.roles.core.impl.hibernate.data.Token;

public class TEIDramaSpeaker extends Token implements ISpeaker {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2535082874550724916L;
	private String name;

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getLabel() {
		return this.getName();
	}
}