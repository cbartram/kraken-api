package service

import (
	"context"
	"errors"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider/types"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"go.uber.org/zap"
	"gorm.io/gorm"
	"kraken-api/src/model"
	"testing"
)

// MockCognitoClient is a mock implementation of the Cognito client
type MockCognitoClient struct {
	mock.Mock
}

type MockUserRepository struct {
	mock.Mock
}

func (m *MockUserRepository) GetUser(discordId string, db *gorm.DB) (*model.User, error) {
	args := m.Called(discordId, db)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.User), args.Error(1)
}

func (m *MockCognitoClient) AdminCreateUser(ctx context.Context, params *cognitoidentityprovider.AdminCreateUserInput, optFns ...func(*cognitoidentityprovider.Options)) (*cognitoidentityprovider.AdminCreateUserOutput, error) {
	args := m.Called(ctx, params)
	return args.Get(0).(*cognitoidentityprovider.AdminCreateUserOutput), args.Error(1)
}

func (m *MockCognitoClient) AdminSetUserPassword(ctx context.Context, params *cognitoidentityprovider.AdminSetUserPasswordInput, optFns ...func(*cognitoidentityprovider.Options)) (*cognitoidentityprovider.AdminSetUserPasswordOutput, error) {
	args := m.Called(ctx, params)
	return args.Get(0).(*cognitoidentityprovider.AdminSetUserPasswordOutput), args.Error(1)
}

func (m *MockCognitoClient) AdminInitiateAuth(ctx context.Context, params *cognitoidentityprovider.AdminInitiateAuthInput, optFns ...func(*cognitoidentityprovider.Options)) (*cognitoidentityprovider.AdminInitiateAuthOutput, error) {
	args := m.Called(ctx, params)
	return args.Get(0).(*cognitoidentityprovider.AdminInitiateAuthOutput), args.Error(1)
}

func (m *MockCognitoClient) AdminGetUser(ctx context.Context, params *cognitoidentityprovider.AdminGetUserInput, optFns ...func(*cognitoidentityprovider.Options)) (*cognitoidentityprovider.AdminGetUserOutput, error) {
	args := m.Called(ctx, params)
	return args.Get(0).(*cognitoidentityprovider.AdminGetUserOutput), args.Error(1)
}

// MockDB is a mock implementation of GORM DB
type MockDB struct {
	mock.Mock
}

// Helper function to create a test CognitoService with mocked client
func createTestCognitoService(mockClient *MockCognitoClient, mockRepo *MockUserRepository) *CognitoService {
	logger := zap.NewNop().Sugar()
	return &CognitoService{
		log:           logger,
		cognitoClient: mockClient,
		userRepo:      mockRepo,
		userPoolID:    "test-user-pool-id",
		clientID:      "test-client-id",
		clientSecret:  "test-client-secret",
	}
}

func TestCognitoService_CreateCognitoUser_Success(t *testing.T) {
	mockClient := &MockCognitoClient{}
	service := createTestCognitoService(mockClient, &MockUserRepository{})
	ctx := context.Background()

	// Test data
	createUserPayload := &model.CognitoCreateUserRequest{
		DiscordID:       "12345",
		DiscordUsername: "testuser",
		DiscordEmail:    "test@example.com",
	}

	// Expected auth result
	expectedAuthResult := &types.AuthenticationResultType{
		AccessToken:  aws.String("access-token"),
		RefreshToken: aws.String("refresh-token"),
		IdToken:      aws.String("id-token"),
		ExpiresIn:    *aws.Int32(3600),
	}

	// Mock AdminCreateUser
	mockClient.On("AdminCreateUser", ctx, mock.MatchedBy(func(input *cognitoidentityprovider.AdminCreateUserInput) bool {
		return *input.UserPoolId == "test-user-pool-id" &&
			*input.Username == "12345" &&
			input.MessageAction == types.MessageActionTypeSuppress
	})).Return(&cognitoidentityprovider.AdminCreateUserOutput{}, nil)

	// Mock AdminSetUserPassword
	mockClient.On("AdminSetUserPassword", ctx, mock.MatchedBy(func(input *cognitoidentityprovider.AdminSetUserPasswordInput) bool {
		return *input.UserPoolId == "test-user-pool-id" &&
			*input.Username == "12345" &&
			input.Permanent == true
	})).Return(&cognitoidentityprovider.AdminSetUserPasswordOutput{}, nil)

	// Mock AdminInitiateAuth for initiateAuthUserPass
	mockClient.On("AdminInitiateAuth", ctx, mock.MatchedBy(func(input *cognitoidentityprovider.AdminInitiateAuthInput) bool {
		return *input.UserPoolId == "test-user-pool-id" &&
			*input.ClientId == "test-client-id" &&
			input.AuthFlow == types.AuthFlowTypeAdminUserPasswordAuth
	})).Return(&cognitoidentityprovider.AdminInitiateAuthOutput{
		AuthenticationResult: expectedAuthResult,
	}, nil)

	// Execute test
	result, err := service.CreateCognitoUser(ctx, createUserPayload)

	// Assertions
	assert.NoError(t, err)
	assert.NotNil(t, result)
	assert.Equal(t, "access-token", *result.AccessToken)
	assert.Equal(t, "refresh-token", *result.RefreshToken)
	assert.Equal(t, "id-token", *result.IdToken)
	assert.Equal(t, int32(3600), result.ExpiresIn)

	mockClient.AssertExpectations(t)
}

func TestCognitoService_CreateCognitoUser_EmptyEmail(t *testing.T) {
	mockClient := &MockCognitoClient{}
	service := createTestCognitoService(mockClient, &MockUserRepository{})
	ctx := context.Background()

	// Test data with empty email
	createUserPayload := &model.CognitoCreateUserRequest{
		DiscordID:       "12345",
		DiscordUsername: "testuser",
		DiscordEmail:    "",
	}

	// Expected auth result
	expectedAuthResult := &types.AuthenticationResultType{
		AccessToken:  aws.String("access-token"),
		RefreshToken: aws.String("refresh-token"),
		IdToken:      aws.String("id-token"),
		ExpiresIn:    *aws.Int32(3600),
	}

	// Mock AdminCreateUser - should be called with generated email
	mockClient.On("AdminCreateUser", ctx, mock.MatchedBy(func(input *cognitoidentityprovider.AdminCreateUserInput) bool {
		// Check that email attribute contains "unknown-" and "@discord.com"
		for _, attr := range input.UserAttributes {
			if *attr.Name == "email" {
				email := *attr.Value
				return len(email) > 0 &&
					email[:8] == "unknown-" &&
					email[len(email)-12:] == "@discord.com"
			}
		}
		return false
	})).Return(&cognitoidentityprovider.AdminCreateUserOutput{}, nil)

	// Mock other calls
	mockClient.On("AdminSetUserPassword", ctx, mock.AnythingOfType("*cognitoidentityprovider.AdminSetUserPasswordInput")).Return(&cognitoidentityprovider.AdminSetUserPasswordOutput{}, nil)
	mockClient.On("AdminInitiateAuth", ctx, mock.AnythingOfType("*cognitoidentityprovider.AdminInitiateAuthInput")).Return(&cognitoidentityprovider.AdminInitiateAuthOutput{
		AuthenticationResult: expectedAuthResult,
	}, nil)

	// Execute test
	result, err := service.CreateCognitoUser(ctx, createUserPayload)

	// Assertions
	assert.NoError(t, err)
	assert.NotNil(t, result)
	mockClient.AssertExpectations(t)
}

func TestCognitoService_CreateCognitoUser_AdminCreateUserError(t *testing.T) {
	mockClient := &MockCognitoClient{}
	service := createTestCognitoService(mockClient, &MockUserRepository{})
	ctx := context.Background()

	createUserPayload := &model.CognitoCreateUserRequest{
		DiscordID:       "12345",
		DiscordUsername: "testuser",
		DiscordEmail:    "test@example.com",
	}

	// Mock AdminCreateUser to return error
	mockClient.On("AdminCreateUser", ctx, mock.AnythingOfType("*cognitoidentityprovider.AdminCreateUserInput")).Return((*cognitoidentityprovider.AdminCreateUserOutput)(nil), errors.New("cognito error"))

	// Execute test
	result, err := service.CreateCognitoUser(ctx, createUserPayload)

	// Assertions
	assert.Error(t, err)
	assert.Nil(t, result)
	assert.Contains(t, err.Error(), "error creating user")
	mockClient.AssertExpectations(t)
}

func TestCognitoService_CreateCognitoUser_SetPasswordError(t *testing.T) {
	mockClient := &MockCognitoClient{}
	service := createTestCognitoService(mockClient, &MockUserRepository{})
	ctx := context.Background()

	createUserPayload := &model.CognitoCreateUserRequest{
		DiscordID:       "12345",
		DiscordUsername: "testuser",
		DiscordEmail:    "test@example.com",
	}

	// Mock AdminCreateUser to succeed
	mockClient.On("AdminCreateUser", ctx, mock.AnythingOfType("*cognitoidentityprovider.AdminCreateUserInput")).Return(&cognitoidentityprovider.AdminCreateUserOutput{}, nil)

	// Mock AdminSetUserPassword to return error
	mockClient.On("AdminSetUserPassword", ctx, mock.AnythingOfType("*cognitoidentityprovider.AdminSetUserPasswordInput")).Return((*cognitoidentityprovider.AdminSetUserPasswordOutput)(nil), errors.New("set password error"))

	// Execute test
	result, err := service.CreateCognitoUser(ctx, createUserPayload)

	// Assertions
	assert.Error(t, err)
	assert.Nil(t, result)
	assert.Contains(t, err.Error(), "error setting permanent password")
	mockClient.AssertExpectations(t)
}

func TestCognitoService_RefreshSession_Success(t *testing.T) {
	mockClient := &MockCognitoClient{}
	service := createTestCognitoService(mockClient, &MockUserRepository{})
	ctx := context.Background()
	discordID := "12345"

	// Mock AdminGetUser
	userAttributes := []types.AttributeType{
		{
			Name:  aws.String("custom:temporary_password"),
			Value: aws.String("temp-password"),
		},
	}
	mockClient.On("AdminGetUser", ctx, mock.MatchedBy(func(input *cognitoidentityprovider.AdminGetUserInput) bool {
		return *input.UserPoolId == "test-user-pool-id" && *input.Username == discordID
	})).Return(&cognitoidentityprovider.AdminGetUserOutput{
		UserAttributes: userAttributes,
	}, nil)

	// Mock AdminInitiateAuth for initiateAuthUserPass
	expectedAuthResult := &types.AuthenticationResultType{
		AccessToken:  aws.String("new-access-token"),
		RefreshToken: aws.String("new-refresh-token"),
		IdToken:      aws.String("new-id-token"),
		ExpiresIn:    *aws.Int32(3600),
	}
	mockClient.On("AdminInitiateAuth", ctx, mock.MatchedBy(func(input *cognitoidentityprovider.AdminInitiateAuthInput) bool {
		return input.AuthFlow == types.AuthFlowTypeAdminUserPasswordAuth
	})).Return(&cognitoidentityprovider.AdminInitiateAuthOutput{
		AuthenticationResult: expectedAuthResult,
	}, nil)

	// Execute test
	result, err := service.RefreshSession(ctx, discordID)

	// Assertions
	assert.NoError(t, err)
	assert.NotNil(t, result)
	assert.Equal(t, "new-access-token", result.AccessToken)
	assert.Equal(t, "new-refresh-token", result.RefreshToken)
	assert.Equal(t, "new-id-token", result.IdToken)
	assert.Equal(t, int32(3600), result.TokenExpiration)

	mockClient.AssertExpectations(t)
}

func TestCognitoService_RefreshSession_GetUserError(t *testing.T) {
	mockClient := &MockCognitoClient{}
	service := createTestCognitoService(mockClient, &MockUserRepository{})
	ctx := context.Background()
	discordID := "12345"

	// Mock AdminGetUser to return error
	mockClient.On("AdminGetUser", ctx, mock.AnythingOfType("*cognitoidentityprovider.AdminGetUserInput")).Return((*cognitoidentityprovider.AdminGetUserOutput)(nil), errors.New("user not found"))

	// Execute test
	result, err := service.RefreshSession(ctx, discordID)

	// Assertions
	assert.Error(t, err)
	assert.Nil(t, result)
	assert.Contains(t, err.Error(), "error: failed to get user for discord id")
	mockClient.AssertExpectations(t)
}

func TestCognitoService_RefreshSession_AuthError(t *testing.T) {
	mockClient := &MockCognitoClient{}
	service := createTestCognitoService(mockClient, &MockUserRepository{})
	ctx := context.Background()
	discordID := "12345"

	// Mock AdminGetUser to succeed
	userAttributes := []types.AttributeType{
		{
			Name:  aws.String("custom:temporary_password"),
			Value: aws.String("temp-password"),
		},
	}
	mockClient.On("AdminGetUser", ctx, mock.AnythingOfType("*cognitoidentityprovider.AdminGetUserInput")).Return(&cognitoidentityprovider.AdminGetUserOutput{
		UserAttributes: userAttributes,
	}, nil)

	// Mock AdminInitiateAuth to return error
	mockClient.On("AdminInitiateAuth", ctx, mock.AnythingOfType("*cognitoidentityprovider.AdminInitiateAuthInput")).Return((*cognitoidentityprovider.AdminInitiateAuthOutput)(nil), errors.New("auth failed"))

	// Execute test
	result, err := service.RefreshSession(ctx, discordID)

	// Assertions
	assert.Error(t, err)
	assert.Nil(t, result)
	assert.Contains(t, err.Error(), "error: failed to auth with user/pass for discord id")
	mockClient.AssertExpectations(t)
}

func TestCognitoService_AuthUser_Success(t *testing.T) {
	mockClient := &MockCognitoClient{}
	mockRepo := &MockUserRepository{}
	service := createTestCognitoService(mockClient, mockRepo)
	ctx := context.Background()
	refreshToken := "refresh-token"
	userId := "12345"

	// Create a mock DB - you can use a real *gorm.DB or create a more sophisticated mock
	var mockDB *gorm.DB

	// Mock user to be returned
	expectedUser := &model.User{
		DiscordID: userId,
		// Add other fields as needed
	}

	// Mock AdminInitiateAuth
	expectedAuthResult := &types.AuthenticationResultType{
		AccessToken: aws.String("new-access-token"),
		IdToken:     aws.String("new-id-token"),
		ExpiresIn:   *aws.Int32(3600),
	}
	mockClient.On("AdminInitiateAuth", ctx, mock.MatchedBy(func(input *cognitoidentityprovider.AdminInitiateAuthInput) bool {
		return *input.UserPoolId == "test-user-pool-id" &&
			*input.ClientId == "test-client-id" &&
			input.AuthFlow == types.AuthFlowTypeRefreshTokenAuth &&
			input.AuthParameters["REFRESH_TOKEN"] == refreshToken
	})).Return(&cognitoidentityprovider.AdminInitiateAuthOutput{
		AuthenticationResult: expectedAuthResult,
	}, nil)

	// Mock GetUser
	mockRepo.On("GetUser", userId, mockDB).Return(expectedUser, nil)

	// Execute test
	result, err := service.AuthUser(ctx, &refreshToken, &userId, mockDB)

	// Assertions
	assert.NoError(t, err)
	assert.NotNil(t, result)
	assert.Equal(t, userId, result.DiscordID)
	assert.Equal(t, "new-access-token", result.Credentials.AccessToken)
	assert.Equal(t, refreshToken, result.Credentials.RefreshToken)
	assert.Equal(t, "new-id-token", result.Credentials.IdToken)
	assert.Equal(t, int32(3600), result.Credentials.TokenExpiration)

	mockClient.AssertExpectations(t)
	mockRepo.AssertExpectations(t)
}
func TestCognitoService_AuthUser_AuthError(t *testing.T) {
	mockClient := &MockCognitoClient{}
	service := createTestCognitoService(mockClient, &MockUserRepository{})
	ctx := context.Background()
	refreshToken := "refresh-token"
	userId := "12345"

	var mockDB *gorm.DB

	// Mock AdminInitiateAuth to return error
	mockClient.On("AdminInitiateAuth", ctx, mock.AnythingOfType("*cognitoidentityprovider.AdminInitiateAuthInput")).Return((*cognitoidentityprovider.AdminInitiateAuthOutput)(nil), errors.New("auth failed"))

	// Execute test - commented out since model.GetUser needs mocking
	result, err := service.AuthUser(ctx, &refreshToken, &userId, mockDB)

	assert.Error(t, err)
	assert.Nil(t, result)
	assert.Contains(t, err.Error(), "auth failed")

	mockClient.AssertExpectations(t)
}

// Benchmark tests
func BenchmarkCognitoService_CreateCognitoUser(b *testing.B) {
	mockClient := &MockCognitoClient{}
	service := createTestCognitoService(mockClient, &MockUserRepository{})
	ctx := context.Background()

	createUserPayload := &model.CognitoCreateUserRequest{
		DiscordID:       "12345",
		DiscordUsername: "testuser",
		DiscordEmail:    "test@example.com",
	}

	expectedAuthResult := &types.AuthenticationResultType{
		AccessToken:  aws.String("access-token"),
		RefreshToken: aws.String("refresh-token"),
		IdToken:      aws.String("id-token"),
		ExpiresIn:    *aws.Int32(3600),
	}

	// Set up mocks for benchmark
	mockClient.On("AdminCreateUser", mock.Anything, mock.Anything).Return(&cognitoidentityprovider.AdminCreateUserOutput{}, nil)
	mockClient.On("AdminSetUserPassword", mock.Anything, mock.Anything).Return(&cognitoidentityprovider.AdminSetUserPasswordOutput{}, nil)
	mockClient.On("AdminInitiateAuth", mock.Anything, mock.Anything).Return(&cognitoidentityprovider.AdminInitiateAuthOutput{
		AuthenticationResult: expectedAuthResult,
	}, nil)

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_, _ = service.CreateCognitoUser(ctx, createUserPayload)
	}
}

// Table-driven test for different email scenarios
func TestCognitoService_CreateCognitoUser_EmailScenarios(t *testing.T) {
	tests := []struct {
		name  string
		email string
		want  string
	}{
		{
			name:  "valid email",
			email: "user@example.com",
			want:  "user@example.com",
		},
		{
			name:  "empty email",
			email: "",
			want:  "unknown-", // should start with this
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mockClient := &MockCognitoClient{}
			service := createTestCognitoService(mockClient, &MockUserRepository{})
			ctx := context.Background()

			createUserPayload := &model.CognitoCreateUserRequest{
				DiscordID:       "12345",
				DiscordUsername: "testuser",
				DiscordEmail:    tt.email,
			}

			expectedAuthResult := &types.AuthenticationResultType{
				AccessToken:  aws.String("access-token"),
				RefreshToken: aws.String("refresh-token"),
				IdToken:      aws.String("id-token"),
				ExpiresIn:    *aws.Int32(3600),
			}

			// Mock the calls
			mockClient.On("AdminCreateUser", ctx, mock.AnythingOfType("*cognitoidentityprovider.AdminCreateUserInput")).Return(&cognitoidentityprovider.AdminCreateUserOutput{}, nil)
			mockClient.On("AdminSetUserPassword", ctx, mock.AnythingOfType("*cognitoidentityprovider.AdminSetUserPasswordInput")).Return(&cognitoidentityprovider.AdminSetUserPasswordOutput{}, nil)
			mockClient.On("AdminInitiateAuth", ctx, mock.AnythingOfType("*cognitoidentityprovider.AdminInitiateAuthInput")).Return(&cognitoidentityprovider.AdminInitiateAuthOutput{
				AuthenticationResult: expectedAuthResult,
			}, nil)

			result, err := service.CreateCognitoUser(ctx, createUserPayload)

			assert.NoError(t, err)
			assert.NotNil(t, result)
			mockClient.AssertExpectations(t)
		})
	}
}
