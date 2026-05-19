dev: kill
	@mvn quarkus:dev

setup-dev:
	@echo "Setting up dev environment..."
	@command -v pre-commit >/dev/null 2>&1 || { echo "Error: pre-commit is not installed. Please install it first."; exit 1; }
	@command -v docker >/dev/null 2>&1 || { echo "Error: docker is not installed. Please install it first."; exit 1; }
	pre-commit install
	pre-commit autoupdate
	pre-commit install --install-hooks

build:
	@mvn -B clean package -DskipTests

kill:
	@lsof -ti:8080 | xargs kill -9 2>/dev/null

start: kill build
	@java -jar target/*-runner.jar