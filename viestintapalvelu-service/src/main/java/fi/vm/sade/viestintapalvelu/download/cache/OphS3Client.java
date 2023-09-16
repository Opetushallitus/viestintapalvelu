package fi.vm.sade.viestintapalvelu.download.cache;

import fi.vm.sade.viestintapalvelu.download.Download;
import fi.vm.sade.viestintapalvelu.download.Header;
import fi.vm.sade.viestintapalvelu.util.S3Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component
class OphS3Client {

    private static final Logger log = LoggerFactory.getLogger(OphS3Client.class);
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final Environment env;
    private final AWSS3ClientFactory clientFactory;

    @Value("${viestintapalvelu.downloadfiles.s3.bucket}")
    private String bucket;

    @Value("${viestintapalvelu.downloadfiles.s3.region}")
    private String region;

    private static final String METADATA_TIMESTAMP = "timestamp";
    private static final String METADATA_FILE_NAME = "filename";
    private static final String METADATA_UUID = "uuid";
    private Region awsRegion;

    @Autowired
    public OphS3Client(Environment env,
                       AWSS3ClientFactory awss3ClientFactory) {
        this.env = env;
        this.clientFactory = awss3ClientFactory;
    }

    @PostConstruct
    public void init() {
        if(env != null) {
            log.info("Active spring profiles={}", Arrays.toString(env.getActiveProfiles()));
        } else {
            log.info("Spring profiles were null, this should not happen");
        }

        awsRegion = Region.of(region);
        log.info("Region {}", region);
        log.info("Bucket {}", bucket);
        boolean isSuccessful = S3Utils.canConnectToBucket(getClient(), bucket);
        if(!isSuccessful) {
            log.error("OphS3Client could not connect to S3 bucket {} in region {}", bucket, region);
        }
    }

    AddObjectResponse<PutObjectResponse> addFileObject(Download download) {
        UUID uuid = UUID.randomUUID();
        return addFileObject(download, new DocumentId(uuid.toString()));
    }

    AddObjectResponse<PutObjectResponse> addFileObject(Download download, DocumentId documentId) {
        String filename = download.getFilename();

        int i = filename.lastIndexOf(".");
        String prefix = filename.substring(0,i);
        String suffix = filename.substring(i);
        final Path tempFile;
        try {
            tempFile = Files.createTempFile(prefix, suffix);
            Files.write(tempFile, download.toByteArray(), StandardOpenOption.WRITE);
        } catch (IOException e) {
            log.error("Error creating temp file for s3 transmission", e);
            return new AddObjectResponse<>(new DocumentId(""), null);
        }
        Long length = (long) download.toByteArray().length;

        Map<String, String> metadata = new HashMap<>();
        ZonedDateTime zdt = ZonedDateTime.ofInstant(download.getTimestamp().toInstant(), ZoneId.of("UTC"));
        metadata.put(METADATA_TIMESTAMP, zdt.format(dateFormat));
        metadata.put(METADATA_FILE_NAME, download.getFilename());
        metadata.put(METADATA_UUID, documentId.getDocumentId());

        final PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .contentType(download.getContentType())
                .contentLength(length)
                .metadata(metadata)
                .key(documentId.getDocumentId())
                .build();
        AsyncRequestBody asyncRequestProvider = AsyncRequestBody.fromFile(tempFile);

        S3AsyncClient client = getClient();
        CompletableFuture<PutObjectResponse> completableFuture =  client.putObject(request, asyncRequestProvider).whenComplete((putObjectResponse, throwable) -> {
            if(putObjectResponse == null) {
                log.error("Error saving file {} to S3", documentId.getDocumentId(), throwable);
            }
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.error("Error deleting temp file {}", tempFile.toAbsolutePath().toString(), e);
            }
            try {
                client.close();
            } catch (Exception e) {
                log.error("Error closing S3 client", e);
            }
        });
        return new AddObjectResponse<>(new DocumentId(documentId.getDocumentId()), completableFuture);
    }

    Download getObject(DocumentId documentId) {
        try(S3AsyncClient client = getClient()) {
            CompletableFuture<ResponseBytes<GetObjectResponse>> responseBytes = getObjectContents(client, documentId);
            Header header = headObject(client, documentId).join();
            return new Download(header.getContentType(), header.getFilename(), responseBytes.join().asByteArray());
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error getting document %s from S3", documentId.getDocumentId()), e);
        }
    }

    /**
     * Returns a list of objects without content
     */
    List<Header> listObjects() {
        ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).build();
        try (final S3AsyncClient client = getClient()) {
            CompletableFuture<ListObjectsV2Response> listResponse = client.listObjectsV2(request);
            CompletableFuture<List<S3Object>> objects = listResponse.thenApply(ListObjectsV2Response::contents);
            try {
                List<CompletableFuture<Header>> list = objects.thenApply(s3Objects -> s3Objects.stream()
                        .map(s3Object -> this.headObject(client, s3Object)).collect(Collectors.toList())).get();
                CompletableFuture.allOf(list.toArray(new CompletableFuture[list.size()]));
                return list.stream().map(CompletableFuture::join).collect(Collectors.toList());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error getting list of objects", e);
                return new ArrayList<>();
            }
        }
    }

    private CompletableFuture<Header> headObject(S3AsyncClient client, S3Object obj) {
        return headObject(client, new DocumentId(obj.key()));
    }

    private CompletableFuture<Header> headObject(S3AsyncClient client, DocumentId id) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(bucket).key(id.getDocumentId()).build();
        return client.headObject(headObjectRequest).thenApply(res -> {
            String s = res.metadata().get(METADATA_TIMESTAMP);
            ZonedDateTime timestamp = ZonedDateTime.parse(s);
            return new Header(res.contentType(),
                    res.metadata().get(METADATA_FILE_NAME),
                    id.getDocumentId(),
                    res.contentLength(),
                    timestamp);
        });
    }


    private CompletableFuture<ResponseBytes<GetObjectResponse>> getObjectContents(S3AsyncClient client, DocumentId id) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(id.getDocumentId()).build();
        return client.getObject(getObjectRequest, AsyncResponseTransformer.toBytes());
    }

    public static class AddObjectResponse<T> {
        private final DocumentId id;
        private final CompletableFuture<T> future;

        AddObjectResponse(DocumentId id, CompletableFuture<T> future) {
            this.id = id;
            this.future = future;
        }

        public DocumentId getId() {
            return id;
        }

        public CompletableFuture<T> getFuture() {
            return future;
        }

    }

    private S3AsyncClient getClient() {
        return clientFactory.getClient(awsRegion);
    }

}
