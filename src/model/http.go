package model

// Request represents the expected request body
type Request struct {
	Code string `json:"code"`
}

// DiscordTokenResponse represents Discord's token response
type DiscordTokenResponse struct {
	AccessToken  string `json:"access_token"`
	TokenType    string `json:"token_type"`
	ExpiresIn    int    `json:"expires_in"`
	RefreshToken string `json:"refresh_token"`
	Scope        string `json:"scope"`
}

// Response represents our API response
type Response struct {
	Message string `json:"message"`
	Status  string `json:"status"`
	Token   string `json:"token,omitempty"` // Only included on success
}
