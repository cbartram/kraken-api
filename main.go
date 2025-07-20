package main

import (
	"context"
	"flag"
	"fmt"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider"
	"github.com/joho/godotenv"
	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
	"github.com/stripe/stripe-go/v81"
	"go.uber.org/zap"
	"kraken-api/src"
	"kraken-api/src/cache"
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

	redisConfig := cache.CacheConfig{
		Host:     os.Getenv("REDIS_HOST"),
		Port:     os.Getenv("REDIS_PORT"),
		Password: os.Getenv("REDIS_PASSWORD"),
		DB:       0,
	}

	redisCache := cache.NewRedisCache(redisConfig, log)
	defer redisCache.Close()

	discordService, err := service.MakeDiscordService(log)
	if err != nil {
		log.Fatalf("failed to make discord service: %v", err)
	}

	username := os.Getenv("MINIO_ROOT_USER")
	password := os.Getenv("MINIO_ROOT_PASSWORD")

	minioClient, err := minio.New(os.Getenv("MINIO_ENDPOINT"), &minio.Options{
		Creds:  credentials.NewStaticV4(username, password, ""),
		Secure: true,
	})

	if err != nil {
		log.Fatalf("failed to create MinIO client: %v", err)
	}

	s3Service, err := service.MakeMinIOService(os.Getenv("BUCKET_NAME"), os.Getenv("MINIO_ENDPOINT"), minioClient, log)
	if err != nil {
		log.Fatalf("failed to create S3 service: %v", err)
	}

	conn := fmt.Sprintf("amqp://%s@%s/?heartbeat=10", fmt.Sprintf("%s:%s", os.Getenv("RABBITMQ_DEFAULT_USER"), os.Getenv("RABBITMQ_DEFAULT_PASS")), os.Getenv("RABBITMQ_BASE_URL"))
	if err != nil {
		log.Fatalf("failed to connect to RabbitMQ: %v", err)
	}

	rabbitMqService, err := service.MakeRabbitMQService(conn, "stripe-webhooks-kraken", "stripe-webhooks-kraken", log)
	if err != nil {
		log.Fatalf("failed to make rabbitmq service: %v", err)
	}

	db := model.Connect(log)

	key := os.Getenv("STRIPE_SECRET_KEY")
	if key == "" {
		log.Fatal("Missing STRIPE_SECRET_KEY environment variable")
	}
	stripe.Key = strings.TrimSpace(key)

	cfg, err := config.LoadDefaultConfig(context.TODO())
	if err != nil {
		log.Errorf("error loading default aws config: %s", err)
	}

	client := cognitoidentityprovider.NewFromConfig(cfg)

	w := service.Wrapper{
		Logger:          log,
		DiscordService:  discordService,
		S3Service:       s3Service,
		CognitoService:  service.MakeCognitoService(log, client),
		Database:        db,
		RabbitMqService: rabbitMqService,
		PluginStore:     service.NewPluginStore(db, redisCache),
		UserRepository: &model.DefaultUserRepository{
			Cache: redisCache,
			Log:   log,
		},
		Cache: redisCache,
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
