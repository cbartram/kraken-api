package service

import (
	"context"
	"errors"
	"fmt"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider/types"
	log "github.com/sirupsen/logrus"
	"gorm.io/gorm"
	"kraken-api/src/model"
	"kraken-api/src/util"
	"os"
)

// CognitoService handles AWS Cognito authentication operations
type CognitoService struct {
	cognitoClient *cognitoidentityprovider.Client
	userPoolID    string
	clientID      string
	clientSecret  string
}

// MakeCognitoService creates a new instance of CognitoAuthManager
func MakeCognitoService() *CognitoService {
	cfg, err := config.LoadDefaultConfig(context.TODO())
	if err != nil {
		log.Errorf("error loading default aws config: %s", err)
	}

	return &CognitoService{
		cognitoClient: cognitoidentityprovider.NewFromConfig(cfg),
		userPoolID:    os.Getenv("USER_POOL_ID"),
		clientID:      os.Getenv("COGNITO_CLIENT_ID"),
		clientSecret:  os.Getenv("COGNITO_CLIENT_SECRET"),
	}
}

func (m *CognitoService) CreateCognitoUser(ctx context.Context, createUserPayload *model.CognitoCreateUserRequest) (*types.AuthenticationResultType, error) {
	password, _ := util.MakeCrypto().GeneratePassword(util.PasswordConfig{
		Length:         15,
		RequireUpper:   true,
		RequireLower:   true,
		RequireNumber:  true,
		RequireSpecial: true,
	})

	attributes := []types.AttributeType{
		{
			Name:  aws.String("email"),
			Value: aws.String(createUserPayload.DiscordEmail),
		},
		{
			Name:  aws.String("custom:discord_id"),
			Value: aws.String(createUserPayload.DiscordID),
		},
		{
			Name:  aws.String("custom:discord_username"),
			Value: aws.String(createUserPayload.DiscordUsername),
		},
		{
			Name:  aws.String("custom:temporary_password"),
			Value: aws.String(password),
		},
	}

	_, err := m.cognitoClient.AdminCreateUser(ctx, &cognitoidentityprovider.AdminCreateUserInput{
		UserPoolId:        aws.String(m.userPoolID),
		Username:          aws.String(createUserPayload.DiscordID),
		UserAttributes:    attributes,
		MessageAction:     types.MessageActionTypeSuppress,
		TemporaryPassword: aws.String(password),
	})

	if err != nil {
		return nil, fmt.Errorf("error creating user: %w", err)
	}

	// Set a permanent password although users will never actually log in with a user/pass combo. The Kraken service will use the Cognito refresh token
	// to try and get an access token for the user and authenticate with the access token.
	_, err = m.cognitoClient.AdminSetUserPassword(ctx, &cognitoidentityprovider.AdminSetUserPasswordInput{
		UserPoolId: aws.String(m.userPoolID),
		Username:   aws.String(createUserPayload.DiscordID),
		Password:   aws.String(password),
		Permanent:  true,
	})
	if err != nil {
		return nil, fmt.Errorf("error setting permanent password: %w", err)
	}

	// Initialize auth session
	return m.initiateAuthUserPass(ctx, createUserPayload.DiscordID, password)
}

// initiateAuthUserPass Happens when a user is initially created with the user pool and uses username + generated pass to login
// The cognito refresh token and access token will be returned in the response along with the discord refresh and access
// token.
func (m *CognitoService) initiateAuthUserPass(ctx context.Context, discordID, password string) (*types.AuthenticationResultType, error) {
	authParams := map[string]string{
		"USERNAME":    discordID,
		"PASSWORD":    password,
		"SECRET_HASH": util.MakeCrypto().MakeCognitoSecretHash(discordID, m.clientID, m.clientSecret),
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

// RefreshSession This method is called when a refresh token is about to expire and a new one needs to be generated.
// There is no direct way to get a new refresh token without a users password. Since we do not store the password we set
// must reset the password and re-auth to get a new refresh token.
func (m *CognitoService) RefreshSession(ctx context.Context, discordID string) (*model.CognitoCredentials, error) {
	user, err := m.cognitoClient.AdminGetUser(ctx, &cognitoidentityprovider.AdminGetUserInput{
		UserPoolId: aws.String(m.userPoolID),
		Username:   &discordID,
	})

	if err != nil {
		log.Errorf("error: failed to get user attributes with for discord id: %s", discordID)
		return nil, errors.New(fmt.Sprintf("error: failed to get user for discord id: %s", discordID))
	}

	password := util.GetUserAttributeString(user.UserAttributes, "custom:temporary_password")

	log.Infof("auth user: %s with password", discordID)
	auth, err := m.initiateAuthUserPass(ctx, discordID, password)

	if err != nil {
		log.Errorf("error: failed to auth with user/pass for discord id: %s", discordID)
		return nil, errors.New(fmt.Sprintf("error: failed to auth with user/pass for discord id: %s", discordID))
	}

	return &model.CognitoCredentials{
		RefreshToken:    *auth.RefreshToken,
		TokenExpiration: auth.ExpiresIn,
		AccessToken:     *auth.AccessToken,
		IdToken:         *auth.IdToken,
	}, nil

}

func (m *CognitoService) AuthUser(ctx context.Context, refreshToken, userId *string, db *gorm.DB) (*model.User, error) {
	auth, err := m.cognitoClient.AdminInitiateAuth(ctx, &cognitoidentityprovider.AdminInitiateAuthInput{
		UserPoolId: aws.String(m.userPoolID),
		ClientId:   aws.String(m.clientID),
		AuthFlow:   types.AuthFlowTypeRefreshTokenAuth,
		AuthParameters: map[string]string{
			"REFRESH_TOKEN": *refreshToken,
			"SECRET_HASH":   util.MakeCrypto().MakeCognitoSecretHash(*userId, m.clientID, m.clientSecret),
		},
	})

	if err != nil {
		log.Errorf("error auth: user %s could not be authenticated: %v", *userId, err)
		return nil, err
	}

	user, err := model.GetUser(*userId, db)
	if err != nil {
		log.Errorf("could not get user from db: %v", err)
		return nil, err
	}

	user.Credentials = model.CognitoCredentials{
		AccessToken:     *auth.AuthenticationResult.AccessToken,
		RefreshToken:    *refreshToken,
		TokenExpiration: auth.AuthenticationResult.ExpiresIn,
		IdToken:         *auth.AuthenticationResult.IdToken,
	}

	return user, nil
}
