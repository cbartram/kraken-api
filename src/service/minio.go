package service

import (
	"context"
	"errors"
	"fmt"
	"go.uber.org/zap"
	"net/url"
	"strings"
	"time"

	"github.com/minio/minio-go/v7"
	"kraken-api/src/util"
)

type MinioClient interface {
	BucketExists(ctx context.Context, bucketName string) (bool, error)
	ListObjects(ctx context.Context, bucketName string, opts minio.ListObjectsOptions) <-chan minio.ObjectInfo
	PresignedGetObject(ctx context.Context, bucketName, objectName string, expiry time.Duration, reqParams url.Values) (*url.URL, error)
	PresignedPutObject(ctx context.Context, bucketName, objectName string, expiry time.Duration) (*url.URL, error)
	RemoveObject(ctx context.Context, bucketName, objectName string, opts minio.RemoveObjectOptions) error
}

// MinIOService encapsulates MinIO operations for object storage
type MinIOService struct {
	log        *zap.SugaredLogger
	BucketName string
	Client     MinioClient
	Endpoint   string
	UseSSL     bool
}

// MakeMinIOService creates a new MinIO service instance
func MakeMinIOService(bucketName, endpoint string, client MinioClient, log *zap.SugaredLogger) (*MinIOService, error) {
	ctx := context.Background()
	exists, err := client.BucketExists(ctx, bucketName)
	if err != nil {
		return nil, fmt.Errorf("failed to check bucket existence: %w", err)
	}
	if !exists {
		return nil, fmt.Errorf("bucket %s does not exist", bucketName)
	}

	log.Infof("MinIO connection establish successfully")

	return &MinIOService{
		log:        log,
		BucketName: bucketName,
		Client:     client,
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
			m.log.Debugf("skipping as plugin is not a jar: %s", key)
			continue
		}

		pluginNameVersion := ""
		if parts := strings.Split(key, "/"); len(parts) > 1 {
			pluginNameVersion = parts[len(parts)-1]
		}

		lastDash := strings.LastIndex(pluginNameVersion, "-")
		if lastDash == -1 {
			m.log.Infof("skipping as plugin cannot be parsed, no \"-\" between plugin name and version: %s", key)
			continue
		}

		pluginName := pluginNameVersion[:lastDash]

		version, err := util.ParseVersion(pluginNameVersion) // use pluginNameVersion, not full key
		if err != nil {
			m.log.Errorf("unable to parse version for object: %s", key)
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
			m.log.Warnf("invalid stored version for %s: %s", pluginName, currentVersionStr)
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
			m.log.Infof("skipping object with invalid format: %s", key)
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
			m.log.Infof("skipping object with different plugin name: %s (looking for: %s)", objPluginName, pluginName)
			continue
		}

		version, err := util.ParseVersion(key)
		m.log.Debugf("version: %v for object key: %s", version, key)
		if err != nil {
			m.log.Infof("unable to parse version for object: %s", key)
			continue
		}

		if latestVersion == nil || version.IsGreaterThan(*latestVersion) {
			latestVersion = version
			latestKey = key
		}
	}

	if objectCount == 0 {
		m.log.Infof("object count is 0, skipping object with invalid format: %s", prefix)
		return false, "", nil
	}

	if latestKey == "" {
		m.log.Infof("latest key is blank, skipping object with invalid format: %s", prefix)
		return false, "", nil
	}

	// Return the object's key trimmed of its .jar extension
	name := strings.TrimSuffix(latestKey, ".jar")
	m.log.Debugf("latest version: %s for plugin: %s", name, prefix)
	return true, name, nil
}

// CreatePresignedUrl creates a presigned URL for downloading an object
func (m *MinIOService) CreatePresignedUrl(ctx context.Context, objectKey string, lifetimeSecs int64) (*url.URL, error) {
	expires := time.Duration(lifetimeSecs) * time.Second

	presignedURL, err := m.Client.PresignedGetObject(ctx, m.BucketName, objectKey, expires, nil)
	if err != nil {
		m.log.Errorf("failed to get a presigned URL to for plugin: %v:%v. err: %v",
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
		m.log.Errorf("Couldn't get a presigned URL to put %v:%v. err: %v",
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
		m.log.Errorf("failed to delete object %v. err: %v", objectKey, err)
		return err
	}

	m.log.Infof("successfully deleted object %v from bucket %v", objectKey, bucketName)
	return nil
}
