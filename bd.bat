docker-compose down -v

mvn clean install -DskipTests -e

docker-compose up -d --build --wait

mvn test -pl pps,as -Dtest="*E2ETest" -DfailIfNoTests=false
