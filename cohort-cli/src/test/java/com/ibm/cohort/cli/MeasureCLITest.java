/*
 * (C) Copyright IBM Corp. 2020, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.cohort.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Test;
import org.opencds.cqf.common.evaluation.MeasurePopulationType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.cohort.engine.measure.BaseMeasureTest;

public class MeasureCLITest extends BaseMeasureTest {
	@Test
	public void testCohortMeasureSinglePatient() throws Exception {
		mockFhirResourceRetrieval("/metadata", getCapabilityStatement());
		
		Patient patient = getPatient("123", AdministrativeGender.MALE, "1592-14-03");
		mockFhirResourceRetrieval(patient);
		
		Library library = mockLibraryRetrieval("Test", "cql/basic/test.cql");
		
		Measure measure = getCohortMeasure("Test", library, "Female");
		mockFhirResourceRetrieval(measure);
		
		File tmpFile = new File("target/fhir-stub.json");
		ObjectMapper om = new ObjectMapper();
		try (Writer w = new FileWriter(tmpFile)) {
			w.write(om.writeValueAsString(getFhirServerConfig()));
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		try {
			MeasureCLI cli = new MeasureCLI();
			cli.runWithArgs(new String[] {
					"-d", tmpFile.getAbsolutePath(),
					"-r", measure.getId(),
					"-c", patient.getId() 
			}, out);	
		} finally {
			tmpFile.delete();
		}
		
		String output = new String(baos.toByteArray());
		String[] lines = output.split(System.getProperty("line.separator"));
		assertEquals( output, 3, lines.length );
		assertTrue( lines[1].endsWith("= 0") );
	}
	
	@Test
	public void testProportionRatioSinglePatient() throws Exception {
		mockFhirResourceRetrieval("/metadata", getCapabilityStatement());
		
		Patient patient = new Patient();
		patient.setId("123");
		patient.setGender(AdministrativeGender.MALE);
		
		OffsetDateTime birthDate = OffsetDateTime.now().minusYears(30);
		patient.setBirthDate(Date.from(birthDate.toInstant()));
		
		mockFhirResourceRetrieval(patient);
		
		Library library = mockLibraryRetrieval("Test", "cql/basic/test.cql");
		
		expressionsByPopulationType.clear();
		expressionsByPopulationType.put(MeasurePopulationType.INITIALPOPULATION, "Male");
		expressionsByPopulationType.put(MeasurePopulationType.DENOMINATOR, "Male");
		expressionsByPopulationType.put(MeasurePopulationType.NUMERATOR, "Over the hill");
		
		expectationsByPopulationType.clear();
		expectationsByPopulationType.put(MeasurePopulationType.INITIALPOPULATION, 1);
		expectationsByPopulationType.put(MeasurePopulationType.DENOMINATOR, 1);
		expectationsByPopulationType.put(MeasurePopulationType.NUMERATOR, 0);
		
		Measure measure = getProportionMeasure("Test", library, expressionsByPopulationType);
		mockFhirResourceRetrieval(measure);
		
		File tmpFile = new File("target/fhir-stub.json");
		ObjectMapper om = new ObjectMapper();
		try (Writer w = new FileWriter(tmpFile)) {
			w.write(om.writeValueAsString(getFhirServerConfig()));
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		try {
			MeasureCLI cli = new MeasureCLI();
			cli.runWithArgs(new String[] {
					"-d", tmpFile.getAbsolutePath(),
					"-r", measure.getId(),
					"-c", patient.getId() 
			}, out);	
		} finally {
			tmpFile.delete();
		}
		
		String output = new String(baos.toByteArray());
		System.out.println(output);
		
		String[] lines = output.split(System.getProperty("line.separator"));
		assertEquals( output, 5, lines.length );
		assertTextPopulationExpectations(lines);
	}
	
	@Test
	public void testProportionRatioMultiplePatients() throws Exception {
		mockFhirResourceRetrieval("/metadata", getCapabilityStatement());
		
		Patient patient1 = mockPatientRetrieval("123", AdministrativeGender.MALE, 30);
		Patient patient2 = mockPatientRetrieval("456", AdministrativeGender.MALE, 45);
		Patient patient3 = mockPatientRetrieval("789", AdministrativeGender.FEMALE, 45);
		
		Library library = mockLibraryRetrieval("Test", "cql/basic/test.cql");
		
		expressionsByPopulationType.clear();
		expressionsByPopulationType.put(MeasurePopulationType.INITIALPOPULATION, "Male");
		expressionsByPopulationType.put(MeasurePopulationType.DENOMINATOR, "Male");
		expressionsByPopulationType.put(MeasurePopulationType.NUMERATOR, "Over the hill");
		
		expectationsByPopulationType.clear();
		expectationsByPopulationType.put(MeasurePopulationType.INITIALPOPULATION, 1);
		expectationsByPopulationType.put(MeasurePopulationType.DENOMINATOR, 1);
		expectationsByPopulationType.put(MeasurePopulationType.NUMERATOR, 0);
		
		Measure measure = getProportionMeasure("Test", library, expressionsByPopulationType);
		mockFhirResourceRetrieval(measure);
		
		File tmpFile = new File("target/fhir-stub.json");
		ObjectMapper om = new ObjectMapper();
		try (Writer w = new FileWriter(tmpFile)) {
			w.write(om.writeValueAsString(getFhirServerConfig()));
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		try {
			MeasureCLI cli = new MeasureCLI();
			cli.runWithArgs(new String[] {
					"-d", tmpFile.getAbsolutePath(),
					"-r", measure.getId(),
					"-c", patient1.getId(),
					"-c", patient2.getId(),
					"-c", patient3.getId()
			}, out);	
		} finally {
			tmpFile.delete();
		}
		
		String output = new String(baos.toByteArray());
		System.out.println(output);
		
		String[] lines = output.split(System.getProperty("line.separator"));
		assertEquals( output, 15, lines.length );
	}
	
	@Test
	public void testCareGapSinglePatient() throws Exception {
		mockFhirResourceRetrieval("/metadata", getCapabilityStatement());
		
		Patient patient1 = mockPatientRetrieval("123", AdministrativeGender.MALE, 30);
		
		Library library = mockLibraryRetrieval("Test", "cql/basic/test.cql");
		
		expressionsByPopulationType.clear();
		expressionsByPopulationType.put(MeasurePopulationType.INITIALPOPULATION, "Male");
		expressionsByPopulationType.put(MeasurePopulationType.DENOMINATOR, "Male");
		expressionsByPopulationType.put(MeasurePopulationType.NUMERATOR, "Over the hill");
		
		expectationsByPopulationType.clear();
		expectationsByPopulationType.put(MeasurePopulationType.INITIALPOPULATION, 1);
		expectationsByPopulationType.put(MeasurePopulationType.DENOMINATOR, 1);
		expectationsByPopulationType.put(MeasurePopulationType.NUMERATOR, 0);
		
		Measure measure = getCareGapMeasure("Test", library, expressionsByPopulationType, "Over the hill");
		mockFhirResourceRetrieval(measure);
		
		File tmpFile = new File("target/fhir-stub.json");
		ObjectMapper om = new ObjectMapper();
		try (Writer w = new FileWriter(tmpFile)) {
			w.write(om.writeValueAsString(getFhirServerConfig()));
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		try {
			MeasureCLI cli = new MeasureCLI();
			cli.runWithArgs(new String[] {
					"-d", tmpFile.getAbsolutePath(),
					"-r", measure.getId(),
					"-c", patient1.getId()
			}, out);	
		} finally {
			tmpFile.delete();
		}
		
		String output = new String(baos.toByteArray());
		System.out.println(output);
		
		String[] lines = output.split(System.getProperty("line.separator"));
		assertEquals( output, 6, lines.length );
	}
	
	@Test
	public void testJsonFormattedOutput() throws Exception {
		mockFhirResourceRetrieval("/metadata", getCapabilityStatement());
		
		Patient patient = getPatient("123", AdministrativeGender.MALE, "1592-14-03");
		mockFhirResourceRetrieval(patient);
		
		Library library = mockLibraryRetrieval("Test", "cql/basic/test.cql");
		
		Measure measure = getCohortMeasure("Test", library, "Female");
		mockFhirResourceRetrieval(measure);
		
		File tmpFile = new File("target/fhir-stub.json");
		ObjectMapper om = new ObjectMapper();
		try (Writer w = new FileWriter(tmpFile)) {
			w.write(om.writeValueAsString(getFhirServerConfig()));
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		try {
			MeasureCLI cli = new MeasureCLI();
			cli.runWithArgs(new String[] {
					"-d", tmpFile.getAbsolutePath(),
					"-r", measure.getId(),
					"-c", patient.getId(),
					"-f", "JSON"
			}, out);	
		} finally {
			tmpFile.delete();
		}
		
		String output = new String(baos.toByteArray());
		System.out.println(output);
		assertTrue( output.contains("\"resourceType\": \"MeasureReport\"") );
	}	
	
	protected void assertTextPopulationExpectations(String[] lines) {
		Pattern p = Pattern.compile("Population: (?<code>[^ ]+) = (?<count>[0-9]+)");
		for( String line : lines ) {
			Matcher m = p.matcher(line);
			if( m.matches() ) {
				MeasurePopulationType type = MeasurePopulationType.fromCode( m.group("code") );
				Integer actualCount = Integer.parseInt( m.group("count") );
				assertEquals( type.toCode(), expectationsByPopulationType.get(type), actualCount );
			}
		}
	}
}
