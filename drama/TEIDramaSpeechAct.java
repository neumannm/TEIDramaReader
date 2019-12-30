package de.uni_koeln.spinfo.tesla.component.reader.drama;

import de.uni_koeln.spinfo.tesla.roles.core.impl.hibernate.data.Token;

public class TEIDramaSpeechAct extends Token implements ISpeechAct {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5331563026783155525L;
	private String text;

	@Override
	public String getLabel() {
		return "Speech act";
	}

	public void addText(String string) {
		if (this.text == null)
			this.text = new String();
		if (!this.text.isEmpty())
			this.text += " ";
		this.text += string;
	}

	@Override
	public String getText() {
		return this.text;
	}	
}