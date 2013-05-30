package fi.vm.sade.viestintapalvelu.domain.address;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.lowagie.text.DocumentException;

import fi.vm.sade.viestintapalvelu.infrastructure.DocumentBuilder;

public class AddressLabelBuilder {

	private final static String ADDRESS_LABEL_PDF_TEMPLATE = "/osoitetarrat.html";
	private final static String ADDRESS_LABEL_XLS_TEMPLATE = "/osoitetarrat.xls";

	private DocumentBuilder documentBuilder;

	@Inject
	public AddressLabelBuilder(DocumentBuilder documentBuilder) {
		this.documentBuilder = documentBuilder;
	}

	public byte[] printPDF(AddressLabelBatch input) throws DocumentException,
			IOException {
		Map<String, Object> context = createDataContext(
				input.getAddressLabels(), new AddressLabelDecoratorBuilder() {
					protected PostalAddressDecorator newAddressLabelDecorator(
							AddressLabel addressLabel) {
						return new HtmlAddressLabelDecorator(addressLabel
								.postalAddress());
					}
				});
		byte[] xhtml = documentBuilder.applyTextTemplate(
				ADDRESS_LABEL_PDF_TEMPLATE, context);
		return documentBuilder.xhtmlToPDF(xhtml);
	}

	public byte[] printCSV(AddressLabelBatch input) throws DocumentException,
			IOException {
		Map<String, Object> context = createDataContext(
				input.getAddressLabels(), new AddressLabelDecoratorBuilder() {
					protected PostalAddressDecorator newAddressLabelDecorator(
							AddressLabel addressLabel) {
						return new XmlAddressLabelDecorator(addressLabel
								.postalAddress());
					}
				});
		return documentBuilder.applyTextTemplate(ADDRESS_LABEL_XLS_TEMPLATE,
				context);
	}

	private Map<String, Object> createDataContext(
			List<AddressLabel> addressLabels,
			final AddressLabelDecoratorBuilder builder) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("labelList", Lists.transform(addressLabels,
				new Function<AddressLabel, PostalAddressDecorator>() {
					public PostalAddressDecorator apply(AddressLabel label) {
						return builder.newAddressLabelDecorator(label);
					}
				}));
		return data;
	}

	private abstract class AddressLabelDecoratorBuilder {
		protected abstract PostalAddressDecorator newAddressLabelDecorator(
				AddressLabel addressLabel);
	}
}
