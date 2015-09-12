FROM java:8

RUN groupadd -r obelix && useradd --uid 1000 -r -g obelix obelix

RUN mkdir -p /app/obelix/database \
    && chown -R obelix:obelix /app

COPY target/obelix-1.0-SNAPSHOT-jar-with-dependencies.jar /app/obelix/obelix.jar

WORKDIR /app/obelix
COPY docker-entrypoint.sh /

USER obelix
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["obelix"]
