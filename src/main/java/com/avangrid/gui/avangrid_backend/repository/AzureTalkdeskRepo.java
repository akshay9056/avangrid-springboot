package com.avangrid.gui.avangrid_backend.repository;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.models.TaggedBlobItem;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

//import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class AzureTalkdeskRepo {

    @Value("${azure.storage.account-name}")
    private String storageAccountName;

    @Value("${azure.storage.talkdesk.container-name}")
    private String containerName;

    @Value("${azure.client-id}")
    private String clientId;

    @Value("${azure.client-secret}")
    private String clientSecret;

    @Value("${azure.tenant-id}")
    private String tenantId;

    private BlobServiceClient blobServiceClient;
    @Getter
    private String nextContinuationToken;

    public void initializeBlobServiceClient() {
        try {
            log.info("Initializing Azure Blob Service Client with Service Principal authentication");

            // Create Service Principal credential
            TokenCredential credential = new ClientSecretCredentialBuilder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .tenantId(tenantId)
                    .build();

            // Create BlobServiceClient with Service Principal authentication
            String endpoint = String.format("https://%s.blob.core.windows.net", storageAccountName);

            this.blobServiceClient = new BlobServiceClientBuilder()
                    .endpoint(endpoint)
                    .credential(credential)
                    .buildClient();

            log.info("Azure Blob Service Client initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize Azure Blob Service Client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Azure Blob Service Client", e);
        }
    }

    public List<Map<String, Object>> fetchBlobMetadata(String query, Integer pageSize, String continuationToken) {
        try {
            log.info("Fetching blob metadata with query: {}", query);

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            // Use Azure Blob Storage tags to filter blobs
            List<TaggedBlobItem> taggedBlobs = findBlobsByTags(containerClient, query, pageSize, continuationToken);

            if (taggedBlobs.isEmpty()) {
                log.info("No blobs found matching the query criteria");
                return Collections.emptyList();
            }

            List<Map<String, Object>> metadataList = new ArrayList<>();

            for (TaggedBlobItem taggedBlob : taggedBlobs) {
                try {
                    BlobClient blobClient = containerClient.getBlobClient(taggedBlob.getName());
                    BlobProperties properties = blobClient.getProperties();

                    Map<String, String> metadata = properties.getMetadata();
                    if (metadata != null && !metadata.isEmpty()) {
                        // Convert metadata to Map<String, Object> for JSON serialization
                        Map<String, Object> metadataMap = metadata.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> (Object) entry.getValue()
                                ));

                        // Add blob name and other properties if needed
                        metadataMap.put("blobName", taggedBlob.getName());
                        metadataMap.put("lastModified", properties.getLastModified());
                        metadataMap.put("contentLength", properties.getBlobSize());

                        metadataList.add(metadataMap);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch metadata for blob: {}, error: {}",
                            taggedBlob.getName(), e.getMessage());
                }
            }

            log.info("Successfully fetched metadata for {} blobs", metadataList.size());
            return metadataList;

        } catch (Exception e) {
            log.error("Error fetching blob metadata: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch blob metadata from Azure Storage: " + e.getMessage(), e);
        }
    }

    private List<TaggedBlobItem> findBlobsByTags(BlobContainerClient containerClient,
                                                 String query,
                                                 Integer pageSize,
                                                 String continuationToken) {
        try {
            List<TaggedBlobItem> results = new ArrayList<>();

            // Use Azure Blob Storage findBlobsByTags method
            containerClient.findBlobsByTags(query)
                    .iterableByPage(continuationToken, pageSize)
                    .forEach(page -> {
                        results.addAll(page.getValue());
                        nextContinuationToken = page.getContinuationToken();
                    });

            return results;

        } catch (Exception e) {
            log.error("Error finding blobs by tags: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to find blobs by tags: " + e.getMessage(), e);
        }
    }

//    public String getNextContinuationToken() {
//        return nextContinuationToken;
//    }

    // Alternative method using ListBlobsOptions if tags are not available
    public List<BlobItem> listBlobsWithFilters(String prefix, Integer pageSize, String continuationToken) {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            ListBlobsOptions options = new ListBlobsOptions()
                    .setMaxResultsPerPage(pageSize)
                    .setPrefix(prefix);

            List<BlobItem> results = new ArrayList<>();

            containerClient.listBlobs(options, null)
                    .iterableByPage(continuationToken, pageSize)
                    .forEach(page -> {
                        results.addAll(page.getValue());
                        nextContinuationToken = page.getContinuationToken();
                    });

            return results;

        } catch (Exception e) {
            log.error("Error listing blobs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list blobs: " + e.getMessage(), e);
        }
    }

    // Method to test the connection
    public boolean testConnection() {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            containerClient.exists();
            log.info("Connection test successful for container: {}", containerName);
            return true;
        } catch (Exception e) {
            log.error("Connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }
}