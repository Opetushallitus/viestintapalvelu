package fi.vm.sade.viestintapalvelu.testdata;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.hibernate.criterion.Example;
import org.springframework.core.io.ClassPathResource;
import fi.vm.sade.viestintapalvelu.template.Template;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.*;

import static java.util.Arrays.*;

/**
 * usage example:
 * -Dtemplate=kk_hyvaksymiskirje_2016
 * -Dtemplate=kk_hyvaksymiskirje_2016,kk_jalkiohjauskirje_2016
 */
public class TemplateTestDataGenerator {



    public static void main(String[] args) throws IOException {
        //String templateKeys = System.getProperty("template");
        //String templateKeys = "kayttooikeus_kutsu";
        //String templateKeys = "omattiedot_email_2aste";
        //String templateKeys = "2aste_koekutsukirje_2022";
        //String templateKeys = "2aste_hyvaksymiskirje_2023,2aste_jalkiohjauskirje_2023,2aste_hyvaksymiskirje_huoltajille_2023,2aste_jalkiohjauskirje_huoltajille_2023";
        //String templateKeys = "kk_toinen_hyvaksymiskirje_2023,kk_toinen_jalkiohjauskirje_2023";
        //String templateKeys = "kk_jalkiohjauskirje_syksy_2023,kk_hyvaksymiskirje_syksy_2023,kk_varasijakirje_syksy_2023";
        //String templateKeys = "2aste_hyvaksymiskirje_2024,2aste_jalkiohjauskirje_2024";
        //String templateKeys = "kk_ensimmainen_hyvaksymiskirje_2024,kk_ensimmainen_jalkiohjauskirje_2024";
        //String templateKeys = "kk_toinen_hyvaksymiskirje_2024,kk_toinen_jalkiohjauskirje_2024";
        //String templateKeys = "kk_jalkiohjauskirje_syksy_2024,kk_hyvaksymiskirje_syksy_2024";
        String templateKeys = "kk_ensimmainen_hyvaksymiskirje_2025,kk_ensimmainen_jalkiohjauskirje_2025";

        if(templateKeys != null) {
            for(String templateKey : templateKeys.split(",")) {
                for (String language : asList("FI", "SV", "EN")) {
                    generateTemplate(templateKey, language);
                }
            }
        }
    }

    private static void generateTemplate(String templateKey, String language) throws IOException {
        final String path = String.format("test_data/%s/%s/", templateKey, language);
        final String templatePrefix = String.format("%s_%s", templateKey, language);
        final String templateFile = String.format("%s.template.json", templatePrefix);
        final String outputFile = String.format("%s.json", templatePrefix);
        final ClassPathResource resource = new ClassPathResource(String.format("%s%s", path, templateFile));
        if (!resource.exists()) {
            System.out.println("Skipping non-existing language " + language);
            return;
        }
        final String template = IOUtils.toString(resource.getInputStream());

        List<String> files = filesInPath(path);
        Map<String, String> replaces = filesToReplacements(path, templatePrefix, templateFile, outputFile, files);

        final Gson gson = new GsonBuilder()
                .disableHtmlEscaping().registerTypeAdapter(String.class, new MustacheStringReader(replaces)).create();
        final Template renderedTemplate = gson.fromJson(template, Template.class);

        final String testDataPath = "/viestintapalvelu-service/src/main/resources";
        final String viestintapalveluPath = new File("").getAbsolutePath();
        final String outputUrl = String.format("%s/%s/%s%s", viestintapalveluPath, testDataPath, path, outputFile);

        System.out.println("Writing to output file " + outputFile);

        FileOutputStream fileOutputStream = new FileOutputStream(outputUrl);
        IOUtils.write(new GsonBuilder().disableHtmlEscaping().create().toJson(renderedTemplate), fileOutputStream);
        IOUtils.closeQuietly(fileOutputStream);

    }

    private static Map<String, String> filesToReplacements(String path, String templatePrefix, String templateFile, String outputFile, List<String> files) throws IOException {
        Map<String,String> replaces = new HashMap<>();
        for(String file : files) {
            if(!file.equals(templateFile) && !file.equals(outputFile) && file.startsWith(templatePrefix)) {
                String replaceKey = file.substring(templatePrefix.length() + 1);
                final String replaceData = IOUtils.toString(new ClassPathResource(
                        String.format("%s%s", path, file)).getInputStream())
                        .replaceAll("\\r\\n|\\r|\\n", " ");
                replaces.put(replaceKey, replaceData);
            }
        }
        return replaces;
    }

    private static List filesInPath(String path) throws IOException {
        return IOUtils.readLines(TemplateTestDataGenerator.class.getClassLoader()
                .getResourceAsStream(StringUtils.cleanPath(path)), Charsets.UTF_8.displayName());
    }


    private static class MustacheStringReader extends TypeAdapter<String> {
        private final MustacheFactory mf = new DefaultMustacheFactory();
        private final StringWriter writer = new StringWriter();
        private final Map<String, String> replaces;
        public MustacheStringReader(Map<String, String> replaces) {
            super();
            this.replaces = replaces;
        }

        @Override
        public void write(JsonWriter jsonWriter, String s) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String value = in.nextString();
            writer.getBuffer().setLength(0);
            Mustache mustache = mf.compile(new StringReader(value), "");
            String rendered = mustache.execute(writer, replaces).toString();
            String unescaped =StringEscapeUtils.unescapeHtml(rendered);
            return unescaped;

        }
    }
}
