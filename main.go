package main

import (
	"flag"
	"fmt"
	"github.com/joho/godotenv"
	"github.com/sirupsen/logrus"
	"github.com/stripe/stripe-go/v81"
	"kraken-api/src"
	"kraken-api/src/handlers/payment"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"log"
	"os"
	"strings"
	"time"
)

func main() {
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

	discordService, err := service.MakeDiscordService()
	if err != nil {
		log.Fatalf("failed to make discord service: %v", err)
	}
	s3Service, err := service.MakeS3Service(os.Getenv("BUCKET_NAME"))
	if err != nil {
		logrus.Fatalf("failed to create S3 service: %v", err)
	}
	rabbitMqService, err := service.MakeRabbitMQService("stripe-webhooks-kraken", "stripe-webhooks-kraken")
	if err != nil {
		logrus.Fatalf("failed to make rabbitmq service: %v", err)
	}

	db := model.Connect()

	key := os.Getenv("STRIPE_SECRET_KEY")
	if key == "" {
		log.Fatal("Missing STRIPE_SECRET_KEY environment variable")
	}
	stripe.Key = strings.TrimSpace(key)
	logrus.Infof("stripe key loaded: %s****", stripe.Key[0:12])

	w := service.Wrapper{
		DiscordService:  discordService,
		S3Service:       s3Service,
		CognitoService:  service.MakeCognitoService(),
		Database:        db,
		RabbitMqService: rabbitMqService,
		PluginStore:     service.NewPluginStore(db),
	}
	router := src.NewRouter(&w)

	// TODO This shouldn't be part of the image. This should be a separate script that's run when creating
	// new plugins and basically just be sql since its a data loader
	//err = model.ImportOrUpdatePluginMetadata("./data/plugin_metadata.json", w.Database)
	//if err != nil {
	//	logrus.Fatalf("failed to import plugin metadata: %v", err)
	//}
	////
	//err = model.ImportOrUpdatePluginPacks("./data/plugin_packs.json", w.Database)
	//if err != nil {
	//	logrus.Fatalf("failed to import plugin packs: %v", err)
	//}

	// Registers a new go routine listening to the stripe-webhooks channel. New messages are enqueued when the /api/v1/stripe/webhook
	// endpoint is called and this function consumes the messages with a 3-second delay in between each message resolving eventual consistency
	// issues with both cognito and stripe when many events are sent at checkout.
	err = rabbitMqService.RegisterConsumer(payment.ConsumeMessageWithDelay, 3*time.Second, w.Database)
	if err != nil {
		logrus.Errorf("failed to register stripe webhook message consumer: %v", err)
	}

	defer func() {
		logrus.Infof("Closing rabbitmq connection and channel")
		rabbitMqService.Close()
	}()

	log.Printf("Server Listening on port %s", *port)
	err = router.Run(fmt.Sprintf(":%v", *port))
	if err != nil {
		log.Fatal("failed to run http server: " + err.Error())
	}
}
