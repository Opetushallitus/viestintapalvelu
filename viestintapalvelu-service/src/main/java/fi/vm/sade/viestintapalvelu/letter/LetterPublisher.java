package fi.vm.sade.viestintapalvelu.letter;

import com.google.common.collect.Lists;
import fi.vm.sade.viestintapalvelu.dao.LetterBatchDAO;
import fi.vm.sade.viestintapalvelu.dao.LetterReceiverLetterDAO;
import fi.vm.sade.viestintapalvelu.model.LetterBatch;
import fi.vm.sade.viestintapalvelu.model.LetterReceiverLetter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.async.AsyncRequestProvider;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface LetterPublisher {
    int publishLetterBatch(long letterBatchId) throws Exception;
}

@Service
@Profile("!aws")
class LocalLetterPublisher implements LetterPublisher {

    @Value("${viestintapalvelu.letter.publish.dir}")
    private File letterPublishDir;

    private final LetterBatchDAO letterBatchDAO;
    private final LetterReceiverLetterDAO letterReceiverLetterDAO;

    public static final Logger logger = LoggerFactory.getLogger(LocalLetterPublisher.class);

    @Autowired
    public LocalLetterPublisher(LetterBatchDAO letterBatchDAO,
                                LetterReceiverLetterDAO letterReceiverLetterDAO) {
        this.letterBatchDAO = letterBatchDAO;
        this.letterReceiverLetterDAO = letterReceiverLetterDAO;
    }

    @Override
    public int publishLetterBatch(long letterBatchId) throws Exception {
        List<Long> letters = letterBatchDAO.getUnpublishedLetterIds(letterBatchId);
        if(letters.size() > 0) {
            new Thread(new LetterPublishTask(letterBatchId, letters)).start();
            return letters.size();
        } else {
            return 0;
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void publishLetter(long letterId, File letterBatchPublishDir) {
        File tempPdfFile = null;
        File finalPdfFile = null;
        try {
            LetterReceiverLetter letter = letterReceiverLetterDAO.read(letterId);
            tempPdfFile = new File(letterBatchPublishDir, letter.getLetterReceivers().getOidApplication() + "_partial_" + System.currentTimeMillis() + ".pdf");
            finalPdfFile = new File(letterBatchPublishDir, letter.getLetterReceivers().getOidApplication() + ".pdf");
            FileOutputStream out = new FileOutputStream(tempPdfFile);
            out.write(letter.getLetter());
            out.close();
            letterReceiverLetterDAO.markAsPublished(letter.getId());
            tempPdfFile.renameTo(finalPdfFile);
        } catch (Exception e) {
            if(tempPdfFile != null) {
                tempPdfFile.delete();
            }
            throw new RuntimeException("Error publishing letter id " + letterId + (finalPdfFile == null ? "" : " to " + finalPdfFile), e);
        }
    }

    private class LetterPublishTask implements Runnable {

        private final long letterBatchId;
        private final List<Long> letterIds;

        protected LetterPublishTask(long letterBatchId, List<Long> letterIds) {
            this.letterBatchId = letterBatchId;
            this.letterIds = letterIds;
        }

        @Override
        public void run() {
            LetterBatch letterBatch = letterBatchDAO.read(letterBatchId);
            String subFolderName = StringUtils.isEmpty(letterBatch.getApplicationPeriod()) ? String.valueOf(letterBatchId) : letterBatch.getApplicationPeriod().trim();
            File letterBatchPublishDir = new File(letterPublishDir, subFolderName);
            logger.info("Publishing {} letters from letter batch {} to dir {}", letterIds.size(), letterBatchId, letterBatchPublishDir);
            letterBatchPublishDir.mkdirs();
            int count = 0;
            try {
                for (Long letterId : letterIds) {
                    publishLetter(letterId, letterBatchPublishDir);
                    count++;
                    if (count % 100 == 0) {
                        logger.info("Published {}/{} letters from letter batch {} to dir {}", count, letterIds.size(), letterBatchId, letterBatchPublishDir);
                    }
                }
            }
            catch (Exception e) {
                logger.error("Letter batch " + letterBatchId + " publish failed. Published " + count + "/" + letterIds.size(), e);
                throw e;
            }
            logger.info("Published successfully {} letters from letter batch {} to dir {}", count, letterBatchId, letterBatchPublishDir);

        }
    }
}

@Service
@Profile("aws")
class S3LetterPublisher implements LetterPublisher {

    private final LetterBatchDAO letterBatchDAO;
    private final LetterReceiverLetterDAO letterReceiverLetterDAO;
    public static final Logger logger = LoggerFactory.getLogger(S3LetterPublisher.class);

    @Value("${viestintapalvelu.letter.publish.dir}")
    private File letterPublishDir;

    @Value("${viestintapalvelu.downloadfiles.s3.bucket}")
    private String bucket;

    @Value("${viestintapalvelu.downloadfiles.s3.region}")
    private Region region;

    @Autowired
    S3LetterPublisher(LetterBatchDAO letterBatchDAO,
                      LetterReceiverLetterDAO letterReceiverLetterDAO) {
        this.letterBatchDAO = letterBatchDAO;
        this.letterReceiverLetterDAO = letterReceiverLetterDAO;
    }


    @Override
    public int publishLetterBatch(long letterBatchId) throws Exception {
        List<Long> letters = letterBatchDAO.getUnpublishedLetterIds(letterBatchId);
        if(letters.size() > 0) {
            publishLettersS3(letterBatchId, letters);
            return letters.size();
        } else {
            return 0;
        }
    }


    private void publishLettersS3(long letterBatchId, List<Long> letterIds) {
        List<List<Long>> partition = Lists.partition(letterIds, 20);

        final LetterBatch letterBatch = letterBatchDAO.read(letterBatchId);

        String subFolderName = StringUtils.isEmpty(letterBatch.getApplicationPeriod()) ? String.valueOf(letterBatchId) : letterBatch.getApplicationPeriod().trim();

        partition.forEach(ids -> {
            List<LetterReceiverLetter> byIds = letterReceiverLetterDAO.findByIds(ids);
            for (LetterReceiverLetter letter : byIds) {
                try {
                    addFileObject(letter, subFolderName);
                } catch (IOException e) {
                    logger.error("Error saving LetterReceiverLetter {} to S3", e);
                }
            }
        });
    }

    private CompletableFuture<PutObjectResponse> addFileObject(final LetterReceiverLetter letter, String foldername) throws IOException {
        final Path tempFile = Files.createTempFile(letter.getLetterReceivers().getOidApplication(), ".pdf");
        Long length = (long) letter.getLetter().length;
        Map<String, String> metadata = new HashMap<>();
        String id = Paths.get(foldername,tempFile.toString()).toString();
        final PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .contentType(letter.getContentType())
                .contentLength(length)
                .metadata(metadata)
                .key(id)
                .build();
        Files.write(tempFile, letter.getLetter(), StandardOpenOption.WRITE);
        AsyncRequestProvider asyncRequestProvider = AsyncRequestProvider.fromFile(tempFile);
        try(S3AsyncClient client = getClient()) {
            return client.putObject(request, asyncRequestProvider).whenComplete((putObjectResponse, throwable) -> {
                if(putObjectResponse == null) {
                    logger.error("Error saving LetterReceiverLetter to S3", throwable);
                }
                letterReceiverLetterDAO.markAsPublished(letter.getId());
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    logger.error("Error deleting temp file {}", tempFile.toAbsolutePath().toString(), e);
                }
            });
        }
    }

    private static final AwsCredentialsProvider AWS_CREDENTIALS_PROVIDER = new DefaultCredentialsProvider();
    private S3AsyncClient getClient() {
        return S3AsyncClient.builder()
                .region(region)
                .credentialsProvider(AWS_CREDENTIALS_PROVIDER)
                .build();
    }
}