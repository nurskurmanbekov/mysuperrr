package com.example.probationbackend.dto

import java.time.LocalDate

data class RegistryCreateRequest(
    val inn: String,
    val noInn: Boolean? = null, // если создают без ИНН
    val lastName: String,
    val firstName: String,
    val middleName: String? = null,
    val birthDate: LocalDate,
    val sex: String? = null, // 'М' или 'Ж'
    val passport: String? = null,
    val regAddress: String? = null,
    val factAddress: String? = null,
    val contact1: String? = null,
    val contact2: String? = null,
    val erpNumber: String? = null,
    val obsStart: LocalDate? = null,
    val obsEnd: LocalDate? = null,
    val obsType: String,
    val degree: String? = null,
    val udNumber: String? = null,
    val code: String? = null,
    val article: String? = null,
    val part: String? = null,
    val point: String? = null,
    val extraInfo: String? = null,
    val measures: String? = null,
    val appPassword: String, // Пароль для приложения
    val photoKey: String? = null,
    val unit: String?
) {

    // Вычисляемое поле для удобства
    val fio: String
        get() = listOfNotNull(firstName, middleName, lastName).joinToString(" ")
}