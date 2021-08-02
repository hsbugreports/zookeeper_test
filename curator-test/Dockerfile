# build code via maven
FROM maven:3-openjdk-8 as build
WORKDIR /app
COPY . .
RUN mvn install
# Create the runtime deployment
FROM openjdk:8
ARG NODE_ID=1
ENV NODE_ID=${NODE_ID}
ARG COUNT=100
ENV COUNT=${COUNT}
ENV SERVERS=${SERVERS}

COPY --from=build /app/target/curator-test-1.0.0-SNAPSHOT-jar-with-dependencies.jar /opt/curator-test/app.jar
WORKDIR /opt/curator-test
CMD java -jar app.jar Main -i ${NODE_ID} -s ${SERVERS} -c ${COUNT}