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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.*;

/**
 * usage example:
 * -Dtemplate=kk_hyvaksymiskirje_2016
 * -Dtemplate=kk_hyvaksymiskirje_2016,kk_jalkiohjauskirje_2016
 */
public class TemplateTestDataGenerator {



    public static void main(String[] args) throws IOException {
        //String templateKeys = System.getProperty("template");
        //String templateKeys = "kk_jalkiohjauskirje_syksy_2024,kk_hyvaksymiskirje_syksy_2024";
        //String templateKeys = "kk_ensimmainen_hyvaksymiskirje_2025,kk_ensimmainen_jalkiohjauskirje_2025";
        //String templateKeys = "2aste_hyvaksymiskirje_2025,2aste_jalkiohjauskirje_2025";
        //String templateKeys = "kk_toinen_hyvaksymiskirje_2025,kk_toinen_jalkiohjauskirje_2025";
        String templateKeys = "kk_jalkiohjauskirje_syksy_2025,kk_hyvaksymiskirje_syksy_2025";

        if(templateKeys != null) {
            for(String templateKey : templateKeys.split(",")) {
                for (String language : asList("FI", "SV", "EN")) {
                    generateTemplate(templateKey, language);
                }
            }
        }
    }

    private static void generateTemplate(String templateKey, String language) throws IOException {
        final String path = String.format("%s/kirjepohjat/%s/%s/", System.getProperty("user.dir"), templateKey, language);
        final String templatePrefix = String.format("%s_%s", templateKey, language);
        final String templateFile = String.format("%s.template.json", templatePrefix);
        final String outputFile = String.format("%s.json", templatePrefix);
        final File resource = new File(String.format("%s/%s", path, templateFile));
        if (!resource.exists()) {
            System.out.println("Skipping non-existing language " + language);
            return;
        }
        final String template = IOUtils.toString(Files.newInputStream(resource.toPath()));

        List<String> files = filesInPath(path);
        files.forEach(f -> System.out.println("File: " + f));
        Map<String, String> replaces = filesToReplacements(path, templatePrefix, templateFile, outputFile, files);

        final Gson gson = new GsonBuilder()
                .disableHtmlEscaping().registerTypeAdapter(String.class, new MustacheStringReader(replaces)).create();
        final Template renderedTemplate = gson.fromJson(template, Template.class);
        final String outputUrl = String.format("%s/%s", path, outputFile);

        System.out.println("Writing to output file " + outputFile);

        FileOutputStream fileOutputStream = new FileOutputStream(outputUrl);
        IOUtils.write(new GsonBuilder().disableHtmlEscaping().create().toJson(renderedTemplate), fileOutputStream);
        IOUtils.closeQuietly(fileOutputStream);

    }

    private static Map<String, String> filesToReplacements(String path, String templatePrefix, String templateFile, String outputFile, List<String> files) throws IOException {
        Map<String,String> replaces = new HashMap<>();
        for(String file : files) {
            if(!file.equals(templateFile) && !file.equals(outputFile) && file.contains(templatePrefix)) {
                String replaceKey = file.substring(templatePrefix.length() + 1);
                final String replaceData = IOUtils.toString(Files.newInputStream(new File(String.format("%s/%s", path, file)).toPath()))
                        .replaceAll("\\r\\n|\\r|\\n", " ");
                replaces.put(replaceKey, replaceData);
            }
        }
        return replaces;
    }

    private static List<String> filesInPath(String path) throws IOException {
        return Files.walk(Paths.get(path))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .map(File::getName)
                .collect(Collectors.toList());
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
