package fi.vm.sade.viestintapalvelu;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.ClassRule;
import org.junit.Test;

/*
 * curl -H "Content-Type: application/json" -X POST -d '["Ville Peurala", "Iina
 * Sipilä"]' -i http://localhost:8080/spike
 */
public class IntegrationTest {
	@ClassRule
	public static TomcatRule tomcat = new TomcatRule();

	@Test
	public void staticResourcesWork() throws Exception {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(Urls.localhost().index());
		HttpResponse response = httpClient.execute(httpGet);
		assertEquals(200, response.getStatusLine().getStatusCode());
	}

	@Test
	public void addressLabelPrinting() throws Exception {
		HttpResponse response = get("/addresslabel_pdf.json", Urls.localhost().addresslabel() + "/pdf");
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals("Content-Type: application/pdf;charset=utf-8", response
				.getFirstHeader("Content-Type").toString());
		assertEquals(
				"Content-Disposition: attachment; filename=\"addresslabels.pdf\"",
				response.getFirstHeader("Content-Disposition").toString());
	}

	@Test
	public void addressLabelXLSPrinting() throws Exception {
		HttpResponse response = get("/addresslabel_xls.json", Urls.localhost().addresslabel() + "/xls");
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals("Content-Type: application/vnd.ms-excel", response
				.getFirstHeader("Content-Type").toString());
		assertEquals(
				"Content-Disposition: attachment; filename=\"addresslabels.xls\"",
				response.getFirstHeader("Content-Disposition").toString());
	}

	@Test
	public void jalkiohjauskirjePDFPrinting() throws Exception {
		HttpResponse response = get("/jalkiohjauskirje_pdf.json", "http://localhost:8080/api/v1/jalkiohjauskirje/pdf");
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals("Content-Type: application/pdf;charset=utf-8", response
				.getFirstHeader("Content-Type").toString());
		assertEquals(
				"Content-Disposition: attachment; filename=\"jalkiohjauskirje.pdf\"",
				response.getFirstHeader("Content-Disposition").toString());
	}

	private HttpResponse get(String jsonFile, String url) throws UnsupportedEncodingException,
		IOException, ClientProtocolException {
		String json = new Scanner(getClass().getResourceAsStream(
				jsonFile), "UTF-8").useDelimiter("\u001a").next();
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(url);
		post.setHeader("Content-Type", "application/json");
		post.setEntity(new StringEntity(json));
		HttpResponse response = client.execute(post);
		String documentId = readResponseBody(response);
		HttpGet get = new HttpGet(Urls.localhost().apiRoot()
				+ "/download/document/" + documentId);
		response = client.execute(get);
		return response;
	}

	private String readResponseBody(HttpResponse response) throws IOException {
		return IOUtils.toString(response.getEntity().getContent());
	}
}
