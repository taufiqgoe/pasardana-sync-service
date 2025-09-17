FROM bellsoft/liberica-native-image-kit-container:jdk-17-nik-23.0.9-musl AS build

WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw -Pnative native:compile

FROM alpine:3.20 AS run
WORKDIR /app
COPY --from=build /app/target/pd-scraper /app/
CMD ["./pd-scraper"]
