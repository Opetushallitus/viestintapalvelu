package fi.vm.sade.viestintapalvelu;

import java.util.ArrayList;
import java.util.List;

public class Urls {
    public static final String API_PATH = "api/v1";
    public static final String ADDRESS_LABEL_RESOURCE_PATH = "addresslabel";
    public static final String DOWNLOAD_RESOURCE_PATH = "download";
    public static final String MESSAGE_RESOURCE_PATH = "message";
    public static final String HYVAKSYMISKIRJE_RESOURCE_PATH = "hyvaksymiskirje";
    public static final String JALKIOHJAUSKIRJE_RESOURCE_PATH = "jalkiohjauskirje";
    public static final String KOEKUTSUKIRJE_RESOURCE_PATH = "koekutsukirje";
    public static final String LETTER_PATH = "letter";
    public static final String PRINTER_PATH = "printer";
    public static final String TEMPLATE_RESOURCE_PATH = "template";
    public static final String IPOSTI_RESOURCE_PATH = "iposti";
    public static final String REPORTING_PATH = "reporting";
    public static final String REPORTING_LIST_PATH = "list";
    public static final String REPORTING_SEARCH_PATH = "search";
    public static final String REPORTING_VIEW_PATH = "view";
    public static final String REPORTING_LETTER_PATH = "letter";
    public static final String REPORTING_CONTENTS_PATH = "contents";
    public static final String ATTACHMENT_RESOURCE_PATH = "attachment";

    public static RestServer localhost() {
        return new Localhost();
    }

    public static RestServer localhost(int port) {
        return new Localhost(port);
    }

    public static interface RestServer {

        String index();

        String addresslabelDownload();

        String addresslabel();
    }

    public static class Localhost implements RestServer {
        private static final String SCHEME = "http";
        private static final String DOMAIN = "localhost";
        private final int port;

        public Localhost() {
            this.port = 8080;
        }

        public Localhost(int port) {
            this.port = port;
        }

        private String root() {
            return SCHEME + "://" + DOMAIN + ":" + port;
        }

        @Override
        public String index() {
            return build(root(), "index.html");
        }

        private String apiRoot() {
            return build(root(), Urls.API_PATH);
        }

        @Override
        public String addresslabel() {
            return build(apiRoot(), ADDRESS_LABEL_RESOURCE_PATH);
        }

        @Override
        public String addresslabelDownload() {
            return build(apiRoot(), ADDRESS_LABEL_RESOURCE_PATH, DOWNLOAD_RESOURCE_PATH);
        }
    }

    public static String build(String... parts) {
        UrlBuilder builder = new UrlBuilder();
        for (String part : parts) {
            builder.add(part);
        }
        return builder.toString();
    }

    private static class UrlBuilder {
        private final List<String> parts = new ArrayList<String>();

        public void add(String part) {
            this.parts.add(part);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                builder.append(parts.get(i));
                if (i != (parts.size() - 1)) {
                    builder.append("/");
                }
            }
            return builder.toString();
        }
    }
}
