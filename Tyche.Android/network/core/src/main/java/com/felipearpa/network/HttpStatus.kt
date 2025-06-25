package com.felipearpa.network

data class HttpStatus(val code: Int) {
    companion object {
        // Informational
        val CONTINUE = HttpStatus(100)
        val SWITCHING_PROTOCOLS = HttpStatus(101)
        val PROCESSING = HttpStatus(102)
        val EARLY_HINTS = HttpStatus(103)

        // Success
        val OK = HttpStatus(200)
        val CREATED = HttpStatus(201)
        val ACCEPTED = HttpStatus(202)
        val NON_AUTHORITATIVE_INFORMATION = HttpStatus(203)
        val NO_CONTENT = HttpStatus(204)
        val RESET_CONTENT = HttpStatus(205)
        val PARTIAL_CONTENT = HttpStatus(206)
        val MULTI_STATUS = HttpStatus(207)
        val ALREADY_REPORTED = HttpStatus(208)
        val IM_USED = HttpStatus(226)

        // Redirection
        val MULTIPLE_CHOICES = HttpStatus(300)
        val MOVED_PERMANENTLY = HttpStatus(301)
        val FOUND = HttpStatus(302)
        val SEE_OTHER = HttpStatus(303)
        val NOT_MODIFIED = HttpStatus(304)
        val USE_PROXY = HttpStatus(305)
        val UNUSED = HttpStatus(306)
        val TEMPORARY_REDIRECT = HttpStatus(307)
        val PERMANENT_REDIRECT = HttpStatus(308)

        // Client errors
        val BAD_REQUEST = HttpStatus(400)
        val UNAUTHORIZED = HttpStatus(401)
        val PAYMENT_REQUIRED = HttpStatus(402)
        val FORBIDDEN = HttpStatus(403)
        val NOT_FOUND = HttpStatus(404)
        val METHOD_NOT_ALLOWED = HttpStatus(405)
        val NOT_ACCEPTABLE = HttpStatus(406)
        val PROXY_AUTHENTICATION_REQUIRED = HttpStatus(407)
        val REQUEST_TIMEOUT = HttpStatus(408)
        val CONFLICT = HttpStatus(409)
        val GONE = HttpStatus(410)
        val LENGTH_REQUIRED = HttpStatus(411)
        val PRECONDITION_FAILED = HttpStatus(412)
        val PAYLOAD_TOO_LARGE = HttpStatus(413)
        val URI_TOO_LONG = HttpStatus(414)
        val UNSUPPORTED_MEDIA_TYPE = HttpStatus(415)
        val RANGE_NOT_SATISFIABLE = HttpStatus(416)
        val EXPECTATION_FAILED = HttpStatus(417)
        val I_AM_A_TEAPOT = HttpStatus(418)
        val MISDIRECTED_REQUEST = HttpStatus(421)
        val UNPROCESSABLE_ENTITY = HttpStatus(422)
        val LOCKED = HttpStatus(423)
        val FAILED_DEPENDENCY = HttpStatus(424)
        val TOO_EARLY = HttpStatus(425)
        val UPGRADE_REQUIRED = HttpStatus(426)
        val PRECONDITION_REQUIRED = HttpStatus(428)
        val TOO_MANY_REQUESTS = HttpStatus(429)
        val REQUEST_HEADER_FIELDS_TOO_LARGE = HttpStatus(431)
        val UNAVAILABLE_FOR_LEGAL_REASONS = HttpStatus(451)

        // Server errors
        val INTERNAL_SERVER_ERROR = HttpStatus(500)
        val NOT_IMPLEMENTED = HttpStatus(501)
        val BAD_GATEWAY = HttpStatus(502)
        val SERVICE_UNAVAILABLE = HttpStatus(503)
        val GATEWAY_TIMEOUT = HttpStatus(504)
        val HTTP_VERSION_NOT_SUPPORTED = HttpStatus(505)
        val VARIANT_ALSO_NEGOTIATES = HttpStatus(506)
        val INSUFFICIENT_STORAGE = HttpStatus(507)
        val LOOP_DETECTED = HttpStatus(508)
        val NOT_EXTENDED = HttpStatus(510)
        val NETWORK_AUTHENTICATION_REQUIRED = HttpStatus(511)
    }
}

fun HttpStatus.isInformational(): Boolean = code in 100..199
fun HttpStatus.isSuccess(): Boolean = code in 200..299
fun HttpStatus.isRedirection(): Boolean = code in 300..399
fun HttpStatus.isClientError(): Boolean = code in 400..499
fun HttpStatus.isServerError(): Boolean = code in 500..599
fun HttpStatus.isError(): Boolean = isClientError() || isServerError()
