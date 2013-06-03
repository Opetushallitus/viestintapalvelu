package fi.vm.sade.viestintapalvelu.test;

import fi.vm.sade.viestintapalvelu.Launcher;
import fi.vm.sade.viestintapalvelu.application.Urls;
import fi.vm.sade.viestintapalvelu.application.ViestintapalveluUrlProvider;

public class Localhost implements ViestintapalveluUrlProvider {
	private static final String SCHEME = "http";
	private static final String DOMAIN = "localhost";
	private static final int PORT = Launcher.DEFAULT_PORT;

	@Override
	public String root() {
		return SCHEME + "://" + DOMAIN + ":" + PORT;
	}

	@Override
	public String index() {
		return Urls.build(root(), "index.html");
	}

	@Override
	public String apiRoot() {
		return Urls.build(root(), Urls.API_PATH);
	}

	@Override
	public String addresslabel() {
		return Urls.build(apiRoot(), Urls.ADDRESS_LABEL_RESOURCE_PATH);
	}

	public String jalkiohjauskirje() {
		return Urls.build(apiRoot(), Urls.JALKIOHJAUSKIRJE_RESOURCE_PATH);
	}

	public String hyvaksymiskirje() {
		return Urls.build(apiRoot(), Urls.HYVAKSYMISKIRJE_RESOURCE_PATH);
	}

	@Override
	public String addresslabelDownload() {
		return Urls.build(apiRoot(), Urls.ADDRESS_LABEL_RESOURCE_PATH,
				Urls.DOWNLOAD_RESOURCE_PATH);
	}

	// FIXME vpeurala 3.6.2013: Path duplication
	public String addresslabelPdf() {
		return Urls.build(addresslabel(), "pdf");
	}

	public String addresslabelXls() {
		return Urls.build(addresslabel(), "xls");
	}

	public String jalkiohjauskirjePdf() {
		return Urls.build(jalkiohjauskirje(), "pdf");
	}

	public String jalkiohjauskirjeZip() {
		return Urls.build(jalkiohjauskirje(), "zip");
	}

	public String hyvaksymiskirjePdf() {
		return Urls.build(hyvaksymiskirje(), "pdf");
	}
}