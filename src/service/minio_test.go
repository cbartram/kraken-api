package service

import (
	"context"
	"errors"
	"net/url"
	"os"
	"testing"
	"time"

	"github.com/minio/minio-go/v7"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

// Mock MinIO client interface
type MockMinIOClient struct {
	mock.Mock
}

func (m *MockMinIOClient) BucketExists(ctx context.Context, bucketName string) (bool, error) {
	args := m.Called(ctx, bucketName)
	return args.Bool(0), args.Error(1)
}

func (m *MockMinIOClient) ListObjects(ctx context.Context, bucketName string, opts minio.ListObjectsOptions) <-chan minio.ObjectInfo {
	args := m.Called(ctx, bucketName, opts)
	return args.Get(0).(<-chan minio.ObjectInfo)
}

func (m *MockMinIOClient) PresignedGetObject(ctx context.Context, bucketName, objectName string, expiry time.Duration, reqParams url.Values) (*url.URL, error) {
	args := m.Called(ctx, bucketName, objectName, expiry, reqParams)
	return args.Get(0).(*url.URL), args.Error(1)
}

func (m *MockMinIOClient) PresignedPutObject(ctx context.Context, bucketName, objectName string, expiry time.Duration) (*url.URL, error) {
	args := m.Called(ctx, bucketName, objectName, expiry)
	return args.Get(0).(*url.URL), args.Error(1)
}

func (m *MockMinIOClient) RemoveObject(ctx context.Context, bucketName, objectName string, opts minio.RemoveObjectOptions) error {
	args := m.Called(ctx, bucketName, objectName, opts)
	return args.Error(0)
}

// Mock util.Version for testing
type MockVersion struct {
	version string
}

func (v *MockVersion) ToString() string {
	return v.version
}

func (v *MockVersion) IsGreaterThan(other MockVersion) bool {
	return v.version > other.version
}

func setEnvVarsMinio(t *testing.T, username, password string) func() {
	originalUsername := os.Getenv("MINIO_ROOT_USER")
	originalPassword := os.Getenv("MINIO_ROOT_PASSWORD")

	_ = os.Setenv("MINIO_ROOT_USER", username)
	_ = os.Setenv("MINIO_ROOT_PASSWORD", password)

	return func() {
		_ = os.Setenv("MINIO_ROOT_USER", originalUsername)
		_ = os.Setenv("MINIO_ROOT_PASSWORD", originalPassword)
	}
}

func createObjectChannel(objects []minio.ObjectInfo) <-chan minio.ObjectInfo {
	ch := make(chan minio.ObjectInfo, len(objects))
	for _, obj := range objects {
		ch <- obj
	}
	close(ch)
	return ch
}

func TestMakeMinIOService(t *testing.T) {
	tests := []struct {
		name         string
		bucketName   string
		endpoint     string
		username     string
		password     string
		bucketExists bool
		bucketError  error
		expectError  bool
		errorMsg     string
	}{
		{
			name:         "successful creation with existing bucket",
			bucketName:   "test-bucket",
			endpoint:     "localhost:9000",
			username:     "admin",
			password:     "password123",
			bucketExists: true,
			expectError:  false,
		},
		{
			name:         "bucket does not exist",
			bucketName:   "nonexistent-bucket",
			endpoint:     "localhost:9000",
			username:     "admin",
			password:     "password123",
			bucketExists: false,
			expectError:  true,
			errorMsg:     "bucket nonexistent-bucket does not exist",
		},
		{
			name:        "bucket check error",
			bucketName:  "test-bucket",
			endpoint:    "localhost:9000",
			username:    "admin",
			password:    "password123",
			bucketError: errors.New("connection error"),
			expectError: true,
			errorMsg:    "failed to check bucket existence",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cleanup := setEnvVarsMinio(t, tt.username, tt.password)
			defer cleanup()

			logger := createTestLogger(t)
			mockClient := &MockMinIOClient{}

			// Set up mock expectations based on test case
			if tt.bucketError != nil {
				mockClient.On("BucketExists", mock.Anything, tt.bucketName).Return(false, tt.bucketError)
			} else {
				mockClient.On("BucketExists", mock.Anything, tt.bucketName).Return(tt.bucketExists, nil)
			}

			service, err := MakeMinIOService(tt.bucketName, tt.endpoint, mockClient, logger)

			// Assert the results
			if tt.expectError {
				assert.Error(t, err)
				assert.Contains(t, err.Error(), tt.errorMsg)
				assert.Nil(t, service) // Service should be nil when there's an error
			} else {
				assert.NoError(t, err)
				assert.NotNil(t, service) // Service should be created successfully
			}

			// Verify all mock expectations were met
			mockClient.AssertExpectations(t)
		})
	}
}

func TestMinIOService_GetAllLatestVersion(t *testing.T) {
	tests := []struct {
		name           string
		objects        []minio.ObjectInfo
		expectError    bool
		errorMsg       string
		expectedResult map[string]string
	}{
		{
			name: "successful retrieval with multiple plugins",
			objects: []minio.ObjectInfo{
				{Key: "plugins/plugin1-1.0.0.jar"},
				{Key: "plugins/plugin1-1.1.0.jar"},
				{Key: "plugins/plugin2-2.0.0.jar"},
				{Key: "plugins/plugin3-0.5.0.jar"},
			},
			expectError: false,
			expectedResult: map[string]string{
				"plugin1": "1.1.0",
				"plugin2": "2.0.0",
				"plugin3": "0.5.0",
			},
		},
		{
			name:        "no plugins found",
			objects:     []minio.ObjectInfo{},
			expectError: true,
			errorMsg:    "no plugins found in the /plugins directory",
		},
		{
			name: "mixed file types - only jars counted",
			objects: []minio.ObjectInfo{
				{Key: "plugins/plugin1-1.0.0.jar"},
				{Key: "plugins/readme.txt"},
				{Key: "plugins/config.json"},
			},
			expectError: false,
			expectedResult: map[string]string{
				"plugin1": "1.0.0",
			},
		},
		{
			name: "invalid plugin names - no dash",
			objects: []minio.ObjectInfo{
				{Key: "plugins/invalidplugin.jar"},
				{Key: "plugins/plugin1-1.0.0.jar"},
			},
			expectError: false,
			expectedResult: map[string]string{
				"plugin1": "1.0.0",
			},
		},
		{
			name: "list objects error",
			objects: []minio.ObjectInfo{
				{Key: "plugins/plugin1-1.0.0.jar", Err: errors.New("list error")},
			},
			expectError: true,
			errorMsg:    "error listing objects",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			logger := createTestLogger(t)
			mockClient := &MockMinIOClient{}
			service := &MinIOService{
				Client:     mockClient,
				BucketName: "test-bucket",
				log:        logger,
			}

			// Mock the ListObjects call
			objectCh := createObjectChannel(tt.objects)
			mockClient.On("ListObjects", mock.Anything, "test-bucket", mock.MatchedBy(func(opts minio.ListObjectsOptions) bool {
				return opts.Prefix == "plugins" && opts.Recursive == true
			})).Return(objectCh)

			// Call the function under test
			result, err := service.GetAllLatestVersion()

			// Assert the results
			if tt.expectError {
				assert.Error(t, err)
				assert.Contains(t, err.Error(), tt.errorMsg)
				assert.Nil(t, result) // Result should be nil when there's an error
			} else {
				assert.NoError(t, err)
				assert.NotNil(t, result)
				assert.Equal(t, tt.expectedResult, result)
			}

			// Verify all mock expectations were met
			mockClient.AssertExpectations(t)
		})
	}
}

func TestMinIOService_CreatePresignedUrl(t *testing.T) {
	tests := []struct {
		name         string
		objectKey    string
		lifetimeSecs int64
		mockURL      *url.URL
		mockError    error
		expectError  bool
	}{
		{
			name:         "successful presigned URL creation",
			objectKey:    "plugins/test-plugin-1.0.0.jar",
			lifetimeSecs: 3600,
			mockURL:      &url.URL{Scheme: "https", Host: "example.com", Path: "/test"},
			expectError:  false,
		},
		{
			name:         "MinIO client error",
			objectKey:    "plugins/test-plugin-1.0.0.jar",
			lifetimeSecs: 3600,
			mockError:    errors.New("presigned URL error"),
			expectError:  true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			logger := createTestLogger(t)
			mockClient := &MockMinIOClient{}

			service := &MinIOService{
				log:        logger,
				BucketName: "test-bucket",
				Client:     mockClient,
			}

			ctx := context.Background()
			expectedDuration := time.Duration(tt.lifetimeSecs) * time.Second

			mockClient.On("PresignedGetObject", ctx, "test-bucket", tt.objectKey, expectedDuration, mock.AnythingOfType("url.Values")).
				Return(tt.mockURL, tt.mockError)

			result, err := service.CreatePresignedUrl(ctx, tt.objectKey, tt.lifetimeSecs)

			if tt.expectError {
				assert.Error(t, err)
				assert.Nil(t, result)
			} else {
				assert.NoError(t, err)
				assert.Equal(t, tt.mockURL, result)
			}

			mockClient.AssertExpectations(t)
		})
	}
}

func TestMinIOService_PutObject(t *testing.T) {
	tests := []struct {
		name         string
		bucketName   string
		objectKey    string
		lifetimeSecs int64
		mockURL      *url.URL
		mockError    error
		expectError  bool
	}{
		{
			name:         "successful presigned PUT URL creation",
			bucketName:   "upload-bucket",
			objectKey:    "plugins/new-plugin-1.0.0.jar",
			lifetimeSecs: 1800,
			mockURL:      &url.URL{Scheme: "https", Host: "example.com", Path: "/upload"},
			expectError:  false,
		},
		{
			name:         "MinIO client error",
			bucketName:   "upload-bucket",
			objectKey:    "plugins/new-plugin-1.0.0.jar",
			lifetimeSecs: 1800,
			mockError:    errors.New("presigned PUT error"),
			expectError:  true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			logger := createTestLogger(t)
			mockClient := &MockMinIOClient{}

			service := &MinIOService{
				log:        logger,
				BucketName: "test-bucket",
				Client:     mockClient,
			}

			ctx := context.Background()
			expectedDuration := time.Duration(tt.lifetimeSecs) * time.Second

			mockClient.On("PresignedPutObject", ctx, tt.bucketName, tt.objectKey, expectedDuration).
				Return(tt.mockURL, tt.mockError)

			result, err := service.PutObject(ctx, tt.bucketName, tt.objectKey, tt.lifetimeSecs)

			if tt.expectError {
				assert.Error(t, err)
				assert.Nil(t, result)
			} else {
				assert.NoError(t, err)
				assert.Equal(t, tt.mockURL, result)
			}

			mockClient.AssertExpectations(t)
		})
	}
}

func TestMinIOService_DeleteObject(t *testing.T) {
	tests := []struct {
		name        string
		bucketName  string
		objectKey   string
		mockError   error
		expectError bool
	}{
		{
			name:        "successful object deletion",
			bucketName:  "test-bucket",
			objectKey:   "plugins/old-plugin-1.0.0.jar",
			expectError: false,
		},
		{
			name:        "MinIO client error",
			bucketName:  "test-bucket",
			objectKey:   "plugins/old-plugin-1.0.0.jar",
			mockError:   errors.New("delete error"),
			expectError: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			logger := createTestLogger(t)
			mockClient := &MockMinIOClient{}

			service := &MinIOService{
				log:        logger,
				BucketName: "test-bucket",
				Client:     mockClient,
			}

			ctx := context.Background()

			mockClient.On("RemoveObject", ctx, tt.bucketName, tt.objectKey, mock.AnythingOfType("minio.RemoveObjectOptions")).
				Return(tt.mockError)

			err := service.DeleteObject(ctx, tt.bucketName, tt.objectKey)

			if tt.expectError {
				assert.Error(t, err)
			} else {
				assert.NoError(t, err)
			}

			mockClient.AssertExpectations(t)
		})
	}
}

// Benchmark tests
func BenchmarkMinIOService_CreatePresignedUrl(b *testing.B) {
	logger := createTestLogger(&testing.T{})
	mockClient := &MockMinIOClient{}

	service := &MinIOService{
		log:        logger,
		BucketName: "test-bucket",
		Client:     mockClient,
	}

	ctx := context.Background()
	testURL := &url.URL{Scheme: "https", Host: "example.com", Path: "/test"}

	mockClient.On("PresignedGetObject", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).
		Return(testURL, nil)

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_, err := service.CreatePresignedUrl(ctx, "test-object", 3600)
		if err != nil {
			b.Fatal(err)
		}
	}
}
