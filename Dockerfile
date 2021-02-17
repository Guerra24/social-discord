FROM adoptopenjdk/openjdk11:alpine-slim
COPY build/install/social-discord /social-discord
WORKDIR /social-discord
CMD ["/social-discord/bin/social-discord"]


