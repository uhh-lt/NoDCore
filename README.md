# NoDCore

## Setup
- Extract lib.zip and resources.zip from the [releases](https://github.com/uhh-lt/NoDCore/releases) into the directory
- Install sbt (scala build tool)
- Run `sbt clean compile assembly`
- Configure database
- Set db name, user and password in resources/application.conf
- Move data with sentences into a directory e.g. content
- Set memory limit. E.g. 4GB `export JAVA_OPTS="-Xmx4g"`
- Run germanere on content \
 `java -jar target/scala-2.11/NoDCore-assembly-1.0.jar --dir content --format compressed`


## Database Configuration
- Install mysql or mariadb
- Run database e.g. `sudo systemctl start mariadb.service` with systemd
- Connect to database `mysql -u root`
- `CREATE DATABASE nodcore CHARACTER SET utf8 COLLATE utf8_general_ci;`
- `CREATE USER 'nod'@'localhost' IDENTIFIED BY 'PASSWORD';`
- `GRANT ALL PRIVILEGES ON nodcore.* TO 'nod'@'localhost';`
- `FLUSH PRIVILEGES;`

## Docker Compose
- Extract lib.zip and resources.zip from the [releases](https://github.com/uhh-lt/NoDCore/releases) into the directory
- Run `docker-compose up`