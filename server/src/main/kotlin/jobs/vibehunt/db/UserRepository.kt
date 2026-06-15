package jobs.procrush.db

import jobs.procrush.auth.AuthUserDto
import jobs.procrush.auth.UserRole
import jobs.procrush.db.tables.UsersTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.util.UUID

class UserRepository {
    fun findByEmail(email: String): AuthUserDto? =
        transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.email eq email }
                .firstOrNull()
                ?.toDto()
        }

    fun findByAuthKey(authProvider: String, authSubject: String): AuthUserDto? =
        transaction {
            UsersTable
                .selectAll()
                .where {
                    (UsersTable.oauthProvider eq authProvider) and
                        (UsersTable.oauthSubject eq authSubject)
                }
                .firstOrNull()
                ?.toDto()
        }

    fun findById(id: UUID): AuthUserDto? =
        transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.id eq id }
                .firstOrNull()
                ?.toDto()
        }

    fun create(
        email: String,
        authProvider: String,
        authSubject: String,
        role: UserRole,
    ): AuthUserDto =
        transaction {
            val now = OffsetDateTime.now()
            val id =
                UsersTable.insert {
                    it[UsersTable.email] = email
                    it[UsersTable.role] = role.name
                    it[UsersTable.oauthProvider] = authProvider
                    it[UsersTable.oauthSubject] = authSubject
                    it[UsersTable.createdAt] = now
                }[UsersTable.id].value
            AuthUserDto(
                id = id.toString(),
                email = email,
                role = role,
            )
        }

    fun deleteById(userId: UUID): Boolean =
        transaction {
            UsersTable.deleteWhere { UsersTable.id eq userId } > 0
        }

    private fun ResultRow.toDto(): AuthUserDto =
        AuthUserDto(
            id = this[UsersTable.id].value.toString(),
            email = this[UsersTable.email],
            role = UserRole.valueOf(this[UsersTable.role]),
        )
}
