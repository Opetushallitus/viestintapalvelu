package fi.vm.sade.viestintapalvelu.download.cache;

import fi.vm.sade.viestintapalvelu.download.Download;
import fi.vm.sade.viestintapalvelu.download.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.async.AsyncRequestProvider;
import software.amazon.awssdk.async.AsyncResponseHandler;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
class OphS3Client {

    private static final Logger log = LoggerFactory.getLogger(OphS3Client.class);
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Autowired
    private Environment env;

    @Value("${viestintapalvelu.downloadfiles.s3.bucket}")
    private String bucket;

    @Value("${viestintapalvelu.downloadfiles.s3.region}")
    private String region;

    private static final String METADATA_TIMESTAMP = "timestamp";
    private static final String METADATA_FILE_NAME = "filename";
    private static final String METADATA_UUID = "uuid";
    private Region awsRegion;

    @PostConstruct
    public void init() {
        log.info("Active spring profiles={}", Arrays.toString(env.getActiveProfiles()));
        awsRegion = Region.of(region);
        log.info("Region {}", region);
        log.info("Bucket {}", bucket);
        try {
            S3AsyncClient client = getClient();
            if (client == null) {
                log.error("Error occurred while initializing s3 client, client is null");
                return;
            }
            CompletableFuture<HeadBucketResponse> resFut = client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            resFut.whenCompleteAsync((headBucketResponse, throwable) -> {
                if (throwable != null) {
                    log.error("Error connecting to S3 bucket {} in region {}", bucket, region, throwable);
                }
            });
        } catch (Exception e) {
            log.error("Error occurred while initializing s3 client", e);
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
        AsyncRequestProvider asyncRequestProvider = AsyncRequestProvider.fromFile(tempFile);
        try(S3AsyncClient client = getClient()) {
            CompletableFuture<PutObjectResponse> completableFuture = client.putObject(request, asyncRequestProvider).whenComplete((putObjectResponse, throwable) -> {
                try {
                    Files.delete(tempFile);
                } catch (IOException e) {
                    log.error("Error deleting temp file {}", tempFile.toAbsolutePath().toString(), e);
                }
            });
            return new AddObjectResponse<>(new DocumentId(documentId.getDocumentId()), completableFuture);
        }
    }

    Download getObject(DocumentId documentId, boolean includeContent) {
        AsyncResponseHandler<GetObjectResponse, byte[]> handler = AsyncResponseHandler.toByteArray();
        CompletableFuture<byte[]> fullObject = includeContent ? getObjectContents(documentId, handler) : CompletableFuture.completedFuture(new byte[0]);
        Header header = headObject(documentId);
        return new Download(header.getContentType(), header.getFilename(), fullObject.join());
    }

    private Header headObject(DocumentId id) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(bucket).key(id.getDocumentId()).build();

        try (S3AsyncClient client = getClient()) {
            HeadObjectResponse res = client.headObject(headObjectRequest).join();

            String s = res.metadata().get(METADATA_TIMESTAMP);
            ZonedDateTime timestamp = ZonedDateTime.parse(s);
            return new Header(res.contentType(),
                    res.metadata().get(METADATA_FILE_NAME),
                    id.getDocumentId(),
                    res.contentLength(),
                    timestamp);
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
                List<CompletableFuture<Header>> list = objects.thenApply(s3Objects -> s3Objects.stream().map(s3Object -> this.headObject(s3Object, client)).collect(Collectors.toList())).get();
                CompletableFuture.allOf(list.toArray(new CompletableFuture[list.size()]));
                return list.stream().map(CompletableFuture::join).collect(Collectors.toList());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error getting list of objects", e);
                return new ArrayList<>();
            }
        }
    }

    private CompletableFuture<Header> headObject(S3Object obj, S3AsyncClient client) {
        return headObject(new DocumentId(obj.key()), client);
    }

    private CompletableFuture<Header> headObject(DocumentId id, S3AsyncClient client) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(bucket).key(id.getDocumentId()).build();
        CompletableFuture<Header> headerCompletableFuture = client.headObject(headObjectRequest).thenApply(res -> {
            String s = res.metadata().get(METADATA_TIMESTAMP);
            ZonedDateTime timestamp = ZonedDateTime.parse(s);
            return new Header(res.contentType(),
                    res.metadata().get(METADATA_FILE_NAME),
                    id.getDocumentId(),
                    res.contentLength(),
                    timestamp);
        });
        return headerCompletableFuture;
    }


    private CompletableFuture<byte[]> getObjectContents(S3Object obj, AsyncResponseHandler<GetObjectResponse, byte[]> asyncResponseHandler) {
        return getObjectContents(new DocumentId(obj.key()), asyncResponseHandler);
    }

    private CompletableFuture<byte[]> getObjectContents(DocumentId id, AsyncResponseHandler<GetObjectResponse, byte[]> asyncResponseHandler) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(id.getDocumentId()).build();
        try (S3AsyncClient client = getClient()) {
            return client.getObject(getObjectRequest, asyncResponseHandler);
        }
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

    private static final AwsCredentialsProvider AWS_CREDENTIALS_PROVIDER = new DefaultCredentialsProvider();
    private S3AsyncClient getClient() {
        return S3AsyncClient.builder()
                .region(awsRegion)
                .credentialsProvider(AWS_CREDENTIALS_PROVIDER)
                .build();
    }

}
