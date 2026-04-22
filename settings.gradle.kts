rootProject.name = "deadlock-check-idiots"

sourceControl {
    gitRepository(uri("https://github.com/deadlock-api/openapi-clients")) {
        producesModule("com.deadlock-api:deadlock-api-client")
        rootDir = "kotlin/api"
    }
}

