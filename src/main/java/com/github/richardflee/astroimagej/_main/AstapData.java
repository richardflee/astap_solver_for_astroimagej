package com.github.richardflee.astroimagej._main;

/**
 * Data object encapsulates progress of astap plate solve processing
 *
 */

public class AstapData {
	private String filename;
	private int passCount;
	private int failCount;

	AstapData(String filename, int passCount, int failCount) {
		this.filename = filename;
		this.passCount = passCount;
		this.failCount = failCount;
	}
	
	public String getFilename() {
		return filename;
	}


	public int getPassCount() {
		return passCount;
	}

	public int getFailCount() {
		return failCount;
	}

	@Override
	public String toString() {
		return "AstapData [filename=" + filename + ", passCount=" + passCount + ", failCount=" + failCount + "]";
	}
	
}
