package service

import (
	"context"
	"errors"
	"fmt"
	"net/url"
	"os"
	"strings"
	"time"

	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
	"github.com/sirupsen/logrus"
	"kraken-api/src/util"
)

// MinIOService encapsulates MinIO operations for object storage
type MinIOService struct {
	BucketName string
	Client     *minio.Client
	Endpoint   string
	UseSSL     bool
}

// MakeMinIOService creates a new MinIO service instance
func MakeMinIOService(bucketName, endpoint string) (*MinIOService, error) {
	username := os.Getenv("MINIO_ROOT_USER")
	password := os.Getenv("MINIO_ROOT_PASSWORD")

	minioClient, err := minio.New(endpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(username, password, ""),
		Secure: true,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to create MinIO client: %w", err)
	}

	ctx := context.Background()
	exists, err := minioClient.BucketExists(ctx, bucketName)
	if err != nil {
		return nil, fmt.Errorf("failed to check bucket existence: %w", err)
	}
	if !exists {
		return nil, fmt.Errorf("bucket %s does not exist", bucketName)
	}

	return &MinIOService{
		BucketName: bucketName,
		Client:     minioClient,
		Endpoint:   endpoint,
		UseSSL:     true,
	}, nil
}

// GetAllLatestVersion Returns the latest version for all plugins in MinIO.
func (m *MinIOService) GetAllLatestVersion() (map[string]string, error) {
	ctx := context.Background()
	var plugins = make(map[string]string)

	// List objects with prefix "plugins"
	objectCh := m.Client.ListObjects(ctx, m.BucketName, minio.ListObjectsOptions{
		Prefix:    "plugins",
		Recursive: true,
	})

	objectCount := 0
	for object := range objectCh {
		if object.Err != nil {
			return nil, fmt.Errorf("error listing objects: %w", object.Err)
		}

		objectCount++
		key := object.Key

		if !strings.HasSuffix(key, ".jar") {
			logrus.Debugf("skipping as plugin is not a jar: %s", key)
			continue
		}

		pluginNameVersion := ""
		if parts := strings.Split(key, "/"); len(parts) > 1 {
			pluginNameVersion = parts[len(parts)-1]
		}

		lastDash := strings.LastIndex(pluginNameVersion, "-")
		if lastDash == -1 {
			logrus.Infof("skipping as plugin cannot be parsed, no \"-\" between plugin name and version: %s", key)
			continue
		}

		pluginName := pluginNameVersion[:lastDash]

		version, err := util.ParseVersion(pluginNameVersion) // use pluginNameVersion, not full key
		if err != nil {
			logrus.Infof("unable to parse version for object: %s", key)
			continue
		}

		// Compare with existing version, if any
		currentVersionStr, exists := plugins[pluginName]
		if !exists {
			plugins[pluginName] = version.ToString()
			continue
		}

		currentVersion, err := util.ParseVersion(pluginName + "-" + currentVersionStr + ".jar")
		if err != nil {
			logrus.Warnf("invalid stored version for %s: %s", pluginName, currentVersionStr)
			plugins[pluginName] = version.ToString()
			continue
		}

		if version.IsGreaterThan(*currentVersion) {
			plugins[pluginName] = version.ToString()
		}
	}

	if objectCount == 0 {
		return nil, errors.New("no plugins found in the /plugins directory")
	}

	return plugins, nil
}

// GetLatestVersion Returns true if an object with a given prefix exists in MinIO, false otherwise.
func (m *MinIOService) GetLatestVersion(prefix string) (bool, string, error) {
	ctx := context.Background()

	// List objects with the given prefix
	objectCh := m.Client.ListObjects(ctx, m.BucketName, minio.ListObjectsOptions{
		Prefix:    prefix,
		Recursive: true,
	})

	// Extract the plugin name from the prefix (assume format is "plugins/PluginName")
	pluginName := prefix
	if parts := strings.Split(prefix, "/"); len(parts) > 1 {
		pluginName = parts[len(parts)-1]
	}

	// Find object with latest version
	var latestVersion *util.Version
	var latestKey string
	objectCount := 0

	for object := range objectCh {
		if object.Err != nil {
			return false, "", fmt.Errorf("error listing objects: %w", object.Err)
		}

		objectCount++
		key := object.Key

		// Skip this object if it doesn't match the exact plugin pattern
		baseName := strings.TrimSuffix(key, ".jar")

		// Split by dash to check the plugin name
		parts := strings.Split(baseName, "-")
		if len(parts) < 2 {
			logrus.Infof("skipping object with invalid format: %s", key)
			continue
		}

		// Get the object's plugin name part (everything before the versions)
		objPluginPath := strings.Join(parts[:len(parts)-1], "-")
		objPluginName := objPluginPath
		if pathParts := strings.Split(objPluginPath, "/"); len(pathParts) > 1 {
			objPluginName = pathParts[len(pathParts)-1]
		}

		// Only consider objects that match the exact plugin name
		if objPluginName != pluginName {
			logrus.Infof("skipping object with different plugin name: %s (looking for: %s)", objPluginName, pluginName)
			continue
		}

		version, err := util.ParseVersion(key)
		logrus.Debugf("version: %v for object key: %s", version, key)
		if err != nil {
			logrus.Infof("unable to parse version for object: %s", key)
			continue
		}

		if latestVersion == nil || version.IsGreaterThan(*latestVersion) {
			latestVersion = version
			latestKey = key
		}
	}

	if objectCount == 0 {
		logrus.Infof("object count is 0, skipping object with invalid format: %s", prefix)
		return false, "", nil
	}

	if latestKey == "" {
		logrus.Infof("latest key is blank, skipping object with invalid format: %s", prefix)
		return false, "", nil
	}

	// Return the object's key trimmed of its .jar extension
	name := strings.TrimSuffix(latestKey, ".jar")
	logrus.Debugf("latest version: %s for plugin: %s", name, prefix)
	return true, name, nil
}

// CreatePresignedUrl creates a presigned URL for downloading an object
func (m *MinIOService) CreatePresignedUrl(ctx context.Context, objectKey string, lifetimeSecs int64) (*url.URL, error) {
	expires := time.Duration(lifetimeSecs) * time.Second

	presignedURL, err := m.Client.PresignedGetObject(ctx, m.BucketName, objectKey, expires, nil)
	if err != nil {
		logrus.Errorf("Couldn't get a presigned URL to get %v:%v. err: %v",
			m.BucketName, objectKey, err)
		return nil, err
	}

	return presignedURL, nil
}

// PutObject creates a presigned URL for uploading an object
func (m *MinIOService) PutObject(ctx context.Context, bucketName string, objectKey string, lifetimeSecs int64) (*url.URL, error) {
	expires := time.Duration(lifetimeSecs) * time.Second

	presignedURL, err := m.Client.PresignedPutObject(ctx, bucketName, objectKey, expires)
	if err != nil {
		logrus.Errorf("Couldn't get a presigned URL to put %v:%v. err: %v",
			bucketName, objectKey, err)
		return nil, err
	}

	return presignedURL, nil
}

// DeleteObject creates a presigned URL for deleting an object (Note: MinIO doesn't support presigned DELETE)
// This method performs the delete operation directly instead
func (m *MinIOService) DeleteObject(ctx context.Context, bucketName string, objectKey string) error {
	err := m.Client.RemoveObject(ctx, bucketName, objectKey, minio.RemoveObjectOptions{})
	if err != nil {
		logrus.Errorf("Couldn't delete object %v. err: %v", objectKey, err)
		return err
	}

	logrus.Infof("Successfully deleted object %v from bucket %v", objectKey, bucketName)
	return nil
}

// MakeBucketPublic sets a bucket policy to make it publicly readable
func (m *MinIOService) MakeBucketPublic(ctx context.Context, bucketName string) error {
	// Policy that allows public read access to all objects in the bucket
	policy := fmt.Sprintf(`{
		"Version": "2012-10-17",
		"Statement": [
			{
				"Effect": "Allow",
				"Principal": {
					"AWS": ["*"]
				},
				"Action": ["s3:GetObject"],
				"Resource": ["arn:aws:s3:::%s/*"]
			}
		]
	}`, bucketName)

	err := m.Client.SetBucketPolicy(ctx, bucketName, policy)
	if err != nil {
		return fmt.Errorf("failed to set bucket policy for %s: %w", bucketName, err)
	}

	logrus.Infof("Successfully made bucket %s public\n", bucketName)
	return nil
}

// GetPublicURL returns the public URL for an object (when bucket is public)
func (m *MinIOService) GetPublicURL(objectKey string) string {
	protocol := "http"
	if m.UseSSL {
		protocol = "https"
	}
	return fmt.Sprintf("%s://%s/%s/%s", protocol, m.Endpoint, m.BucketName, objectKey)
}
