package service

import (
	"context"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/sirupsen/logrus"
	"kraken-api/src/util"
	"log"
	"strings"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	v4 "github.com/aws/aws-sdk-go-v2/aws/signer/v4"
	"github.com/aws/aws-sdk-go-v2/service/s3"
)

// Presigner encapsulates the Amazon Simple Storage Service (Amazon S3) presign actions
// used in the examples.
// It contains PresignClient, a service that is used to presign requests to Amazon S3.
// Presigned requests contain temporary credentials and can be made from any HTTP service.
type S3Service struct {
	BucketName    string
	S3Client      *s3.Client
	PresignClient *s3.PresignClient
}

func MakeS3Service(bucketName string) (*S3Service, error) {
	cfg, err := config.LoadDefaultConfig(context.TODO(),
		config.WithRegion("us-east-1"),
	)
	if err != nil {
		return nil, err
	}

	s3Client := s3.NewFromConfig(cfg)
	return &S3Service{
		BucketName:    bucketName,
		S3Client:      s3Client,
		PresignClient: s3.NewPresignClient(s3Client),
	}, nil
}

// GetLatestVersion Returns true if an object with a given prefix exists in the S3 bucket, false otherwise. This
// function automatically trims any *.jar extension off of the S3 object name if it exists.
func (p *S3Service) GetLatestVersion(prefix string) (bool, string, error) {
	input := &s3.ListObjectsV2Input{
		Bucket: aws.String(p.BucketName),
		Prefix: aws.String(prefix),
	}

	result, err := p.S3Client.ListObjectsV2(context.TODO(), input)
	if err != nil {
		return false, "", err
	}

	if len(result.Contents) == 0 {
		return false, "", nil
	}

	// Extract the plugin name from the prefix (assume format is "plugins/PluginName")
	pluginName := prefix
	if parts := strings.Split(prefix, "/"); len(parts) > 1 {
		pluginName = parts[len(parts)-1]
	}

	// Find object with latest version
	var latestVersion *util.Version
	var latestKey string

	for _, obj := range result.Contents {
		key := *obj.Key
		// Skip this object if it doesn't match the exact plugin pattern
		// Check if the key is in the format: prefix-version.jar or prefix-version
		baseName := strings.TrimSuffix(key, ".jar")

		// Split by dash to check the plugin name
		parts := strings.Split(baseName, "-")
		if len(parts) < 2 {
			logrus.Infof("skipping object with invalid format: %s", key)
			continue
		}

		// Get the object's plugin name part (everything before the versions)
		// Handle case where prefix is "plugins/PluginName"
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
			// Skip objects with invalid version format
			logrus.Infof("unable to parse version for object: %s", key)
			continue
		}

		if latestVersion == nil || version.IsGreaterThan(*latestVersion) {
			latestVersion = version
			latestKey = key
		}
	}

	if latestKey == "" {
		return false, "", nil
	}

	// Return the object's key trimmed of its .jar extension
	name := strings.TrimSuffix(latestKey, ".jar")
	logrus.Debugf("latest version: %s for plugin: %s", name, prefix)
	return true, name, nil
}

// CreatePresignedUrl makes a presigned request that can be used to get an object from a bucket.
// The presigned request is valid for the specified number of seconds.
func (p *S3Service) CreatePresignedUrl(ctx context.Context, objectKey string, lifetimeSecs int64) (*v4.PresignedHTTPRequest, error) {
	request, err := p.PresignClient.PresignGetObject(ctx, &s3.GetObjectInput{
		Bucket: aws.String(p.BucketName),
		Key:    aws.String(objectKey),
	}, func(opts *s3.PresignOptions) {
		opts.Expires = time.Duration(lifetimeSecs * int64(time.Second))
	})
	if err != nil {
		log.Printf("Couldn't get a presigned request to get %v:%v. Here's why: %v\n",
			p.BucketName, objectKey, err)
	}
	return request, err
}

// PutObject makes a presigned request that can be used to put an object in a bucket.
// The presigned request is valid for the specified number of seconds.
func (p *S3Service) PutObject(
	ctx context.Context, bucketName string, objectKey string, lifetimeSecs int64) (*v4.PresignedHTTPRequest, error) {
	request, err := p.PresignClient.PresignPutObject(ctx, &s3.PutObjectInput{
		Bucket: aws.String(bucketName),
		Key:    aws.String(objectKey),
	}, func(opts *s3.PresignOptions) {
		opts.Expires = time.Duration(lifetimeSecs * int64(time.Second))
	})
	if err != nil {
		log.Printf("Couldn't get a presigned request to put %v:%v. Here's why: %v\n",
			bucketName, objectKey, err)
	}
	return request, err
}

// DeleteObject makes a presigned request that can be used to delete an object from a bucket.
func (p *S3Service) DeleteObject(ctx context.Context, bucketName string, objectKey string) (*v4.PresignedHTTPRequest, error) {
	request, err := p.PresignClient.PresignDeleteObject(ctx, &s3.DeleteObjectInput{
		Bucket: aws.String(bucketName),
		Key:    aws.String(objectKey),
	})
	if err != nil {
		log.Printf("Couldn't get a presigned request to delete object %v. Here's why: %v\n", objectKey, err)
	}
	return request, err
}

func (p *S3Service) PresignPostObject(ctx context.Context, bucketName string, objectKey string, lifetimeSecs int64) (*s3.PresignedPostRequest, error) {
	request, err := p.PresignClient.PresignPostObject(ctx, &s3.PutObjectInput{
		Bucket: aws.String(bucketName),
		Key:    aws.String(objectKey),
	}, func(options *s3.PresignPostOptions) {
		options.Expires = time.Duration(lifetimeSecs) * time.Second
	})
	if err != nil {
		log.Printf("Couldn't get a presigned post request to put %v:%v. Here's why: %v\n", bucketName, objectKey, err)
	}
	return request, nil
}
