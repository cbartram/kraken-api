package main

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"os"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

// Response represents the API response structure
type Response struct {
	Message string      `json:"message"`
	Data    interface{} `json:"data,omitempty"`
}

// ErrorResponse represents the error response structure
type ErrorResponse struct {
	Error string `json:"error"`
}

// Handler is our lambda handler invoked by the `lambda.Start` function
func Handler(ctx context.Context, request events.APIGatewayProxyRequest) (events.APIGatewayProxyResponse, error) {
	// Set up logger
	logger := log.New(os.Stdout, "", log.LstdFlags)
	logger.Printf("Processing request data for request %s\n", request.RequestContext.RequestID)
	logger.Printf("Body size = %d\n", len(request.Body))
	logger.Printf("Path: %s\n", request.Path)
	logger.Printf("Method: %s\n", request.HTTPMethod)

	// Get environment variables
	stage := os.Getenv("STAGE")
	if stage == "" {
		stage = "dev"
	}

	// Route requests based on HTTP method and path
	switch request.HTTPMethod {
	case "GET":
		return handleGet(request)
	case "POST":
		return handlePost(request)
	default:
		return createResponse(http.StatusMethodNotAllowed, ErrorResponse{
			Error: "Method not allowed",
		})
	}
}

// handleGet processes GET requests
func handleGet(request events.APIGatewayProxyRequest) (events.APIGatewayProxyResponse, error) {
	// Handle different paths
	switch request.Path {
	case "/hello":
		// Get name from query parameters
		name := request.QueryStringParameters["name"]
		if name == "" {
			name = "World"
		}

		return createResponse(http.StatusOK, Response{
			Message: "Hello, " + name + "!",
		})

	case "/health":
		return createResponse(http.StatusOK, Response{
			Message: "Healthy",
		})

	default:
		return createResponse(http.StatusNotFound, ErrorResponse{
			Error: "Path not found",
		})
	}
}

// handlePost processes POST requests
func handlePost(request events.APIGatewayProxyRequest) (events.APIGatewayProxyResponse, error) {
	switch request.Path {
	case "/echo":
		// Echo back the request body
		var requestData map[string]interface{}
		if err := json.Unmarshal([]byte(request.Body), &requestData); err != nil {
			return createResponse(http.StatusBadRequest, ErrorResponse{
				Error: "Invalid JSON body",
			})
		}

		return createResponse(http.StatusOK, Response{
			Message: "Echo response",
			Data:    requestData,
		})

	default:
		return createResponse(http.StatusNotFound, ErrorResponse{
			Error: "Path not found",
		})
	}
}

// createResponse creates an APIGatewayProxyResponse with proper headers and serialized body
func createResponse(statusCode int, body interface{}) (events.APIGatewayProxyResponse, error) {
	jsonBody, err := json.Marshal(body)
	if err != nil {
		return events.APIGatewayProxyResponse{
			StatusCode: http.StatusInternalServerError,
			Body:       `{"error": "Failed to serialize response"}`,
			Headers: map[string]string{
				"Content-Type": "application/json",
			},
		}, nil
	}

	return events.APIGatewayProxyResponse{
		StatusCode: statusCode,
		Body:       string(jsonBody),
		Headers: map[string]string{
			"Content-Type":                "application/json",
			"Access-Control-Allow-Origin": "*", // Enable CORS
		},
	}, nil
}

func main() {
	lambda.Start(Handler)
}
