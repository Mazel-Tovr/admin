package com.epam.drill.admin.jwt.user

import io.ktor.auth.*

data class User(
    val id: Int,
    val name: String,
    val password: String ,
    val role: String
) : Principal
