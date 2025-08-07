package discord

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"kraken-api/src/handlers"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

type TicketHandler struct {
	Log   *zap.SugaredLogger
	Token string
}

type TicketRequestBody struct {
	IngameUsername  string `json:"ingameUsername"`
	Tokens          string `json:"tokens"`
	Gp              string `json:"gp"`
	DiscordUsername string `json:"-"`
	DiscordId       string `json:"-"`
}

type ChannelPermissionOverwrite struct {
	ID    string `json:"id"`
	Type  int    `json:"type"`
	Allow string `json:"allow"`
	Deny  string `json:"deny"`
}

type CreateChannelRequest struct {
	Name                 string                       `json:"name"`
	Type                 int                          `json:"type"`
	ParentID             string                       `json:"parent_id,omitempty"`
	PermissionOverwrites []ChannelPermissionOverwrite `json:"permission_overwrites,omitempty"`
}

type DiscordChannel struct {
	ID   string `json:"id"`
	Name string `json:"name"`
}

type DiscordEmbed struct {
	Title       string              `json:"title,omitempty"`
	Description string              `json:"description,omitempty"`
	Color       int                 `json:"color,omitempty"`
	Fields      []DiscordEmbedField `json:"fields,omitempty"`
	Timestamp   string              `json:"timestamp,omitempty"`
}

type DiscordEmbedField struct {
	Name   string `json:"name"`
	Value  string `json:"value"`
	Inline bool   `json:"inline,omitempty"`
}

type DiscordMessage struct {
	Content string         `json:"content,omitempty"`
	Embeds  []DiscordEmbed `json:"embeds,omitempty"`
}

// HandleRequest Handles the /api/v1/discord/create-ticket endpoint for when users purchase a plugin with GP and a new
// discord ticket needs to be made to communicate with them.
func (d *TicketHandler) HandleRequest(c *gin.Context) {
	tmp, exists := c.Get("user")
	if !exists {
		d.Log.Errorf("user not found in context")
		c.JSON(http.StatusUnauthorized, gin.H{"message": "user not found in context"})
		return
	}

	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		d.Log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var ticketRequest TicketRequestBody
	if err := json.Unmarshal(bodyRaw, &ticketRequest); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	user := tmp.(*model.User)
	ticketRequest.DiscordId = user.DiscordID
	ticketRequest.DiscordUsername = user.DiscordUsername
	d.Log.Infof("Received Ticket Create request for user: %s with In game username: %s", ticketRequest.DiscordUsername, ticketRequest.IngameUsername)

	serverId := os.Getenv("DISCORD_SERVER_ID")
	staffUserId := os.Getenv("DISCORD_USER_ID")
	categoryId := os.Getenv("DISCORD_CATEGORY_ID")

	if serverId == "" || staffUserId == "" || categoryId == "" {
		d.Log.Errorf("missing required Discord environment variables")
		c.JSON(http.StatusInternalServerError, gin.H{"error": "server configuration error"})
		return
	}

	channel, err := d.createTicketChannel(serverId, categoryId, ticketRequest.DiscordId, staffUserId)
	if err != nil {
		d.Log.Errorf("failed to create ticket channel: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to create Discord ticket: " + err.Error()})
		return
	}

	err = d.sendTicketMessage(channel.ID, ticketRequest, user, staffUserId)
	if err != nil {
		d.Log.Errorf("failed to send ticket message: %s", err)
		// Don't fail the request if message sending fails, channel was created successfully
		// But we should try to delete the channel or at least log this
	}

	d.Log.Infof("Successfully created Discord ticket channel: %s for user: %s", channel.ID, user.DiscordUsername)

	c.JSON(http.StatusOK, gin.H{
		"success":     true,
		"channelId":   channel.ID,
		"channelName": channel.Name,
		"message":     "Discord ticket created successfully",
	})
}

// createTicketChannel creates a private Discord channel with proper permissions
func (d *TicketHandler) createTicketChannel(serverId, categoryId, userDiscordId, staffUserId string) (*DiscordChannel, error) {
	ticketName := fmt.Sprintf("gp-token-purchase-%d", time.Now().Unix())
	permissionOverwrites := []ChannelPermissionOverwrite{
		// Deny @everyone from viewing the channel
		{
			ID:    serverId, // Using server ID as @everyone role ID
			Type:  0,        // Role type
			Allow: "0",
			Deny:  "1024", // VIEW_CHANNEL permission (2^10)
		},
		// Allow the user to view, send messages, and read history
		{
			ID:    userDiscordId,
			Type:  1,       // Member type
			Allow: "68608", // VIEW_CHANNEL (1024) + SEND_MESSAGES (2048) + READ_MESSAGE_HISTORY (65536)
			Deny:  "0",
		},
		// Allow staff to view, send messages, and read history
		{
			ID:    staffUserId,
			Type:  1,       // Member type
			Allow: "68608", // VIEW_CHANNEL (1024) + SEND_MESSAGES (2048) + READ_MESSAGE_HISTORY (65536)
			Deny:  "0",
		},
	}

	channelRequest := CreateChannelRequest{
		Name:                 ticketName,
		Type:                 0, // Text channel
		ParentID:             categoryId,
		PermissionOverwrites: permissionOverwrites,
	}

	jsonData, err := json.Marshal(channelRequest)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal channel request: %w", err)
	}

	req, err := http.NewRequest("POST",
		fmt.Sprintf("https://discord.com/api/v10/guilds/%s/channels", serverId),
		bytes.NewBuffer(jsonData))
	if err != nil {
		return nil, fmt.Errorf("failed to create HTTP request: %w", err)
	}

	req.Header.Set("Authorization", fmt.Sprintf("Bot %s", d.Token))
	req.Header.Set("Content-Type", "application/json")

	// Send request
	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to send channel creation request: %w", err)
	}
	defer resp.Body.Close()

	// Check response status
	if resp.StatusCode != http.StatusCreated {
		bodyBytes, _ := io.ReadAll(resp.Body)
		d.Log.Errorf("Discord API error - Status: %d, Body: %s", resp.StatusCode, string(bodyBytes))
		return nil, fmt.Errorf("discord API returned status %d", resp.StatusCode)
	}

	// Parse response
	var channel DiscordChannel
	if err := json.NewDecoder(resp.Body).Decode(&channel); err != nil {
		return nil, fmt.Errorf("failed to decode channel response: %w", err)
	}

	return &channel, nil
}

// sendTicketMessage sends the formatted ticket information to the Discord channel
func (d *TicketHandler) sendTicketMessage(channelId string, ticketRequest TicketRequestBody, user *model.User, staffUserId string) error {
	// Create the embed with ticket details
	embed := DiscordEmbed{
		Title:       "🪙 GP Payment Request",
		Description: fmt.Sprintf("New GP payment ticket for @%s", ticketRequest.DiscordUsername),
		Color:       0x00ff00, // Green color
		Fields: []DiscordEmbedField{
			{
				Name:   "🛒 Tokens",
				Value:  ticketRequest.Tokens,
				Inline: true,
			},
			{
				Name:   "💰 GP Amount",
				Value:  d.formatGP(ticketRequest.Gp),
				Inline: true,
			},
			{
				Name:   "⚔️ RuneScape Username",
				Value:  ticketRequest.IngameUsername,
				Inline: true,
			},
			{
				Name:   "👤 Discord User Id",
				Value:  fmt.Sprintf("<@%s>", ticketRequest.DiscordId),
				Inline: true,
			},
			{
				Name:   "📧 Account Email",
				Value:  user.Email,
				Inline: true,
			},
			{
				Name:   "⏰ Status",
				Value:  "🟡 Awaiting Payment",
				Inline: true,
			},
		},
		Timestamp: time.Now().Format(time.RFC3339),
	}

	// Create the message with mentions and embed
	message := DiscordMessage{
		Content: fmt.Sprintf("🎫 **New GP Payment Ticket**\n\n<@%s> <@%s>\n\n**Instructions:**\n• Customer: Wait for meetup coordination from staff\n• Staff: Coordinate meetup, confirm GP received, apply tokens to users account.\n• Use `!close` when transaction is complete\n\n",
			ticketRequest.DiscordId, staffUserId),
		Embeds: []DiscordEmbed{embed},
	}

	return d.sendDiscordMessage(channelId, message)
}

// sendDiscordMessage sends a message to a Discord channel
func (d *TicketHandler) sendDiscordMessage(channelId string, message DiscordMessage) error {
	jsonData, err := json.Marshal(message)
	if err != nil {
		return fmt.Errorf("failed to marshal message: %w", err)
	}

	req, err := http.NewRequest("POST",
		fmt.Sprintf("https://discord.com/api/v10/channels/%s/messages", channelId),
		bytes.NewBuffer(jsonData))
	if err != nil {
		return fmt.Errorf("failed to create message request: %w", err)
	}

	req.Header.Set("Authorization", fmt.Sprintf("Bot %s", d.Token))
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{Timeout: 30 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to send message: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		bodyBytes, _ := io.ReadAll(resp.Body)
		d.Log.Errorf("Discord message API error - Status: %d, Body: %s", resp.StatusCode, string(bodyBytes))
		return fmt.Errorf("discord API returned status %d", resp.StatusCode)
	}

	return nil
}

// formatGP formats the GP amount with commas and proper suffix
func (d *TicketHandler) formatGP(gpStr string) string {
	if gp, err := strconv.Atoi(gpStr); err == nil {
		return d.formatNumber(gp) + " GP"
	}
	return gpStr + " GP"
}

// formatNumber formats large numbers with commas
func (d *TicketHandler) formatNumber(n int) string {
	str := strconv.Itoa(n)
	if len(str) <= 3 {
		return str
	}

	result := ""
	for i, v := range str {
		if i > 0 && (len(str)-i)%3 == 0 {
			result += ","
		}
		result += string(v)
	}
	return result
}

func (d *TicketHandler) GetChannel(channelId string) (*DiscordChannel, error) {
	req, err := http.NewRequest("GET",
		fmt.Sprintf("https://discord.com/api/v10/channels/%s", channelId), nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create get channel request: %w", err)
	}
	req.Header.Set("Authorization", fmt.Sprintf("Bot %s", d.Token))

	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to retrieve channel from discord API: %w", err)
	}
	defer resp.Body.Close()

	var channel DiscordChannel
	if err := json.NewDecoder(resp.Body).Decode(&channel); err != nil {
		return nil, fmt.Errorf("failed to decode channel: %w", err)
	}

	return &channel, nil
}

func (d *TicketHandler) CloseTicket(channelId string) error {
	botToken := os.Getenv("DISCORD_BOT_TOKEN")

	channel, err := d.GetChannel(channelId)
	if err != nil {
		return fmt.Errorf("failed to get channel: %w", err)
	}

	if !strings.Contains(channel.Name, "gp-token-purchase-") {
		return fmt.Errorf("attempt to close non gp-token-purchase-* channel")
	}

	req, err := http.NewRequest("DELETE",
		fmt.Sprintf("https://discord.com/api/v10/channels/%s", channelId), nil)
	if err != nil {
		return fmt.Errorf("failed to create delete request: %w", err)
	}

	req.Header.Set("Authorization", fmt.Sprintf("Bot %s", botToken))

	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to delete channel: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusNoContent {
		bodyBytes, _ := io.ReadAll(resp.Body)
		d.Log.Errorf("Discord delete channel API error - Status: %d, Body: %s", resp.StatusCode, string(bodyBytes))
		return fmt.Errorf("failed to delete channel, status: %d", resp.StatusCode)
	}

	return nil
}

type CloseTicketRequestBody struct {
	ChannelID string `json:"channel_id"`
	ClosedBy  string `json:"closed_by"`
}

// HandleCloseTicket handles the /api/v1/discord/close-ticket endpoint
func (d *TicketHandler) HandleCloseTicket(c *gin.Context, w *service.Wrapper) {
	log := handlers.GetLoggerWithTrace(c, w.Logger)

	tmp, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusUnauthorized, gin.H{"message": "user not found in context"})
		return
	}

	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var closeRequest CloseTicketRequestBody
	if err := json.Unmarshal(bodyRaw, &closeRequest); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	if closeRequest.ChannelID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "channel_id is required"})
		return
	}

	user := tmp.(*model.User)

	log.Infof("Closing ticket channel %s, closed by Discord user %s, account user %s",
		closeRequest.ChannelID, closeRequest.ClosedBy, user.DiscordUsername)

	err = d.CloseTicket(closeRequest.ChannelID)
	if err != nil {
		log.Errorf("failed to close ticket channel: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to close ticket: " + err.Error()})
		return
	}

	log.Infof("successfully closed ticket channel: %s", closeRequest.ChannelID)

	c.JSON(http.StatusOK, gin.H{
		"success":    true,
		"message":    "Ticket closed successfully",
		"closed_by":  closeRequest.ClosedBy,
		"channel_id": closeRequest.ChannelID,
	})
}
