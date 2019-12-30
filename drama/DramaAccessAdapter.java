package de.uni_koeln.spinfo.tesla.component.reader.drama;

import de.uni_koeln.spinfo.tesla.roles.core.data.ISubSequence;
import de.uni_koeln.spinfo.tesla.roles.core.impl.tunguska.access.SequenceTunguskaAccessAdapter;

public class DramaAccessAdapter extends SequenceTunguskaAccessAdapter<ISubSequence> implements IDramaAccessAdapter<IDrama>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8772681150515068972L;

	public DramaAccessAdapter() {
		super(ISubSequence.class);
	}
	
	public DramaAccessAdapter(Class<ISubSequence> clazz) {
		super(clazz);
	}
	
}
