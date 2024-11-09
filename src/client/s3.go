package client

import (
	"context"
	"github.com/aws/aws-sdk-go-v2/config"
	"log"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	v4 "github.com/aws/aws-sdk-go-v2/aws/signer/v4"
	"github.com/aws/aws-sdk-go-v2/service/s3"
)

// Presigner encapsulates the Amazon Simple Storage Service (Amazon S3) presign actions
// used in the examples.
// It contains PresignClient, a client that is used to presign requests to Amazon S3.
// Presigned requests contain temporary credentials and can be made from any HTTP client.
type S3Service struct {
	BucketName    string
	PresignClient *s3.PresignClient
}

func MakeS3Service(bucketName string) (*S3Service, error) {
	cfg, err := config.LoadDefaultConfig(context.TODO(),
		//config.WithCredentialsProvider(),
		config.WithRegion("us-east-1"),
	)
	if err != nil {
		return nil, err
	}

	return &S3Service{
		BucketName:    bucketName,
		PresignClient: s3.NewPresignClient(s3.NewFromConfig(cfg)),
	}, nil
}

// GetObject makes a presigned request that can be used to get an object from a bucket.
// The presigned request is valid for the specified number of seconds.
func (p *S3Service) GetObject(ctx context.Context, objectKey string, lifetimeSecs int64) (*v4.PresignedHTTPRequest, error) {
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

func (p S3Service) PresignPostObject(ctx context.Context, bucketName string, objectKey string, lifetimeSecs int64) (*s3.PresignedPostRequest, error) {
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
