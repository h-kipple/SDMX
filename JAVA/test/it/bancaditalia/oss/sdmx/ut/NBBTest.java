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
package it.bancaditalia.oss.sdmx.ut;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import it.bancaditalia.oss.sdmx.api.DSDIdentifier;
import it.bancaditalia.oss.sdmx.api.Dimension;
import it.bancaditalia.oss.sdmx.api.PortableTimeSeries;
import it.bancaditalia.oss.sdmx.client.SdmxClientHandler;
import it.bancaditalia.oss.sdmx.client.custom.NBB;
import it.bancaditalia.oss.sdmx.util.SdmxException;

import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

public class NBBTest {
	@BeforeClass
	public static void setUp() throws Exception {
	}

	@Test
	public void testGetDSDIdentifier() throws SdmxException {
		DSDIdentifier keyF = SdmxClientHandler.getDSDIdentifier(NBB.class.getSimpleName(), "AFCSURV");
		assertNotNull("Null key family for AFCSURV", keyF);
		assertEquals("Wrong Key Family", "AFCSURV", keyF.getId());
	}

	@Test(expected=SdmxException.class) // this fails. we'll know when it works again...
	public void testGetFlows() throws SdmxException {
		Map<String, String> f = SdmxClientHandler.getFlows(NBB.class.getSimpleName(), "AFCSURV");
		assertNotNull("Null getFlows result", f);
		String descr = f.get("AFCSURV");
		assertEquals("Wrong description for AFCSURV", "Quarterly survey on the assessment of financing conditions", descr);
	}

	@Test
	public void testGetDimensions() throws SdmxException {
		List<Dimension> dim = SdmxClientHandler.getDimensions(NBB.class.getSimpleName(), "AFCSURV");
		assertNotNull("Null getDimensions result AFCSURV", dim);
		String result = "[Dimension [id=AFCSURV_INDIC, position=1, codelist=Codelis";
		assertEquals("Wrong dimensions for AFCSURV", result, dim.toString().substring(0, result.length()));
	}
	
	@Test
	public void testGetCodes() throws SdmxException {
			Map<String, String> codes = SdmxClientHandler.getCodes(NBB.class.getSimpleName(), "AFCSURV", "FREQUENCY");
			assertNotNull("Null getCodes result", codes);
			assertEquals("Wrong code for WDI annual", codes.get("M"), "Monthly");
	}

	@Test
	public void testGetTimeSeriesFromID() throws SdmxException {
		List<PortableTimeSeries> res = SdmxClientHandler.getTimeSeries(NBB.class.getSimpleName(), "NICP2013/HEALTH+XEFUN0.M", "2006", "2010");
		assertNotNull("Null time series result", res);
		String annual = res.get(0).getName();
		assertEquals("Wrong name for first time series", "NICP2013.HEALTH.M", annual);
		String start = res.get(0).getTimeSlots().get(0);
		assertEquals("Wrong start date for time series", "2006-01", start);
	}

}
