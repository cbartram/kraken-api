#!/bin/bash

# Check if both parameters are provided
if [ $# -ne 2 ]; then
    echo "Usage: $0 <discord_id> <refresh_token>"
    exit 1
fi

# Store parameters in variables
DISCORD_ID="$1"
REFRESH_TOKEN="$2"

# Function to get access token
get_access_token() {
    local RESPONSE=$(curl --silent --location 'https://rog742w0fa.execute-api.us-east-1.amazonaws.com/prod/api/v1/cognito/auth' \
    --header 'Content-Type: application/json' \
    --data "{
        \"refreshToken\": \"$REFRESH_TOKEN\",
        \"discordId\": \"$DISCORD_ID\"
    }")

    # Check if curl command was successful
    if [ $? -ne 0 ]; then
        echo "Error: Failed to make HTTP request for token"
        exit 1
    fi

    # Check if jq is installed
    if ! command -v jq &> /dev/null; then
        echo "Error: jq is not installed. Please install jq to parse JSON."
        echo "Raw response: $RESPONSE"
        exit 1
    fi

    echo "$RESPONSE"
}

# Get plugin name from user
read -p "Enter plugin name: " PLUGIN_NAME

# Validate plugin name is not empty
if [ -z "$PLUGIN_NAME" ]; then
    echo "Error: Plugin name cannot be empty"
    exit 1
fi

# Get duration from user
read -p "Enter duration in days: " DURATION

# Validate duration is a positive integer
if ! [[ "$DURATION" =~ ^[0-9]+$ ]]; then
    echo "Error: Duration must be a positive integer"
    exit 1
fi

# Get tokens
echo "Retrieving authentication tokens..."
AUTH_RESPONSE=$(get_access_token)

# Extract access token
ACCESS_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.credentials.access_token')
ID_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.credentials.id_token')


# Check if access token was successfully extracted
if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
    echo "Error: Failed to extract access token from response"
    echo "Raw response: $AUTH_RESPONSE"
    exit 1
fi

# Make the purchase request
echo "Making plugin purchase request..."
PURCHASE_RESPONSE=$(curl --silent --location 'https://rog742w0fa.execute-api.us-east-1.amazonaws.com/prod/api/v1/plugin/purchase' \
--header 'Content-Type: application/json' \
--header "Authorization: Bearer $ID_TOKEN" \
--data "{
    \"pluginName\": \"$PLUGIN_NAME\",
    \"purchaseDurationDays\": $DURATION,
    \"credentials\": {
        \"access_token\": \"$ACCESS_TOKEN\"
    }
}")

# Check if purchase request was successful
if [ $? -ne 0 ]; then
    echo "Error: Failed to make purchase request"
    exit 1
fi

# Extract and format the response
PLUGIN_NAME=$(echo "$PURCHASE_RESPONSE" | jq -r '.pluginName')
LICENSE_KEY=$(echo "$PURCHASE_RESPONSE" | jq -r '.licenseKey')
EXPIRATION=$(echo "$PURCHASE_RESPONSE" | jq -r '.expirationTimestamp')

# Convert UTC timestamp to local time
if command -v date &> /dev/null; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        LOCAL_EXPIRATION=$(date -j -f "%Y-%m-%dT%H:%M:%SZ" "$EXPIRATION" "+%Y-%m-%d %I:%M:%S %p")
    else
        # Linux
        LOCAL_EXPIRATION=$(date -d "$EXPIRATION" "+%Y-%m-%d %I:%M:%S %p")
    fi
else
    LOCAL_EXPIRATION=$EXPIRATION
fi

# Print formatted response
echo ""
echo "─────────────────────────────────────────"
echo "           Purchase Successful            "
echo "─────────────────────────────────────────"
echo "Plugin Name: $PLUGIN_NAME"
echo "License Key: $LICENSE_KEY"
echo "Expires:     $LOCAL_EXPIRATION"
echo "─────────────────────────────────────────"
echo ""