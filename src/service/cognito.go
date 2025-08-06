package service

import (
	"context"
	"errors"
	"fmt"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider/types"
	"go.uber.org/zap"
	"gorm.io/gorm"
	"kraken-api/src/model"
	"kraken-api/src/util"
	"os"
)

type CognitoClient interface {
	AdminCreateUser(ctx context.Context, params *cognitoidentityprovider.AdminCreateUserInput, optFns ...func(*cognitoidentityprovider.Options)) (*cognitoidentityprovider.AdminCreateUserOutput, error)
	AdminSetUserPassword(ctx context.Context, params *cognitoidentityprovider.AdminSetUserPasswordInput, optFns ...func(*cognitoidentityprovider.Options)) (*cognitoidentityprovider.AdminSetUserPasswordOutput, error)
	AdminInitiateAuth(ctx context.Context, params *cognitoidentityprovider.AdminInitiateAuthInput, optFns ...func(*cognitoidentityprovider.Options)) (*cognitoidentityprovider.AdminInitiateAuthOutput, error)
	AdminGetUser(ctx context.Context, params *cognitoidentityprovider.AdminGetUserInput, optFns ...func(*cognitoidentityprovider.Options)) (*cognitoidentityprovider.AdminGetUserOutput, error)
	AdminAddUserToGroup(ctx context.Context, params *cognitoidentityprovider.AdminAddUserToGroupInput, optFns ...func(*cognitoidentityprovider.Options)) (*cognitoidentityprovider.AdminAddUserToGroupOutput, error)
	AdminListGroupsForUser(ctx context.Context, params *cognitoidentityprovider.AdminListGroupsForUserInput, optFns ...func(*cognitoidentityprovider.Options)) (*cognitoidentityprovider.AdminListGroupsForUserOutput, error)
}

// CognitoService handles AWS Cognito authentication operations
type CognitoService struct {
	Log           *zap.SugaredLogger
	CognitoClient CognitoClient
	UserRepo      model.UserRepository
	UserPoolID    string
	ClientID      string
	ClientSecret  string
}

// MakeCognitoService creates a new instance of CognitoAuthManager
func MakeCognitoService(log *zap.SugaredLogger, userRepository *model.DefaultUserRepository) *CognitoService {
	cfg, err := config.LoadDefaultConfig(context.TODO())
	if err != nil {
		log.Errorf("error loading default aws config: %s", err)
	}

	return &CognitoService{
		Log:           log,
		CognitoClient: cognitoidentityprovider.NewFromConfig(cfg),
		UserRepo:      userRepository,
		UserPoolID:    os.Getenv("USER_POOL_ID"),
		ClientID:      os.Getenv("COGNITO_CLIENT_ID"),
		ClientSecret:  os.Getenv("COGNITO_CLIENT_SECRET"),
	}
}

func (c *CognitoService) CreateCognitoUser(ctx context.Context, createUserPayload *model.CognitoCreateUserRequest) (*types.AuthenticationResultType, error) {
	password, _ := util.MakeCrypto().GeneratePassword(util.PasswordConfig{
		Length:         15,
		RequireUpper:   true,
		RequireLower:   true,
		RequireNumber:  true,
		RequireSpecial: true,
	})

	if createUserPayload.DiscordEmail == "" || &createUserPayload.DiscordEmail == nil {
		c.Log.Infof("no discord email or email provided setting to default value")
		hash, _ := util.MakeCrypto().GeneratePassword(util.PasswordConfig{
			Length:        10,
			RequireUpper:  true,
			RequireLower:  true,
			RequireNumber: true,
		})
		createUserPayload.DiscordEmail = fmt.Sprintf("unknown-%s@discord.com", hash)
	}

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

	_, err := c.CognitoClient.AdminCreateUser(ctx, &cognitoidentityprovider.AdminCreateUserInput{
		UserPoolId:        aws.String(c.UserPoolID),
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
	_, err = c.CognitoClient.AdminSetUserPassword(ctx, &cognitoidentityprovider.AdminSetUserPasswordInput{
		UserPoolId: aws.String(c.UserPoolID),
		Username:   aws.String(createUserPayload.DiscordID),
		Password:   aws.String(password),
		Permanent:  true,
	})
	if err != nil {
		return nil, fmt.Errorf("error setting permanent password: %w", err)
	}

	_, err = c.CognitoClient.AdminAddUserToGroup(ctx, &cognitoidentityprovider.AdminAddUserToGroupInput{
		UserPoolId: aws.String(c.UserPoolID),
		Username:   aws.String(createUserPayload.DiscordID),
		GroupName:  aws.String("user"),
	})

	if err != nil {
		c.Log.Errorf("failed to add user %s to 'user' group: %v", createUserPayload.DiscordID, err)
	} else {
		c.Log.Infof("successfully added user %s to 'user' group", createUserPayload.DiscordUsername)
	}

	// Initialize auth session
	return c.InitiateAuthUserPass(ctx, createUserPayload.DiscordID, password)
}

// InitiateAuthUserPass Happens when a user is initially created with the user pool and uses username + generated pass to login
// The cognito refresh token and access token will be returned in the response along with the discord refresh and access
// token.
func (c *CognitoService) InitiateAuthUserPass(ctx context.Context, discordID, password string) (*types.AuthenticationResultType, error) {
	authParams := map[string]string{
		"USERNAME":    discordID,
		"PASSWORD":    password,
		"SECRET_HASH": util.MakeCrypto().MakeCognitoSecretHash(discordID, c.ClientID, c.ClientSecret),
	}

	result, err := c.CognitoClient.AdminInitiateAuth(ctx, &cognitoidentityprovider.AdminInitiateAuthInput{
		UserPoolId:     aws.String(c.UserPoolID),
		ClientId:       aws.String(c.ClientID),
		AuthFlow:       types.AuthFlowTypeAdminUserPasswordAuth,
		AuthParameters: authParams,
	})
	if err != nil {
		return nil, fmt.Errorf("error initiating sale user/pass auth with user pool: %w", err)
	}

	return result.AuthenticationResult, nil
}

// RefreshSession This method is called when a refresh token is about to expire and a new one needs to be generated.
// There is no direct way to get a new refresh token without a users password. Since we do not store the password we set
// must reset the password and re-auth to get a new refresh token.
func (c *CognitoService) RefreshSession(ctx context.Context, discordID string) (*model.CognitoCredentials, error) {
	user, err := c.CognitoClient.AdminGetUser(ctx, &cognitoidentityprovider.AdminGetUserInput{
		UserPoolId: aws.String(c.UserPoolID),
		Username:   &discordID,
	})

	if err != nil {
		c.Log.Errorf("error: failed to get user attributes with for discord id: %s", discordID)
		return nil, errors.New(fmt.Sprintf("error: failed to get user for discord id: %s", discordID))
	}

	password := util.GetUserAttributeString(user.UserAttributes, "custom:temporary_password")

	c.Log.Infof("auth user: %s with password", discordID)
	auth, err := c.InitiateAuthUserPass(ctx, discordID, password)

	if err != nil {
		c.Log.Errorf("error: failed to auth with user/pass for discord id: %s", discordID)
		return nil, errors.New(fmt.Sprintf("error: failed to auth with user/pass for discord id: %s", discordID))
	}

	return &model.CognitoCredentials{
		RefreshToken:    *auth.RefreshToken,
		TokenExpiration: auth.ExpiresIn,
		AccessToken:     *auth.AccessToken,
		IdToken:         *auth.IdToken,
	}, nil

}

func (c *CognitoService) AuthUser(ctx context.Context, refreshToken, userId *string, db *gorm.DB) (*model.User, error) {
	auth, err := c.CognitoClient.AdminInitiateAuth(ctx, &cognitoidentityprovider.AdminInitiateAuthInput{
		UserPoolId: aws.String(c.UserPoolID),
		ClientId:   aws.String(c.ClientID),
		AuthFlow:   types.AuthFlowTypeRefreshTokenAuth,
		AuthParameters: map[string]string{
			"REFRESH_TOKEN": *refreshToken,
			"SECRET_HASH":   util.MakeCrypto().MakeCognitoSecretHash(*userId, c.ClientID, c.ClientSecret),
		},
	})

	if err != nil {
		c.Log.Errorf("error auth: user %s could not be authenticated: %v", *userId, err)
		return nil, err
	}

	user, err := c.UserRepo.GetUser(*userId, db)
	if err != nil {
		c.Log.Errorf("could not get user from db: %v", err)
		return nil, err
	}

	// Synchronizes a users groups in cognito with the database
	groups, err := c.GetUserGroupDetails(ctx, *userId)
	if err != nil {
		c.Log.Errorf("could not get user groups: %v", err)
		return nil, err
	}

	// Build lookup maps
	cognitoGroupMap := make(map[string]bool)
	for _, group := range groups {
		cognitoGroupMap[*group.GroupName] = true
	}

	dbGroupMap := make(map[string]model.Group)
	for _, group := range user.Groups {
		dbGroupMap[group.GroupName] = group
	}

	// Add missing Cognito groups to DB
	for _, group := range groups {
		if _, exists := dbGroupMap[*group.GroupName]; !exists {
			err := c.UserRepo.AddUserToGroup(user.ID, *group.GroupName, db)
			if err != nil {
				c.Log.Errorf("error adding user %d to group %s: %v", user.ID, *group.GroupName, err)
				return nil, err
			}
			c.Log.Infof("added user: %s to group: %s", user.DiscordUsername, *group.GroupName)
		}
	}

	// Remove DB groups not in Cognito
	for groupName, group := range dbGroupMap {
		if !cognitoGroupMap[groupName] {
			err := c.UserRepo.RemoveUserFromGroup(group.ID, db)
			if err != nil {
				c.Log.Errorf("error removing user %d from group %s: %v", user.ID, groupName, err)
				return nil, err
			}
			c.Log.Infof("removed user: %s from group: %s", user.DiscordUsername, groupName)
		}
	}

	// Clear and re-populate in memory groups
	user.Groups = []model.Group{}
	for _, group := range groups {
		user.Groups = append(user.Groups, model.Group{
			UserID:    user.ID,
			GroupName: *group.GroupName,
		})
	}

	user.Credentials = model.CognitoCredentials{
		AccessToken:     *auth.AuthenticationResult.AccessToken,
		RefreshToken:    *refreshToken,
		TokenExpiration: auth.AuthenticationResult.ExpiresIn,
		IdToken:         *auth.AuthenticationResult.IdToken,
	}

	return user, nil
}

func (c *CognitoService) IsUserInGroup(ctx context.Context, userId, groupName string) (bool, error) {
	groupsResp, err := c.CognitoClient.AdminListGroupsForUser(ctx, &cognitoidentityprovider.AdminListGroupsForUserInput{
		UserPoolId: aws.String(c.UserPoolID),
		Username:   aws.String(userId),
	})
	if err != nil {
		return false, fmt.Errorf("error getting groups for user %s: %w", userId, err)
	}

	for _, group := range groupsResp.Groups {
		if group.GroupName != nil && *group.GroupName == groupName {
			return true, nil
		}
	}

	return false, nil
}

func (c *CognitoService) GetUserGroupDetails(ctx context.Context, userId string) ([]types.GroupType, error) {
	groupsResp, err := c.CognitoClient.AdminListGroupsForUser(ctx, &cognitoidentityprovider.AdminListGroupsForUserInput{
		UserPoolId: aws.String(c.UserPoolID),
		Username:   aws.String(userId),
	})
	if err != nil {
		return nil, fmt.Errorf("error getting groups for user %s: %w", userId, err)
	}

	return groupsResp.Groups, nil
}
