package me.innerworks.iw_mobile_auth_android.utils.threading.safe

import java.util.concurrent.TimeoutException

internal class ExecutionTimeoutException(
    timeoutException: TimeoutException,
    val executionThreadStackTrace: List<StackTraceElement>?,
) : Exception(timeoutException) {
    override val message: String
        get() = "The execution took too long to complete." +
                " Original exception: $cause," +
                " execution thread stacktrace: ${executionThreadStackTrace?.joinToString { it.toString() }}."
}
