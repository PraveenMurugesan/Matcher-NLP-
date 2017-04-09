package matcher;

import postagger.VerbType;

/*
 Verbs are classified in to three types as follows 
 Producer Verbs - provide, give, distribute
 Consumer verbs - need, want,demand
 Supplementary - am, will, be
 */

class Verb {

	private String mainVerb;
	private String supplementaryVerb;
	private VerbType verbType;

	public String getMainVerb() {
		return mainVerb;
	}

	public String getSupplementaryVerb() {
		return supplementaryVerb;
	}

	public VerbType getVerbType() {
		return verbType;
	}

	public void setMainVerb(String mainVerb) {
		this.mainVerb = mainVerb;
	}

	public void setSupplementaryVerb(String supplementaryVerb) {
		this.supplementaryVerb = supplementaryVerb;
	}

	public void setVerbType(VerbType verbType) {
		this.verbType = verbType;
	}
}
