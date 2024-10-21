FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1
WORKDIR /app
COPY project/plugins.sbt .
COPY build.sbt .
COPY  src .
COPY ./project ./project
RUN sbt clean assembly
RUN mkdir src
RUN mv main src/