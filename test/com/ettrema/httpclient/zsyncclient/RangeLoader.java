package com.ettrema.httpclient.zsyncclient;

import java.util.List;

import com.dotcms.repackage.io.milton.http.Range;

/**
 * Used to load selected range data to satisfy the zsync process
 *
 * @author brad
 */
public interface RangeLoader {

	/**
	 * Fetch a set of ranges, usually over HTTP
	 *
	 * @param rangeList
	 * @return
	 * @throws Exception
	 */
	public byte[] get(List<Range> rangeList) throws Exception;

}
