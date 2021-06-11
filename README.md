# Spring Dockerfile 예제

> 모든 예제는 CLI: Windows PowerShell, Build Tool: Gradle 기준입니다.

## 1. 기본 Spring Dockerfile

기본적인 Dockerfile로서 빌드 Tool을 정하지 않았을 경우 docker run 실행시 매개변수로 값을 받아서 빌드하는 예제이다.

*Dockerfile*:

~~~dockerfile
FROM openjdk:12-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
~~~

~~~powershell
docker build --build-arg JAR_FILE=build/libs/*.jar -t actionmandocker/springdocker .
docker run -p 8585:8585 actionmandocker/springdocker
~~~

빌드 시스템이 정해져 있는 경우 매개변수로 값을 받을 필요없이 사용한다.

*Dockerfile*:

~~~dockerfile
FROM openjdk:12-jdk-alpine
VOLUME /tmp
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
~~~

~~~powershell
docker build -t actionmandocker/springdocker .
docker run -p 8585:8585 actionmandocker/springdocker
~~~

## 2. Entry Point 사용

Entry Point 사용시 shell을 warpping하여 자바를 구동하지 않는다. 이렇게 되면 java의 kill신호를 도커 컨테이너에서 받을 수 있다.

Entry Point에 필요한 변수를 docker run 실행시 받는 방법 shell script를 실행해서 받는 방법 2가지가 있는데 편의성 측면에서 shell script 방법을 쓰는것을 권장한다.

### 2.1.2. shell script 사용

*Dockerfile*:

~~~dockerfile
FROM openjdk:12-jdk-alpine
VOLUME /tmp
COPY run.sh .
COPY build/libs/*.jar app.jar
ENTRYPOINT ["./run.sh"]
~~~

*run.sh*:

~~~bash
#!/bin/sh
exec java -jar /app.jar
~~~

~~~powershell
docker build -t actionmandocker/springdocker .
docker run -p 8585:8585 actionmandocker/springdocker
~~~

### 2.1.2. docker run 실행시 매개변수로 입력받는 방법

해당 방법 사용시 ${}를 이용하는데 반드시 shell 이 필요하므로 명시적으로 shell을 생성해야 한다.

*Dockerfile*:

~~~dockerfile
FROM openjdk:12-jdk-alpine
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
~~~

~~~powershell
docker build -t actionmandocker/springdocker .
docker run -p 9000:9000 -e JAVA_OPTS=-Dserver.port=9000 actionmandocker/springdocker
~~~

Spring 옵션 사용을 위해서는 다음과 같이 추가적으로 매개변수를 받을 수 있도록 처리해야한다.

### 2.2.1. shell script 사용(Spring option 추가)

*Dockerfile*:

~~~dockerfile
FROM openjdk:12-jdk-alpine
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY run.sh .
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["./run.sh"]
~~~

*run.sh*:

~~~bash
#!/bin/sh
exec java ${JAVA_OPTS} -jar /app.jar ${@}
~~~

~~~powershell
docker build -t actionmandocker/springdocker .
docker run -p 9000:9000 actionmandocker/springdocker --server.port=9000
~~~

### 2.2.2. docker run 실행시 매개변수로 입력받는 방법(Spring option 추가)

*Dockerfile*:

~~~dockerfile
FROM openjdk:12-jdk-alpine
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar ${0} ${@}"]
~~~

~~~powershell
docker build -t actionmandocker/springdocker .
docker run -p 9000:9000 actionmandocker/springdocker --server.port=9000
~~~

## 3. Dockerfile 최적화

### 3.1. 작은 이미지

alpine 이미지를 사용하도록 한다. alpine 이미지는 리눅스 이미지중 가장 가벼운 이미지이다. jre를 사용한다. 요즘에는 jdk만 배포 되는 경향이 있는데 jLink를 사용하면 jdk내에서 jre를 추출할 수 있다. 작은 이미지 만들기는 이미지 만들기의 중요한 부분이지만 가장 중요한 목표는 많이 변화하는 항목을 바깥쪽 레이어에 배치하는 것이다.

### 3.2. 이미지 캐시

종속성이 변경되지 않는한 library 파일이 변경될 일은 없기 때문에 변경되지 않는 요소를 최상단에 두고 캐시 시킨다. 맨 바깥쪽 이미지의 경우 자주 변경되는 클래스 파일을 놓아둔다.

*Dockerfile*:

~~~dockerfile
FROM openjdk:12-jdk-alpine
VOLUME /tmp
ARG DEPENDENCY=build/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","com.actionman.springbootapiexample.SpringBootApiExampleApplication"]
~~~

~~~powershell
docker build -t actionmandocker/springdocker .
docker run -p 9000:9000 actionmandocker/springdocker --server.port=9000
~~~

### 3.3. 스프링 최적화

1. spring-context-indexer 사용.
2. Spring Boot Actuator는 필요할 때만 사용.
3. Spring 최신 버전 사용 필요.(Spring 5, SpringBoot 2.1 이상)
4. spring.config.location 옵션을 사용하여 설정 파일 위치를 지정한다.
5. JMX 기능을 끈다.
6. -noverify 옵션을 사용하여 JVM을 실행.(클래스 파일 검증하지 않음) 혹은 -XX:TieredStopAtLevel=1 옵션 사용. (시작은 빠르나 JIT가 느려짐)
7. Java8용 힌트 사용. -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap Java11에서는 기본 사용.

## 4. 다단계 빌드



~~~dockerfile
FROM openjdk:12-jdk-alpine as build
WORKDIR /workspace/app

COPY . /workspace/app
RUN --mount=type=cache,target=/root/.gradle ./gradlew clean build
RUN mkdir -p build/dependency && (cd build/dependency; jar -xf ../libs/*.jar)

FROM openjdk:12-jdk-alpine
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/build/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","com.actionman.springbootapiexample.SpringBootApiExampleApplication"]
~~~

~~~powershell
docker build -t actionmandocker/springdocker .
docker run -p 9000:9000 actionmandocker/springdocker --server.port=9000
~~~


