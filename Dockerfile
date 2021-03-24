FROM ubuntu:latest
COPY build/graal/social-discord-native /social-discord/
WORKDIR /social-discord
ENTRYPOINT ["/social-discord/social-discord-native"]


