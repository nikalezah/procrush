package db.migration

import org.jetbrains.exposed.sql.Transaction

internal fun Transaction.execSqlResource(resourcePath: String) {
    val sql =
        checkNotNull(V1__Initial_schema::class.java.classLoader.getResourceAsStream(resourcePath)) {
            "SQL resource not found: $resourcePath"
        }.bufferedReader(Charsets.UTF_8).readText()
    for (statement in splitSqlStatements(sql)) {
        exec(statement)
    }
}

private fun splitSqlStatements(sql: String): List<String> {
    val withoutComments =
        sql
            .lineSequence()
            .filter { line -> !line.trimStart().startsWith("--") }
            .joinToString("\n")
    return withoutComments
        .split(';')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}
