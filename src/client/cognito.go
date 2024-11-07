package client

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider/types"
	"os"
	"path/filepath"
)

// CognitoAuthManager handles AWS Cognito authentication operations
type CognitoAuthManager struct {
	cognitoClient *cognitoidentityprovider.Client
	userPoolID    string
	clientID      string
	configPath    string
}

// SessionData represents locally stored session information
type SessionData struct {
	RefreshToken string `json:"refresh_token"`
}

// NewCognitoAuthManager creates a new instance of CognitoAuthManager
func NewCognitoAuthManager(cfg aws.Config, userPoolID, clientID string) *CognitoAuthManager {
	return &CognitoAuthManager{
		cognitoClient: cognitoidentityprovider.NewFromConfig(cfg),
		userPoolID:    userPoolID,
		clientID:      clientID,
		configPath:    filepath.Join(os.Getenv("HOME"), ".config", "your-app", "session.json"),
	}
}

// HandleDiscordAuth processes Discord OAuth results and manages Cognito user creation/session
func (m *CognitoAuthManager) HandleDiscordAuth(ctx context.Context, discordID, email string) error {

	// Try to find existing user
	_, err := m.cognitoClient.AdminGetUser(ctx, &cognitoidentityprovider.AdminGetUserInput{
		UserPoolId: aws.String(m.userPoolID),
		Username:   aws.String(discordID),
	})

	if err != nil {
		var notFoundErr *types.UserNotFoundException
		if errors.As(err, &notFoundErr) {
			// User doesn't exist, create new user
			return m.createCognitoUser(ctx, discordID, email)
		}
		return fmt.Errorf("error checking user existence: %w", err)
	}

	// User exists, refresh session
	return m.refreshUserSession(ctx, discordID)
}

func (m *CognitoAuthManager) createCognitoUser(ctx context.Context, discordID, email string) error {
	// Create user attributes
	attributes := []types.AttributeType{
		{
			Name:  aws.String("email"),
			Value: aws.String(email),
		},
		{
			Name:  aws.String("custom:discord_id"),
			Value: aws.String(discordID),
		},
	}

	// Create user in Cognito
	password, _ := MakeSecurePassword().GeneratePassword(PasswordConfig{
		Length:         15,
		RequireUpper:   true,
		RequireLower:   true,
		RequireNumber:  true,
		RequireSpecial: true,
	})

	_, err := m.cognitoClient.AdminCreateUser(ctx, &cognitoidentityprovider.AdminCreateUserInput{
		UserPoolId:        aws.String(m.userPoolID),
		Username:          aws.String(discordID),
		UserAttributes:    attributes,
		MessageAction:     types.MessageActionTypeSuppress,
		TemporaryPassword: aws.String(password),
	})

	if err != nil {
		return fmt.Errorf("error creating user: %w", err)
	}

	// Set permanent password
	_, err = m.cognitoClient.AdminSetUserPassword(ctx, &cognitoidentityprovider.AdminSetUserPasswordInput{
		UserPoolId: aws.String(m.userPoolID),
		Username:   aws.String(discordID),
		Password:   aws.String(password),
		Permanent:  true,
	})
	if err != nil {
		return fmt.Errorf("error setting permanent password: %w", err)
	}

	// Initialize auth session
	return m.initiateAuth(ctx, discordID, password)
}

func (m *CognitoAuthManager) initiateAuth(ctx context.Context, discordID, password string) error {
	authParams := map[string]string{
		"USERNAME": discordID,
		"PASSWORD": password,
	}

	result, err := m.cognitoClient.AdminInitiateAuth(ctx, &cognitoidentityprovider.AdminInitiateAuthInput{
		UserPoolId:     aws.String(m.userPoolID),
		ClientId:       aws.String(m.clientID),
		AuthFlow:       types.AuthFlowTypeAdminUserPasswordAuth,
		AuthParameters: authParams,
	})
	if err != nil {
		return fmt.Errorf("error initiating auth: %w", err)
	}

	return m.storeRefreshToken(result.AuthenticationResult.RefreshToken)
}

// VerifyUserSession checks if the current session is valid
func (m *CognitoAuthManager) VerifyUserSession(ctx context.Context) bool {
	refreshToken, err := m.loadRefreshToken()
	if err != nil {
		return false
	}

	_, err = m.cognitoClient.AdminInitiateAuth(ctx, &cognitoidentityprovider.AdminInitiateAuthInput{
		UserPoolId: aws.String(m.userPoolID),
		ClientId:   aws.String(m.clientID),
		AuthFlow:   types.AuthFlowTypeRefreshTokenAuth,
		AuthParameters: map[string]string{
			"REFRESH_TOKEN": *refreshToken,
		},
	})

	return err == nil
}

func (m *CognitoAuthManager) refreshUserSession(ctx context.Context, discordID string) error {
	// TODO BIG TODO
	//password := retrieveSecurePassword(discordID) // Implement this function
	return m.initiateAuth(ctx, discordID, "foo")
}

func (m *CognitoAuthManager) storeRefreshToken(refreshToken *string) error {
	if refreshToken == nil {
		return errors.New("refresh token is nil")
	}

	// Ensure directory exists
	dir := filepath.Dir(m.configPath)
	if err := os.MkdirAll(dir, 0700); err != nil {
		return fmt.Errorf("error creating config directory: %w", err)
	}

	data := SessionData{
		RefreshToken: *refreshToken,
	}

	jsonData, err := json.Marshal(data)
	if err != nil {
		return fmt.Errorf("error marshaling session data: %w", err)
	}

	return os.WriteFile(m.configPath, jsonData, 0600)
}

func (m *CognitoAuthManager) loadRefreshToken() (*string, error) {
	data, err := os.ReadFile(m.configPath)
	if err != nil {
		return nil, fmt.Errorf("error reading session file: %w", err)
	}

	var sessionData SessionData
	if err := json.Unmarshal(data, &sessionData); err != nil {
		return nil, fmt.Errorf("error unmarshaling session data: %w", err)
	}

	return &sessionData.RefreshToken, nil
}
