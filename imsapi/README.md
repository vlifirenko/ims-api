ims-api
=======

Сборка проекта
--------------

# Установите gephi.jar артифакт
  mvn install:install-file -DgroupId=org.gephi -DartifactId=gephi -Dversion=1.0 -Dpackaging=jar -Dfile=lib/gephi-1.0.jar
# соберите проект
  mvn assembly:single

Запуск сервера
--------------

java -jar ims-api-jar-with-dependencies.jar "path_to_properties_file"