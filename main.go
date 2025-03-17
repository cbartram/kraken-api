package main

import (
	"flag"
	"fmt"
	"github.com/joho/godotenv"
	"github.com/sirupsen/logrus"
	"kraken-api/src"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"log"
	"os"
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

	w := service.Wrapper{
		DiscordService: discordService,
		S3Service:      s3Service,
		CognitoService: service.MakeCognitoService(),
		Database:       model.Connect(),
	}
	router := src.NewRouter(&w)

	log.Printf("Server Listening on port %s", *port)
	err = router.Run(fmt.Sprintf(":%v", *port))
	if err != nil {
		log.Fatal("failed to run http server: " + err.Error())
	}
}
