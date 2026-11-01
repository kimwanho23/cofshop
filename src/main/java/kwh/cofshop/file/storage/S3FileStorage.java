package kwh.cofshop.file.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

// S3 호환 API를 쓰는 스토리지(AWS S3, Oracle Object Storage, NCP Object Storage, MinIO 등)
@Component
@ConditionalOnProperty(name = "file.storage", havingValue = "s3")
public class S3FileStorage implements FileStorage {

    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable"; // 파일명이 UUID라 내용이 바뀌지 않음

    private final S3Client s3Client;
    private final String bucket;
    private final String publicUrl;

    public S3FileStorage(
            @Value("${file.s3.bucket}") String bucket,
            @Value("${file.s3.public-url}") String publicUrl,
            @Value("${file.s3.region}") String region,
            @Value("${file.s3.endpoint:}") String endpoint,
            @Value("${file.s3.access-key:}") String accessKey,
            @Value("${file.s3.secret-key:}") String secretKey,
            @Value("${file.s3.path-style:false}") boolean pathStyle
    ) {
        this.bucket = bucket;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;

        S3ClientBuilder builder = S3Client.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .region(Region.of(region))
                .credentialsProvider(credentials(accessKey, secretKey))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build())
                // 호환 스토리지 일부는 SDK 기본값인 추가 체크섬을 지원하지 않음
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        this.s3Client = builder.build();
    }

    private AwsCredentialsProvider credentials(String accessKey, String secretKey) {
        if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        return DefaultCredentialsProvider.builder().build();
    }

    @Override
    public void store(String storeFileName, MultipartFile file, String contentType) throws IOException {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storeFileName)
                .contentType(contentType)
                .cacheControl(CACHE_CONTROL)
                .build();
        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
        }
    }

    @Override
    public void delete(String storeFileName) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storeFileName).build());
    }

    @Override
    public String getUrl(String storeFileName) {
        return publicUrl + "/" + storeFileName;
    }
}
