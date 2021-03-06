/*
 * (C) Copyright IBM Corp. 2020, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.cohort.engine.measure;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Provide a very basic implementation of a measurement period determination
 * strategy. This is used to inject different behaviors into the
 * MeasurementEvaluator and only when the caller does not want to provide the
 * periodStart and periodEnd directly. Callers should plan on customizing this
 * as needed for their use case.
 * 
 * The most simple use is where the period is defined as the current datetime in
 * the system timezone adjusted by by minus one year. Users can customize this
 * behavior by initializing the strategy with an <code>amount</code> and
 * <code>unit</code> where the unit comes from {@link java.util.Calendar}.
 * 
 * Users may provide a fixed date for "now" for cases where they want this
 * strategy to always return the same period across multiple invocations.
 */
public class DefaultMeasurementPeriodStrategy implements MeasurementPeriodStrategy {

	public static final String TARGET_DATE_FORMAT = "yyyy-MM-dd";

	private static final int DEFAULT_AMOUNT = -1;
	private static final int DEFAULT_UNIT = Calendar.YEAR;

	private int unit;
	private int amount;

	private Date now;

	/**
	 * Use a measurement strategy with the default logic.
	 */
	public DefaultMeasurementPeriodStrategy() {
		this(DEFAULT_UNIT, DEFAULT_AMOUNT);
	}

	/**
	 * Use a measurement strategy where the duration of the period is determined by
	 * the user.
	 * 
	 * @param unit   Date part constant from {@link java.util.Calendar}.
	 * @param amount Amount of <code>unit</code> that should be added to current
	 *               system datetime to create the measurement period.
	 */
	public DefaultMeasurementPeriodStrategy(int unit, int amount) {
		this.unit = unit;
		this.amount = amount;
	}

	/**
	 * Allow the user to provide the "now" date as needed.
	 * 
	 * @param date the "now" date for the system timezone.
	 */
	public void setNow(Date date) {
		this.now = date;
	}

	/**
	 * Return the now date for this instance. If the user has not specified a fixed
	 * "now" date, then each time this is invoked the current system time is
	 * returned.
	 * 
	 * @return current system time or the fixed now date if set.
	 */
	public Date getNow() {
		Date date = this.now;
		if (date == null) {
			date = new Date();
		}
		return date;
	}

	@Override
	public Pair<String, String> getMeasurementPeriod() {
		Calendar end = Calendar.getInstance();
		end.setTime(getNow());

		Calendar start = Calendar.getInstance();
		start.setTime(end.getTime());
		start.add(unit, amount);

		if (end.getTime().before(start.getTime())) {
			Calendar tmp = end;
			end = start;
			start = tmp;
		}

		DateFormat sdf = new SimpleDateFormat(TARGET_DATE_FORMAT);
		String periodStart = sdf.format(start.getTime());
		String periodEnd = sdf.format(end.getTime());

		return new ImmutablePair<String, String>(periodStart, periodEnd);
	}
}
