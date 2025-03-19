package handlers

import (
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/model"
	"net/http"
	"net/smtp"
	"os"
)

type EmailHandler struct{}

type SupportRequest struct {
	Message string `json:"message" binding:"required"`
	Subject string `json:"subject" binding:"required"`
}

func (e *EmailHandler) HandleRequest(c *gin.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody SupportRequest
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	tmp, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusInternalServerError, gin.H{"error": "user not found in context"})
		return
	}

	smtpHost := os.Getenv("SMTP_HOST")
	smtpUser := os.Getenv("SMTP_USER")
	smtpPass := os.Getenv("SMTP_PASS")

	user := tmp.(*model.User)
	toEmail := "runewraith.yt@gmail.com"
	fromEmail := toEmail

	auth := smtp.PlainAuth("", smtpUser, smtpPass, smtpHost)

	emailSubject := "Support Request: " + reqBody.Subject
	emailBody := "User: " + user.DiscordID + "\r\n" +
		"Customer Id: " + user.CustomerId + "\r\n" +
		"User Email: " + user.Email + "\r\n" +
		"Discord Username: " + user.DiscordUsername + "\r\n" +
		"Message: " + reqBody.Message
	emailHeaders := "From: " + fromEmail + "\r\n" +
		"To: " + toEmail + "\r\n" +
		"Subject: " + emailSubject + "\r\n" +
		"MIME-Version: 1.0\r\n" +
		"Content-Type: text/plain; charset=UTF-8\r\n\r\n"
	emailMessage := emailHeaders + emailBody

	err = smtp.SendMail(
		fmt.Sprintf("%s:587", smtpHost),
		auth,
		fromEmail,
		[]string{toEmail},
		[]byte(emailMessage),
	)

	if err != nil {
		log.Errorf("error sending email: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"error":   "failed to send email: " + err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "email sent",
	})
}
