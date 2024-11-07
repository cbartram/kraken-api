package client

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider/types"
	log "github.com/sirupsen/logrus"
	"kraken-api/src/model"
	"os"
	"path/filepath"
)

// CognitoAuthManager handles AWS Cognito authentication operations
type CognitoAuthManager struct {
	cognitoClient *cognitoidentityprovider.Client
	userPoolID    string
	clientID      string
	clientSecret  string
	configPath    string
}

// SessionData represents locally stored session information
type SessionData struct {
	RefreshToken string `json:"refresh_token"`
}

// MakeCognitoAuthManager creates a new instance of CognitoAuthManager
func MakeCognitoAuthManager() *CognitoAuthManager {
	cfg, err := config.LoadDefaultConfig(context.TODO())
	if err != nil {
		log.Errorf("error loading default aws config: %s", err)
	}

	return &CognitoAuthManager{
		cognitoClient: cognitoidentityprovider.NewFromConfig(cfg),
		userPoolID:    os.Getenv("USER_POOL_ID"),
		clientID:      os.Getenv("COGNITO_CLIENT_ID"),
		clientSecret:  os.Getenv("COGNITO_CLIENT_SECRET"),
		configPath:    filepath.Join(os.Getenv("HOME"), ".config", "your-app", "session.json"),
	}
}

// DoesUserExist Checks the user pool for the existence of a user with a given discord ID.
func (m *CognitoAuthManager) DoesUserExist(ctx context.Context, discordID string) bool {
	log.Infof("checking user-pool for user with discord id: %s", discordID)
	// Try to find existing user
	_, err := m.cognitoClient.AdminGetUser(ctx, &cognitoidentityprovider.AdminGetUserInput{
		UserPoolId: aws.String(m.userPoolID),
		Username:   aws.String(discordID),
	})

	if err != nil {
		var notFoundErr *types.UserNotFoundException
		if errors.As(err, &notFoundErr) {
			log.Infof("user with discord ID: %s not found in user pool. creating new user.", discordID)
			return false
		}
		log.Error(fmt.Errorf("error checking user existence: %w", err))
		return false
	}
	return true
}

func (m *CognitoAuthManager) CreateCognitoUser(ctx context.Context, discordID, discordUsername, email string) (*types.AuthenticationResultType, error) {
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
		{
			Name:  aws.String("custom:discord_username"),
			Value: aws.String(discordUsername),
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
		return nil, fmt.Errorf("error creating user: %w", err)
	}

	// Set permanent password although users will never actually log in with a user/pass combo. The Kraken client will use the Cognito refresh token
	// to try and get an access token for the user and authenticate with the access token.
	_, err = m.cognitoClient.AdminSetUserPassword(ctx, &cognitoidentityprovider.AdminSetUserPasswordInput{
		UserPoolId: aws.String(m.userPoolID),
		Username:   aws.String(discordID),
		Password:   aws.String(password),
		Permanent:  true,
	})
	if err != nil {
		return nil, fmt.Errorf("error setting permanent password: %w", err)
	}

	// Initialize auth session
	return m.initiateAuthUserPass(ctx, discordID, password)
}

// initiateAuthUserPass Happens when a user is initially created with the user pool and uses username + generated pass to login
// The cognito refresh token and access token will be returned in the response along with the discord refresh and access
// token.
func (m *CognitoAuthManager) initiateAuthUserPass(ctx context.Context, discordID, password string) (*types.AuthenticationResultType, error) {
	usernameClientID := discordID + m.clientID
	hash := hmac.New(sha256.New, []byte(m.clientSecret))
	hash.Write([]byte(usernameClientID))
	digest := hash.Sum(nil)

	// Encode the digest to base64
	secretHash := base64.StdEncoding.EncodeToString(digest)

	authParams := map[string]string{
		"USERNAME":    discordID,
		"PASSWORD":    password,
		"SECRET_HASH": secretHash,
	}

	result, err := m.cognitoClient.AdminInitiateAuth(ctx, &cognitoidentityprovider.AdminInitiateAuthInput{
		UserPoolId:     aws.String(m.userPoolID),
		ClientId:       aws.String(m.clientID),
		AuthFlow:       types.AuthFlowTypeAdminUserPasswordAuth,
		AuthParameters: authParams,
	})
	if err != nil {
		return nil, fmt.Errorf("error initiating admin user/pass auth with user pool: %w", err)
	}

	return result.AuthenticationResult, nil
}

func (m *CognitoAuthManager) AuthUser(ctx context.Context, refreshToken, username *string) (bool, *model.CognitoUser) {
	auth, err := m.cognitoClient.AdminInitiateAuth(ctx, &cognitoidentityprovider.AdminInitiateAuthInput{
		UserPoolId: aws.String(m.userPoolID),
		ClientId:   aws.String(m.clientID),
		AuthFlow:   types.AuthFlowTypeRefreshTokenAuth,
		AuthParameters: map[string]string{
			"REFRESH_TOKEN": *refreshToken,
		},
	})

	if err != nil {
		log.Errorf("user with refresh token: %s could not be authenticated: %s", *refreshToken, err.Error())
		return false, nil
	}

	user, err := m.cognitoClient.AdminGetUser(ctx, &cognitoidentityprovider.AdminGetUserInput{
		UserPoolId: aws.String(m.userPoolID),
		Username:   username,
	})

	if err != nil {
		log.Errorf("could not get user with username: %s", *username, err.Error())
		return false, nil
	}

	if !user.Enabled {
		log.Warnf("user with username: %s found but not enabled.", *username)
		return false, nil
	}

	var email, discordID, discordUsername, cognitoID string
	for _, attr := range user.UserAttributes {
		switch aws.ToString(attr.Name) {
		case "email":
			email = aws.ToString(attr.Value)
		case "sub":
			cognitoID = aws.ToString(attr.Value)
		case "custom:discord_id":
			discordID = aws.ToString(attr.Value)
		case "custom:discord_username":
			discordUsername = aws.ToString(attr.Value)
		}
	}

	return true, &model.CognitoUser{
		DiscordUsername: discordUsername,
		DiscordID:       discordID,
		Email:           email,
		CognitoID:       cognitoID,
		Credentials: model.CognitoCredentials{
			AccessToken:  *auth.AuthenticationResult.AccessToken,
			RefreshToken: *auth.AuthenticationResult.RefreshToken,
		},
	}
}

// TODO needs to be done on the kraken client java side
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

// TODO needs to be done on java side
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
