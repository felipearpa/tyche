package com.felipearpa.network

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class HttpStatusTest {
    @TestFactory
    fun `given a status code in the informational range when checking if it's informational then true is returned`() =
        informationalStatuses.map { httpStatus ->
            dynamicTest("given $httpStatus when isInformational then true is returned") {
                httpStatus.isInformational().shouldBeTrue()
            }
        }

    @TestFactory
    fun `given a status code not in the informational range when checking if it's informational then false is returned`() =
        (successStatuses + redirectionStatuses + clientErrorStatuses + serverErrorsStatuses).map { httpStatus ->
            dynamicTest("given $httpStatus when isInformational then false is returned") {
                httpStatus.isInformational().shouldBeFalse()
            }
        }

    @TestFactory
    fun `given a status code in the success range when checking if it's successful then true is returned`() =
        successStatuses.map { httpStatus ->
            dynamicTest("given $httpStatus when isSuccess then true is returned") {
                httpStatus.isSuccess().shouldBeTrue()
            }
        }

    @TestFactory
    fun `given a status code not in the success range when checking if it's successful then false is returned`() =
        (informationalStatuses + redirectionStatuses + clientErrorStatuses + serverErrorsStatuses).map { httpStatus ->
            dynamicTest("given $httpStatus when isSuccess then false is returned") {
                httpStatus.isSuccess().shouldBeFalse()
            }
        }

    @TestFactory
    fun `given a status code in the redirection range when checking if it's a redirect then true is returned`() =
        redirectionStatuses.map { httpStatus ->
            dynamicTest("given $httpStatus when isRedirection then true is returned") {
                httpStatus.isRedirection().shouldBeTrue()
            }
        }

    @TestFactory
    fun `given a status code not in the redirection range when checking if it's a redirect then false is returned`() =
        (informationalStatuses + successStatuses + clientErrorStatuses + serverErrorsStatuses).map { httpStatus ->
            dynamicTest("given $httpStatus when isRedirection then false is returned") {
                httpStatus.isRedirection().shouldBeFalse()
            }
        }

    @TestFactory
    fun `given a status code in the client error range when checking if it's a client error then true is returned`() =
        clientErrorStatuses.map { httpStatus ->
            dynamicTest("given $httpStatus when isClientError then true is returned") {
                httpStatus.isClientError().shouldBeTrue()
            }
        }

    @TestFactory
    fun `given a status code not in the client error range when checking if it's a client error then false is returned`() =
        (informationalStatuses + redirectionStatuses + serverErrorsStatuses).map { httpStatus ->
            dynamicTest("given $httpStatus when isClientError then false is returned") {
                httpStatus.isClientError().shouldBeFalse()
            }
        }

    @TestFactory
    fun `given a status code in the server error range when checking if it's a server error then true is returned`() =
        serverErrorsStatuses.map { httpStatus ->
            dynamicTest("given $httpStatus when isServerError then true is returned") {
                httpStatus.isServerError().shouldBeTrue()
            }
        }

    @TestFactory
    fun `given a status code not in the server error range when checking if it's a server error then false is returned`() =
        (informationalStatuses + redirectionStatuses + clientErrorStatuses + successStatuses).map { httpStatus ->
            dynamicTest("given $httpStatus when isServerError then false is returned") {
                httpStatus.isServerError().shouldBeFalse()
            }
        }

    @TestFactory
    fun `given a status code in the error range when checking if it's an error then true is returned`() =
        (clientErrorStatuses + serverErrorsStatuses).map { httpStatus ->
            dynamicTest("given $httpStatus when isError then true is returned") {
                httpStatus.isError().shouldBeTrue()
            }
        }

    @TestFactory
    fun `given a status code not in the error range when checking if it's an error then false is returned`() =
        (informationalStatuses + redirectionStatuses + successStatuses).map { httpStatus ->
            dynamicTest("given $httpStatus when isError then false is returned") {
                httpStatus.isError().shouldBeFalse()
            }
        }
}

private val informationalStatuses = listOf(
    HttpStatus.CONTINUE,
    HttpStatus.SWITCHING_PROTOCOLS,
    HttpStatus.PROCESSING,
    HttpStatus.EARLY_HINTS,
)

private val successStatuses = listOf(
    HttpStatus.OK,
    HttpStatus.CREATED,
    HttpStatus.ACCEPTED,
    HttpStatus.NON_AUTHORITATIVE_INFORMATION,
    HttpStatus.NO_CONTENT,
    HttpStatus.RESET_CONTENT,
    HttpStatus.PARTIAL_CONTENT,
    HttpStatus.MULTI_STATUS,
    HttpStatus.ALREADY_REPORTED,
    HttpStatus.IM_USED,
)

private val redirectionStatuses = listOf(
    HttpStatus.MULTIPLE_CHOICES,
    HttpStatus.MOVED_PERMANENTLY,
    HttpStatus.FOUND,
    HttpStatus.SEE_OTHER,
    HttpStatus.NOT_MODIFIED,
    HttpStatus.USE_PROXY,
    HttpStatus.UNUSED,
    HttpStatus.TEMPORARY_REDIRECT,
    HttpStatus.PERMANENT_REDIRECT,
)

private val clientErrorStatuses =
    listOf(
        HttpStatus.BAD_REQUEST,
        HttpStatus.UNAUTHORIZED,
        HttpStatus.PAYMENT_REQUIRED,
        HttpStatus.FORBIDDEN,
        HttpStatus.NOT_FOUND,
        HttpStatus.METHOD_NOT_ALLOWED,
        HttpStatus.NOT_ACCEPTABLE,
        HttpStatus.PROXY_AUTHENTICATION_REQUIRED,
        HttpStatus.REQUEST_TIMEOUT,
        HttpStatus.CONFLICT,
        HttpStatus.GONE,
        HttpStatus.LENGTH_REQUIRED,
        HttpStatus.PRECONDITION_FAILED,
        HttpStatus.PAYLOAD_TOO_LARGE,
        HttpStatus.URI_TOO_LONG,
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        HttpStatus.RANGE_NOT_SATISFIABLE,
        HttpStatus.EXPECTATION_FAILED,
        HttpStatus.I_AM_A_TEAPOT,
        HttpStatus.MISDIRECTED_REQUEST,
        HttpStatus.UNPROCESSABLE_ENTITY,
        HttpStatus.LOCKED,
        HttpStatus.FAILED_DEPENDENCY,
        HttpStatus.TOO_EARLY,
        HttpStatus.UPGRADE_REQUIRED,
        HttpStatus.PRECONDITION_REQUIRED,
        HttpStatus.TOO_MANY_REQUESTS,
        HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE,
        HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS,
    )

private val serverErrorsStatuses = listOf(
    HttpStatus.INTERNAL_SERVER_ERROR,
    HttpStatus.NOT_IMPLEMENTED,
    HttpStatus.BAD_GATEWAY,
    HttpStatus.SERVICE_UNAVAILABLE,
    HttpStatus.GATEWAY_TIMEOUT,
    HttpStatus.HTTP_VERSION_NOT_SUPPORTED,
    HttpStatus.VARIANT_ALSO_NEGOTIATES,
    HttpStatus.INSUFFICIENT_STORAGE,
    HttpStatus.LOOP_DETECTED,
    HttpStatus.NOT_EXTENDED,
    HttpStatus.NETWORK_AUTHENTICATION_REQUIRED,
)
