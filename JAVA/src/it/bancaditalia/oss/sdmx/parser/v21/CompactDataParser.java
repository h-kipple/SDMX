/* Copyright 2010,2014 Bank Of Italy
*
* Licensed under the EUPL, Version 1.1 or - as soon they
* will be approved by the European Commission - subsequent
* versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the
* Licence.
* You may obtain a copy of the Licence at:
*
*
* http://ec.europa.eu/idabc/eupl
*
* Unless required by applicable law or agreed to in
* writing, software distributed under the Licence is
* distributed on an "AS IS" basis,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied.
* See the Licence for the specific language governing
* permissions and limitations under the Licence.
*/
/**
 * 
 */
package it.bancaditalia.oss.sdmx.parser.v21;

import it.bancaditalia.oss.sdmx.api.DataFlowStructure;
import it.bancaditalia.oss.sdmx.api.PortableTimeSeries;
import it.bancaditalia.oss.sdmx.util.Configuration;
import it.bancaditalia.oss.sdmx.util.LocalizedText;
import it.bancaditalia.oss.sdmx.util.SdmxException;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * @author Attilio Mattiocco
 *
 */
public class CompactDataParser {
	private static final String sourceClass = CompactDataParser.class.getSimpleName();
	protected static Logger logger = Configuration.getSdmxLogger();
	
	private static final String MESSAGE = "Message";
	private static final String CODE = "code";
	private static final String SEVERITY = "severity";
	
	private static final String DATASET = "DataSet";
	private static final String ACTION = "action";
	private static final String VALID_FROM = "validFromDate";
	private static final String VALID_TO = "validToDate";
	private static final String SERIES = "Series";
	private static final String OBS = "Obs";

	public static List<PortableTimeSeries> parse(InputStreamReader xmlBuffer, DataFlowStructure dsd, String dataflow, boolean data) throws XMLStreamException, UnsupportedEncodingException, SdmxException {
		final String sourceMethod = "parse";
		logger.entering(sourceClass, sourceMethod);
		
		List<PortableTimeSeries> tsList = new ArrayList<PortableTimeSeries>();
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		//InputStream in = new ByteArrayInputStream(xmlBuffer);
		XMLEventReader eventReader = inputFactory.createXMLEventReader(xmlBuffer);

		PortableTimeSeries ts = null;
		String currentAction = null;
		String currentValidFromDate = null;
		String currentValidToDate = null;

		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());
			
			if (event.isStartElement()) {
				StartElement startElement = event.asStartElement();

				if (startElement.getName().getLocalPart() == (DATASET)) {
					logger.finer("Got new dataset");
					@SuppressWarnings("unchecked")
					Iterator<Attribute> attributes = startElement.getAttributes();
					while (attributes.hasNext()) {
						Attribute attr = attributes.next();
						String id = attr.getName().getLocalPart().toString();
						String value = attr.getValue();
						if(id.equalsIgnoreCase(ACTION)){
							logger.finer("action: " + value);
							currentAction = value;
						}
						else if(id.equalsIgnoreCase(VALID_FROM)){
							logger.finer("VALID_FROM: " + value);
							currentValidFromDate = value;
						}
						else if(id.equalsIgnoreCase(VALID_TO)){
							logger.finer("VALID_TO: " + value);
							currentValidToDate = value;
						}
					}
				}

				if (startElement.getName().getLocalPart() == (MESSAGE)) {
					String errorCode = null;
					String errorSeverity = null;
					LocalizedText errorMsg = new LocalizedText(Configuration.getLang());
					@SuppressWarnings("unchecked")
					Iterator<Attribute> attributes = startElement.getAttributes();
					while (attributes.hasNext()) {
						Attribute attr = attributes.next();
						String id = attr.getName().toString();
						String value = attr.getValue();
						if(id.equalsIgnoreCase(CODE)){
							errorCode = value;
						}
						else if(id.equalsIgnoreCase(SEVERITY)){
							errorSeverity = value;
						}
					}
					eventReader.nextEvent();
					errorMsg.setText(startElement, eventReader);
					throw new SdmxException("SDMX Error received. Code: " + errorCode + "; Severity: " + errorSeverity + ". Message: " + errorMsg.getText());
				}

				if (startElement.getName().getLocalPart() == (SERIES)) {
					logger.finer("Got new time series");
					ts = new PortableTimeSeries();
					ts.setDataflow(dataflow);
					
					@SuppressWarnings("unchecked")
					Iterator<Attribute> attributes = startElement.getAttributes();
					setMetadata(ts, dsd, attributes, currentAction, currentValidFromDate, currentValidToDate);
				}

				if (startElement.getName().getLocalPart().equals(OBS) && data) {
					event = eventReader.nextEvent();
					logger.finest(event.toString());
					@SuppressWarnings("unchecked")
					Iterator<Attribute> attributes = startElement.getAttributes();
					String time = null;
					String obs_val = null;
					Hashtable<String, String> obs_attr = new Hashtable<String, String>();
					while (attributes.hasNext()) {
						Attribute attribute = attributes.next();
						String name = attribute.getName().toString();
						if (name.equals(dsd.getTimeDimension())) {
							time=attribute.getValue();
						}
						else if (name.equals(dsd.getMeasure())) {
							obs_val=attribute.getValue();
						}
						else{
							obs_attr.put(name, attribute.getValue());
						}
					}
					if(time!= null && !time.isEmpty() && obs_val!= null && !obs_val.isEmpty() ){
						ts.addObservation(new Double(obs_val), time, obs_attr);
					}
					else{
						throw new RuntimeException("Error during CompactData Parsing. Invalid Observation Time: " + time + " or value: " + obs_val);
					}
					continue;
				}
			}
		
			if (event.isEndElement()) {
				EndElement endElement = event.asEndElement();
				if (endElement.getName().getLocalPart() == (SERIES)) {
					logger.finer("Adding time series " + ts);
					List<String> dates = ts.getTimeSlots();
					int n = dates.size();
					if(n > 1){
						if(dates.get(n-1).compareToIgnoreCase(dates.get(0)) < 0){
							ts.reverse();
						}
					}
					tsList.add(ts);
				}
			}

		}
		logger.exiting(sourceClass, sourceMethod);
		return tsList;
	}

	private static void setMetadata(PortableTimeSeries ts, DataFlowStructure dsd, Iterator<Attribute> attributes, 
			String action, String validFrom, String validTo) {
		final String sourceMethod = "setMetadata";
		logger.entering(sourceClass, sourceMethod);
		if(action != null){
			ts.addAttribute(ACTION + "=" + action);
		}
		if(validFrom != null){
			ts.addAttribute(VALID_FROM + "=" + validFrom);
		}
		if(validTo != null){
			ts.addAttribute(VALID_TO + "=" + validTo);
		}
		String[] dimensions = new String[dsd.getDimensions().size()];
		while (attributes.hasNext()) {
			Attribute attr = attributes.next();
			String id = attr.getName().toString();
			String value = attr.getValue();
			if(dsd.isDimension(id)){
				dimensions[dsd.getDimensionPosition(id)-1] = id+"="+value;
				if(id.equalsIgnoreCase("FREQ") || id.equalsIgnoreCase("FREQUENCY")){
					ts.setFrequency(value);
				}
			}
			else{
				ts.addAttribute(id+"="+value);
			}
			ts.setDimensions(Arrays.asList(dimensions));
		}
		logger.exiting(sourceClass, sourceMethod);
	}

} 
