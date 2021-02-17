FROM adoptopenjdk/openjdk11:alpine-slim

RUN mkdir /social-discord
WORKDIR /social-discord

COPY build/install/social-discord /social-discord
CMD ["/social-discord/bin/social-discord"]


