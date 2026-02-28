deployment:
tomcat:

1) startup.sh:
   export JRE_HOME=$JAVA_HOME
   export CATALINA_OPTS="$CATALINA_OPTS -Dliquibase.secureParsing=false"
2) db.changelog-master.xml:
   <databaseChangeLog
   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:
   schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.3.xsd">
