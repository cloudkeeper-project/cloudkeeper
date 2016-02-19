package xyz.cloudkeeper.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.util.ArrayList;
import java.util.List;

final class S3Utilities {
    /**
     * Maximum number of keys that will be deleted together.
     *
     * <p>{@link DeleteObjectsRequest} specifies that at most 1000 keys can be deleted at once.
     */
    private static final int BULK_DELETE_SIZE = 1000;

    private S3Utilities() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    static void deletePrefix(AmazonS3 s3Client, String bucketName, String prefix) {
        List<DeleteObjectsRequest.KeyVersion> trashKeys = new ArrayList<>();
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
        for (S3ObjectSummary summary: S3Objects.withPrefix(s3Client, bucketName, prefix)) {
            trashKeys.add(new DeleteObjectsRequest.KeyVersion(summary.getKey()));
            if (trashKeys.size() == BULK_DELETE_SIZE) {
                deleteObjectsRequest.setKeys(trashKeys);
                s3Client.deleteObjects(deleteObjectsRequest);
                trashKeys.clear();
            }
        }
        if (!trashKeys.isEmpty()) {
            deleteObjectsRequest.setKeys(trashKeys);
            s3Client.deleteObjects(deleteObjectsRequest);
        }
    }
}
