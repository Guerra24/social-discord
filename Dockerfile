FROM adoptopenjdk/openjdk11:alpine-slim

RUN mkdir /social-discord
WORKDIR /social-discord

ADD . .


