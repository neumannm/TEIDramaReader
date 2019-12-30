package de.uni_koeln.spinfo.tesla.component.reader.drama;

import de.uni_koeln.spinfo.tesla.roles.core.impl.hibernate.data.Token;

public class TEIDramaStage extends Token implements IStage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5824774567538683761L;
	private String text;

	@Override
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	@Override
	public String getLabel() {
		return "StageDirection";
	}
}