package fi.vm.sade.viestintapalvelu;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFText2HTML;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.odftoolkit.odfdom.converter.core.utils.ByteArrayOutputStream;
import org.w3c.tidy.Configuration;
import org.w3c.tidy.Tidy;

import fi.vm.sade.viestintapalvelu.testdata.Generator;

@RunWith(Enclosed.class)
public class AddressLabelsInPDFFormatTest {
		
	@BeforeClass
	public static void setUp() throws Exception {
		Launcher.start();
	}

	public static class WhenCreatingLabelForValidForeignAddress {

		private static AddressLabel label = new AddressLabel("Åle", "Öistämö", "Brännkyrksgatan 177 B 149", "65330", "Stockholm", "Sweden");
		private static String[] pdf;
		
		@BeforeClass
		public static void setUp() throws Exception {
			pdf = callGenerateLabels(Arrays.asList(label)).get(0);
		}

		@Test
		public void firstNameAndLastNameAreMappedToFirstRow() throws Exception {
			Assert.assertEquals(label.getFirstName() + " " + label.getLastName(), pdf[0]);
		}

		@Test
		public void streetAddressIsMappedToSecondRow() throws Exception {
			Assert.assertEquals(label.getStreetAddress(), pdf[1]);
		}

		@Test
		public void postalCodeAndPostOfficeAreMappedToThirdRow() throws Exception {
			Assert.assertEquals(label.getPostalCode() + " " + label.getPostOffice(), pdf[2]);
		}
		
		@Test
		public void countryIsMappedToFourthRow() throws Exception {
			Assert.assertEquals(label.getCountry(), pdf[3]);
		}

		@Test
		public void labelContainsFourRows() throws Exception {
			Assert.assertEquals(4, pdf.length);
		}
	}

	public static class WhenFirstNameIsEmpty {
		@Test
		public void firstRowContainsLastName() throws Exception {
			Assert.assertEquals("Öistämö", callGenerateLabels("", "Öistämö", "Brännkyrksgatan 177 B 149", "65330", "Stockholm", "Sweden")[0]);
		}
	}

	public static class WhenLastNameIsEmpty {
		@Test
		public void firstRowContainsFirstName() throws Exception {
			Assert.assertEquals("Åle", callGenerateLabels("Åle", "", "Brännkyrksgatan 177 B 149", "65330", "Stockholm", "Sweden")[0]);
		}
	}

	public static class WhenStreetAddressIsEmpty {
		@Test
		public void labelContainsNamePostOfficeAndCountry() throws Exception {
			String[] label = callGenerateLabels("Åle", "Öistämö", "", "65330", "Stockholm", "Sweden");
			Assert.assertEquals("Åle Öistämö", label[0]);
			Assert.assertEquals("65330 Stockholm", label[1]);
			Assert.assertEquals("Sweden", label[2]);
			Assert.assertEquals(3, label.length);
		}
	}

	public static class WhenPostalCodeIsEmpty {
		@Test
		public void thirdRowContainsPostOffice() throws Exception {
			Assert.assertEquals("Stockholm", callGenerateLabels("Åle", "Öistämö", "Brännkyrksgatan 177 B 149", "", "Stockholm", "Sweden")[2]);
		}
	}

	public static class WhenPostOfficeIsEmpty {
		@Test
		public void thirdRowContainsPostalCode() throws Exception {
			Assert.assertEquals("65330", callGenerateLabels("Åle", "Öistämö", "Brännkyrksgatan 177 B 149", "65330", "", "Sweden")[2]);
		}
	}

	public static class WhenCountryIsEmpty {
		@Test
		public void labelHasOnlyThreeRows() throws Exception {
			Assert.assertEquals(3, callGenerateLabels("Åle", "Öistämö", "Brännkyrksgatan 177 B 149", "65330", "Stockholm", "").length);
		}
	}

	public static class WhenAddressIsLocal {
		@Test
		public void labelHasOnlyThreeRows() throws Exception {
			Assert.assertEquals(3, callGenerateLabels("Åle", "Öistämö", "Mannerheimintie 177 B 149", "65330", "Helsinki", "Finland").length);
		}
	}

	public static class WhenAddressIsLocalAndCountryIsUppercaseFINLAND {
		@Test
		public void labelHasOnlyThreeRows() throws Exception {
			Assert.assertEquals(3, callGenerateLabels("Åle", "Öistämö", "Mannerheimintie 177 B 149", "65330", "Helsinki", "FINLAND").length);
		}
	}

	public static class WhenCreatingLabelsForDomesticAndForeignAddresses {

		private static AddressLabel domestic = new AddressLabel("Åle", "Öistämö", "Mannerheimintie 177 B 149", "65330", "Helsinki", "FINLAND");
		private static AddressLabel foreign = new AddressLabel("Åle", "Öistämö", "Brännkyrksgatan 177 B 149", "65330", "Stockholm", "Sweden");
		private static List<String[]> response;

		@BeforeClass
		public static void setUp() throws Exception {
			response = callGenerateLabels(Arrays.asList(domestic, foreign));
		}

		@Test
		public void responseContainsTwoAddressLabels() throws Exception {
			Assert.assertEquals(2, response.size());
		}

		@Test
		public void domesticAddressHasThreeRows() throws Exception {
			Assert.assertEquals(3, response.get(1).length); //Order reversed when parsing pdf to html
		}

		@Test
		public void foreignAddressHasFourRows() throws Exception {
			Assert.assertEquals(4, response.get(0).length); //Order reversed when parsing pdf to html
		}
	}

	public static class WhenCreatingLabelsInABigBatch {

		private static List<AddressLabel> batch = createLabels(1000);
		private static List<String[]> response;

		@BeforeClass
		public static void setUp() throws Exception {
			response = callGenerateLabels(batch);
		}

		@Test
		public void returnedCSVContainsEqualAmountOfRows() throws Exception {
			Assert.assertEquals(batch.size(), response.size());
		}
	}

	private static List<String[]> readResponseBody(HttpResponse response) throws IOException {
		PDDocument document = PDDocument.load(response.getEntity().getContent());
		PDFText2HTML stripper = new PDFText2HTML("UTF-8");
		StringWriter writer = new StringWriter();
		stripper.setLineSeparator("<br/>");
		stripper.writeText(document, writer);
		document.close();
		return parseHTML(new String(toXhtml(writer.toString().getBytes())));
	}
	
	private static byte[] toXhtml(byte[] document) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		newTidy().parseDOM(new ByteArrayInputStream(document), out);
		return out.toByteArray();
	}

	private static Tidy newTidy() {
		Tidy tidy = new Tidy();
		tidy.setTidyMark(false);
		tidy.setDocType("omit");
		tidy.setXHTML(true);
		tidy.setCharEncoding(Configuration.UTF8);
		return tidy;
	}

	
	private static List<String[]> parseHTML(String xml) {
		SAXReader reader = new SAXReader();
		List<String[]> labels = new ArrayList<String[]>();
		try {
			Document document = reader.read(new StringReader(xml));
			for (Object object : document.selectNodes("//div/p")) {
				Node p = (Node) object;
				labels.add(p.getText().split("\n"));
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return labels;
	}

	private static List<AddressLabel> createLabels(int count) {
		return new Generator<AddressLabel>(){
			protected AddressLabel create() {
				String postOffice = random("postOffice");
				return new AddressLabel(
						random("firstname"), 
						random("lastname"), 
						random("street") + " " + random("houseNumber"), 
						postOffice.substring(0, postOffice.indexOf(" ")),
						postOffice.substring(postOffice.indexOf(" ") + 1),
						random("country"));
			}
		}.enumerate(count);
	}

	private static String[] callGenerateLabels(String firstName, String lastName, String streetAddress, String postalCode, String postOffice, String country) throws UnsupportedEncodingException,
			IOException, JsonGenerationException, JsonMappingException,	ClientProtocolException {
		return callGenerateLabels(Arrays.asList(new AddressLabel(firstName, lastName, streetAddress, postalCode, postOffice, country))).get(0);
	}
	
	private final static String PDF_TEMPLATE = "/osoitetarrat.html";
	private static List<String[]> callGenerateLabels(List<AddressLabel> labels) throws UnsupportedEncodingException,
			IOException, JsonGenerationException, JsonMappingException,
			ClientProtocolException {
		AddressLabelBatch batch = new AddressLabelBatch(PDF_TEMPLATE, labels);
		DefaultHttpClient client = new DefaultHttpClient();
		client.getParams().setParameter("http.protocol.content-charset", "UTF-8");
		HttpPost post = new HttpPost("http://localhost:8080/api/v1/addresslabel");
		post.setHeader("Content-Type", "application/json;charset=utf-8");
		post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(batch), ContentType.APPLICATION_JSON));
		HttpResponse response = client.execute(post);
		return readResponseBody(response);
	}

}
