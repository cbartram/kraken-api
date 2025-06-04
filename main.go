package main

import (
	"flag"
	"fmt"
	"github.com/joho/godotenv"
	"github.com/stripe/stripe-go/v81"
	"go.uber.org/zap"
	"kraken-api/src"
	"kraken-api/src/handlers/payment"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"os"
	"strings"
	"time"
)

func main() {
	logLevel := os.Getenv("LOG_LEVEL")
	var logger *zap.Logger
	if strings.ToUpper(logLevel) == "INFO" || strings.ToUpper(logLevel) == "DEBUG" {
		logger, _ = zap.NewDevelopment()
	} else {
		logger, _ = zap.NewProduction()
	}
	defer logger.Sync()
	log := logger.Sugar()

	ginMode := os.Getenv("GIN_MODE")
	port := flag.String("port", "8080", "port to listen on")
	flag.Parse()

	// Running locally
	if ginMode == "" {
		err := godotenv.Load()
		if err != nil {
			log.Fatalf(fmt.Sprintf("error loading .env file: %v", err))
		}
	}

	discordService, err := service.MakeDiscordService(log)
	if err != nil {
		log.Fatalf("failed to make discord service: %v", err)
	}
	s3Service, err := service.MakeMinIOService(os.Getenv("BUCKET_NAME"), os.Getenv("MINIO_ENDPOINT"), log)
	if err != nil {
		log.Fatalf("failed to create S3 service: %v", err)
	}
	rabbitMqService, err := service.MakeRabbitMQService("stripe-webhooks-kraken", "stripe-webhooks-kraken", log)
	if err != nil {
		log.Fatalf("failed to make rabbitmq service: %v", err)
	}

	db := model.Connect(log)

	key := os.Getenv("STRIPE_SECRET_KEY")
	if key == "" {
		log.Fatal("Missing STRIPE_SECRET_KEY environment variable")
	}
	stripe.Key = strings.TrimSpace(key)

	w := service.Wrapper{
		Logger:          log,
		DiscordService:  discordService,
		S3Service:       s3Service,
		CognitoService:  service.MakeCognitoService(log),
		Database:        db,
		RabbitMqService: rabbitMqService,
		PluginStore:     service.NewPluginStore(db),
	}
	router := src.NewRouter(&w)

	// Registers a new go routine listening to the stripe-webhooks channel. New messages are enqueued when the /api/v1/stripe/webhook
	// endpoint is called and this function consumes the messages with a 3-second delay in between each message resolving eventual consistency
	// issues with both cognito and stripe when many events are sent at checkout.
	err = rabbitMqService.RegisterConsumer(payment.ConsumeMessageWithDelay, 3*time.Second, w.Database)
	if err != nil {
		log.Errorf("failed to register stripe webhook message consumer: %v", err)
	}

	defer func() {
		log.Infof("closing rabbitmq connection and channel")
		rabbitMqService.Close()
	}()

	log.Infof("server listening on port %s", *port)
	err = router.Run(fmt.Sprintf(":%v", *port))
	if err != nil {
		log.Fatal("failed to run http server: " + err.Error())
	}
}
